package dissonance.cunninglinguist.fragmentry.core.data.repository

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentDao
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.domain.analysis.MotifAnalyzer
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of the repository handling CRUD.
 */
class FragmentRepositoryImpl(
    private val dao: FragmentDao
) : FragmentRepository {

    override fun getFragments(): Flow<List<FragmentEntity>> = dao.getAllFragments()

    override suspend fun getFragment(id: Long): FragmentEntity? = dao.getFragmentById(id)

    override suspend fun saveFragment(fragment: FragmentEntity): Long {
        val tokens = MotifAnalyzer.extractTokens(fragment.text)
        val tags = MotifAnalyzer.serializeTags(tokens)
        return dao.insertFragment(fragment.copy(tags = tags))
    }

    override suspend fun saveFragments(fragments: List<FragmentEntity>) {
        val processed = fragments.map { fragment ->
            val tokens = MotifAnalyzer.extractTokens(fragment.text)
            val tags = MotifAnalyzer.serializeTags(tokens)
            fragment.copy(tags = tags)
        }
        dao.insertFragments(processed)
    }

    override suspend fun updateFragment(fragment: FragmentEntity) {
        val tokens = MotifAnalyzer.extractTokens(fragment.text)
        val tags = MotifAnalyzer.serializeTags(tokens)
        dao.updateFragment(fragment.copy(tags = tags))
    }

    override suspend fun deleteFragment(fragment: FragmentEntity) = dao.deleteFragment(fragment)

    override suspend fun touchLastViewed(id: Long) = dao.updateLastViewed(id)

    override suspend fun setPinned(id: Long, isPinned: Boolean) = dao.updatePin(id, isPinned)
}
