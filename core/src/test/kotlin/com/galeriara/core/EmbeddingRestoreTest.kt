package com.galeriara.core

import org.junit.Assert.*
import org.junit.Test

class EmbeddingRestoreTest {
    private val model = ModelIdentity("test", "normalized-f32-v1", 3)

    @Test fun `restore preserves near unit values and signed zero bit for bit`() {
        val original = floatArrayOf(.6000012f, .8000016f, -0.0f)
        val restored = Embedding.fromNormalized(model, original).values()
        assertArrayEquals(original.map { it.toRawBits() }.toIntArray(), restored.map { it.toRawBits() }.toIntArray())
        assertFalse(original.map { it.toRawBits() } == Embedding(model, original).values().map { it.toRawBits() })
    }

    @Test fun `restore and values access keep defensive copies`() {
        val original = floatArrayOf(.6f, .8f, 0f)
        val restored = Embedding.fromNormalized(model, original)
        original[0] = 0f
        val returned = restored.values(); returned[1] = 0f
        assertArrayEquals(floatArrayOf(.6f, .8f, 0f), restored.values(), 0f)
    }

    @Test fun `restore rejects malformed or non normalized vectors`() {
        for (values in listOf(floatArrayOf(1f), floatArrayOf(0f, 0f, 0f),
            floatArrayOf(Float.NaN, 0f, 1f), floatArrayOf(Float.POSITIVE_INFINITY, 0f, 0f),
            floatArrayOf(3f, 4f, 0f))) {
            assertThrows(IllegalArgumentException::class.java) { Embedding.fromNormalized(model, values) }
        }
    }

    @Test fun `raw constructor still normalizes model output once`() {
        assertArrayEquals(floatArrayOf(.6f, .8f, 0f), Embedding(model, floatArrayOf(3f, 4f, 0f)).values(), 0f)
    }
}
