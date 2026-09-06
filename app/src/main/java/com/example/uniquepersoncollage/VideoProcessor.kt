package com.example.uniquepersoncollage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProcessingProgress(
    val processedFrames: Int,
    val totalFrames: Int,
    val facesFound: Int
) {
    val percent: Int
        get() = if (totalFrames == 0) {
            0
        } else {
            (processedFrames * 100 / totalFrames).coerceIn(0, 100)
        }
}

class VideoProcessor(
    private val context: Context
) {

    suspend fun process(
        uri: Uri,
        onProgress: (ProcessingProgress) -> Unit
    ): List<ProcessedFace> = withContext(Dispatchers.Default) {

        val extractor = VideoFrameExtractor(context)
        val detector = FaceDetector()
        val embeddingModel = FaceEmbeddingModel(context)

        try {

            val frames = extractor.extractFrames(
                uri = uri,
                intervalMs = 200L
            )

            val results = mutableListOf<ProcessedFace>()

            frames.forEachIndexed { index, frameData ->

                val timeMs = frameData.first
                val bitmap = frameData.second

                val faces = detector.detect(
                    bitmap = bitmap,
                    timeMs = timeMs
                )

                for (face in faces) {

                    val box = face.boundingBox

                    val left = box.left.coerceIn(
                        0,
                        bitmap.width - 1
                    )

                    val top = box.top.coerceIn(
                        0,
                        bitmap.height - 1
                    )

                    val right = box.right.coerceIn(
                        left + 1,
                        bitmap.width
                    )

                    val bottom = box.bottom.coerceIn(
                        top + 1,
                        bitmap.height
                    )

                    val faceCrop = Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                    )

                    val embedding =
                        embeddingModel.getEmbedding(faceCrop)

                    results.add(
                        ProcessedFace(
                            timeMs = timeMs,
                            bitmap = bitmap,
                            detection = face,
                            embedding = embedding
                        )
                    )

                    faceCrop.recycle()
                }

                onProgress(
                    ProcessingProgress(
                        processedFrames = index + 1,
                        totalFrames = frames.size,
                        facesFound = results.size
                    )
                )
            }

            results

        } finally {
            detector.close()
            embeddingModel.close()
        }
    }
}