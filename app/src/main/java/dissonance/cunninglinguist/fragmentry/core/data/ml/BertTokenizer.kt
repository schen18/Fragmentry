package dissonance.cunninglinguist.fragmentry.core.data.ml

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer

/**
 * WordPiece tokenizer for BERT-based models (MiniLM).
 */
class BertTokenizer(private val context: Context, vocabAssetPath: String) {
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

        val inputIds = IntArray(maxSequenceLength) { 0 }
        val attentionMask = IntArray(maxSequenceLength) { 0 }
        val tokenTypeIds = IntArray(maxSequenceLength) { 0 }

        for (i in 0 until maxSequenceLength) {
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
     * curly quotes never glue two words into a single unknown. Non-Latin
     * scripts flow through as ordinary (unknown) words.
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
