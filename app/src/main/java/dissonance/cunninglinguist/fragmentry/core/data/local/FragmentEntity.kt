package dissonance.cunninglinguist.fragmentry.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core domain entity representing a captured poetic fragment.
 */
@Entity(
    tableName = "fragments",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isPinned"])
    ]
)
data class FragmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastViewedAt: Long = System.currentTimeMillis(),
    val embedding: FloatArray? = null,
    val modelFingerprint: String? = null,
    val tags: String? = null,
    val isPinned: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FragmentEntity
        if (id != other.id) return false
        if (text != other.text) return false
        if (createdAt != other.createdAt) return false
        if (lastViewedAt != other.lastViewedAt) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (modelFingerprint != other.modelFingerprint) return false
        if (tags != other.tags) return false
        if (isPinned != other.isPinned) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastViewedAt.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + (modelFingerprint?.hashCode() ?: 0)
        result = 31 * result + (tags?.hashCode() ?: 0)
        result = 31 * result + isPinned.hashCode()
        return result
    }
}
