package com.example.uniquepersoncollage

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer

class FaceEmbeddingModel(
    context: Context
) {

    private val environment = OrtEnvironment.getEnvironment()

    private val session: OrtSession

    init {

        val modelFile = File(
            context.filesDir,
            "face_embedding.onnx"
        )

        if (!modelFile.exists()) {

            context.assets
                .open("face_embedding.onnx")
                .use { input ->

                    modelFile.outputStream()
                        .use { output ->

                            input.copyTo(output)
                        }
                }
        }

        session = environment.createSession(
            modelFile.absolutePath,
            OrtSession.SessionOptions()
        )
    }

    fun getEmbedding(faceBitmap: Bitmap): FloatArray {

        val inputBitmap = Bitmap.createScaledBitmap(
            faceBitmap,
            112,
            112,
            true
        )

        val input = FloatArray(
            1 * 112 * 112 * 3
        )

        var index = 0

        for (y in 0 until 112) {

            for (x in 0 until 112) {

                val pixel =
                    inputBitmap.getPixel(x, y)

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                input[index++] =
                    (r - 127.5f) / 128.0f

                input[index++] =
                    (g - 127.5f) / 128.0f

                input[index++] =
                    (b - 127.5f) / 128.0f
            }
        }

        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(
                1,
                112,
                112,
                3
            )
        )

        val inputName =
            session.inputNames.first()

        val result = session.run(
            mapOf(
                inputName to tensor
            )
        )

        val outputValue =
            result[0].value

        val embedding: FloatArray

        when (outputValue) {

            is FloatArray -> {
                embedding = outputValue
            }

            is Array<*> -> {

                val first =
                    outputValue.firstOrNull()

                if (first is FloatArray) {
                    embedding = first
                } else {
                    throw IllegalStateException(
                        "Unexpected ONNX output type: ${first?.javaClass}"
                    )
                }
            }

            else -> {
                throw IllegalStateException(
                    "Unexpected ONNX output type: ${outputValue?.javaClass}"
                )
            }
        }

        tensor.close()
        result.close()

        normalize(embedding)

        return embedding
    }

    private fun normalize(
        vector: FloatArray
    ) {

        var sum = 0.0f

        for (value in vector) {
            sum += value * value
        }

        val norm =
            kotlin.math.sqrt(sum)

        if (norm > 0f) {

            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }

    fun close() {
        session.close()
    }
}