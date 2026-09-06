package com.example.uniquepersoncollage

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

data class RepresentativeShot(
    val personId: Int,
    val detection: ProcessedFace,
    val score: Float
)

class RepresentativeShotSelector {

    fun select(
        clusters: List<PersonCluster>
    ): List<RepresentativeShot> {

        return clusters.map { cluster ->

            val best = cluster.detections
                .map { detection ->
                    detection to scoreDetection(detection)
                }
                .maxByOrNull { it.second }
                ?: error(
                    "Person ${cluster.id} has no detections"
                )

            RepresentativeShot(
                personId = cluster.id,
                detection = best.first,
                score = best.second
            )
        }
    }

    private fun scoreDetection(
        detection: ProcessedFace
    ): Float {

        val face = detection.detection
        val bitmap = detection.bitmap

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val faceWidth =
            face.boundingBox.width().toFloat()

        val faceHeight =
            face.boundingBox.height().toFloat()

        // Larger face = generally better candidate.
        val faceArea =
            (faceWidth * faceHeight) /
                    (bitmapWidth * bitmapHeight)

        val sizeScore =
            (faceArea * 10f)
                .coerceIn(0f, 1f)

        // Prefer faces closer to the centre.
        val faceCenterX =
            face.boundingBox.centerX().toFloat()

        val faceCenterY =
            face.boundingBox.centerY().toFloat()

        val frameCenterX =
            bitmapWidth / 2f

        val frameCenterY =
            bitmapHeight / 2f

        val distanceX =
            abs(faceCenterX - frameCenterX) /
                    frameCenterX

        val distanceY =
            abs(faceCenterY - frameCenterY) /
                    frameCenterY

        val centerScore =
            (
                    1f -
                            (distanceX + distanceY) / 2f
                    )
                .coerceIn(0f, 1f)

        // Measure sharpness of the detected face.
        val sharpness =
            calculateSharpness(
                bitmap,
                face.boundingBox
            )

        /*
         * Convert sharpness into a 0..1 score.
         *
         * 2 = very soft
         * 5 = reasonably sharp
         * 15+ = strong candidate
         */
        val sharpnessScore =
            ((sharpness - 2f) / 13f)
                .coerceIn(0f, 1f)

        /*
         * Current representative-shot score:
         *
         * 50% sharpness
         * 30% face size
         * 20% position
         */
        return (
                sharpnessScore * 0.50f +
                        sizeScore * 0.30f +
                        centerScore * 0.20f
                )
    }

    private fun calculateSharpness(
        bitmap: Bitmap,
        boundingBox: android.graphics.Rect
    ): Float {

        val left =
            boundingBox.left.coerceIn(
                0,
                bitmap.width - 1
            )

        val top =
            boundingBox.top.coerceIn(
                0,
                bitmap.height - 1
            )

        val right =
            boundingBox.right.coerceIn(
                left + 1,
                bitmap.width
            )

        val bottom =
            boundingBox.bottom.coerceIn(
                top + 1,
                bitmap.height
            )

        val width = right - left
        val height = bottom - top

        // Downsample for speed.
        val scale =
            minOf(
                1f,
                100f / width.toFloat(),
                100f / height.toFloat()
            )

        val sampleWidth =
            (width * scale)
                .toInt()
                .coerceAtLeast(3)

        val sampleHeight =
            (height * scale)
                .toInt()
                .coerceAtLeast(3)

        val crop = Bitmap.createBitmap(
            bitmap,
            left,
            top,
            width,
            height
        )

        val small = Bitmap.createScaledBitmap(
            crop,
            sampleWidth,
            sampleHeight,
            true
        )

        crop.recycle()

        val gray = FloatArray(
            sampleWidth * sampleHeight
        )

        var index = 0

        for (y in 0 until sampleHeight) {

            for (x in 0 until sampleWidth) {

                val pixel =
                    small.getPixel(x, y)

                val r =
                    (pixel shr 16) and 0xFF

                val g =
                    (pixel shr 8) and 0xFF

                val b =
                    pixel and 0xFF

                gray[index++] =
                    0.299f * r +
                            0.587f * g +
                            0.114f * b
            }
        }

        small.recycle()

        /*
         * Simple Laplacian variance.
         * Higher value = more high-frequency detail
         * = sharper image.
         */
        var sum = 0f
        var sumSquared = 0f
        var count = 0

        for (y in 1 until sampleHeight - 1) {

            for (x in 1 until sampleWidth - 1) {

                val center =
                    gray[y * sampleWidth + x]

                val up =
                    gray[(y - 1) * sampleWidth + x]

                val down =
                    gray[(y + 1) * sampleWidth + x]

                val leftPixel =
                    gray[y * sampleWidth + x - 1]

                val rightPixel =
                    gray[y * sampleWidth + x + 1]

                val laplacian =
                    up +
                            down +
                            leftPixel +
                            rightPixel -
                            4f * center

                sum += laplacian
                sumSquared +=
                    laplacian * laplacian

                count++
            }
        }

        if (count == 0) {
            return 0f
        }

        val mean =
            sum / count

        val variance =
            (sumSquared / count) -
                    (mean * mean)

        return sqrt(
            variance.coerceAtLeast(0f)
        )
    }
}