package dissonance.cunninglinguist.fragmentry.core.domain.analysis

import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.utils.ResonanceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Merges motif tags that resonate semantically: "dream" and "dreams" embed
 * nearly identically, so they should gather into one constellation instead
 * of splitting the thread. Tags are clustered by embedding similarity; the
 * cluster's most frequent surface form becomes its representative.
 */
class SemanticMotifGrouper(
    private val textEmbedder: TextEmbedder,
    // Tuned for inflection-level merging: MiniLM gives singular/plural and
    // conjugation pairs ~0.85+, while unrelated words sit well below 0.6.
    private val resonanceThreshold: Float = 0.85f,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val cacheLock = Any()
    private val embeddingCache = HashMap<String, FloatArray>()

    /**
     * Returns tag -> representative-tag for every tag in [frequencies].
     * Tags are processed most-frequent-first; each joins the first existing
     * leader whose embedding resonates above the threshold, or becomes a new
     * leader. A tag whose embedding is unavailable stands alone.
     */
    suspend fun buildCanonicalMap(frequencies: Map<String, Int>): Map<String, String> =
        withContext(computeDispatcher) {
            if (frequencies.size <= 1) {
                return@withContext frequencies.keys.associateWith { it }
            }

            val orderedTags = frequencies.entries
                .sortedByDescending { it.value }
                .map { it.key }

            data class Leader(val tag: String, val embedding: FloatArray)

            val leaders = mutableListOf<Leader>()
            val canonical = HashMap<String, String>(frequencies.size)

            for (tag in orderedTags) {
                val embedding = embeddingOf(tag)
                if (embedding == null) {
                    canonical[tag] = tag
                    continue
                }
                val leader = leaders.firstOrNull {
                    ResonanceUtils.normalizedDotProduct(it.embedding, embedding) >= resonanceThreshold
                }
                if (leader != null) {
                    canonical[tag] = leader.tag
                } else {
                    leaders.add(Leader(tag, embedding))
                    canonical[tag] = tag
                }
            }
            canonical
        }

    private suspend fun embeddingOf(tag: String): FloatArray? {
        synchronized(cacheLock) { embeddingCache[tag] }?.let { return it }
        val embedding = textEmbedder.embed(tag).getOrNull() ?: return null
        if (embedding.isEmpty()) return null
        synchronized(cacheLock) { embeddingCache[tag] = embedding }
        return embedding
    }
}
