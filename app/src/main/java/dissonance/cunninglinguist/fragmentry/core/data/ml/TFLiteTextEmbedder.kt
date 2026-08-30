package dissonance.cunninglinguist.fragmentry.core.data.ml

import android.content.Context
import android.util.Log
import dissonance.cunninglinguist.fragmentry.core.domain.ml.TextEmbedder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.sqrt

/**
 * On-device semantic embedding generator using TFLite.
 */
class TFLiteTextEmbedder(
    private val context: Context,
    private val modelPath: String,
    private val vocabPath: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : TextEmbedder {

    private val maxSequenceLength = 128
    private val hiddenDimension = 384
    private val inferenceLock = ReentrantLock()
    private val initLock = Any()

    private var initialization: Deferred<Pair<Interpreter, BertTokenizer>> = newInitialization()
    private var failedAttempt = false

    private fun newInitialization(): Deferred<Pair<Interpreter, BertTokenizer>> =
        scope.async {
            val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
                useXNNPACK = true
            }
            val interpreter = Interpreter(modelBuffer, options)
            val tokenizer = BertTokenizer(context, vocabPath)
            interpreter to tokenizer
        }

    /**
     * Awaits initialization; a previously failed attempt is replaced with a
     * fresh one, so a transient load failure never permanently disables
     * resonance for the process lifetime.
     */
    private suspend fun awaitInitialization(): Pair<Interpreter, BertTokenizer> {
        synchronized(initLock) {
            if (failedAttempt) {
                failedAttempt = false
                initialization = newInitialization()
            }
        }
        return try {
            initialization.await()
        } catch (t: Throwable) {
            synchronized(initLock) { failedAttempt = true }
            throw t
        }
    }

    override suspend fun embed(text: String): Result<FloatArray> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext Result.success(FloatArray(hiddenDimension))

        runCatching {
            val (interpreter, tokenizer) = awaitInitialization()
            val (inputIds, attentionMask, _) = tokenizer.tokenize(text, maxSequenceLength)

            val inputs = arrayOf(
                arrayOf(inputIds),      // Index 0: input_ids
                arrayOf(attentionMask)  // Index 1: attention_mask
            )

            // Output tensor: [1, 384] (Model already performs pooling)
            val outputBuffer = Array(1) { FloatArray(hiddenDimension) }
            val outputMap = mapOf(0 to outputBuffer)

            inferenceLock.withLock {
                interpreter.runForMultipleInputsOutputs(inputs, outputMap)
            }

            val pooled = outputBuffer[0]
            normalize(pooled)
        }.onFailure { e ->
            Log.e("TFLiteTextEmbedder", "Inference failed for text: ${text.take(50)}...", e)
        }
    }

    override fun getModelFingerprint(): String = "all-MiniLM-L6-v2-qint8"

    override suspend fun isReady(): Boolean = runCatching {
        awaitInitialization()
        true
    }.getOrDefault(false)

    private fun normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0.0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares.toDouble()).toFloat()
        if (norm > 1e-9f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun close() {
        if (initialization.isCompleted) {
            runCatching {
                val (interpreter, _) = initialization.getCompleted()
                interpreter.close()
            }
        } else {
            initialization.cancel()
        }
    }
}
