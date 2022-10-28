package com.binishmatheww.camera.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.binishmatheww.camera.CameraController

@Composable
fun rememberCameraController() : CameraController {

    val context = LocalContext.current

    return remember{ CameraController(context) }

}