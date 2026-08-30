package dissonance.cunninglinguist.fragmentry.core.data.local

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance converter to store FloatArray embeddings as BLOBs.
 */
class EmbeddingConverter {
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val byteBuffer = ByteBuffer.allocate(array.size * 4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val byteBuffer = ByteBuffer.wrap(bytes)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.float
        }
        return floatArray
    }
}
