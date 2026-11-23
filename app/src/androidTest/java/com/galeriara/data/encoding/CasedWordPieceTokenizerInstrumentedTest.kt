package com.galeriara.data.encoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CasedWordPieceTokenizerInstrumentedTest {
    @Test
    fun fixedCasesMatchIndependentPythonTokenizer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val metadata = JSONObject(assets.open("a06/tokenizer_cases.json").bufferedReader().use { it.readText() })
        val vocabulary = File(instrumentation.targetContext.cacheDir, "a06-wordpiece-vocab.txt")
        assets.open("a06/vocab.txt").use { input -> vocabulary.outputStream().use { input.copyTo(it) } }
        val tokenizer = CasedWordPieceTokenizer(vocabulary, metadata.getString("vocab_sha256"))
        val cases = metadata.getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val profiles = case.getJSONObject("profiles")
            for (name in listOf("dynamic", "fixed_128")) {
                val reference = profiles.getJSONObject(name)
                val actual = tokenizer.encode(case.getString("text"), fixedPadding = name == "fixed_128")
                val ids = reference.getJSONArray("input_ids")
                val mask = reference.getJSONArray("attention_mask")
                val label = "${case.getString("id")}/$name"
                assertArrayEquals(label, LongArray(ids.length()) { ids.getLong(it) }, actual.inputIds)
                assertArrayEquals(label, LongArray(mask.length()) { mask.getLong(it) }, actual.attentionMask)
            }
        }
        assertTrue("Expected original 14 references plus edge cases", cases.length() >= 24)
        val report = JSONObject().put("status", "passed").put("cases", cases.length())
            .put("profiles", 2).put("implementation", "pure Kotlin cased WordPiece")
        File(instrumentation.targetContext.filesDir, "a06").apply { mkdirs() }
            .resolve("tokenizer_report.json").writeText(report.toString(2))
    }
}
