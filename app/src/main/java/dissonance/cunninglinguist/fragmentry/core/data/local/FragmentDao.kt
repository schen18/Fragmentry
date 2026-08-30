package dissonance.cunninglinguist.fragmentry.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for fragments, supporting reactive streams and semantic queries.
 */
@Dao
interface FragmentDao {
    @Query("SELECT * FROM fragments ORDER BY createdAt DESC")
    fun getAllFragments(): Flow<List<FragmentEntity>>

    @Query("SELECT * FROM fragments WHERE id = :id")
    suspend fun getFragmentById(id: Long): FragmentEntity?

    @Query("SELECT * FROM fragments WHERE id IN (:ids)")
    suspend fun getFragmentsByIds(ids: List<Long>): List<FragmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragment(fragment: FragmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragments(fragments: List<FragmentEntity>)

    @Update
    suspend fun updateFragment(fragment: FragmentEntity)

    @Delete
    suspend fun deleteFragment(fragment: FragmentEntity)

    @Query("UPDATE fragments SET lastViewedAt = :timestamp WHERE id = :id")
    suspend fun updateLastViewed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE fragments SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePin(id: Long, isPinned: Boolean)
}
