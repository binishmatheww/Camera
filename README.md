<h1 align="center">Camera</h1></br>

<br>
<br>

<p align="center">
    <b>Camera</b> is a library for Android built using the Camera2 API.
</p>

<br>

> :warning: **This library is still under development**: Do not use this in production!

<br>

The [Camera2][1] API allows users to capture RAW images, i.e. unprocessed pixel data
directly from the camera sensor that has not yet been converted into a format and
colorspace typically used for displaying and storing images viewed by humans.
The [DngCreator][2] class is provided as part of the Camera2 API as a utility for saving
RAW images as DNG files.

Aim of this lib is to make the use of [Camera2][1] APIs with [Jetpack Compose][3] easier.

<br>

This lib usees code from [Camera2Basic][4] sample by google.

[1]: https://developer.android.com/reference/android/hardware/camera2/package-summary.html
[2]: https://developer.android.com/reference/android/hardware/camera2/DngCreator.html
[3]: https://developer.android.com/jetpack/compose
[4]: https://github.com/android/camera-samples/tree/main/Camera2Basic

<br>

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

Support
-------

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

# License
```xml
Copyright 2022 Binish Mathew

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
