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
    surfaceDestroyed : () -> Unit = { },
    surfaceChanged : (Int, Int) -> Unit = { _, _ -> },
    surfaceCreated : () -> Unit = { },
) {

    AndroidView(
        modifier = modifier,
        factory = { con ->

            cameraController.viewFinder = AutoFitSurfaceView(con)

            cameraController.viewFinder?.holder?.addCallback(object : SurfaceHolder.Callback {

                override fun surfaceDestroyed(
                    holder: SurfaceHolder
                ) = surfaceDestroyed.invoke()

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) = surfaceChanged.invoke(width, height)

                override fun surfaceCreated(holder: SurfaceHolder) {

                    surfaceCreated.invoke()

                    cameraController.initialize()

                }

            })

            cameraController.viewFinder!!

        }
    )

}
