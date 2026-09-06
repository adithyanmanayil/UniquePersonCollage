package com.example.uniquepersoncollage

import android.graphics.Rect

data class FaceDetectionResult(
    val timeMs: Long,
    val boundingBox: Rect,

    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,

    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)