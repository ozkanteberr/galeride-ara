package com.galeriara.data.encoding

import android.os.Build
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galeriara.core.Embedding
import com.galeriara.core.QueryEncoder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipQueryEncoderInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val assets get() = instrumentation.context.assets
    private fun metadata() = JSONObject(assets.open("a06/fixture.json").bufferedReader().use { it.readText() })
    private fun vocabulary(): File {
        val file = File(instrumentation.targetContext.cacheDir, "a07-vocab.txt")
        assets.open("a06/vocab.txt").use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
    private fun files(): ClipTextFiles {
        val args = InstrumentationRegistry.getArguments()
        val directory = File(instrumentation.targetContext.filesDir, "a06")
        return ClipTextFiles(
            args.getString("modelPath")?.let { File(it) } ?: File(directory, "model_quint8_avx2.onnx"),
            args.getString("densePath")?.let { File(it) } ?: File(directory, "clip_multilingual_dense.f32"),
            vocabulary(),
        )
    }
    private fun tokenInput() = ClipTextInput(longArrayOf(101, 100, 102), longArrayOf(1, 1, 1))
    private fun vector() = FloatArray(512).apply { this[0] = 3f; this[1] = 4f }
    private inner class FakeSession(private val action: (ClipTextInput) -> FloatArray = { vector() }) : ClipQuerySession {
        val calls = AtomicInteger()
        val closes = AtomicInteger()
        override fun encode(input: ClipTextInput): FloatArray { calls.incrementAndGet(); return action(input) }
        override fun close() { closes.incrementAndGet() }
    }
    private fun fake(session: ClipQuerySession) = ClipQueryEncoder({ tokenInput() }, session)
    private fun waitFor(latch: CountDownLatch) = assertTrue("Timed out waiting for test signal", latch.await(10, TimeUnit.SECONDS))
    private suspend inline fun <reified T : Throwable> expect(noinline block: suspend () -> Unit): T {
        try { block() } catch (failure: Throwable) {
            assertTrue("Expected ${T::class.java.simpleName}, got $failure", failure is T)
            return failure as T
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }

    @Test fun pinnedManifestAndComponentIdentityAgree() {
        val manifest = JSONObject(assets.open("a07/runtime_manifest.json").bufferedReader().use { it.readText() })
        val components = manifest.getJSONObject("components")
        val model = MultilingualClipProfile.identity
        assertEquals(manifest.getString("id"), model.id)
        assertEquals(manifest.getString("revision"), model.revision)
        assertEquals(manifest.getInt("dimensions"), model.dimensions)
        assertEquals(components.getJSONObject("text_onnx").getString("sha256"), MultilingualClipProfile.TEXT_SHA256)
        assertEquals(components.getJSONObject("dense").getString("runtime_sha256"), MultilingualClipProfile.DENSE_SHA256)
        assertEquals(components.getJSONObject("tokenizer").getString("vocab_sha256"), MultilingualClipProfile.VOCAB_SHA256)
        assertEquals(components.getJSONObject("text_onnx").getString("output_name"), MultilingualClipProfile.OUTPUT_NAME)
        assertEquals(metadata().getString("model_sha256"), MultilingualClipProfile.TEXT_SHA256)
    }

    @Test fun realAdapterNormalizesTurkishQueriesAgainstIndependentReferences() = runBlocking<Unit> {
        val directory = File(instrumentation.targetContext.filesDir, "a07").apply { mkdirs() }
        val reportFile = File(directory, "android_report.json").apply { delete() }
        fun readVector(name: String): FloatArray {
            val bytes = assets.open("a06/$name").use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            return FloatArray(buffer.remaining()).also { buffer.get(it) }
        }
        val results = JSONArray()
        var minCosine = 1.0; var maxRelativeL2 = 0.0; var maxNormError = 0.0
        ClipQueryEncoder.open(files()).use { owned ->
            val encoder: QueryEncoder = owned
            val cases = metadata().getJSONArray("cases")
            for (i in 0 until cases.length()) {
                val case = cases.getJSONObject(i)
                val profile = case.getJSONObject("profiles").getJSONObject("dynamic")
                if (profile.getJSONArray("input_ids").length() == 2) continue
                val actual = withContext(Dispatchers.Main) { encoder.encodeTurkish(case.getString("text")) }
                assertEquals(MultilingualClipProfile.identity, actual.model)
                val values = actual.values()
                val desktop = Embedding(encoder.model, readVector(profile.getString("desktop_file"))).values()
                assertArrayEquals(case.getString("id"), desktop, values, 1e-6f)
                val reference = Embedding(encoder.model, readVector(profile.getString("reference_file"))).values()
                val cosine = values.indices.sumOf { values[it].toDouble() * reference[it] }
                val refNorm = sqrt(reference.sumOf { it.toDouble() * it })
                val relativeL2 = sqrt(values.indices.sumOf { (values[it].toDouble() - reference[it]).let { d -> d * d } }) / refNorm
                val normError = abs(sqrt(values.sumOf { it.toDouble() * it }) - 1)
                assertTrue(cosine.toString(), cosine >= .99)
                assertTrue(relativeL2.toString(), relativeL2 <= .15)
                assertTrue(normError.toString(), normError <= 1e-5)
                minCosine = minOf(minCosine, cosine); maxRelativeL2 = maxOf(maxRelativeL2, relativeL2)
                maxNormError = maxOf(maxNormError, normError)
                results.put(JSONObject().put("id", case.getString("id")).put("cosine_vs_fp32", cosine)
                    .put("normalized_relative_l2", relativeL2).put("norm_error", normError))
            }
            assertEquals(22, results.length())
            // Extra whitespace is handled by the pinned tokenizer, preserving case and accents.
            val trimmed = encoder.encodeTurkish("kırmızı bir araba").values()
            assertArrayEquals(trimmed, encoder.encodeTurkish(" \t kırmızı bir araba \n").values(), 0f)
        }
        reportFile.writeText(JSONObject().put("status", "passed").put("api", Build.VERSION.SDK_INT)
            .put("abi", Build.SUPPORTED_ABIS.first()).put("model_revision", MultilingualClipProfile.identity.revision)
            .put("queries", results.length()).put("results", results).put("min_cosine_vs_fp32", minCosine)
            .put("max_normalized_relative_l2", maxRelativeL2).put("max_norm_error", maxNormError)
            .put("scope", "real QueryEncoder API; fixed queries, no gallery or physical ARM validation").toString(2))
    }

    @Test fun blankAndCleanedEmptyQueriesDoNotReachInference() = runBlocking<Unit> {
        val tokenizer = CasedWordPieceTokenizer(vocabulary(), MultilingualClipProfile.VOCAB_SHA256)
        val session = FakeSession()
        ClipQueryEncoder({ tokenizer.encode(it) }, session).use { encoder ->
            for (query in listOf("", " \t\n", "\u00A0", "\u001C", "\u0000", "\uE000", "\u200D")) {
                expect<IllegalArgumentException> { encoder.encodeTurkish(query) }
            }
            assertEquals(0, session.calls.get())
        }
        assertEquals(1, session.closes.get())
    }

    @Test fun nativeFailurePropagatesWithoutPoisoningTheSession() = runBlocking<Unit> {
        val failure = IllegalStateException("controlled inference failure")
        var attempts = 0
        val session = FakeSession { if (attempts++ == 0) throw failure else vector() }
        fake(session).use { encoder ->
            assertSame(failure, expect<IllegalStateException> { encoder.encodeTurkish("araba") })
            assertArrayEquals(floatArrayOf(.6f, .8f), encoder.encodeTurkish("kedi").values().take(2).toFloatArray(), 1e-7f)
            assertEquals(2, session.calls.get())
        }
    }

    @Test fun requestsRunOffMainAndShareOneSerializedSession() = runBlocking<Unit> {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val secondEntered = CountDownLatch(1); val wasMain = AtomicBoolean(false)
        val active = AtomicInteger(); val maximum = AtomicInteger(); val calls = AtomicInteger()
        val session = FakeSession {
            wasMain.compareAndSet(false, Looper.myLooper() == Looper.getMainLooper())
            maximum.accumulateAndGet(active.incrementAndGet()) { a, b -> maxOf(a, b) }
            try {
                if (calls.incrementAndGet() == 1) { entered.countDown(); waitFor(release) }
                else secondEntered.countDown()
                vector()
            } finally { active.decrementAndGet() }
        }
        val encoder = fake(session)
        try {
            val first = async(Dispatchers.Main) { encoder.encodeTurkish("araba") }
            waitFor(entered)
            val second = async(start = CoroutineStart.UNDISPATCHED) { encoder.encodeTurkish("kedi") }
            assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS))
            release.countDown()
            first.await(); second.await()
            assertFalse(wasMain.get()); assertEquals(1, maximum.get()); assertEquals(2, session.calls.get())
        } finally { release.countDown(); encoder.close() }
    }

    @Test fun cancelledQueuedQueryDoesNotRunAndDoesNotCloseTheSession() = runBlocking<Unit> {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val session = FakeSession { entered.countDown(); waitFor(release); vector() }
        val encoder = fake(session)
        try {
            val first = async(Dispatchers.Default) { encoder.encodeTurkish("araba") }
            waitFor(entered)
            val queued = async(start = CoroutineStart.UNDISPATCHED) { encoder.encodeTurkish("kedi") }
            withTimeout(5_000) { queued.cancelAndJoin() }
            assertEquals(1, session.calls.get()); assertEquals(0, session.closes.get())
            release.countDown(); first.await()
        } finally { release.countDown(); encoder.close() }
    }

    @Test fun cancellationDuringNativeWorkDiscardsResultAndAllowsReuse() = runBlocking<Unit> {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val session = FakeSession { entered.countDown(); waitFor(release); vector() }
        val encoder = fake(session)
        try {
            val cancelled = async(Dispatchers.Default) { encoder.encodeTurkish("araba") }
            waitFor(entered); cancelled.cancel(); release.countDown()
            expect<CancellationException> { cancelled.await() }
            assertEquals(0, session.closes.get())
            assertEquals(MultilingualClipProfile.identity, encoder.encodeTurkish("kedi").model)
            assertEquals(2, session.calls.get())
        } finally { release.countDown(); encoder.close() }
    }

    @Test fun closeWaitsForCurrentWorkAndRejectsNewQueries() = runBlocking<Unit> {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val closeStarted = CountDownLatch(1); val closeFinished = CountDownLatch(1)
        val session = FakeSession { entered.countDown(); waitFor(release); vector() }
        val encoder = fake(session)
        try {
            val query = async(Dispatchers.Default) { encoder.encodeTurkish("araba") }
            waitFor(entered)
            val closing = launch(Dispatchers.IO) { closeStarted.countDown(); encoder.close(); closeFinished.countDown() }
            waitFor(closeStarted)
            assertFalse(closeFinished.await(100, TimeUnit.MILLISECONDS))
            assertEquals(0, session.closes.get())
            release.countDown(); query.await(); closing.join()
            encoder.close()
            assertEquals(1, session.closes.get())
            expect<IllegalStateException> { encoder.encodeTurkish("kedi") }
            assertEquals(1, session.calls.get())
        } finally { release.countDown(); encoder.close() }
    }

    @Test fun cancelledInitializationClosesTheAllocatedSession() = runBlocking<Unit> {
        val created = CountDownLatch(1); val release = CountDownLatch(1)
        val session = FakeSession()
        val opening = async(Dispatchers.Default) {
            ClipQueryEncoder.create({ { tokenInput() } }, { created.countDown(); waitFor(release); session })
        }
        try {
            waitFor(created); opening.cancel(); release.countDown()
            expect<CancellationException> { opening.await() }
            opening.join()
            assertEquals(1, session.closes.get()); assertEquals(0, session.calls.get())
        } finally { release.countDown() }
    }

    @Test fun missingOrMismatchedModelFilesFailBeforeEncoding() = runBlocking<Unit> {
        val correct = files()
        val invalid = File(instrumentation.targetContext.cacheDir, "a07-invalid-component").apply { writeText("invalid") }
        for (bad in listOf(correct.copy(model = File(invalid, "missing.onnx")),
            correct.copy(dense = invalid), correct.copy(vocabulary = invalid))) {
            expect<IllegalArgumentException> { ClipQueryEncoder.open(bad).close() }
        }
    }

    @Test fun adaptersOwnIndependentSessionsAndCloseIdempotently() = runBlocking<Unit> {
        val first = FakeSession(); val second = FakeSession()
        val a = fake(first); val b = fake(second)
        try {
            a.close(); a.close()
            assertEquals(1, first.closes.get()); assertEquals(0, second.closes.get())
            assertEquals(MultilingualClipProfile.identity, b.encodeTurkish("araba").model)
            assertEquals(1, second.calls.get())
            expect<IllegalStateException> { a.encodeTurkish("kedi") }
            assertEquals(0, first.calls.get())
        } finally { a.close(); b.close() }
        assertEquals(1, second.closes.get())
    }
}
