package com.binishmatheww.camera.composables

import android.view.SurfaceHolder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.binishmatheww.camera.CameraController
import com.binishmatheww.camera.utils.AutoFitSurfaceView
import kotlinx.coroutines.launch

@Composable
fun CameraPreviewLayout(
    modifier: Modifier = Modifier,
    cameraController: CameraController,
    onSurfaceDestroyed : () -> Unit = { },
    onSurfaceChanged : (Int, Int) -> Unit = { _, _ -> },
    onSurfaceCreated : () -> Unit = { },
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {

        AndroidView(
            modifier = Modifier,
            factory = { con ->

                cameraController.viewFinder = AutoFitSurfaceView(con)

                cameraController.viewFinder?.holder?.addCallback(object : SurfaceHolder.Callback {

                    override fun surfaceDestroyed(
                        holder: SurfaceHolder
                    ) = onSurfaceDestroyed.invoke()

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) = onSurfaceChanged.invoke(width, height)

                    override fun surfaceCreated(holder: SurfaceHolder) {

                        onSurfaceCreated.invoke()

                        cameraController.cameraScope.launch {

                            cameraController.selectSize(cameraController.selectedCameraSize)

                            cameraController.initialize()

                        }

                    }

                })

                cameraController.viewFinder!!

            }
        )

    }

}
