package dissonance.cunninglinguist.fragmentry.core.domain.repository

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain bridge for fragment data operations and semantic retrieval.
 */
interface FragmentRepository {
    fun getFragments(): Flow<List<FragmentEntity>>
    suspend fun getFragment(id: Long): FragmentEntity?
    suspend fun saveFragment(fragment: FragmentEntity): Long
    suspend fun saveFragments(fragments: List<FragmentEntity>)
    suspend fun updateFragment(fragment: FragmentEntity)
    suspend fun deleteFragment(fragment: FragmentEntity)
    suspend fun touchLastViewed(id: Long)
    suspend fun setPinned(id: Long, isPinned: Boolean)
}
