<h1 align="center">Camera</h1></br>

> :warning: **This library is still under development**: Do not use this in production!

<br>
<br>

<p align="center">
    <b>Camera</b> is a library for Android built using Camera2 APIs.
</p>

<br>

This lib makes the use of Camera2 APIs with [Jetpack Compose](https://developer.android.com/jetpack/compose) easier.

<br>

This lib usees code from [Camera2Basic](https://github.com/android/camera-samples/tree/main/Camera2Basic) sample by google.

### Usage
Use `CameraPreviewLayout` composable to preview frames from the camera.
You must pass a `CameraController` to the composable.
An instance of `CameraController` can be obtained using `rememberCameraController()`.
By default, the `CameraController` will select the back facing camera.

```kotlin

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

```

# License
```xml
Copyright 2022 Binish Mathew and The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
```
