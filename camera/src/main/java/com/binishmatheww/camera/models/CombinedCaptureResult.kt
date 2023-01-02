package com.binishmatheww.camera.models

import android.hardware.camera2.CaptureResult
import android.media.Image
import java.io.Closeable


/** Helper data class used to hold capture metadata with their associated image */
data class CombinedCaptureResult(
    val image: Image,
    val metadata: CaptureResult,
    val orientation: Int,
    val format: Int
) : Closeable {
    override fun close() = image.close()
}