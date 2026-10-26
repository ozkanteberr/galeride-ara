package com.galeriara

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.galeriara.data.*
import com.galeriara.ui.GalleryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF225B51), background = Color(0xFFF5F6F1),
                surface = Color(0xFFF5F6F1),
            )) {
                val vm: GalleryViewModel = viewModel(factory = viewModelFactory {
                    initializer { GalleryViewModel((application as GalleryApplication).gallery) }
                })
                val state by vm.state.collectAsStateWithLifecycle()
                val permissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { vm.refresh(galleryAccess()) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh(galleryAccess()) }
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
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE1ECE5))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Galerin bu cihazda kalır", style = MaterialTheme.typography.titleMedium)
                                Text(when (state.access) {
                                    GalleryAccess.DENIED -> "Başlamak için fotoğraflarına erişim ver."
                                    GalleryAccess.LIMITED -> "Yalnızca izin verdiğin fotoğraflar görüntüleniyor."
                                    GalleryAccess.FULL -> "${state.photos.size} fotoğrafa erişim var."
                                })
                                Button(onClick = { permissions.launch(galleryPermissions()) }) {
                                    Text(if (state.access == GalleryAccess.DENIED) "Fotoğraflara erişim ver" else "Erişimi değiştir")
                                }
                                if (state.access == GalleryAccess.DENIED) {
                                    TextButton(onClick = {
                                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:$packageName")))
                                    }) { Text("Uygulama ayarları") }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = "", onValueChange = {}, enabled = false,
                            label = { Text("Örn. sahilde gün batımı") }, modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Anlamsal arama, model kurulumu tamamlandığında açılacak.",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fotoğrafların", style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 10.dp))
                            TextButton(onClick = { vm.refresh(galleryAccess()) },
                                enabled = !state.loading && state.access != GalleryAccess.DENIED) { Text("Yenile") }
                        }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (!state.loading && state.photos.isEmpty() && state.access != GalleryAccess.DENIED) {
                            Text("Erişilebilir fotoğraf bulunamadı. Seçimini değiştirebilirsin.")
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp), modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            items(state.photos, key = { it.uri }) { photo ->
                                AsyncImage(model = photo.uri, contentDescription = photo.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                        .background(Color(0xFFE1E5E0), RoundedCornerShape(8.dp))
                                        .clickable { selectedUri = photo.uri })
                            }
                        }
                    }
                }
                selectedUri?.let { uri ->
                    AlertDialog(onDismissRequest = { selectedUri = null },
                        title = { Text("Fotoğraf") },
                        text = { AsyncImage(model = uri, contentDescription = "Seçilen fotoğraf",
                            modifier = Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit) },
                        confirmButton = { TextButton(onClick = { selectedUri = null }) { Text("Kapat") } })
                }
            }
        }
    }
}
