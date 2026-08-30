package dissonance.cunninglinguist.fragmentry.core.data.backup

import kotlinx.serialization.Serializable

/**
 * Portable representation of a fragment for backup and migration.
 */
@Serializable
data class PortableFragment(
    val text: String,
    val createdAt: Long,
    val lastViewedAt: Long,
    // v1 format, kept for reading older backups.
    val embedding: List<Float>? = null,
    // v2 format: Base64 of the little-endian BLOB, roughly half the size.
    val embeddingBase64: String? = null,
    val modelFingerprint: String? = null,
    val tags: String? = null,
    val isPinned: Boolean = false
)

/**
 * Root backup structure.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val fragments: List<PortableFragment>
)
