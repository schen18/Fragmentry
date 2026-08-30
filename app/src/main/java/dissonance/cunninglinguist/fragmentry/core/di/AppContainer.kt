package dissonance.cunninglinguist.fragmentry.core.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentDatabase
import dissonance.cunninglinguist.fragmentry.core.data.ml.EmbeddingBackfiller
import dissonance.cunninglinguist.fragmentry.core.data.ml.TFLiteTextEmbedder
import dissonance.cunninglinguist.fragmentry.core.data.repository.FragmentRepositoryImpl
import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository

import dissonance.cunninglinguist.fragmentry.core.data.backup.BackupManager
import dissonance.cunninglinguist.fragmentry.core.domain.analysis.SemanticMotifGrouper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import android.content.SharedPreferences

/**
 * Dependency container for the Fragmentry application.
 * Handles the singleton lifecycle of persistence and ML services.
 */
class AppContainer(private val context: Context) {
    
    // IO-based so the embedder's lazy init (mmap of the model, vocab parsing)
    // never lands on the main thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("fragmentry_prefs", Context.MODE_PRIVATE)
    }

    private val database: FragmentDatabase by lazy {
        Room.databaseBuilder(
            context,
            FragmentDatabase::class.java,
            "fragmentry.db"
        )
        .addMigrations(FragmentDatabase.MIGRATION_1_2)
        // Upgrades must always go through real migrations; only a downgrade
        // (reinstalling an older build) may rebuild the schema from scratch.
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }

    val repository: FragmentRepository by lazy {
        FragmentRepositoryImpl(database.fragmentDao())
    }

    val backupManager: BackupManager by lazy {
        BackupManager(repository)
    }

    private val embedderDelegate = lazy {
        TFLiteTextEmbedder(
            context = context,
            modelPath = "all-MiniLM-L6-v2-quant.tflite",
            vocabPath = "vocab.txt",
            scope = scope
        )
    }
    val textEmbedder: TextEmbedder by embedderDelegate

    val embeddingBackfiller: EmbeddingBackfiller by lazy {
        EmbeddingBackfiller(repository, textEmbedder)
    }

    // Singleton so the tag-embedding cache survives configuration changes.
    val motifGrouper: SemanticMotifGrouper by lazy {
        SemanticMotifGrouper(textEmbedder)
    }

    /**
     * Silently repairs fragments that were saved without a usable embedding.
     */
    fun startEmbeddingBackfill() {
        scope.launch(Dispatchers.Default) {
            if (!textEmbedder.isReady()) return@launch
            runCatching { embeddingBackfiller.backfillMissing() }
                .onFailure { Log.w("AppContainer", "Embedding backfill failed", it) }
        }
    }

    /**
     * Safely releases heavy resources.
     */
    fun shutdown() {
        if (embedderDelegate.isInitialized()) {
            (embedderDelegate.value as TFLiteTextEmbedder).close()
        }
    }
}
