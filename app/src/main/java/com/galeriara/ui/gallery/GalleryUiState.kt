package com.galeriara.ui.gallery

import com.galeriara.data.GalleryAccess
import com.galeriara.data.PhotoEntity

data class GalleryUiState(
    val access: GalleryAccess = GalleryAccess.DENIED,
    val photos: List<PhotoEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)
