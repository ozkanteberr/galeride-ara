package com.galeriara.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MediaStoreGallery(private val context: Context) {
    suspend fun scan(): List<PhotoEntity> = withContext(Dispatchers.IO) {
        check(context.galleryAccess() != GalleryAccess.DENIED) { "Galeri erişim izni gerekli." }
        val collection = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE,
        )
        val photos = mutableListOf<PhotoEntity>()
        val cursor = context.contentResolver.query(
            collection, columns, null, null, "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        ) ?: error("Galeri okunamadı. Lütfen yeniden deneyin.")
        cursor.use {
            while (it.moveToNext()) {
                ensureActive()
                photos += PhotoEntity(
                    ContentUris.withAppendedId(collection, it.getLong(0)).toString(),
                    it.getString(1) ?: "Fotoğraf", it.getLong(2), it.getLong(3),
                )
            }
        }
        photos
    }
}
