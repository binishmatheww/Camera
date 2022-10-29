package com.binishmatheww.camera.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.binishmatheww.camera.CameraController
import kotlinx.coroutines.cancel

@Composable
fun rememberCameraController() : CameraController {

    val context = LocalContext.current

    val cameraController = remember{ CameraController(context) }

    DisposableEffect(
        key1 = context,
        effect = {

            onDispose {
                cameraController.relativeOrientationListener?.disable()
                cameraController.session?.stopRepeating()
                cameraController.cameraCoroutineScope.cancel("Recomposition triggered.")
            }

        }
    )

    return cameraController

}