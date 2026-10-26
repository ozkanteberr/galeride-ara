package com.galeriara.ui.gallery.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoPreviewDialog(uri: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fotoğraf") },
        text = {
            AsyncImage(
                model = uri,
                contentDescription = "Seçilen fotoğraf",
                modifier = Modifier.fillMaxWidth().height(360.dp),
                contentScale = ContentScale.Fit,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}
