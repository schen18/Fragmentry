package dissonance.cunninglinguist.fragmentry.core.data.ml

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer

/**
 * WordPiece tokenizer for BERT-based models (MiniLM).
 *
 * The TFLite graph's sequence dimension is dynamic, so sequences are padded
 * only to the next [LENGTH_BUCKET] boundary rather than a fixed length —
 * attention cost grows with the square of the padded sequence length.
 */
class BertTokenizer(private val context: Context, vocabAssetPath: String) {

    /** Padding granularity; one bucket covers typical one-line fragments. */
    private companion object {
        const val LENGTH_BUCKET = 16
    }

    private val vocab: Map<String, Int>

    init {
        val vocabMap = HashMap<String, Int>(30522) // Default BERT vocab size
        context.assets.open(vocabAssetPath).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var index = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    vocabMap[line.trim()] = index++
                }
            }
        }
        vocab = vocabMap
    }

    /** Typographic punctuation the vocabulary only knows in ASCII form. */
    private val asciiPunctuation = mapOf(
        '—' to '-', '–' to '-', '―' to '-', '‐' to '-', '‑' to '-', '‒' to '-',
        '‘' to '\'', '’' to '\'', '´' to '\'', '“' to '"', '”' to '"', '„' to '"',
        '…' to '.', '·' to '-',
    )

    /**
     * Tokenizes into (inputIds, attentionMask, tokenTypeIds) padded to the
     * smallest [LENGTH_BUCKET] multiple that holds the text, capped at
     * [maxSequenceLength]. Content is truncated to maxSequenceLength - 2 so
     * [CLS] and [SEP] always survive.
     */
    fun tokenize(text: String, maxSequenceLength: Int): Triple<IntArray, IntArray, IntArray> {
        val tokens = mutableListOf<Int>()
        tokens.add(vocab["[CLS]"] ?: 101)

        val words = splitWords(text.lowercase())

        // Max content length = maxSequenceLength - 2 (for [CLS] and [SEP])
        val maxContentLength = maxSequenceLength - 2

        for (word in words) {
            if (tokens.size - 1 >= maxContentLength) break // Already full (minus [CLS])

            var start = 0
            while (start < word.length) {
                var end = word.length
                var curSubwordId = -1

                while (start < end) {
                    val subword = (if (start > 0) "##" else "") + word.substring(start, end)
                    val id = vocab[subword]
                    if (id != null) {
                        curSubwordId = id
                        break
                    }
                    end--
                }

                if (curSubwordId == -1) {
                    tokens.add(vocab["[UNK]"] ?: 100)
                    start = word.length // Standard BERT: treat whole word as UNK if no subword match
                } else {
                    tokens.add(curSubwordId)
                    start = end
                }

                if (tokens.size - 1 >= maxContentLength) break
            }
        }

        tokens.add(vocab["[SEP]"] ?: 102)

        val paddedLength = (((tokens.size + LENGTH_BUCKET - 1) / LENGTH_BUCKET) * LENGTH_BUCKET)
            .coerceAtMost(maxSequenceLength)

        val inputIds = IntArray(paddedLength) { 0 }
        val attentionMask = IntArray(paddedLength) { 0 }
        val tokenTypeIds = IntArray(paddedLength) { 0 }

        for (i in 0 until paddedLength) {
            if (i < tokens.size) {
                inputIds[i] = tokens[i]
                attentionMask[i] = 1
            } else {
                inputIds[i] = vocab["[PAD]"] ?: 0
                attentionMask[i] = 0
            }
        }

        return Triple(inputIds, attentionMask, tokenTypeIds)
    }

    /**
     * BERT-style basic tokenization for an English-only model: accents are
     * stripped (café -> cafe), typographic punctuation folds to ASCII, and
     * remaining punctuation stands as separate tokens — so em-dashes and
     * curly quotes never glue two words into a single unknown.
     */
    private fun splitWords(text: String): List<String> {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        val words = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                words.add(current.toString())
                current.clear()
            }
        }
        for (original in normalized) {
            if (Character.getType(original) == Character.NON_SPACING_MARK.toInt()) continue
            val ch = asciiPunctuation[original] ?: original
            when {
                ch.isWhitespace() -> flush()
                ch.isLetterOrDigit() -> current.append(ch)
                else -> {
                    flush()
                    words.add(ch.toString())
                }
            }
        }
        flush()
        return words
    }
}
