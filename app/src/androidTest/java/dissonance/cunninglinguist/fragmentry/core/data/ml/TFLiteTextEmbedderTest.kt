package dissonance.cunninglinguist.fragmentry.core.data.ml

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class TFLiteTextEmbedderTest {

    private lateinit var embedder: TFLiteTextEmbedder
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        embedder = TFLiteTextEmbedder(
            context = context,
            modelPath = "all-MiniLM-L6-v2-quant.tflite",
            vocabPath = "vocab.txt"
        )
    }

    @Test
    fun testEmbeddingGeneration() = runBlocking {
        val text = "The quick brown fox jumps over the lazy dog."
        val result = embedder.embed(text)
        
        assertTrue("Embedding should be successful", result.isSuccess)
        val embedding = result.getOrThrow()
        
        assertEquals("Embedding dimension should be 384", 384, embedding.size)
        
        // Check normalization
        var sumSquares = 0f
        for (v in embedding) sumSquares += v * v
        val magnitude = sqrt(sumSquares.toDouble()).toFloat()
        
        assertTrue("Embedding should be normalized (magnitude ~1.0, got $magnitude)", 
            magnitude > 0.99f && magnitude < 1.01f)
    }

    @Test
    fun testConsistency() = runBlocking {
        val text = "Semantic similarity test."
        val result1 = embedder.embed(text).getOrThrow()
        val result2 = embedder.embed(text).getOrThrow()
        
        for (i in result1.indices) {
            assertEquals("Embeddings should be consistent at index $i", result1[i], result2[i], 1e-6f)
        }
    }
}
