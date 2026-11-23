package com.galeriara.data.vectors

import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galeriara.core.Embedding
import com.galeriara.core.ModelIdentity
import com.galeriara.core.VectorRepository
import com.galeriara.data.EmbeddingEntity
import com.galeriara.data.GalleryDatabase
import com.galeriara.data.PhotoEntity
import com.galeriara.data.encoding.MultilingualClipProfile
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomVectorRepositoryInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: GalleryDatabase
    private lateinit var repository: RoomVectorRepository
    private val model = ModelIdentity("a08-test", "revision-one", 3)
    private fun photo(uri: String) = PhotoEntity(uri, "controlled fixture", 1L, 1L)
    private fun embedding(identity: ModelIdentity = model, direction: Int = 0): Embedding =
        Embedding(identity, FloatArray(identity.dimensions).apply { this[direction] = 1f })
    private fun bits(values: FloatArray) = values.map { it.toRawBits() }.toIntArray()
    // Independent byte construction verifies endianness without using the production codec.
    private fun rawBytes(values: FloatArray): ByteArray = ByteArray(values.size * 4) { offset ->
        (values[offset / 4].toRawBits() ushr (8 * (offset % 4))).toByte()
    }
    private suspend inline fun <reified T : Throwable> expect(noinline block: suspend () -> Unit): T {
        try { block() } catch (failure: Throwable) {
            assertTrue("Expected ${T::class.java.simpleName}, got $failure", failure is T)
            return failure as T
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }

    @Before fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, GalleryDatabase::class.java).build()
        repository = RoomVectorRepository(db)
    }
    @After fun closeDatabase() { db.close() }

    @Test fun normalizedFloat32BitsSurviveDiskCloseReopenAndRepeatedRestoration() = runBlocking<Unit> {
        val directory = File(context.filesDir, "a08").apply { mkdirs() }
        val report = File(directory, "android_report.json").apply { delete() }
        val name = "a08-test-${UUID.randomUUID()}.db"
        val identity = MultilingualClipProfile.identity
        val values = FloatArray(512).apply { this[0] = .6000012f; this[1] = .8000016f; this[2] = -0.0f; this[3] = Float.MIN_VALUE }
        val original = Embedding.fromNormalized(identity, values)
        val expected = rawBytes(values)
        var disk: GalleryDatabase? = null
        try {
            disk = Room.databaseBuilder(context, GalleryDatabase::class.java, name).build()
            disk.photos().upsert(photo("content://a08/disk"))
            withContext(Dispatchers.Main) { RoomVectorRepository(disk!!).save("content://a08/disk", original) }
            disk.close(); disk = null
            repeat(3) {
                disk = Room.databaseBuilder(context, GalleryDatabase::class.java, name).build()
                val store = RoomVectorRepository(disk!!)
                val readOnly: VectorRepository = store
                val restored = withContext(Dispatchers.Main) { readOnly.load(identity).single() }
                assertEquals(identity, restored.embedding.model)
                assertArrayEquals(bits(values), bits(restored.embedding.values()))
                val row = disk!!.embeddings().forModel(identity.id, identity.revision).single()
                assertArrayEquals(expected, row.vector)
                store.save(restored.uri, restored.embedding)
                disk!!.close(); disk = null
            }
            val hash = MessageDigest.getInstance("SHA-256").digest(expected).joinToString("") { "%02x".format(it.toInt() and 255) }
            report.writeText(JSONObject().put("status", "passed").put("api", Build.VERSION.SDK_INT)
                .put("abi", Build.SUPPORTED_ABIS.first()).put("dimensions", 512).put("bytes", expected.size)
                .put("disk_reopen_cycles", 3).put("bitwise_round_trip", true).put("byte_order", "little-endian")
                .put("model_revision", identity.revision).put("controlled_blob_sha256", hash)
                .put("normalization", "validate and copy; no second normalization")
                .put("scope", "controlled SQLite vectors; not image indexing or retrieval quality").toString(2))
        } finally {
            disk?.close()
            check(name.startsWith("a08-test-"))
            context.deleteDatabase(name)
        }
    }

    @Test fun codecPreservesLittleEndianBytesSignedZeroAndDefensiveCopies() {
        val original = floatArrayOf(.6000012f, .8000016f, -0.0f)
        val value = Embedding.fromNormalized(model, original)
        val bytes = Float32VectorCodec.encode(value)
        assertArrayEquals(rawBytes(original), bytes)
        val restored = Float32VectorCodec.decode(model, bytes)
        bytes.fill(0)
        assertArrayEquals(bits(original), bits(restored.values()))
        assertThrows(IllegalArgumentException::class.java) { Float32VectorCodec.decode(model, ByteArray(11)) }
    }

    @Test fun differentRevisionsAndModelFamiliesCoexistAndLoadSeparately() = runBlocking<Unit> {
        val uri = "content://a08/shared"
        db.photos().upsert(photo(uri))
        val revisionTwo = model.copy(revision = "revision-two")
        val otherFamily = model.copy(id = "other-family")
        for ((identity, direction) in listOf(model to 0, revisionTwo to 1, otherFamily to 2)) {
            repository.save(uri, embedding(identity, direction))
        }
        for ((identity, direction) in listOf(model to 0, revisionTwo to 1, otherFamily to 2)) {
            val result = repository.load(identity).single()
            assertEquals(identity, result.embedding.model)
            assertArrayEquals(bits(embedding(identity, direction).values()), bits(result.embedding.values()))
        }
        assertTrue(repository.load(model.copy(revision = "not-stored")).isEmpty())
        assertTrue(repository.load(model.copy(id = "A08-TEST")).isEmpty())
    }

    @Test fun upsertChangesOnlyItsOwnPhotoAndModelKey() = runBlocking<Unit> {
        val a = "content://a08/a"; val b = "content://a08/b"
        db.photos().upsert(photo(a)); db.photos().upsert(photo(b))
        val other = model.copy(revision = "older")
        repository.save(a, embedding()); repository.save(a, embedding(other, 1)); repository.save(b, embedding())
        repository.save(a, embedding(direction = 2))
        val rows = repository.load(model)
        assertEquals(2, rows.size)
        assertArrayEquals(bits(embedding(direction = 2).values()), bits(rows.first { it.uri == a }.embedding.values()))
        assertArrayEquals(bits(embedding().values()), bits(rows.first { it.uri == b }.embedding.values()))
        assertArrayEquals(bits(embedding(other, 1).values()), bits(repository.load(other).single().embedding.values()))
        db.photos().upsert(photo(a).copy(name = "renamed"))
        assertEquals(2, repository.load(model).size)
        assertEquals(1, repository.load(other).size)
    }

    @Test fun loadOrderIsStableAndEmptyModelsHaveNoResults() = runBlocking<Unit> {
        assertTrue(repository.load(model).isEmpty())
        for (uri in listOf("content://a08/z", "content://a08/a", "content://a08/m")) {
            db.photos().upsert(photo(uri)); repository.save(uri, embedding())
        }
        val expected = listOf("content://a08/a", "content://a08/m", "content://a08/z")
        assertEquals(expected, repository.load(model).map { it.uri })
        assertEquals(expected, repository.load(model).map { it.uri })
    }

    @Test fun malformedBlobStopsEntireReadAndLeavesAllRowsUntouched() = runBlocking<Unit> {
        val valid = "content://a08/a-valid"; val bad = "content://a08/z-bad"
        db.photos().upsert(photo(valid)); db.photos().upsert(photo(bad))
        repository.save(valid, embedding())
        val invalid = byteArrayOf(1, 2, 3)
        db.embeddings().upsert(EmbeddingEntity(bad, model.id, model.revision, 3, invalid))
        val failure = expect<VectorIntegrityException> { repository.load(model) }
        assertEquals(bad, failure.photoUri); assertEquals(model, failure.model)
        assertTrue(failure.cause is IllegalArgumentException)
        val rows = db.embeddings().forModel(model.id, model.revision)
        assertEquals(2, rows.size)
        assertArrayEquals(invalid, rows.first { it.photoUri == bad }.vector)
        assertArrayEquals(rawBytes(embedding().values()), rows.first { it.photoUri == valid }.vector)
    }

    @Test fun nonFiniteZeroAndNonNormalizedStoredValuesAreRejectedWithoutRepair() = runBlocking<Unit> {
        val uri = "content://a08/invalid-values"; db.photos().upsert(photo(uri))
        for (values in listOf(floatArrayOf(Float.NaN, 1f, 0f), floatArrayOf(Float.POSITIVE_INFINITY, 0f, 0f),
            floatArrayOf(0f, 0f, 0f), floatArrayOf(3f, 4f, 0f))) {
            val bytes = rawBytes(values)
            db.embeddings().upsert(EmbeddingEntity(uri, model.id, model.revision, 3, bytes))
            expect<VectorIntegrityException> { repository.load(model) }
            assertArrayEquals(bytes, db.embeddings().forModel(model.id, model.revision).single().vector)
        }
    }

    @Test fun dimensionMismatchIsNotSilentlyFilteredOrOverwritten() = runBlocking<Unit> {
        val uri = "content://a08/dimension"; db.photos().upsert(photo(uri))
        val bytes = rawBytes(floatArrayOf(1f, 0f))
        db.embeddings().upsert(EmbeddingEntity(uri, model.id, model.revision, 2, bytes))
        expect<VectorIntegrityException> { repository.load(model) }
        expect<VectorIntegrityException> { repository.save(uri, embedding()) }
        val row = db.embeddings().forModel(model.id, model.revision).single()
        assertEquals(2, row.dimensions); assertArrayEquals(bytes, row.vector)
    }

    @Test fun oneModelRevisionCannotChangeDimensionsAcrossDifferentPhotos() = runBlocking<Unit> {
        val a = "content://a08/a"; val b = "content://a08/b"
        db.photos().upsert(photo(a)); db.photos().upsert(photo(b))
        repository.save(a, embedding())
        val different = model.copy(dimensions = 2)
        expect<VectorIntegrityException> { repository.save(b, embedding(different)) }
        assertEquals(listOf(a), repository.load(model).map { it.uri })
        assertEquals(1, db.embeddings().forModel(model.id, model.revision).size)
    }

    @Test fun invalidationAndPhotoDeletionRemoveAllLinkedRevisionsOnly() = runBlocking<Unit> {
        val a = "content://a08/a"; val b = "content://a08/b"
        db.photos().upsert(photo(a)); db.photos().upsert(photo(b))
        val older = model.copy(revision = "older")
        repository.save(a, embedding()); repository.save(a, embedding(older)); repository.save(b, embedding())
        db.photos().invalidate(a)
        assertEquals(listOf(b), repository.load(model).map { it.uri })
        assertTrue(repository.load(older).isEmpty())
        assertEquals(2, db.photos().all().size)
        repository.save(a, embedding()); repository.save(a, embedding(older))
        db.photos().delete(a)
        assertEquals(listOf(b), repository.load(model).map { it.uri })
        assertTrue(repository.load(older).isEmpty())
    }

    @Test fun missingPhotoAndBlankUriFailWithoutOrphansAndDatabaseRemainsUsable() = runBlocking<Unit> {
        expect<IllegalArgumentException> { repository.save(" ", embedding()) }
        expect<SQLiteConstraintException> { repository.save("content://a08/missing", embedding()) }
        assertTrue(db.embeddings().forModel(model.id, model.revision).isEmpty())
        db.photos().upsert(photo("content://a08/valid"))
        repository.save("content://a08/valid", embedding())
        assertEquals(1, repository.load(model).size)
    }

    @Test fun concurrentWritersCannotMixDimensionsWithinOneRevision() = runBlocking<Unit> {
        val a = "content://a08/a"; val b = "content://a08/b"
        db.photos().upsert(photo(a)); db.photos().upsert(photo(b))
        val smaller = model.copy(dimensions = 2)
        val results = supervisorScope {
            listOf(async(Dispatchers.Default) { runCatching { repository.save(a, embedding()) } },
                async(Dispatchers.Default) { runCatching { repository.save(b, embedding(smaller)) } }).awaitAll()
        }
        assertEquals(1, results.count { it.isSuccess })
        assertTrue(results.first { it.isFailure }.exceptionOrNull() is VectorIntegrityException)
        val row = db.embeddings().forModel(model.id, model.revision).single()
        assertEquals(1, repository.load(model.copy(dimensions = row.dimensions)).size)
    }
}
