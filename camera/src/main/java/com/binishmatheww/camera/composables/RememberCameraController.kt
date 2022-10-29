package com.binishmatheww.camera.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.binishmatheww.camera.CameraController

@Composable
fun rememberCameraController() : CameraController {

    val context = LocalContext.current

    val cameraController = remember{ CameraController(context) }

    DisposableEffect(
        key1 = true,
        effect = {

            onDispose {
                cameraController.dispose("Recomposition triggered.")
            }

        }
    )

    return cameraController

}