package com.galeriara.data

import androidx.room.*

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val modifiedSeconds: Long,
    val sizeBytes: Long,
)

@Entity(
    tableName = "embeddings",
    primaryKeys = ["photoUri", "modelId", "revision"],
    foreignKeys = [ForeignKey(
        entity = PhotoEntity::class, parentColumns = ["uri"], childColumns = ["photoUri"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class EmbeddingEntity(
    val photoUri: String,
    val modelId: String,
    val revision: String,
    val dimensions: Int,
    val vector: ByteArray,
)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY modifiedSeconds DESC, uri")
    suspend fun all(): List<PhotoEntity>
    @Upsert suspend fun upsert(photo: PhotoEntity)
    @Query("DELETE FROM photos WHERE uri = :uri") suspend fun delete(uri: String)
    @Query("DELETE FROM embeddings WHERE photoUri = :uri") suspend fun invalidate(uri: String)
}

@Database(entities = [PhotoEntity::class, EmbeddingEntity::class], version = 1, exportSchema = true)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun photos(): PhotoDao
}
