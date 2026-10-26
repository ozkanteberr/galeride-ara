package com.galeriara.ui.gallery

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.galeriara.GalleryApplication
import com.galeriara.data.galleryAccess
import com.galeriara.data.galleryPermissions

@Composable
fun GalleryRoute() {
    val context = LocalContext.current
    val application = context.applicationContext as GalleryApplication
    val viewModel: GalleryViewModel = viewModel(factory = viewModelFactory {
        initializer { GalleryViewModel(application.gallery) }
    })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh(context.galleryAccess()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh(context.galleryAccess())
    }

    GalleryScreen(
        state = state,
        onRequestAccess = { permissions.launch(galleryPermissions()) },
        onOpenSettings = {
            context.startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ))
        },
        onRefresh = { viewModel.refresh(context.galleryAccess()) },
    )
}
