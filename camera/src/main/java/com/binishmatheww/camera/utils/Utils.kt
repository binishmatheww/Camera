package com.binishmatheww.camera.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.exifinterface.media.ExifInterface
import com.binishmatheww.camera.models.CameraProp
import com.binishmatheww.camera.models.SmartSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


//@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun Any.log(
    message: String?,
    throwable: Throwable? = null
) = log(
    tag = this::class.java.simpleName,
    message = message,
    throwable = throwable
)

/*@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun log(
    message : String?,
    throwable: Throwable? = null
) = log(
    tag = "com.binishmatheww.camera",
    message = message,
    throwable = throwable
)*/

//@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun log(
    tag : String,
    message : String?,
    throwable: Throwable? = null
) = Log.wtf(
    tag,
    message.toString(),
    throwable
)

/** Maximum number of images that will be held in the reader's buffer */
const val IMAGE_BUFFER_SIZE: Int = 3

/** Maximum time allowed to wait for the result of an image capture */
const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

/** Helper function used to list all compatible cameras and supported pixel formats */
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
                    outputSizes = streamConfigurationMap?.getOutputSizes(formatId)?.asList()?.map { SmartSize(it.width,it.height) } ?: emptyList()
                )
            )

        }

    }

    return availableCameras

}

/** Helper function used check if Camera2 API features are enabled or not */
fun CameraManager.isCamera2ApiEnabled(): Boolean {

    return this.cameraIdList.any {
        val characteristics = this.getCameraCharacteristics(it)
        val supportedHardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        supportedHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && supportedHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
                || supportedHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

}

/** Helper function to get format name from id */
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

/** Helper function to get orientation from id */
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
fun createFile(
    context: Context,
    fileLocation: File? = null,
    fileName: String? = null,
    extension: String
) = File(
    fileLocation ?: context.getExternalFilesDir(null),
    ( fileName ?: "IMG_${SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())}" ).plus(".$extension")
)


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

private const val TAG: String = "ExifUtils"

/** Transforms rotation and mirroring information into one of the [ExifInterface] constants */
fun computeExifOrientation(rotationDegrees: Int, mirrored: Boolean) = when {
    rotationDegrees == 0 && !mirrored -> ExifInterface.ORIENTATION_NORMAL
    rotationDegrees == 0 && mirrored -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
    rotationDegrees == 180 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_180
    rotationDegrees == 180 && mirrored -> ExifInterface.ORIENTATION_FLIP_VERTICAL
    rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
    rotationDegrees == 90 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_90
    rotationDegrees == 90 && mirrored -> ExifInterface.ORIENTATION_TRANSPOSE
    rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_ROTATE_270
    rotationDegrees == 270 && !mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
    else -> ExifInterface.ORIENTATION_UNDEFINED
}

/**
 * Helper function used to convert an EXIF orientation enum into a transformation matrix
 * that can be applied to a bitmap.
 *
 * @return matrix - Transformation required to properly display [Bitmap]
 */
fun decodeExifOrientation(exifOrientation: Int): Matrix {
    val matrix = Matrix()

    // Apply transformation corresponding to declared EXIF orientation
    when (exifOrientation) {
        ExifInterface.ORIENTATION_NORMAL -> Unit
        ExifInterface.ORIENTATION_UNDEFINED -> Unit
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1F, -1F)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postScale(-1F, 1F)
            matrix.postRotate(270F)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postScale(-1F, 1F)
            matrix.postRotate(90F)
        }

        // Error out if the EXIF orientation is invalid
        else -> log(TAG, "Invalid orientation: $exifOrientation")
    }

    // Return the resulting matrix
    return matrix
}

/** Standard High Definition size for pictures and video */
val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

/** Returns a [SmartSize] object for the given [Display] */
fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * Returns the largest available PREVIEW size. For more information, see:
 * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
 * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
 */
fun <T>getPreviewOutputSize(
    display: Display,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
    format: Int? = null
): Size {

    // Find which is smaller: screen or 1080p
    val screenSize = getDisplaySmartSize(display)

    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short

    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // If image format is provided, use it to determine supported sizes; else use target class
    val config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

    if (format == null)
        assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
    else
        assert(config.isOutputSupportedFor(format))

    val allSizes = if (format == null) config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // Get available sizes and sort them by area from largest to smallest
    val validSizes = allSizes
        .sortedByDescending { it.height * it.width }
        .map { SmartSize(it.width, it.height) }

    // Then, get the largest output size that is smaller or equal than our max size
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size

    //return validSizes.first { it.size.width == 176 && it.size.height == 144 }.size

}