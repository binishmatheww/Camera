package com.binishmatheww.camera.composables

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.util.Log
import android.view.SurfaceHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.camera.CameraController
import com.binishmatheww.camera.utils.AutoFitSurfaceView
import com.binishmatheww.camera.utils.IMAGE_BUFFER_SIZE
import com.binishmatheww.camera.utils.getPreviewOutputSize
import kotlinx.coroutines.launch

private const val TAG = "CameraPreview"

@Composable
fun CameraPreviewLayout(
    cameraController: CameraController,
    modifier: Modifier = Modifier
) {

    ConstraintLayout(
        modifier = modifier
    ) {

        val coroutineScope = rememberCoroutineScope()

        val cameraPreviewConstraint = createRef()

        AndroidView(
            modifier = Modifier
                .constrainAs(cameraPreviewConstraint) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                },
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

                        // Selects appropriate preview size and configures view finder
                        val previewSize = getPreviewOutputSize(
                            viewFinder.display,
                            cameraController.characteristics,
                            SurfaceHolder::class.java
                        )

                        Log.d(TAG, "View finder size: ${viewFinder.width} x ${viewFinder.height}")
                        Log.d(TAG, "Selected preview size: $previewSize")

                        viewFinder.setAspectRatio(
                            previewSize.width,
                            previewSize.height
                        )

                        coroutineScope.launch {

                            // Open the selected camera
                            cameraController.camera = CameraController.openCamera(
                                cameraController = cameraController
                            )

                            // Initialize an image reader which will be used to capture still photos
                            val size = cameraController.characteristics
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                ?.getOutputSizes(cameraController.selectedCameraFormat.format)
                                ?.maxByOrNull { it.height * it.width }!!

                            cameraController.imageReader = ImageReader.newInstance(
                                size.width,
                                size.height,
                                cameraController.selectedCameraFormat.format,
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

}
