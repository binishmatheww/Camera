package com.binishmatheww.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.binishmatheww.camera.composables.CameraPreviewLayout
import com.binishmatheww.camera.composables.rememberCameraController
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
                cameraPreviewLayoutConstraint,
                captureButtonConstraint
                ) = createRefs()

                val cameraController = rememberCameraController()

                val coroutineScope = rememberCoroutineScope()

                var isCaptureButtonEnabled by remember { mutableStateOf(true) }

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
                        coroutineScope.launch {

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