package com.galeriara.data.vectors

import com.galeriara.core.Embedding
import com.galeriara.core.ModelIdentity
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Storage format v1: headerless, little-endian IEEE-754 Float32, already normalized. */
object Float32VectorCodec {
    fun encode(embedding: Embedding): ByteArray {
        val values = embedding.values()
        val bytes = ByteArray(Math.multiplyExact(values.size, Float.SIZE_BYTES))
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(values)
        return bytes
    }

    fun decode(model: ModelIdentity, bytes: ByteArray): Embedding {
        require(bytes.size.toLong() == model.dimensions.toLong() * Float.SIZE_BYTES) {
            "Stored Float32 byte count does not match dimensions"
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val values = FloatArray(model.dimensions).also { buffer.get(it) }
        return Embedding.fromNormalized(model, values)
    }
}
