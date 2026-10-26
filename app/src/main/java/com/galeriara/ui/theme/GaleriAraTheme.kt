package com.galeriara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalleryColorScheme = lightColorScheme(
    primary = Color(0xFF225B51),
    background = Color(0xFFF5F6F1),
    surface = Color(0xFFF5F6F1),
)

@Composable
fun GaleriAraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GalleryColorScheme, content = content)
}
