package dissonance.cunninglinguist.fragmentry.core.domain.retrieval

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.domain.utils.ResonanceUtils
import kotlin.random.Random

/**
 * A ranking engine for poetic fragments that prioritizes "Resonance" 
 * over exact matching. Uses a hybrid scoring formula.
 */
class ResonanceRanker(
    private val similarityWeight: Float = 0.7f,
    private val patinaWeight: Float = 0.2f,
    private val surpriseWeight: Float = 0.1f,
    private val duplicateThreshold: Float = 0.98f
) {
    /**
     * Ranks candidates based on their resonance with a query vector.
     * 
     * Formula: (Sim * W_sim) + (Patina * W_patina) + (Surprise * W_surprise)
     */
    fun rank(
        query: FloatArray,
        candidates: List<FragmentEntity>
    ): List<FragmentEntity> {
        return candidates
            .filter { it.embedding != null }
            .map { fragment ->
                // 1. Semantic Resonance
                val sim = ResonanceUtils.normalizedDotProduct(query, fragment.embedding!!)
                
                // 2. Patina (Temporal depth)
                val patina = calculatePatina(fragment.createdAt)
                
                // 3. Surprise (Controlled semantic drift - Seeded for stability)
                val surpriseSeed = (fragment.id xor (fragment.text.hashCode().toLong())).toInt()
                val surprise = Random(surpriseSeed).nextFloat()

                val score = (sim * similarityWeight) + 
                            (patina * patinaWeight) + 
                            (surprise * surpriseWeight)
                
                fragment to Pair(score, sim)
            }
            // Filter out near-duplicates to preserve symbolic variety
            .filter { it.second.second < duplicateThreshold }
            .sortedByDescending { it.second.first }
            .map { it.first }
    }

    private fun calculatePatina(createdAt: Long): Float {
        val ageMillis = System.currentTimeMillis() - createdAt
        val ageDays = ageMillis / (1000 * 60 * 60 * 24).toFloat()
        // Surfaces older fragments; caps at 1.0 (approx 1 year)
        return (ageDays / 365f).coerceIn(0f, 1f)
    }
}
