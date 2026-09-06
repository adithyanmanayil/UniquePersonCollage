package com.example.uniquepersoncollage

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun testEmbeddingModel(context: Context) {

    withContext(Dispatchers.Default) {

        val model = FaceEmbeddingModel(context)

        try {
            // Temporary test image from the model assets.
            // We'll replace this with an actual detected face next.
            val inputStream =
                context.assets.open("test_face.jpg")

            val bitmap =
                BitmapFactory.decodeStream(inputStream)

            inputStream.close()

            requireNotNull(bitmap) {
                "Could not load test_face.jpg"
            }

            val embedding =
                model.getEmbedding(bitmap)

            println("Embedding size: ${embedding.size}")

            println(
                "First values: ${
                    embedding.take(5)
                }"
            )

        } finally {
            model.close()
        }
    }
}