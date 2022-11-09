package com.binishmatheww.cam

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
import com.binishmatheww.camera.utils.CameraProp
import com.binishmatheww.camera.utils.SmartSize
import com.binishmatheww.camera.utils.log
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    @OptIn(ExperimentalLifecycleComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                val context = LocalContext.current

                val (
                cameraPropsConstraint,
                cameraSizesConstraint,
                cameraPreviewLayoutConstraint,
                captureButtonConstraint
                ) = createRefs()

                val cameraController = rememberCameraController()

                LaunchedEffect(
                    key1 = cameraController,
                    block = {
                        cameraController.addRequiredOrientations(listOf(CameraCharacteristics.LENS_FACING_BACK))
                        cameraController.addRequiredFormats(listOf(ImageFormat.JPEG))
                    }
                )

                val availableCameraProps by cameraController.availableCameraPropsFlow.collectAsStateWithLifecycle()

                val selectedCameraProp by cameraController.selectedCameraPropFlow.collectAsStateWithLifecycle()

                val availableCameraSizes by cameraController.availableCameraSizesFlow.collectAsStateWithLifecycle()

                val selectedCameraSize by cameraController.selectedCameraSizeFlow.collectAsStateWithLifecycle()

                var isCaptureButtonEnabled by remember { mutableStateOf(true) }

                CameraPreviewLayout(
                    modifier = Modifier
                        .constrainAs(cameraPreviewLayoutConstraint){
                            linkTo(top = parent.top, bottom = parent.bottom)
                            linkTo(start = parent.start, end = parent.end)
                        },
                    cameraController = cameraController,
                    onSurfaceCreated = {
                        log("Surface created.")
                    },
                    onSurfaceChanged = { width, height ->
                        log("Surface changed. $width x $height")
                    },
                    onSurfaceDestroyed = {
                        log("Surface destroyed.")
                    }
                )

                CameraPropsLayout(
                    modifier = Modifier
                        .constrainAs(cameraPropsConstraint){
                            top.linkTo(parent.top)
                            linkTo(start = parent.start, end = parent.end)
                        },
                    availableCameraProps = availableCameraProps,
                    selectedCameraProp = selectedCameraProp,
                    onCameraPropSelected = { cameraProp ->
                        cameraController.cameraScope.launch {
                            cameraController.selectCamera(cameraProp)
                            cameraController.selectSize(cameraProp?.outputSizes?.firstOrNull())
                            cameraController.initialize()
                        }
                    }
                )

                CameraSizesLayout(
                    modifier = Modifier
                        .constrainAs(cameraSizesConstraint){
                            top.linkTo(cameraPropsConstraint.bottom, 12.dp)
                            linkTo(start = parent.start, end = parent.end)
                        },
                    availableCameraSizes = availableCameraSizes,
                    selectedCameraSize = selectedCameraSize,
                    onCameraSizeSelected = { cameraSize ->
                        cameraController.cameraScope.launch {
                            cameraController.selectSize(cameraSize)
                            cameraController.initialize()
                        }
                    }
                )

                Button(
                    modifier = Modifier
                        .constrainAs(captureButtonConstraint) {
                            linkTo(start = parent.start, end = parent.end)
                            bottom.linkTo(parent.bottom, 24.dp)
                        },
                    enabled = isCaptureButtonEnabled,
                    onClick = {

                        cameraController.cameraScope.launch {

                            isCaptureButtonEnabled = false

                            cameraController.captureImage{ result ->

                                cameraController.saveImage(
                                    result = result,
                                    fileLocation = context.getExternalFilesDir(null),
                                    fileName = null
                                )

                                isCaptureButtonEnabled = true

                            }

                        }

                    }
                ) {

                    Text(
                        text = "Capture"
                    )

                }

            }

        }

    }

    @Composable
    fun CameraPropsLayout(
        modifier: Modifier,
        availableCameraProps : List<CameraProp>,
        selectedCameraProp : CameraProp?,
        onCameraPropSelected : (CameraProp?) -> Unit
    ){

        LazyRow(
            modifier = modifier,
        ){

            items(
                items = availableCameraProps,
            ){ cameraProp ->

                Button(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(
                            horizontal = 8.dp
                        ),
                    onClick = {
                        onCameraPropSelected.invoke(cameraProp)
                    },
                    colors = buttonColors(
                        contentColor = Color.Black,
                        backgroundColor = if(cameraProp == selectedCameraProp) Color.Green else Color.Gray
                    )
                ) {
                    Text(text = cameraProp.formatName)
                }
            }

        }

    }

    @Composable
    fun CameraSizesLayout(
        modifier: Modifier,
        availableCameraSizes: List<SmartSize>,
        selectedCameraSize: SmartSize?,
        onCameraSizeSelected : (SmartSize) -> Unit
    ){

        LazyRow(
            modifier = modifier,
        ){

            items(
                items = availableCameraSizes,
            ){ cameraSize ->

                Button(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(
                            horizontal = 8.dp
                        ),
                    onClick = {
                        onCameraSizeSelected.invoke(cameraSize)
                    },
                    colors = buttonColors(
                        contentColor = Color.Black,
                        backgroundColor = if(cameraSize == selectedCameraSize) Color.Green else Color.Gray
                    )
                ) {
                    Text(text = "${cameraSize.size.width} x ${cameraSize.size.height}")
                }

            }

        }

    }

}