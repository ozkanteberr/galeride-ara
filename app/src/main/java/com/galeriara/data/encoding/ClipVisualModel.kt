package com.galeriara.data.encoding

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.Closeable
import java.io.File
import java.nio.FloatBuffer
import java.security.MessageDigest

/** Blocking, raw 512-dimensional visual inference. Not connected to gallery indexing yet. */
class ClipVisualModel(modelFile: File, expectedSha256: String) : Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private var closed = false
    private val session: OrtSession

    init {
        require(expectedSha256.matches(Regex("[0-9a-f]{64}"))) { "A verified model SHA-256 is required" }
        require(modelFile.isFile) { "Visual model is missing" }
        require(sha256(modelFile) == expectedSha256) { "Visual model checksum mismatch" }
        session = OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            environment.createSession(modelFile.absolutePath, options)
        }
        try {
            require(session.inputNames == setOf("pixel_values")) { "Unexpected visual input names" }
            require(session.outputNames == setOf("image_features")) { "Unexpected visual output names" }
            val input = session.inputInfo.getValue("pixel_values").info as? TensorInfo
                ?: error("Visual input must be a tensor")
            val output = session.outputInfo.getValue("image_features").info as? TensorInfo
                ?: error("Visual output must be a tensor")
            require(input.type == OnnxJavaType.FLOAT && input.shape.size == 4 &&
                input.shape.drop(1) == listOf(3L, 224L, 224L)) { "Unexpected visual input type/shape" }
            require(output.type == OnnxJavaType.FLOAT && output.shape.size == 2 &&
                output.shape[1] == 512L) { "Unexpected visual output type/shape" }
        } catch (failure: Throwable) {
            session.close()
            throw failure
        }
    }

    /** Input is normalized RGB/CHW, [1,3,224,224]. Caller chooses its background dispatcher. */
    @Synchronized
    fun encodeTensor(pixels: FloatArray): FloatArray {
        check(!closed) { "Visual model is closed" }
        require(pixels.size == 3 * 224 * 224 && pixels.all { it.isFinite() }) {
            "Expected finite Float32 [1,3,224,224] input"
        }
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixels), longArrayOf(1, 3, 224, 224)).use { input ->
            session.run(mapOf("pixel_values" to input)).use { result ->
                val value = result.get("image_features").orElseThrow {
                    IllegalStateException("Visual output is missing")
                } as? OnnxTensor ?: error("Visual output must be a tensor")
                require(value.info.shape.contentEquals(longArrayOf(1, 512))) { "Invalid visual output shape" }
                @Suppress("UNCHECKED_CAST")
                val vector = (value.value as Array<FloatArray>).single().copyOf()
                require(vector.all { it.isFinite() } && vector.any { it != 0f }) { "Invalid visual embedding" }
                return vector
            }
        }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            closed = true
            session.close()
        }
        // OrtEnvironment is shared; this model owns only its session and per-run values.
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
