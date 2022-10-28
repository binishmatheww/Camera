package com.binishmatheww.camera.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.media.Image
import android.view.Surface
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/** Helper class used as a data holder for each selectable camera format item */
data class CameraFormat(val title: String, val cameraId: String, val format: Int)

/** Helper function used to list all compatible cameras and supported pixel formats */
@SuppressLint("InlinedApi")
fun CameraManager.enumerateCameras(): List<CameraFormat> {
    val availableCameras: MutableList<CameraFormat> = mutableListOf()

    // Get list of all compatible cameras
    val cameraIds = this.cameraIdList.filter {
        val characteristics = this.getCameraCharacteristics(it)
        val capabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
    }


    // Iterate over the list of cameras and return all the compatible ones
    cameraIds.forEach { id ->
        val characteristics = this.getCameraCharacteristics(id)
        val orientation = when(characteristics.get(CameraCharacteristics.LENS_FACING)!!) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        // Query the available capabilities and output formats
        val capabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
        val outputFormats = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

        // All cameras *must* support JPEG output so we don't need to check characteristics
        availableCameras.add(CameraFormat(
            "$orientation JPEG ($id)", id, ImageFormat.JPEG))

        // Return cameras that support RAW capability
        if (capabilities.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
            outputFormats.contains(ImageFormat.RAW_SENSOR)) {
            availableCameras.add(CameraFormat(
                "$orientation RAW ($id)", id, ImageFormat.RAW_SENSOR))
        }

        // Return cameras that support JPEG DEPTH capability
        if (capabilities.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
            outputFormats.contains(ImageFormat.DEPTH_JPEG)) {
            availableCameras.add(CameraFormat(
                "$orientation DEPTH ($id)", id, ImageFormat.DEPTH_JPEG))
        }
    }

    return availableCameras
}

/** Maximum number of images that will be held in the reader's buffer */
const val IMAGE_BUFFER_SIZE: Int = 3

/** Maximum time allowed to wait for the result of an image capture */
const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

/** Helper data class used to hold capture metadata with their associated image */
data class CombinedCaptureResult(
    val image: Image,
    val metadata: CaptureResult,
    val orientation: Int,
    val format: Int
) : Closeable {
    override fun close() = image.close()
}

/**
 * Create a [File] named a using formatted timestamp with the current date and time.
 *
 * @return [File] created.
 */
fun createFile(context: Context, extension: String): File {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
    return File(context.getExternalFilesDir(null), "IMG_${sdf.format(Date())}.$extension")
}


/**
 * Computes rotation required to transform from the camera sensor orientation to the
 * device's current orientation in degrees.
 *
 * @param characteristics the [CameraCharacteristics] to query for the sensor orientation.
 * @param surfaceRotation the current device orientation as a Surface constant
 * @return the relative rotation from the camera sensor to the current device orientation.
 */
fun computeRelativeRotation(
    characteristics: CameraCharacteristics,
    surfaceRotation: Int
): Int {
    val sensorOrientationDegrees =
        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

    val deviceOrientationDegrees = when (surfaceRotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }

    // Reverse device orientation for front-facing cameras
    val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
        CameraCharacteristics.LENS_FACING_FRONT) 1 else -1

    // Calculate desired JPEG orientation relative to camera orientation to make
    // the image upright relative to the device orientation
    return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360
}