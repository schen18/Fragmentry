package dissonance.cunninglinguist.fragmentry.features.explore

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity

/**
 * State model for the exploratory memory field.
 */
data class FieldState(
    val fragments: List<FragmentEntity> = emptyList(),
    val groupedFragments: Map<String?, List<FragmentEntity>> = emptyMap(),
    val expandedMotifs: Set<String?> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)
