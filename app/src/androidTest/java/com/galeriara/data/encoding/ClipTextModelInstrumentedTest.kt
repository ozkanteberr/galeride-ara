package com.galeriara.data.encoding

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galeriara.core.Embedding
import com.galeriara.core.ModelIdentity
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipTextModelInstrumentedTest {
    @Test
    fun turkishQueriesMatchIndependentPythonReference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val args = InstrumentationRegistry.getArguments()
        val directory = File(instrumentation.targetContext.filesDir, "a06").apply { mkdirs() }
        val reportFile = File(directory, "android_report.json")
        reportFile.delete()
        val metadata = JSONObject(assets.open("a06/fixture.json").bufferedReader().use { it.readText() })
        fun component(argument: String, filename: String): File =
            args.getString(argument)?.let { File(it) } ?: File(directory, filename)
        val vocab = File(instrumentation.targetContext.cacheDir, "a06-model-vocab.txt")
        assets.open("a06/vocab.txt").use { input -> vocab.outputStream().use { input.copyTo(it) } }
        val tokenizer = CasedWordPieceTokenizer(vocab, metadata.getString("vocab_sha256"))
        fun readVector(filename: String): FloatArray {
            val bytes = assets.open("a06/$filename").use { it.readBytes() }
            require(bytes.size == 512 * 4)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            return FloatArray(512).also { buffer.get(it) }
        }
        fun compare(expected: FloatArray, actual: FloatArray): JSONObject {
            assertTrue(actual.size == 512 && actual.all { it.isFinite() })
            var aa = 0.0; var bb = 0.0; var dot = 0.0; var diff = 0.0
            for (i in expected.indices) {
                val a = expected[i].toDouble(); val b = actual[i].toDouble()
                aa += a * a; bb += b * b; dot += a * b; diff += (a - b) * (a - b)
            }
            assertTrue(aa > 0 && bb > 0)
            return JSONObject().put("cosine", dot / sqrt(aa * bb)).put("relative_l2", sqrt(diff / aa))
        }
        val results = org.json.JSONArray()
        var minDesktopCosine = 1.0; var maxDesktopL2 = 0.0
        var minReferenceCosine = 1.0; var maxReferenceL2 = 0.0
        var maxNormError = 0.0
        val identity = ModelIdentity("a06-numerical-validation", metadata.getString("model_sha256"), 512)
        ClipTextModel(component("modelPath", "model_quint8_avx2.onnx"), metadata.getString("model_sha256"),
            component("densePath", "clip_multilingual_dense.f32"), metadata.getString("dense_float32_sha256"),
            metadata.getString("output_name")).use { model ->
            val cases = metadata.getJSONArray("cases")
            for (i in 0 until cases.length()) {
                val case = cases.getJSONObject(i)
                for (profileName in listOf("dynamic", "fixed_128")) {
                    val profile = case.getJSONObject("profiles").getJSONObject(profileName)
                    val tokens = tokenizer.encode(case.getString("text"), fixedPadding = profileName == "fixed_128")
                    val ids = profile.getJSONArray("input_ids")
                    val mask = profile.getJSONArray("attention_mask")
                    val label = "${case.getString("id")}/$profileName"
                    assertArrayEquals(label, LongArray(ids.length()) { ids.getLong(it) }, tokens.inputIds)
                    assertArrayEquals(label, LongArray(mask.length()) { mask.getLong(it) }, tokens.attentionMask)
                    val vector = model.encodeTokens(tokens)
                    val desktop = compare(readVector(profile.getString("desktop_file")), vector)
                    val reference = compare(readVector(profile.getString("reference_file")), vector)
                    assertTrue("$label desktop: $desktop", desktop.getDouble("cosine") >= .99999 && desktop.getDouble("relative_l2") <= .001)
                    assertTrue("$label FP32: $reference", reference.getDouble("cosine") >= .99 && reference.getDouble("relative_l2") <= .15)
                    val normalized = Embedding(identity, vector).values()
                    val normError = abs(sqrt(normalized.sumOf { it.toDouble() * it }) - 1)
                    assertTrue("$label normalization", normError <= 1e-5)
                    minDesktopCosine = minOf(minDesktopCosine, desktop.getDouble("cosine"))
                    maxDesktopL2 = maxOf(maxDesktopL2, desktop.getDouble("relative_l2"))
                    minReferenceCosine = minOf(minReferenceCosine, reference.getDouble("cosine"))
                    maxReferenceL2 = maxOf(maxReferenceL2, reference.getDouble("relative_l2"))
                    maxNormError = maxOf(maxNormError, normError)
                    results.put(JSONObject().put("id", case.getString("id")).put("profile", profileName)
                        .put("vs_desktop", desktop).put("vs_fp32", reference).put("normalized_norm_error", normError))
                }
            }
        }
        val report = JSONObject().put("status", "passed").put("api", Build.VERSION.SDK_INT)
            .put("abi", Build.SUPPORTED_ABIS.first()).put("model_sha256", metadata.getString("model_sha256"))
            .put("comparisons", results.length()).put("results", results)
            .put("min_desktop_cosine", minDesktopCosine).put("max_desktop_relative_l2", maxDesktopL2)
            .put("min_reference_cosine", minReferenceCosine).put("max_reference_relative_l2", maxReferenceL2)
            .put("max_normalized_norm_error", maxNormError)
            .put("scope", "fixed numerical queries only; not physical ARM, retrieval quality or a benchmark")
        reportFile.writeText(report.toString(2))
    }
}
