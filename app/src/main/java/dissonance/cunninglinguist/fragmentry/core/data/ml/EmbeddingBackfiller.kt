package dissonance.cunninglinguist.fragmentry.core.data.ml

import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repairs fragments whose embeddings are missing or were produced by a
 * different model. Runs silently at startup once the embedder settles, so a
 * capture that raced the model load still gains resonance later.
 */
class EmbeddingBackfiller(
    private val repository: FragmentRepository,
    private val textEmbedder: TextEmbedder,
) {

    /**
     * Re-embeds every fragment lacking a current-model embedding.
     * Returns the number of fragments repaired.
     */
    suspend fun backfillMissing(): Int = withContext(Dispatchers.Default) {
        if (!textEmbedder.isReady()) return@withContext 0

        val fingerprint = textEmbedder.getModelFingerprint()
        val pending = repository.getFragments().first().filter {
            it.text.isNotBlank() && (it.embedding == null || it.modelFingerprint != fingerprint)
        }
        if (pending.isEmpty()) return@withContext 0

        var repaired = 0
        for (fragment in pending) {
            val embedding = textEmbedder.embed(fragment.text).getOrNull() ?: continue
            repository.updateFragment(
                fragment.copy(embedding = embedding, modelFingerprint = fingerprint)
            )
            repaired++
        }
        repaired
    }
}
