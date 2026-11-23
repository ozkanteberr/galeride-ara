package com.galeriara.data.encoding

import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipVisualModelInstrumentedTest {
    @Test
    fun fixedImageMatchesIndependentPythonReference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val directory = File(instrumentation.targetContext.filesDir, "a05")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create validation output directory" }
        val reportFile = File(directory, "android_report.json")
        reportFile.delete()
        val metadata = JSONObject(assets.open("a05/fixture.json").bufferedReader().use { it.readText() })
        fun floats(name: String): FloatArray {
            val bytes = assets.open("a05/$name").use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            return FloatArray(buffer.remaining()).also { buffer.get(it) }
        }
        val expectedInput = floats("input.f32")
        val reference = floats("reference.f32")
        val desktop = floats("onnx.f32")
        val bitmap = assets.open("a05/a05-pattern.png").use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inScaled = false })
        } ?: error("Fixture cannot be decoded")
        val actualInput = try { ClipImagePreprocessor.toTensor(bitmap) } finally { bitmap.recycle() }
        val std = doubleArrayOf(0.26862954, 0.26130258, 0.27577711)
        var inputMaxError = 0.0
        actualInput.indices.forEach { i ->
            val difference = abs(actualInput[i].toDouble() - expectedInput[i])
            inputMaxError = maxOf(inputMaxError, difference)
            val limit = 2.0 / (255 * std[i / (224 * 224)]) + 1e-6
            assertTrue("BICUBIC/normalization mismatch at $i: $difference", difference <= limit)
        }
        fun compare(expected: FloatArray, actual: FloatArray): JSONObject {
            var aa = 0.0; var bb = 0.0; var dot = 0.0; var difference = 0.0
            assertTrue(actual.size == 512 && actual.all { it.isFinite() })
            for (i in expected.indices) {
                val a = expected[i].toDouble(); val b = actual[i].toDouble()
                aa += a * a; bb += b * b; dot += a * b; difference += (a - b) * (a - b)
            }
            assertTrue(aa > 0 && bb > 0)
            return JSONObject().put("cosine", dot / sqrt(aa * bb)).put("relative_l2", sqrt(difference / aa))
        }
        fun checkGate(metrics: JSONObject, cosine: Double, relativeL2: Double) {
            assertTrue(metrics.toString(), metrics.getDouble("cosine") >= cosine)
            assertTrue(metrics.toString(), metrics.getDouble("relative_l2") <= relativeL2)
        }
        val modelFile = InstrumentationRegistry.getArguments().getString("modelPath")?.let { File(it) }
            ?: File(directory, "clip_vit_b32_visual_fp32.onnx")
        ClipVisualModel(modelFile, metadata.getString("model_sha256")).use { model ->
            val androidOutput = model.encodeTensor(expectedInput)
            val desktopMetrics = compare(desktop, androidOutput)
            val pytorchMetrics = compare(reference, androidOutput)
            val imageMetrics = compare(reference, model.encodeTensor(actualInput))
            checkGate(desktopMetrics, 0.99999, 0.001)
            checkGate(pytorchMetrics, 0.99999, 0.001)
            checkGate(imageMetrics, 0.999, 0.02)
            val report = JSONObject().put("status", "passed").put("api", Build.VERSION.SDK_INT)
                .put("abi", Build.SUPPORTED_ABIS.first()).put("model_sha256", metadata.getString("model_sha256"))
                .put("input_max_abs_error", inputMaxError).put("same_tensor_vs_desktop", desktopMetrics)
                .put("same_tensor_vs_pytorch", pytorchMetrics).put("full_png_pipeline_vs_pytorch", imageMetrics)
                .put("scope", "controlled RGB PNG; not URI/EXIF/HDR validation")
            reportFile.writeText(report.toString(2))
        }
    }
}
