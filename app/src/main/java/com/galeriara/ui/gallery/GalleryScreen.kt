package com.galeriara.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.galeriara.data.GalleryAccess
import com.galeriara.ui.gallery.components.GalleryAccessCard
import com.galeriara.ui.gallery.components.PhotoGrid
import com.galeriara.ui.gallery.components.PhotoPreviewDialog

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var selectedUri by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.photos) {
        if (state.photos.none { it.uri == selectedUri }) selectedUri = null
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("GaleriAra", style = MaterialTheme.typography.headlineLarge)
            Text("Fotoğraflarına kendi kelimelerinle ulaş.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            GalleryAccessCard(
                access = state.access,
                photoCount = state.photos.size,
                onRequestAccess = onRequestAccess,
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                label = { Text("Örn. sahilde gün batımı") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Anlamsal arama, model kurulumu tamamlandığında açılacak.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Fotoğrafların",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp),
                )
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.loading && state.access != GalleryAccess.DENIED,
                ) { Text("Yenile") }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!state.loading && state.photos.isEmpty() && state.access != GalleryAccess.DENIED) {
                Text("Erişilebilir fotoğraf bulunamadı. Seçimini değiştirebilirsin.")
            }
            PhotoGrid(
                photos = state.photos,
                onPhotoClick = { selectedUri = it },
                modifier = Modifier.weight(1f),
            )
        }
    }

    selectedUri?.let { uri ->
        PhotoPreviewDialog(uri = uri, onDismiss = { selectedUri = null })
    }
}
