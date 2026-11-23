package com.galeriara.data.encoding

import java.io.File
import java.security.MessageDigest

data class ClipTextInput(val inputIds: LongArray, val attentionMask: LongArray)

/** The selected model's cased, single-query WordPiece profile; not a general tokenizer loader. */
class CasedWordPieceTokenizer(vocabFile: File, expectedSha256: String) {
    private val vocabulary: Map<String, Long>

    init {
        require(expectedSha256.matches(Regex("[0-9a-f]{64}")))
        require(vocabFile.isFile)
        val bytes = vocabFile.readBytes()
        val checksum = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        require(checksum == expectedSha256) { "Tokenizer vocabulary checksum mismatch" }
        val entries = HashMap<String, Long>(160_000)
        bytes.inputStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEachIndexed { index, token ->
                require(entries.put(token, index.toLong()) == null) { "Duplicate vocabulary token" }
            }
        }
        require(entries.size == 119_547) { "Unexpected vocabulary size" }
        SPECIAL_IDS.forEach { (token, id) -> require(entries[token] == id) }
        vocabulary = entries
    }

    fun encode(text: String, fixedPadding: Boolean = false): ClipTextInput {
        // Python str.strip() and Rust's Unicode whitespace differ for U+001C..U+001F.
        val prepared = text.trim { isPythonStripSpace(it.code) }
        val content = ArrayList<Long>()
        var cursor = 0
        // Added special tokens are matched before BERT cleaning, including inside ordinary words.
        for (match in SPECIAL_PATTERN.findAll(prepared)) {
            appendOrdinary(prepared.substring(cursor, match.range.first), content)
            content.add(SPECIAL_IDS.getValue(match.value))
            cursor = match.range.last + 1
            if (content.size >= 126) break
        }
        if (content.size < 126) appendOrdinary(prepared.substring(cursor), content)
        val count = minOf(126, content.size)
        val length = if (fixedPadding) 128 else count + 2
        val ids = LongArray(length)
        val mask = LongArray(length)
        ids[0] = 101
        for (i in 0 until count) ids[i + 1] = content[i]
        ids[count + 1] = 102
        for (i in 0 until count + 2) mask[i] = 1
        return ClipTextInput(ids, mask)
    }

    private fun appendOrdinary(text: String, output: MutableList<Long>) {
        val word = StringBuilder()
        fun flush() {
            if (word.isNotEmpty()) {
                output.addAll(wordPieces(word.toString()))
                word.setLength(0)
            }
        }
        var offset = 0
        while (offset < text.length && output.size < 126) {
            val point = text.codePointAt(offset)
            offset += Character.charCount(point)
            if (point == 0 || point == 0xFFFD || isRemovedControl(point)) continue
            when {
                isRustSpace(point) -> flush()
                isChinese(point) || isPunctuation(point) -> {
                    flush()
                    output.addAll(wordPieces(String(Character.toChars(point))))
                }
                else -> word.appendCodePoint(point)
            }
        }
        if (output.size < 126) flush()
    }

    private fun wordPieces(word: String): List<Long> {
        val points = word.codePointCount(0, word.length)
        if (points > 100) return listOf(100)
        val bounds = IntArray(points + 1)
        for (i in 0 until points) bounds[i + 1] = bounds[i] + Character.charCount(word.codePointAt(bounds[i]))
        val pieces = ArrayList<Long>()
        var begin = 0
        while (begin < points) {
            var end = points
            var chosen: Long? = null
            while (end > begin) {
                val candidate = (if (begin == 0) "" else "##") + word.substring(bounds[begin], bounds[end])
                chosen = vocabulary[candidate]
                if (chosen != null) break
                end--
            }
            if (chosen == null) return listOf(100)
            pieces.add(chosen)
            begin = end
        }
        return pieces
    }

    private fun isRemovedControl(point: Int): Boolean {
        if (point == 9 || point == 10 || point == 13) return false
        return Character.getType(point) in REMOVED_TYPES
    }

    private fun isPunctuation(point: Int): Boolean = point in 33..47 || point in 58..64 ||
        point in 91..96 || point in 123..126 || Character.getType(point) in PUNCTUATION_TYPES

    private fun isChinese(point: Int): Boolean = point in 0x4E00..0x9FFF || point in 0x3400..0x4DBF ||
        point in 0x20000..0x2A6DF || point in 0x2A700..0x2B73F || point in 0x2B740..0x2B81F ||
        point in 0x2B920..0x2CEAF || point in 0xF900..0xFAFF || point in 0x2F800..0x2FA1F

    private fun isRustSpace(point: Int): Boolean = point in 9..13 || point == 0x20 || point == 0x85 ||
        point == 0xA0 || point == 0x1680 || point in 0x2000..0x200A || point == 0x2028 ||
        point == 0x2029 || point == 0x202F || point == 0x205F || point == 0x3000

    private fun isPythonStripSpace(point: Int): Boolean = isRustSpace(point) || point in 0x1C..0x1F

    private companion object {
        val SPECIAL_IDS = linkedMapOf("[PAD]" to 0L, "[UNK]" to 100L, "[CLS]" to 101L,
            "[SEP]" to 102L, "[MASK]" to 103L)
        val SPECIAL_PATTERN = Regex(SPECIAL_IDS.keys.joinToString("|") { Regex.escape(it) })
        val REMOVED_TYPES = setOf(Character.CONTROL.toInt(), Character.FORMAT.toInt(),
            Character.PRIVATE_USE.toInt(), Character.SURROGATE.toInt())
        val PUNCTUATION_TYPES = setOf(Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt())
    }
}
