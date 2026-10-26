package com.galeriara.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galeriara.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val access: GalleryAccess = GalleryAccess.DENIED,
    val photos: List<PhotoEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class GalleryViewModel(private val gallery: GalleryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(GalleryUiState())
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh(access: GalleryAccess) {
        refreshJob?.cancel()
        mutableState.value = GalleryUiState(access = access, loading = access != GalleryAccess.DENIED)
        if (access == GalleryAccess.DENIED) return
        refreshJob = viewModelScope.launch {
            try {
                val photos = gallery.refresh()
                mutableState.update { it.copy(photos = photos, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                mutableState.value = GalleryUiState(error = "Galeri izni değişti. Yeniden erişim verin.")
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Galeri okunamadı. Yeniden deneyin.") }
            }
        }
    }
}
