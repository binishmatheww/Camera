package com.binishmatheww.camera.models

import android.util.Size
import kotlin.math.max
import kotlin.math.min

/** Helper class used to pre-compute shortest and longest sides of a [Size] */
class SmartSize(width: Int, height: Int) {
    val size = Size(width, height)
    val long = max(size.width, size.height)
    val short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}