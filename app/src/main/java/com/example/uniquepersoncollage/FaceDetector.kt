package com.example.uniquepersoncollage

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FaceDetector {

    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setLandmarkMode(
                    FaceDetectorOptions.LANDMARK_MODE_NONE
                )
                .setClassificationMode(
                    FaceDetectorOptions.CLASSIFICATION_MODE_ALL
                )
                .setMinFaceSize(0.10f)
                .build()
        )

    suspend fun detect(
        bitmap: Bitmap,
        timeMs: Long
    ): List<FaceDetectionResult> =
        suspendCancellableCoroutine { continuation ->

            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->

                    val results = faces.map { face ->

                        FaceDetectionResult(
                            timeMs = timeMs,
                            boundingBox = face.boundingBox,

                            headEulerAngleY =
                                face.headEulerAngleY,

                            headEulerAngleZ =
                                face.headEulerAngleZ,

                            leftEyeOpenProbability =
                                face.leftEyeOpenProbability,

                            rightEyeOpenProbability =
                                face.rightEyeOpenProbability,

                            smilingProbability =
                                face.smilingProbability
                        )
                    }

                    continuation.resume(results)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }

    fun close() {
        detector.close()
    }
}