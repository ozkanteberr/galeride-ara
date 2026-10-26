package com.galeriara.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SearchTest {
    private val model = ModelIdentity("test-clip", "preprocess-v1", 2)
    private fun vector(x: Float, y: Float) = Embedding(model, floatArrayOf(x, y))

    @Test fun `normalization makes similarity independent of magnitude`() {
        assertEquals(1f, vector(2f, 0f).similarity(vector(100f, 0f)), 0.00001f)
        assertEquals(0f, vector(2f, 0f).similarity(vector(0f, 4f)), 0.00001f)
    }

    @Test fun `top k excludes incompatible revisions and orders ties deterministically`() {
        val other = ModelIdentity("test-clip", "preprocess-v2", 2)
        val photos = listOf(
            IndexedPhoto("c", vector(0f, 1f)), IndexedPhoto("b", vector(1f, 0f)),
            IndexedPhoto("a", vector(1f, 0f)),
            IndexedPhoto("old", Embedding(other, floatArrayOf(1f, 0f))),
        )
        assertEquals(listOf("a", "b"), rank(vector(1f, 0f), photos, 2).map { it.uri })
    }

    @Test fun `invalid vectors are rejected`() {
        listOf(floatArrayOf(0f, 0f), floatArrayOf(Float.NaN, 1f), floatArrayOf(1f)).forEach {
            assertThrows(IllegalArgumentException::class.java) { Embedding(model, it) }
        }
    }

    @Test fun `search passes Turkish query and only asks for compatible saved vectors`() = runTest {
        var calls = 0
        val encoder = object : QueryEncoder {
            override val model = this@SearchTest.model
            override suspend fun encodeTurkish(query: String): Embedding {
                assertEquals("sahilde gün batımı", query)
                calls++
                return vector(1f, 0f)
            }
        }
        val repository = object : VectorRepository {
            override suspend fun load(model: ModelIdentity): List<IndexedPhoto> {
                assertEquals(this@SearchTest.model, model)
                return listOf(IndexedPhoto("content://photo/1", vector(1f, 0f)))
            }
        }
        assertEquals("content://photo/1", SearchPhotos(encoder, repository)(" sahilde gün batımı ").single().uri)
        assertEquals(1, calls)
    }

    @Test fun `blank queries do not invoke the model`() = runTest {
        val encoder = object : QueryEncoder {
            override val model = this@SearchTest.model
            override suspend fun encodeTurkish(query: String): Embedding = error("Must not run")
        }
        val repository = object : VectorRepository {
            override suspend fun load(model: ModelIdentity): List<IndexedPhoto> = error("Must not run")
        }
        try { SearchPhotos(encoder, repository)(" "); fail("Expected validation") }
        catch (_: IllegalArgumentException) { }
    }
}
