package dissonance.cunninglinguist.fragmentry.features.capture

/**
 * State model for the Spark capture void.
 */
data class SparkState(
    val text: String = "",
    val isSaving: Boolean = false
)
