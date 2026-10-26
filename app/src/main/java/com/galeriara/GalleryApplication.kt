package com.galeriara

import android.app.Application
import androidx.room.Room
import com.galeriara.data.GalleryDatabase
import com.galeriara.data.GalleryRepository
import com.galeriara.data.MediaStoreGallery

class GalleryApplication : Application() {
    val gallery: GalleryRepository by lazy {
        GalleryRepository(
            MediaStoreGallery(this),
            Room.databaseBuilder(this, GalleryDatabase::class.java, "gallery.db").build(),
        )
    }
}
