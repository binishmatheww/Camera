package com.binishmatheww.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
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

                val (
                cameraPropsConstraint,
                cameraSizesConstraint,
                cameraPreviewLayoutConstraint,
                captureButtonConstraint
                ) = createRefs()

                val cameraController = rememberCameraController()

                val availableCameraProps by cameraController.availableCameraPropsFlow.collectAsStateWithLifecycle()

                val selectedCameraProp by cameraController.selectedCameraPropFlow.collectAsStateWithLifecycle()

                val availableCameraSizes by cameraController.availableCameraSizesFlow.collectAsStateWithLifecycle()

                val selectedCameraSize by cameraController.selectedCameraSizeFlow.collectAsStateWithLifecycle()

                var isCaptureButtonEnabled by remember { mutableStateOf(true) }

                LazyRow(
                    modifier = Modifier
                        .constrainAs(cameraPropsConstraint){
                            top.linkTo(parent.top)
                            linkTo(start = parent.start, end = parent.end)
                        },
                   ){

                    items(availableCameraProps){ cameraProp ->

                        Button(
                            modifier = Modifier
                                .height(32.dp)
                                .padding(
                                    horizontal = 8.dp
                                ),
                            onClick = {
                                cameraController.cameraScope.launch {
                                    cameraController.selectCamera(cameraProp)
                                    cameraController.setSize(cameraProp.outputSizes.firstOrNull())
                                }
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

                LazyRow(
                    modifier = Modifier
                        .constrainAs(cameraSizesConstraint){
                            top.linkTo(cameraPropsConstraint.bottom, 12.dp)
                            linkTo(start = parent.start, end = parent.end)
                        },
                ){

                    items(availableCameraSizes){ cameraSize ->

                        Button(
                            modifier = Modifier
                                .height(32.dp)
                                .padding(
                                    horizontal = 8.dp
                                ),
                            onClick = {
                                cameraController.cameraScope.launch {
                                    cameraController.setSize(cameraSize)
                                }
                            },
                            colors = buttonColors(
                                contentColor = Color.Black,
                                backgroundColor = if(cameraSize == selectedCameraSize) Color.Green else Color.Gray
                            )
                        ) {
                            Text(text = "${cameraSize.width} x ${cameraSize.height}")
                        }

                    }

                }

                CameraPreviewLayout(
                    modifier = Modifier
                        .constrainAs(cameraPreviewLayoutConstraint){
                            linkTo(top = parent.top, bottom = parent.bottom)
                            linkTo(start = parent.start, end = parent.end)
                        },
                    cameraController = cameraController,
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
                            cameraController.captureImage()
                            isCaptureButtonEnabled = true

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

}