package com.binishmatheww.camera.composables

import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.view.SurfaceHolder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.binishmatheww.camera.CameraController
import com.binishmatheww.camera.utils.AutoFitSurfaceView
import com.binishmatheww.camera.utils.IMAGE_BUFFER_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraPreviewLayout(
    cameraController: CameraController,
    modifier: Modifier = Modifier
) {

    AndroidView(
        modifier = modifier,
        factory = { con ->

            val viewFinder = AutoFitSurfaceView(con)

            viewFinder.holder.addCallback(object : SurfaceHolder.Callback {

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

                    cameraController.cameraCoroutineScope.launch {

                        val sizes = cameraController.selectedCameraProp.outputSizes

                        val size = sizes.maxByOrNull { it.height * it.width } ?: sizes.first()

                        withContext(Dispatchers.Main){

                            viewFinder.setAspectRatio(
                                size.width,
                                size.height
                            )

                        }

                        // Open the selected camera
                        cameraController.camera = CameraController.openCamera(
                            cameraController = cameraController
                        )

                        // Initialize an image reader which will be used to capture still photos
                        cameraController.imageReader = ImageReader.newInstance(
                            size.width,
                            size.height,
                            cameraController.selectedCameraProp.formatId,
                            IMAGE_BUFFER_SIZE
                        )

                        // Creates list of Surfaces where the camera will output frames
                        cameraController.targets = listOf(viewFinder.holder.surface, cameraController.imageReader.surface)

                        // Start a capture session using our open camera and list of Surfaces where frames will go
                        cameraController.session = CameraController.createCaptureSession(
                            cameraController = cameraController
                        )

                        val captureRequest = cameraController.camera.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW
                        ).apply { addTarget(viewFinder.holder.surface) }

                        // This will keep sending the capture request as frequently as possible until the
                        // session is torn down or session.stopRepeating() is called
                        cameraController.session.setRepeatingRequest(
                            captureRequest.build(),
                            null,
                            cameraController.cameraHandler
                        )

                    }

                }
            })

            viewFinder

        }
    )

}
