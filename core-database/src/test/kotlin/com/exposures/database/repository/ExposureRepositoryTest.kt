package com.exposures.database.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.database.mapper.toEntity
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExposureRepositoryTest {

    private lateinit var database: ExposuresDatabase
    private lateinit var repository: ExposureRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExposuresDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExposureRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun draftExposure(filmRollId: String, notes: String? = null) = Exposure(
        id = java.util.UUID.randomUUID().toString(),
        filmRollId = filmRollId,
        frameNumber = 0, // unset — saveExposure should assign it
        lensId = DefaultSeedData.sekor110mmF28.id,
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        notes = notes,
        capturedAt = 0L,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    @Test
    fun `seedIfEmpty populates default equipment and rolls`() = runTest {
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.cameraBodies.toSet(), repository.observeCameraBodies().first().toSet())
        assertEquals(DefaultSeedData.lenses.toSet(), repository.observeLenses().first().toSet())
        assertEquals(DefaultSeedData.filmRolls.toSet(), repository.observeAvailableRolls().first().toSet())
    }

    @Test
    fun `seedIfEmpty does not duplicate rows when called twice`() = runTest {
        repository.seedIfEmpty()
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.filmRolls.size, repository.observeAvailableRolls().first().size)
    }

    @Test
    fun `available rolls excludes completed and archived rolls`() = runTest {
        repository.seedIfEmpty()
        val completedRoll = DefaultSeedData.portra400Roll.copy(
            id = "extra-completed-roll",
            status = RollStatus.COMPLETED,
        )
        database.filmRollDao().upsertAll(listOf(completedRoll.toEntity()))

        val available = repository.observeAvailableRolls().first()

        assertTrue(available.none { it.id == completedRoll.id })
    }

    @Test
    fun `saving the first exposure on a roll assigns frame number 1`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id

        val saved = repository.saveExposure(draftExposure(rollId))

        assertEquals(1, saved.frameNumber)
    }

    @Test
    fun `saving successive exposures increments the frame number`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id

        repository.saveExposure(draftExposure(rollId, notes = "first"))
        repository.saveExposure(draftExposure(rollId, notes = "second"))
        val third = repository.saveExposure(draftExposure(rollId, notes = "third"))

        assertEquals(3, third.frameNumber)
        assertEquals(3, repository.observeExposures(rollId).first().size)
    }

    @Test
    fun `frame numbering is independent per roll`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))

        val firstOnOtherRoll = repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id))

        assertEquals(1, firstOnOtherRoll.frameNumber)
    }

    @Test
    fun `exposures survive a fresh repository over the same underlying database`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id
        val saved = repository.saveExposure(draftExposure(rollId, notes = "restart check"))

        val reopened = ExposureRepository(database)
        val persisted = reopened.getExposure(saved.id)

        assertEquals(saved, persisted)
    }

    @Test
    fun `getRoll returns null for an unknown id`() = runTest {
        assertNull(repository.getRoll("does-not-exist"))
    }
}
