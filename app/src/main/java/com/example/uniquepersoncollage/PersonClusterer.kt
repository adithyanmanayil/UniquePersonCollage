package com.example.uniquepersoncollage

import kotlin.math.sqrt

data class PersonCluster(
    val id: Int,
    val detections: MutableList<ProcessedFace>
)

class PersonClusterer(
    private val similarityThreshold: Float = 0.40f
) {

    fun cluster(
        detections: List<ProcessedFace>
    ): List<PersonCluster> {

        val clusters = mutableListOf<PersonCluster>()

        for (detection in detections) {

            var bestCluster: PersonCluster? = null
            var bestSimilarity = -1f

            for (cluster in clusters) {

                val similarity =
                    averageSimilarity(
                        detection.embedding,
                        cluster.detections
                    )

                if (
                    similarity >= similarityThreshold &&
                    similarity > bestSimilarity
                ) {
                    bestSimilarity = similarity
                    bestCluster = cluster
                }
            }

            if (bestCluster != null) {

                bestCluster.detections.add(detection)

            } else {

                clusters.add(
                    PersonCluster(
                        id = clusters.size,
                        detections = mutableListOf(detection)
                    )
                )
            }
        }

        return clusters
    }

    private fun averageSimilarity(
        embedding: FloatArray,
        detections: List<ProcessedFace>
    ): Float {

        if (detections.isEmpty()) {
            return 0f
        }

        var total = 0f

        for (detection in detections) {

            total += cosineSimilarity(
                embedding,
                detection.embedding
            )
        }

        return total / detections.size
    }

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray
    ): Float {

        require(a.size == b.size) {
            "Embedding sizes do not match: ${a.size} vs ${b.size}"
        }

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {

            dot += a[i] * b[i]

            normA += a[i] * a[i]

            normB += b[i] * b[i]
        }

        if (normA == 0f || normB == 0f) {
            return 0f
        }

        return dot / (
                sqrt(normA) * sqrt(normB)
                )
    }
}