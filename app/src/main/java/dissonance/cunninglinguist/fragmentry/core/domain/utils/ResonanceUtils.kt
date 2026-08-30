package dissonance.cunninglinguist.fragmentry.core.domain.utils

import kotlin.math.sqrt

/**
 * Utility functions for semantic resonance math.
 * Optimized for local-only computation on Android.
 */
object ResonanceUtils {

    /**
     * Calculates the Dot Product of two normalized vectors.
     * Since our TFLite model pre-normalizes vectors to unit length,
     * this is mathematically equivalent to Cosine Similarity but faster.
     */
    fun normalizedDotProduct(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0f
        var dotProduct = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
        }
        return dotProduct
    }
}
