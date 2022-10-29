package com.binishmatheww.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
import com.binishmatheww.camera.utils.log
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                val (
                cameraPropsConstraint,
                cameraPreviewLayoutConstraint,
                captureButtonConstraint
                ) = createRefs()

                val cameraController = rememberCameraController()

                val availableCameraProps by cameraController.availableCameraPropsFlow.collectAsState()

                val selectedCameraProp by cameraController.selectedCameraPropFlow.collectAsState()

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

                CameraPreviewLayout(
                    modifier = Modifier
                        .constrainAs(cameraPreviewLayoutConstraint){
                            linkTo(top = parent.top, bottom = parent.bottom)
                            linkTo(start = parent.start, end = parent.end)
                        },
                    cameraController = cameraController,
                )

                Button(
                    modifier = Modifier.constrainAs(captureButtonConstraint){
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