package com.binishmatheww.camera.composables

import android.hardware.camera2.CameraCharacteristics
import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.binishmatheww.camera.utils.computeRelativeRotation

@Composable
fun orientationChangeListener(
    characteristics: CameraCharacteristics,
): MutableState<Int> {

    val context = LocalContext.current

    val value = remember { mutableStateOf(0) }

    LaunchedEffect(
        key1 = true,
        block = {

            val listener = object : OrientationEventListener(context.applicationContext) {

                override fun onOrientationChanged(orientation: Int) {
                    val rotation = when {
                        orientation <= 45 -> Surface.ROTATION_0
                        orientation <= 135 -> Surface.ROTATION_90
                        orientation <= 225 -> Surface.ROTATION_180
                        orientation <= 315 -> Surface.ROTATION_270
                        else -> Surface.ROTATION_0
                    }
                    val relative = computeRelativeRotation(characteristics, rotation)
                    if (relative != value.value) {
                        value.value = relative
                    }
                }

            }

            listener.enable()

        }
    )

    return value

}