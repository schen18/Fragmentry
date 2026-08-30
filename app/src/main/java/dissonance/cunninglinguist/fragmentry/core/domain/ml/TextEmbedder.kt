package dissonance.cunninglinguist.fragmentry.core.domain.ml

/**
 * Interface for on-device semantic embedding services.
 */
interface TextEmbedder {
    /**
     * Generates a normalized semantic vector for the provided text.
     */
    suspend fun embed(text: String): Result<FloatArray>

    /**
     * Returns a fingerprint of the model used for version tracking.
     */
    fun getModelFingerprint(): String

    /**
     * Checks if the embedder is ready for inference.
     */
    suspend fun isReady(): Boolean
}
