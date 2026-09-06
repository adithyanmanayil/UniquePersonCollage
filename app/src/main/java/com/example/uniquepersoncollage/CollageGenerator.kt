package com.example.uniquepersoncollage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlin.math.min

object CollageGenerator {

    fun createCollage(
        shots: List<RepresentativeShot>
    ): Bitmap {

        require(shots.isNotEmpty()) {
            "Cannot create collage with no people"
        }

        val tileWidth = 500
        val tileHeight = 700

        val columns = 2
        val rows =
            (shots.size + columns - 1) / columns

        val spacing = 24
        val padding = 32

        val collageWidth =
            padding * 2 +
                    columns * tileWidth +
                    (columns - 1) * spacing

        val collageHeight =
            padding * 2 +
                    rows * tileHeight +
                    (rows - 1) * spacing

        val collage = createBitmap(
            collageWidth,
            collageHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(collage)

        canvas.drawColor(
            android.graphics.Color.WHITE
        )

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
        )

        shots.forEachIndexed { index, shot ->

            val column =
                index % columns

            val row =
                index / columns

            val left =
                padding +
                        column * (tileWidth + spacing)

            val top =
                padding +
                        row * (tileHeight + spacing)

            val destination = RectF(
                left.toFloat(),
                top.toFloat(),
                (left + tileWidth).toFloat(),
                (top + tileHeight).toFloat()
            )

            drawCenterCrop(
                canvas = canvas,
                bitmap = shot.detection.bitmap,
                destination = destination,
                paint = paint
            )
        }

        return collage
    }

    private fun drawCenterCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF,
        paint: Paint
    ) {

        val sourceWidth =
            bitmap.width.toFloat()

        val sourceHeight =
            bitmap.height.toFloat()

        val destinationWidth =
            destination.width()

        val destinationHeight =
            destination.height()

        val sourceAspect =
            sourceWidth / sourceHeight

        val destinationAspect =
            destinationWidth / destinationHeight

        val sourceRect: android.graphics.Rect

        if (sourceAspect > destinationAspect) {

            val newWidth =
                sourceHeight * destinationAspect

            val left =
                ((sourceWidth - newWidth) / 2f).toInt()

            sourceRect = android.graphics.Rect(
                left,
                0,
                (left + newWidth).toInt(),
                bitmap.height
            )

        } else {

            val newHeight =
                sourceWidth / destinationAspect

            val top =
                ((sourceHeight - newHeight) / 2f).toInt()

            sourceRect = android.graphics.Rect(
                0,
                top,
                bitmap.width,
                (top + newHeight).toInt()
            )
        }

        canvas.drawBitmap(
            bitmap,
            sourceRect,
            destination,
            paint
        )
    }
}