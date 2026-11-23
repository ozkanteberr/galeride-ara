package com.galeriara.data.vectors

import androidx.room.withTransaction
import com.galeriara.core.Embedding
import com.galeriara.core.IndexedPhoto
import com.galeriara.core.ModelIdentity
import com.galeriara.core.VectorRepository
import com.galeriara.data.EmbeddingEntity
import com.galeriara.data.GalleryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class VectorIntegrityException(
    val photoUri: String,
    val model: ModelIdentity,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/** Persists existing catalogue photos. Image inference and indexing are separate operations. */
class RoomVectorRepository(private val db: GalleryDatabase) : VectorRepository {
    suspend fun save(photoUri: String, embedding: Embedding): Unit = withContext(Dispatchers.Default) {
        require(photoUri.isNotBlank()) { "Photo URI must not be blank" }
        currentCoroutineContext().ensureActive()
        val model = embedding.model
        val entity = EmbeddingEntity(photoUri, model.id, model.revision, model.dimensions,
            Float32VectorCodec.encode(embedding))
        db.withTransaction {
            val mismatch = db.embeddings().dimensionMismatch(model.id, model.revision, model.dimensions)
            if (mismatch != null) throw VectorIntegrityException(mismatch, model,
                "A model id and revision cannot contain different vector dimensions")
            db.embeddings().upsert(entity)
            currentCoroutineContext().ensureActive()
        }
    }

    override suspend fun load(model: ModelIdentity): List<IndexedPhoto> = withContext(Dispatchers.Default) {
        val rows = db.embeddings().forModel(model.id, model.revision)
        rows.map { row ->
            currentCoroutineContext().ensureActive()
            if (row.photoUri.isBlank() || row.dimensions != model.dimensions) {
                throw VectorIntegrityException(row.photoUri, model, "Stored vector metadata does not match the requested model")
            }
            val embedding = try {
                Float32VectorCodec.decode(model, row.vector)
            } catch (failure: IllegalArgumentException) {
                throw VectorIntegrityException(row.photoUri, model, "Stored vector bytes are invalid", failure)
            }
            IndexedPhoto(row.photoUri, embedding)
        }
    }
}
