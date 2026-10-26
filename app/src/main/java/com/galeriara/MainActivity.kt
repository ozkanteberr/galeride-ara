package com.galeriara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.galeriara.ui.gallery.GalleryRoute
import com.galeriara.ui.theme.GaleriAraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GaleriAraTheme {
                GalleryRoute()
            }
        }
    }
}
