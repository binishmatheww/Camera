package com.binishmatheww.camera.models

/** Helper class used as a data holder for each selectable camera format item */
data class CameraProp(
    val cameraId: String,
    val orientationId: Int,
    val formatId: Int,
    val formatName: String,
    val outputSizes: List<SmartSize>
)