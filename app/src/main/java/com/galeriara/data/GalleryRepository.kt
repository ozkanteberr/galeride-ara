package com.galeriara.data

import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Catalog metadata is distinct from a semantic index. Scanning never marks a photo as encoded. */
class GalleryRepository(private val media: MediaStoreGallery, private val db: GalleryDatabase) {
    private val mutex = Mutex()
    suspend fun refresh(): List<PhotoEntity> = mutex.withLock {
        val current = media.scan() // Never reconcile after an incomplete/failed scan.
        db.withTransaction {
            val old = db.photos().all().associateBy { it.uri }
            val visible = current.mapTo(HashSet()) { it.uri }
            current.forEach { photo ->
                val previous = old[photo.uri]
                if (previous != null && (previous.modifiedSeconds != photo.modifiedSeconds ||
                        previous.sizeBytes != photo.sizeBytes)) db.photos().invalidate(photo.uri)
                db.photos().upsert(photo)
            }
            // Removal means "outside current accessible scope", not necessarily physically deleted.
            old.keys.filterNot { it in visible }.forEach { db.photos().delete(it) }
        }
        current
    }
}
