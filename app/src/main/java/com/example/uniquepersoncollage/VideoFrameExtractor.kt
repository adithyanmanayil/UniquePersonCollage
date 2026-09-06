package com.example.uniquepersoncollage

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameExtractor(
    private val context: Context
) {

    fun extractFrames(
        uri: Uri,
        intervalMs: Long = 200L
    ): List<Pair<Long, Bitmap>> {

        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            val durationMs =
                retriever
                    .extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                    ?.toLongOrNull()
                    ?: 0L

            val frames = mutableListOf<Pair<Long, Bitmap>>()

            var timeMs = 0L

            while (timeMs < durationMs) {

                val bitmap = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    frames.add(timeMs to bitmap)
                }

                timeMs += intervalMs
            }

            return frames

        } finally {
            retriever.release()
        }
    }
}