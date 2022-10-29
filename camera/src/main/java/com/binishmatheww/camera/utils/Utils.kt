package com.binishmatheww.camera.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.media.Image
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

/** Helper class used as a data holder for each selectable camera format item */
data class CameraProp(
    val cameraId: String,
    val orientationId: Int,
    val formatId: Int,
    val formatName: String,
    val outputSizes: List<Size>
    )

/** Helper function used to list all compatible cameras and supported pixel formats */
@SuppressLint("InlinedApi")
fun CameraManager.enumerateCameras(): List<CameraProp> {

    val availableCameras = mutableListOf<CameraProp>()

    // Get list of all compatible cameras
    val cameraIds = this.cameraIdList.filter {
        val characteristics = this.getCameraCharacteristics(it)
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
    }


    // Iterate over the list of cameras and return all the compatible ones
    cameraIds.forEach { id ->

        val characteristics = this.getCameraCharacteristics(id)

        val orientationId = characteristics.get(CameraCharacteristics.LENS_FACING) ?: -1

        val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // Query the available output formats
        val outputFormats = streamConfigurationMap?.outputFormats ?: IntArray(0)

        outputFormats.forEach { formatId ->

            availableCameras.add(
                CameraProp(
                    cameraId = id,
                    orientationId = orientationId,
                    formatId = formatId,
                    formatName = "${getOrientation(orientationId)} ${getFormatName(formatId)}",
                    outputSizes = streamConfigurationMap?.getOutputSizes(formatId)?.asList() ?: emptyList()
                )
            )

        }

    }

    return availableCameras

}

fun getFormatName( int : Int ) : String {

    return when(int){

        ImageFormat.JPEG -> "JPEG"

        ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"

        ImageFormat.DEPTH16 -> "DEPTH16"

        ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"

        ImageFormat.RAW_SENSOR -> "RAW_SENSOR"

        ImageFormat.RAW10 -> "RAW10"

        ImageFormat.RAW12 -> "RAW12"

        ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"

        ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"

        ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"

        ImageFormat.HEIC -> "HEIC"

        ImageFormat.NV16 -> "NV16"

        ImageFormat.NV21 -> "NV21"

        ImageFormat.RGB_565 -> "RGB_565"

        ImageFormat.Y8 -> "Y8"

        ImageFormat.YCBCR_P010 -> "YCBCR_P010"

        ImageFormat.YUV_420_888 -> "YUV_420_888"

        ImageFormat.YUV_422_888 -> "YUV_422_888"

        ImageFormat.YUV_444_888 -> "YUV_444_888"

        ImageFormat.YUY2 -> "YUY2"

        ImageFormat.YV12 -> "YV12"

        ImageFormat.PRIVATE -> "PRIVATE"

        ImageFormat.UNKNOWN -> "UNKNOWN"

        else -> "UNKNOWN($int)"

    }

}

fun getOrientation( int : Int ) : String{

    return when(int) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

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