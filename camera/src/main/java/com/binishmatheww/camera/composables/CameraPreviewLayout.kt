package com.binishmatheww.camera.composables

import android.view.SurfaceHolder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.binishmatheww.camera.CameraController
import com.binishmatheww.camera.utils.AutoFitSurfaceView

@Composable
fun CameraPreviewLayout(
    modifier: Modifier = Modifier,
    cameraController: CameraController,
) {

    AndroidView(
        modifier = modifier,
        factory = { con ->

            cameraController.viewFinder = AutoFitSurfaceView(con)

            cameraController.viewFinder?.holder?.addCallback(object : SurfaceHolder.Callback {

                override fun surfaceDestroyed(
                    holder: SurfaceHolder
                ) = Unit

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) = Unit

                override fun surfaceCreated(holder: SurfaceHolder) {

                    cameraController.initialize()

                }

            })

            cameraController.viewFinder!!

        }
    )

}
