package dissonance.cunninglinguist.fragmentry.core.domain.analysis

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity

/**
 * Detects recurring motifs (keywords/symbols) across fragments.
 */
object MotifAnalyzer {
    private val stopWords = setOf(
        "the", "and", "a", "of", "to", "in", "is", "it", "with", "that", "was", "for", "on", "as", "at", "by", "an", "be", "this", "which",
        "from", "they", "we", "he", "she", "has", "have", "had", "been", "were", "are", "will", "would", "should", "could", "there", "their"
    )

    /**
     * Identifies potential motifs in a fragment that also appear elsewhere in the library.
     * Uses pre-computed tags for performance.
     */
    fun findSharedMotifs(focus: FragmentEntity, library: List<FragmentEntity>): Set<String> {
        val focusTokens = deserializeTags(focus.tags)
        if (focusTokens.isEmpty()) return emptySet()

        val libraryTokens = library
            .filter { it.id != focus.id }
            .flatMap { deserializeTags(it.tags) }
            .toSet()

        return focusTokens.intersect(libraryTokens)
    }

    fun extractTokens(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.length > 3 && it !in stopWords }
            .toSet()
    }

    /**
     * Serializes a set of tokens into a single searchable string.
     */
    fun serializeTags(tokens: Set<String>): String {
        return tokens.joinToString(separator = " ") { "[$it]" }
    }

    /**
     * Deserializes a tag string into a set of tokens.
     */
    fun deserializeTags(tagString: String?): Set<String> {
        if (tagString.isNullOrBlank()) return emptySet()
        return tagString.split(" ")
            .map { it.removeSurrounding("[", "]") }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
