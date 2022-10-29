package com.binishmatheww.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.exifinterface.media.ExifInterface
import com.binishmatheww.camera.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Runnable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraController(
    private val context : Context
) {

    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    val cameraHandler = Handler(cameraThread.looper)

    private val imageReaderThread = HandlerThread("ImageReaderThread").apply { start() }

    private val imageReaderHandler = Handler(imageReaderThread.looper)

    val cameraScope = CoroutineScope(Dispatchers.IO)

    var cameraManager : CameraManager

    val availableCameraProps = mutableListOf<CameraProp>()

    var selectedCameraProp : CameraProp? = null

    var selectedCameraCharacteristics : CameraCharacteristics? = null

    var imageReader : ImageReader? = null

    val targets  = mutableListOf<Surface>()

    var cameraDevice : CameraDevice? = null

    var cameraCaptureSession : CameraCaptureSession? = null

    var relativeOrientationListener : OrientationEventListener? = null

    var relativeOrientation = 0

    init {

        relativeOrientationListener = object : OrientationEventListener(context.applicationContext) {

            override fun onOrientationChanged(orientation: Int) {
                val rotation = when {
                    orientation <= 45 -> Surface.ROTATION_0
                    orientation <= 135 -> Surface.ROTATION_90
                    orientation <= 225 -> Surface.ROTATION_180
                    orientation <= 315 -> Surface.ROTATION_270
                    else -> Surface.ROTATION_0
                }
                val relative = computeRelativeRotation(selectedCameraCharacteristics!!, rotation)
                if (relative != relativeOrientation) {
                    relativeOrientation = relative
                }
            }

        }

        relativeOrientationListener?.enable()

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        availableCameraProps.clear()
        availableCameraProps.addAll(cameraManager.enumerateCameras())

        selectedCameraProp = availableCameraProps[0]

        selectedCameraCharacteristics = cameraManager.getCameraCharacteristics(selectedCameraProp!!.cameraId)

    }

    fun selectCamera( cameraProp: CameraProp ){

        selectedCameraProp = cameraProp

        selectedCameraCharacteristics = cameraManager.getCameraCharacteristics(selectedCameraProp!!.cameraId)

    }

    suspend fun captureImage() {

        takePhoto(
            cameraController = this
        ).use { result ->

            Log.wtf(TAG, "Result received: $result")

            // Save the result to disk
            val output = saveResult(
                context = context,
                characteristics = selectedCameraCharacteristics!!,
                result = result,
            )

            Log.wtf(TAG, "Image saved: ${output.absolutePath}")

            // If the result is a JPEG file, update EXIF metadata with orientation info
            if (output.extension == "jpg") {
                val exif = ExifInterface(output.absolutePath)
                exif.setAttribute(
                    ExifInterface.TAG_ORIENTATION, result.orientation.toString())
                exif.saveAttributes()
                Log.wtf(TAG, "EXIF metadata saved: ${output.absolutePath}")
            }

        }

    }


    companion object{

        private const val TAG = "CameraController"


        /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
        @SuppressLint("MissingPermission")
        suspend fun openCamera(
            manager: CameraManager,
            cameraId: String,
            handler: Handler? = null
        ): CameraDevice = suspendCancellableCoroutine { cont ->
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = cont.resume(device)

                override fun onDisconnected(device: CameraDevice) {
                    Log.w(TAG, "Camera $cameraId has been disconnected")
                }

                override fun onError(device: CameraDevice, error: Int) {

                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }

                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    Log.e(TAG, exc.message, exc)
                    if (cont.isActive) cont.resumeWithException(exc)
                }
            }, handler)
        }

        suspend fun openCamera(
            cameraController: CameraController
        ) : CameraDevice  = openCamera(
            manager = cameraController.cameraManager,
            cameraId = cameraController.selectedCameraProp!!.cameraId,
            handler = cameraController.cameraHandler
        )

        /**
         * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
         * suspend coroutine
         */
        @Suppress("DEPRECATION")
        suspend fun createCaptureSession(
            device: CameraDevice,
            targets: List<Surface>,
            handler: Handler? = null
        ): CameraCaptureSession = suspendCoroutine { cont ->

            // Create a capture session using the predefined targets; this also involves defining the
            // session state callback to be notified of when the session is ready
            device.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        val exc = RuntimeException("Camera ${device.id} session configuration failed")
                        Log.e(TAG, exc.message, exc)
                        cont.resumeWithException(exc)
                    }

                },
                handler
            )

        }

        suspend fun createCaptureSession(
            cameraController: CameraController
        ): CameraCaptureSession = createCaptureSession(
            device = cameraController.cameraDevice!!,
            targets = cameraController.targets,
            handler = cameraController.cameraHandler
        )

        /**
         * Helper function used to capture a still image using the [CameraDevice.TEMPLATE_STILL_CAPTURE]
         * template. It performs synchronization between the [CaptureResult] and the [Image] resulting
         * from the single capture, and outputs a [CombinedCaptureResult] object.
         */
        suspend fun takePhoto(
            imageReader : ImageReader,
            imageReaderHandler : Handler,
            session : CameraCaptureSession,
            cameraHandler : Handler,
            characteristics : CameraCharacteristics,
            coroutineScope: CoroutineScope,
            relativeOrientation : Int,
            onCaptureStarted: () -> Unit = {}
        ): CombinedCaptureResult = suspendCoroutine { cont ->

            // Flush any images left in the image reader
            @Suppress("ControlFlowWithEmptyBody")
            while (imageReader.acquireNextImage() != null) {
                Log.wtf(TAG,"Flush any images left in the image reader...")
            }

            // Start a new image queue
            val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireNextImage()
                Log.wtf(TAG, "Image available in queue: ${image.timestamp}")
                imageQueue.add(image)
            }, imageReaderHandler)

            val captureRequest = session.device.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
            session.capture(captureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber)
                    onCaptureStarted.invoke()
                }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                    Log.wtf(TAG, "Capture result received: $resultTimestamp")

                    // Set a timeout in case image captured is dropped from the pipeline
                    val exc = TimeoutException("Image dequeuing took too long")
                    val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                    imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                    // Loop in the coroutine's context until an image with matching timestamp comes
                    // We need to launch the coroutine context again because the callback is done in
                    //  the handler provided to the `capture` method, not in our coroutine context
                    @Suppress("BlockingMethodInNonBlockingContext")
                    coroutineScope.launch(cont.context) {
                        while (true) {

                            Log.wtf(TAG, "Dequeue images while timestamps don't match")

                            // Dequeue images while timestamps don't match
                            val image = imageQueue.take()
                            // b/142011420
                            if (image.timestamp != resultTimestamp) continue
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                image.format != ImageFormat.DEPTH_JPEG &&
                                image.timestamp != resultTimestamp) continue
                            Log.wtf(TAG, "Matching image dequeued: ${image.timestamp}")

                            // Unset the image reader listener
                            imageReaderHandler.removeCallbacks(timeoutRunnable)
                            imageReader.setOnImageAvailableListener(null, null)

                            // Clear the queue of images, if there are left
                            while (imageQueue.size > 0) {
                                Log.wtf(TAG,"Remaining images in queue : ${imageQueue.size}")
                                imageQueue.take().close()
                            }

                            // Compute EXIF orientation metadata
                            val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                            val exifOrientation = computeExifOrientation(relativeOrientation, mirrored)

                            Log.wtf(TAG,"Resuming continuation...")

                            // Build the result and resume progress
                            cont.resume(
                                CombinedCaptureResult(
                                    image, result, exifOrientation, imageReader.imageFormat)
                            )

                            // There is no need to break out of the loop, this coroutine will suspend

                            /**
                             * Comment by @binishmatheww
                             * I had to add a break because the thread was waiting indefinitely.
                             *
                             * stacktrace :
                             *
                             * Event:APP_SCOUT_WARNING Thread:main backtrace:
                            at jdk.internal.misc.Unsafe.park(Native Method)
                            at java.util.concurrent.locks.LockSupport.park(LockSupport.java:194)
                            at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2081)
                            at java.util.concurrent.ArrayBlockingQueue.take(ArrayBlockingQueue.java:417)
                            at com.binishmatheww.camera.CameraController$Companion$takePhoto$3$2$onCaptureCompleted$1.invokeSuspend(CameraController.kt:250) ( val image = imageQueue.take() )
                            at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
                            at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
                            at androidx.compose.ui.platform.AndroidUiDispatcher.performTrampolineDispatch(AndroidUiDispatcher.android.kt:81)
                            at androidx.compose.ui.platform.AndroidUiDispatcher.access$performTrampolineDispatch(AndroidUiDispatcher.android.kt:41)
                            at androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1.run(AndroidUiDispatcher.android.kt:57)
                            at android.os.Handler.handleCallback(Handler.java:938)
                            at android.os.Handler.dispatchMessage(Handler.java:99)
                            at android.os.Looper.loopOnce(Looper.java:210)
                            at android.os.Looper.loop(Looper.java:299)
                            at android.app.ActivityThread.main(ActivityThread.java:8261)
                            at java.lang.reflect.Method.invoke(Native Method)
                            at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:556)
                            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1045)
                             *
                             */
                            break

                        }
                    }
                }
            },
                cameraHandler
            )
        }

        suspend fun takePhoto(
            cameraController: CameraController
        ): CombinedCaptureResult = takePhoto(
            imageReader = cameraController.imageReader!!,
            imageReaderHandler = cameraController.imageReaderHandler,
            session = cameraController.cameraCaptureSession!!,
            cameraHandler = cameraController.cameraHandler,
            characteristics = cameraController.selectedCameraCharacteristics!!,
            coroutineScope = cameraController.cameraScope,
            relativeOrientation = cameraController.relativeOrientation,
            onCaptureStarted = {}
        )

        /** Helper function used to save a [CombinedCaptureResult] into a [File] */
        suspend fun saveResult(
            context: Context,
            characteristics : CameraCharacteristics,
            result: CombinedCaptureResult,
        ): File = suspendCoroutine { cont ->

            when (result.format) {

                // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
                ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                    val buffer = result.image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                    try {
                        val output = createFile(context, "jpg")
                        FileOutputStream(output).use { it.write(bytes) }
                        cont.resume(output)
                    } catch (exc: IOException) {
                        Log.e(TAG, "Unable to write JPEG image to file", exc)
                        cont.resumeWithException(exc)
                    }
                }

                // When the format is RAW we use the DngCreator utility library
                ImageFormat.RAW_SENSOR -> {
                    val dngCreator = DngCreator(characteristics, result.metadata)
                    try {
                        val output = createFile(context, "dng")
                        FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                        cont.resume(output)
                    } catch (exc: IOException) {
                        Log.e(TAG, "Unable to write DNG image to file", exc)
                        cont.resumeWithException(exc)
                    }
                }

                // No other formats are supported by this sample
                else -> {
                    val exc = RuntimeException("Unknown image format: ${result.image.format}")
                    Log.e(TAG, exc.message, exc)
                    cont.resumeWithException(exc)
                }

            }

        }

    }

}