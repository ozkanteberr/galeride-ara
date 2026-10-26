package com.galeriara.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import kotlin.math.sqrt

data class ModelIdentity(val id: String, val revision: String, val dimensions: Int) {
    init { require(id.isNotBlank() && revision.isNotBlank() && dimensions > 0) }
}

/** Immutable, validated and normalized vector; both encoders must share the same identity. */
class Embedding(val model: ModelIdentity, values: FloatArray) {
    private val normalized: FloatArray
    init {
        require(values.size == model.dimensions && values.all { it.isFinite() })
        val norm = sqrt(values.sumOf { it.toDouble() * it.toDouble() })
        require(norm > 0.0) { "Zero vectors cannot represent an image or query" }
        normalized = FloatArray(values.size) { (values[it] / norm).toFloat() }
    }
    fun values(): FloatArray = normalized.copyOf()
    fun similarity(other: Embedding): Float {
        require(model == other.model) { "Incompatible model or preprocessing revisions" }
        return normalized.indices.sumOf {
            normalized[it].toDouble() * other.normalized[it].toDouble()
        }.toFloat().coerceIn(-1f, 1f)
    }
}

data class IndexedPhoto(val uri: String, val embedding: Embedding)
data class SearchHit(val uri: String, val similarity: Float)

interface QueryEncoder {
    val model: ModelIdentity
    suspend fun encodeTurkish(query: String): Embedding
}

interface ImageEncoder {
    val model: ModelIdentity
    suspend fun encodeImage(uri: String): Embedding
}

interface VectorRepository {
    suspend fun load(model: ModelIdentity): List<IndexedPhoto>
}

/** Only text encoding and persisted vectors are available here: no image inference dependency. */
class SearchPhotos(private val encoder: QueryEncoder, private val vectors: VectorRepository) {
    suspend operator fun invoke(query: String, limit: Int = 30): List<SearchHit> {
        require(query.isNotBlank())
        require(limit > 0)
        val embedding = encoder.encodeTurkish(query.trim())
        require(embedding.model == encoder.model)
        val photos = vectors.load(encoder.model)
        return withContext(Dispatchers.Default) { rank(embedding, photos, limit) }
    }
}

/** Exact top-k in O(n log k), with deterministic tie breaking. */
fun rank(query: Embedding, photos: List<IndexedPhoto>, limit: Int): List<SearchHit> {
    require(limit > 0)
    val bestFirst = compareByDescending<SearchHit> { it.similarity }.thenBy { it.uri }
    val heap = PriorityQueue(bestFirst.reversed())
    photos.forEach { photo ->
        if (photo.embedding.model == query.model) {
            heap.add(SearchHit(photo.uri, query.similarity(photo.embedding)))
            if (heap.size > limit) heap.poll()
        }
    }
    return heap.sortedWith(bestFirst)
}
