package com.example.uniquepersoncollage

import android.graphics.Bitmap

data class ProcessedFace(
    val timeMs: Long,
    val bitmap: Bitmap,
    val detection: FaceDetectionResult,
    val embedding: FloatArray
)

data class PersonResult(
    val personId: Int,
    val faces: List<ProcessedFace>,
    val appearanceCount: Int,
    val representative: ProcessedFace
)

data class ProcessingResult(
    val people: List<PersonResult>
)