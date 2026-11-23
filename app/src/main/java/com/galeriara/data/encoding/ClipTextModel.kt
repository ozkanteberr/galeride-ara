package com.galeriara.data.encoding

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import java.security.MessageDigest

/** Blocking raw text inference. URI/gallery/search wiring is outside this numerical prototype. */
class ClipTextModel(
    modelFile: File,
    expectedModelSha256: String,
    denseFile: File,
    expectedDenseSha256: String,
    private val outputName: String,
) : Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private val projection: FloatArray
    private val session: OrtSession
    private var closed = false

    init {
        verifyFile(modelFile, expectedModelSha256)
        verifyFile(denseFile, expectedDenseSha256)
        require(denseFile.length() == 512L * 768 * 4) { "Unexpected Dense byte count" }
        val floats = ByteBuffer.wrap(denseFile.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        projection = FloatArray(512 * 768).also { floats.get(it) }
        require(projection.all { it.isFinite() }) { "Non-finite Dense weights" }
        session = OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            environment.createSession(modelFile.absolutePath, options)
        }
        try {
            require(session.inputNames == setOf("input_ids", "attention_mask")) { "Unexpected text inputs" }
            require(session.outputNames == setOf(outputName)) { "Unexpected text outputs" }
            session.inputInfo.values.forEach { node ->
                val info = node.info as? TensorInfo ?: error("Text input must be a tensor")
                require(info.type == OnnxJavaType.INT64 && info.shape.size == 2) { "Unexpected text input type/shape" }
            }
            val output = session.outputInfo.getValue(outputName).info as? TensorInfo
                ?: error("Text output must be a tensor")
            require(output.type == OnnxJavaType.FLOAT && output.shape.size == 3 && output.shape[2] == 768L) {
                "Expected Float32 token states [batch,sequence,768]"
            }
        } catch (failure: Throwable) {
            session.close()
            throw failure
        }
    }

    /** Returns a raw 512-vector; shared core Embedding applies L2 at the application boundary. */
    @Synchronized
    fun encodeTokens(input: ClipTextInput): FloatArray {
        check(!closed) { "Text model is closed" }
        val ids = input.inputIds
        val mask = input.attentionMask
        require(ids.size in 2..128 && mask.size == ids.size && ids.all { it in 0L..119546L })
        require(mask.all { it == 0L || it == 1L })
        val active = mask.takeWhile { it == 1L }.size
        require(active >= 2 && mask.drop(active).all { it == 0L }) { "Expected nonempty prefix and right padding" }
        require(ids.first() == 101L && ids[active - 1] == 102L && ids.drop(active).all { it == 0L })
        val shape = longArrayOf(1, ids.size.toLong())
        OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape).use { idsTensor ->
            OnnxTensor.createTensor(environment, LongBuffer.wrap(mask), shape).use { maskTensor ->
                session.run(mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor)).use { result ->
                    val value = result.get(outputName).orElseThrow {
                        IllegalStateException("Text output is missing")
                    } as? OnnxTensor ?: error("Text output must be a tensor")
                    require(value.info.shape.contentEquals(longArrayOf(1, ids.size.toLong(), 768)))
                    val buffer = value.floatBuffer ?: error("Float32 states unavailable")
                    val hidden = FloatArray(ids.size * 768).also { buffer.get(it) }
                    require(hidden.all { it.isFinite() }) { "Non-finite token states" }
                    // Only padding is excluded; CLS, SEP and literal special tokens remain included.
                    val pooled = DoubleArray(768)
                    for (t in ids.indices) if (mask[t] == 1L) {
                        for (j in 0 until 768) pooled[j] += hidden[t * 768 + j].toDouble()
                    }
                    for (j in pooled.indices) pooled[j] /= active.toDouble()
                    val vector = FloatArray(512) { d ->
                        var sum = 0.0
                        for (j in 0 until 768) sum += pooled[j] * projection[d * 768 + j]
                        sum.toFloat()
                    }
                    require(vector.all { it.isFinite() } && vector.any { it != 0f }) { "Invalid text vector" }
                    return vector
                }
            }
        }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            closed = true
            session.close()
        }
    }

    private fun verifyFile(file: File, expected: String) {
        require(expected.matches(Regex("[0-9a-f]{64}")) && file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(bytes)
                if (count < 0) break
                digest.update(bytes, 0, count)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == expected) {
            "Text model component checksum mismatch: ${file.name}"
        }
    }
}
