package dissonance.cunninglinguist.fragmentry.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main persistence hub for Fragmentry.
 */
@Database(entities = [FragmentEntity::class], version = 2, exportSchema = true)
@TypeConverters(EmbeddingConverter::class)
abstract class FragmentDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao

    companion object {
        /**
         * v1 -> v2 only added indices on createdAt and isPinned; the columns
         * are unchanged, so every thought survives the upgrade in place.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fragments_createdAt` ON `fragments` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fragments_isPinned` ON `fragments` (`isPinned`)")
            }
        }
    }
}
