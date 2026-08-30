package dissonance.cunninglinguist.fragmentry.core.data.backup

import dissonance.cunninglinguist.fragmentry.core.data.local.FragmentEntity
import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import dissonance.cunninglinguist.fragmentry.core.domain.repository.FragmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import java.io.OutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/**
 * Handles the serialization and deserialization of the memory field.
 */
class BackupManager(
    private val repository: FragmentRepository
) {
    companion object {
        const val CURRENT_VERSION = 2
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Exports all fragments to the provided stream.
     */
    suspend fun export(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allFragments = repository.getFragments().first()
        val portableFragments = allFragments.map { entity ->
            PortableFragment(
                text = entity.text,
                createdAt = entity.createdAt,
                lastViewedAt = entity.lastViewedAt,
                embeddingBase64 = entity.embedding?.let(::encodeEmbedding),
                modelFingerprint = entity.modelFingerprint,
                tags = entity.tags,
                isPinned = entity.isPinned
            )
        }

        val backup = BackupData(
            version = CURRENT_VERSION,
            exportTime = System.currentTimeMillis(),
            fragments = portableFragments,
        )
        val jsonString = json.encodeToString(backup)

        outputStream.use { it.write(jsonString.toByteArray()) }
    }

    /**
     * Restores fragments from the provided stream.
     * Note: This currently merges with existing data to prevent loss.
     */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    suspend fun restore(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val backup = try {
            inputStream.use { stream ->
                json.decodeFromStream<BackupData>(stream)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup format", e)
        }

        if (backup.version > CURRENT_VERSION) {
            throw IllegalArgumentException("Backup version ${backup.version} is not supported (Current: $CURRENT_VERSION)")
        }

        if (backup.fragments.isEmpty()) return@withContext

        val existingFragments = repository.getFragments().first()
        val toInsert = mutableListOf<FragmentEntity>()

        for (portable in backup.fragments) {
            if (portable.text.isBlank()) continue // Skip corrupted entries

            // Smart De-duplication: Check if a fragment with the same text and timestamp exists
            val isDuplicate = existingFragments.any { fragment ->
                fragment.text == portable.text && fragment.createdAt == portable.createdAt
            }
            if (isDuplicate) continue

            toInsert.add(
                FragmentEntity(
                    text = portable.text,
                    createdAt = portable.createdAt,
                    lastViewedAt = portable.lastViewedAt,
                    embedding = portable.embeddingBase64?.let(::decodeEmbedding)
                        ?: portable.embedding?.toFloatArray(),
                    modelFingerprint = portable.modelFingerprint,
                    tags = portable.tags,
                    isPinned = portable.isPinned
                )
            )
        }

        if (toInsert.isNotEmpty()) {
            repository.saveFragments(toInsert)
        }
    }

    /**
     * Migrates fragments from a text file, where chunks are separated by ___.
     * Skips chunks whose text already exists anywhere in the field, so the
     * same file can safely be migrated twice. Returns the imported count.
     */
    suspend fun migrate(
        inputStream: InputStream,
        textEmbedder: TextEmbedder,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val text = inputStream.bufferedReader().use { it.readText() }
        val chunks = text.split(Regex("\\n___\\n|\\r\\n___\\r\\n|\\n___|___\\n|___"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (chunks.isEmpty()) return@withContext 0

        val existingTexts = repository.getFragments().first()
            .map { it.text.trim() }
            .toHashSet()

        var imported = 0
        val total = chunks.size
        chunks.forEachIndexed { index, chunk ->
            if (chunk !in existingTexts) {
                val embedding = textEmbedder.embed(chunk).getOrNull()
                repository.saveFragment(
                    FragmentEntity(
                        text = chunk,
                        embedding = embedding,
                        modelFingerprint = if (embedding != null) textEmbedder.getModelFingerprint() else null
                    )
                )
                existingTexts.add(chunk)
                imported++
            }
            onProgress(index + 1, total)
        }
        imported
    }

    /** Encodes an embedding as Base64 of its little-endian BLOB form. */
    private fun encodeEmbedding(embedding: FloatArray): String {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (value in embedding) buffer.putFloat(value)
        return Base64.getEncoder().encodeToString(buffer.array())
    }

    /** Returns null on malformed input; the backfill pass repairs such rows. */
    private fun decodeEmbedding(base64: String): FloatArray? = try {
        val bytes = Base64.getDecoder().decode(base64)
        if (bytes.size % 4 != 0) null
        else ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).let { buffer ->
            FloatArray(bytes.size / 4) { buffer.float }
        }
    } catch (e: IllegalArgumentException) {
        null
    }
}
