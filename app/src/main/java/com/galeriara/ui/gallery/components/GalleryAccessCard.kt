package com.galeriara.ui.gallery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.galeriara.data.GalleryAccess

@Composable
fun GalleryAccessCard(
    access: GalleryAccess,
    photoCount: Int,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE1ECE5))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Galerin bu cihazda kalır", style = MaterialTheme.typography.titleMedium)
            Text(when (access) {
                GalleryAccess.DENIED -> "Başlamak için fotoğraflarına erişim ver."
                GalleryAccess.LIMITED -> "Yalnızca izin verdiğin fotoğraflar görüntüleniyor."
                GalleryAccess.FULL -> "$photoCount fotoğrafa erişim var."
            })
            Button(onClick = onRequestAccess) {
                Text(if (access == GalleryAccess.DENIED) "Fotoğraflara erişim ver" else "Erişimi değiştir")
            }
            if (access == GalleryAccess.DENIED) {
                TextButton(onClick = onOpenSettings) { Text("Uygulama ayarları") }
            }
        }
    }
}
