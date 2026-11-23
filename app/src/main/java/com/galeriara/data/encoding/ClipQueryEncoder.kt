package com.galeriara.data.encoding

import com.galeriara.core.Embedding
import com.galeriara.core.QueryEncoder
import java.io.Closeable
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ClipTextFiles(val model: File, val dense: File, val vocabulary: File)

/** Internal seam for testing failure, cancellation and native resource ownership. */
internal interface ClipQuerySession : Closeable {
    fun encode(input: ClipTextInput): FloatArray
}

/** One verified, owned text session per encoder. No translation, network or gallery access. */
class ClipQueryEncoder internal constructor(
    private val tokenize: (String) -> ClipTextInput,
    private val session: ClipQuerySession,
) : QueryEncoder, Closeable {
    override val model = MultilingualClipProfile.identity
    private val requests = Mutex()
    private val lifecycle = ReentrantLock()
    private var closed = false

    override suspend fun encodeTurkish(query: String): Embedding = withContext(Dispatchers.Default) {
        val context = currentCoroutineContext()
        context.ensureActive()
        require(query.isNotBlank()) { "Query must not be blank" }
        // Waiting queries suspend cancellably instead of blocking a native-monitor waiter.
        requests.withLock {
            context.ensureActive()
            lifecycle.withLock {
                check(!closed) { "Query encoder is closed" }
                val input = tokenize(query)
                require(input.attentionMask.count { it == 1L } > 2) { "Query is empty after tokenization" }
                context.ensureActive()
                val raw = session.encode(input)
                // A native call already in progress can finish, but its cancelled result is discarded.
                context.ensureActive()
                Embedding(model, raw)
            }
        }
    }

    /** Blocking: waits for current inference, then closes once. Call off the main thread. */
    override fun close() {
        lifecycle.withLock {
            if (!closed) {
                closed = true
                session.close()
            }
        }
    }

    companion object {
        /** Loads and verifies files on IO; returns only a fully initialized owned encoder. */
        suspend fun open(files: ClipTextFiles): ClipQueryEncoder = create(
            tokenizerFactory = {
                val tokenizer = CasedWordPieceTokenizer(files.vocabulary, MultilingualClipProfile.VOCAB_SHA256)
                val encode: (String) -> ClipTextInput = { query -> tokenizer.encode(query) }
                encode
            },
            sessionFactory = {
                val native = ClipTextModel(files.model, MultilingualClipProfile.TEXT_SHA256,
                    files.dense, MultilingualClipProfile.DENSE_SHA256, MultilingualClipProfile.OUTPUT_NAME)
                object : ClipQuerySession {
                    override fun encode(input: ClipTextInput) = native.encodeTokens(input)
                    override fun close() = native.close()
                }
            },
        )

        internal suspend fun create(
            tokenizerFactory: () -> (String) -> ClipTextInput,
            sessionFactory: () -> ClipQuerySession,
        ): ClipQueryEncoder {
            var allocated: ClipQuerySession? = null
            try {
                return withContext(Dispatchers.IO) {
                    currentCoroutineContext().ensureActive()
                    val tokenize = tokenizerFactory()
                    currentCoroutineContext().ensureActive()
                    val session = sessionFactory().also { allocated = it }
                    currentCoroutineContext().ensureActive()
                    ClipQueryEncoder(tokenize, session)
                }
            } catch (failure: Throwable) {
                // Includes cancellation while initializing and while dispatching the result back.
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        try {
                            allocated?.close()
                        } catch (cleanup: Throwable) {
                            if (cleanup !== failure) failure.addSuppressed(cleanup)
                        }
                    }
                }
                throw failure
            }
        }
    }
}
