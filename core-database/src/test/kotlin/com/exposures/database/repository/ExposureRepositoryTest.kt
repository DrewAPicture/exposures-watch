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

    private fun draftExposure(
        filmRollId: String,
        notes: String? = null,
        zone: Int? = null,
        focalLengthMm: Int? = null,
    ) = Exposure(
        id = java.util.UUID.randomUUID().toString(),
        filmRollId = filmRollId,
        frameNumber = 0, // unset — saveExposure should assign it
        lensId = DefaultSeedData.sekor110mmF28.id,
        focalLengthMm = focalLengthMm,
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = zone,
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
        assertEquals(DefaultSeedData.lightMeters.toSet(), repository.observeLightMeters().first().toSet())
        assertEquals(DefaultSeedData.filmBacks.toSet(), repository.observeFilmBacks().first().toSet())
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
    fun `switcher rolls include available and completed, sorted available-first`() = runTest {
        repository.seedIfEmpty()
        val completedRoll = DefaultSeedData.portra400Roll.copy(id = "extra-completed-roll", status = RollStatus.COMPLETED)
        val archivedRoll = DefaultSeedData.portra400Roll.copy(id = "extra-archived-roll", status = RollStatus.ARCHIVED)
        database.filmRollDao().upsertAll(listOf(completedRoll.toEntity(), archivedRoll.toEntity()))

        val switcherRolls = repository.observeSwitcherRolls().first()

        assertTrue(switcherRolls.none { it.id == archivedRoll.id })
        assertTrue(switcherRolls.any { it.id == completedRoll.id })
        val lastAvailableIndex = switcherRolls.indexOfLast { it.status == RollStatus.AVAILABLE }
        val firstCompletedIndex = switcherRolls.indexOfFirst { it.status == RollStatus.COMPLETED }
        assertTrue(lastAvailableIndex < firstCompletedIndex)
    }

    @Test
    fun `markRollCompletedLocally flips the roll's status immediately`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.filmRolls.first().id

        repository.markRollCompletedLocally(rollId)

        assertEquals(RollStatus.COMPLETED, repository.getRoll(rollId)?.status)
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
    fun `observeExposures returns frames most-recent-first`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id
        repository.saveExposure(draftExposure(rollId, notes = "one"))
        repository.saveExposure(draftExposure(rollId, notes = "two"))
        repository.saveExposure(draftExposure(rollId, notes = "three"))

        val frameNumbers = repository.observeExposures(rollId).first().map { it.frameNumber }

        assertEquals(listOf(3, 2, 1), frameNumbers)
    }

    @Test
    fun `updateExposure persists field changes and marks the exposure pending sync`() = runTest {
        repository.seedIfEmpty()
        val saved = repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))

        repository.updateExposure(saved.copy(isoUsed = 800, aperture = 11.0, syncStatus = SyncStatus.SYNCED))

        val updated = repository.getExposure(saved.id)
        assertEquals(800, updated?.isoUsed)
        assertEquals(11.0, updated?.aperture)
        assertEquals(SyncStatus.PENDING_SYNC, updated?.syncStatus)
    }

    @Test
    fun `updateExposure does not change the frame number`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id
        repository.saveExposure(draftExposure(rollId))
        val second = repository.saveExposure(draftExposure(rollId))

        repository.updateExposure(second.copy(isoUsed = 1600))

        assertEquals(2, repository.getExposure(second.id)?.frameNumber)
    }

    @Test
    fun `updateExposure does not change the last-used-settings defaults`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id
        val first = repository.saveExposure(draftExposure(rollId))
        repository.saveExposure(draftExposure(rollId))
        val lastUsedBeforeEdit = repository.observeLastUsedExposureSettings().first()

        repository.updateExposure(first.copy(isoUsed = 3200, aperture = 22.0))

        assertEquals(lastUsedBeforeEdit, repository.observeLastUsedExposureSettings().first())
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

    @Test
    fun `seedIfEmpty defaults the active roll to the first seed roll`() = runTest {
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.filmRolls.first().id, repository.observeActiveRollId().first())
    }

    @Test
    fun `setActiveRoll updates the observed active roll id`() = runTest {
        repository.seedIfEmpty()

        repository.setActiveRoll(DefaultSeedData.hp5Roll.id)

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `seedIfEmpty does not overwrite an already-selected active roll`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.hp5Roll.id)

        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `ensureAppStateInitialized starts with no active roll and no seeded equipment`() = runTest {
        repository.ensureAppStateInitialized()

        assertNull(repository.observeActiveRollId().first())
        assertTrue(repository.observeCameraBodies().first().isEmpty())
        assertTrue(repository.observeLenses().first().isEmpty())
        assertTrue(repository.observeAvailableRolls().first().isEmpty())
    }

    @Test
    fun `ensureAppStateInitialized does not overwrite an already-selected active roll`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.hp5Roll.id)

        repository.ensureAppStateInitialized()

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyCameraBodiesSync merges incoming camera body updates`() = runTest {
        repository.applyCameraBodiesSync(listOf(DefaultSeedData.rz67ProII))
        repository.applyCameraBodiesSync(listOf(DefaultSeedData.rz67ProII.copy(name = "RZ67 Updated")))

        val bodies = repository.observeCameraBodies().first()

        assertTrue(bodies.any { it.id == DefaultSeedData.rz67ProII.id && it.name == "RZ67 Updated" })
    }

    @Test
    fun `applyLensesSync merges incoming lens updates`() = runTest {
        repository.applyCameraBodiesSync(listOf(DefaultSeedData.rz67ProII))
        repository.applyLensesSync(listOf(DefaultSeedData.sekor110mmF28, DefaultSeedData.sekor50mmF45))
        repository.applyLensesSync(listOf(DefaultSeedData.sekor110mmF28.copy(name = "110 Updated")))

        val lenses = repository.observeLenses().first()
        assertTrue(lenses.any { it.id == DefaultSeedData.sekor110mmF28.id && it.name == "110 Updated" })
        assertTrue(lenses.any { it.id == DefaultSeedData.sekor50mmF45.id })
    }

    @Test
    fun `applyLensesSync keeps incoming lenses and preserves any lens referenced by exposures`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(
            draftExposure(DefaultSeedData.portra400Roll.id).copy(lensId = DefaultSeedData.sekor50mmF45.id),
        )

        repository.applyLensesSync(listOf(DefaultSeedData.sekor110mmF28))

        val remainingIds = repository.observeLenses().first().map { it.id }.toSet()
        assertTrue(DefaultSeedData.sekor110mmF28.id in remainingIds)
        assertTrue(DefaultSeedData.sekor50mmF45.id in remainingIds)
    }

    @Test
    fun `applyLightMetersSync merges incoming light meter updates`() = runTest {
        repository.applyLightMetersSync(listOf(DefaultSeedData.pentaxSpotMeter))
        repository.applyLightMetersSync(listOf(DefaultSeedData.pentaxSpotMeter.copy(name = "Spotmeter Updated")))

        val meters = repository.observeLightMeters().first()

        assertTrue(meters.any { it.id == DefaultSeedData.pentaxSpotMeter.id && it.name == "Spotmeter Updated" })
    }

    @Test
    fun `getLightMeter returns null for an unknown id`() = runTest {
        assertNull(repository.getLightMeter("does-not-exist"))
    }

    @Test
    fun `getLightMeter returns the matching meter after a sync`() = runTest {
        repository.applyLightMetersSync(listOf(DefaultSeedData.pentaxSpotMeter))

        val meter = repository.getLightMeter(DefaultSeedData.pentaxSpotMeter.id)

        assertEquals(DefaultSeedData.pentaxSpotMeter, meter)
    }

    @Test
    fun `applyFilmBacksSync merges incoming film back updates`() = runTest {
        repository.applyFilmBacksSync(listOf(DefaultSeedData.rz67Back))
        repository.applyFilmBacksSync(listOf(DefaultSeedData.rz67Back.copy(name = "6x7 back (updated)")))

        val backs = repository.observeFilmBacks().first()

        assertTrue(backs.any { it.id == DefaultSeedData.rz67Back.id && it.name == "6x7 back (updated)" })
    }

    @Test
    fun `getFilmBack returns null for an unknown id`() = runTest {
        assertNull(repository.getFilmBack("does-not-exist"))
    }

    @Test
    fun `getFilmBack returns the matching back after a sync`() = runTest {
        repository.applyFilmBacksSync(listOf(DefaultSeedData.rz67Back))

        val back = repository.getFilmBack(DefaultSeedData.rz67Back.id)

        assertEquals(DefaultSeedData.rz67Back, back)
    }

    @Test
    fun `applyFilmRollsSync keeps the active roll when it still exists`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.hp5Roll.id)

        repository.applyFilmRollsSync(DefaultSeedData.filmRolls)

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyFilmRollsSync keeps active roll when payload omits it`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.hp5Roll.id)

        repository.applyFilmRollsSync(listOf(DefaultSeedData.portra400Roll)) // partial payload update only

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyFilmRollsSync keeps active roll when payload is empty`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.portra400Roll.id)

        repository.applyFilmRollsSync(emptyList())

        assertEquals(DefaultSeedData.portra400Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyFilmRollsSync does not delete referenced historical rolls`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        repository.setActiveRoll(DefaultSeedData.portra400Roll.id)

        repository.applyFilmRollsSync(emptyList())

        val remainingRollIds = repository.observeRoll(DefaultSeedData.portra400Roll.id).first()
        assertEquals(DefaultSeedData.portra400Roll.id, remainingRollIds?.id)
        assertEquals(DefaultSeedData.portra400Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyFilmRollsSync falls back to another roll when the active one is completed, not just removed`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.portra400Roll.id)

        // The roll is still present in the sync — just no longer AVAILABLE (e.g. just completed).
        val completed = DefaultSeedData.portra400Roll.copy(status = RollStatus.COMPLETED)
        repository.applyFilmRollsSync(listOf(completed, DefaultSeedData.hp5Roll))

        assertEquals(DefaultSeedData.hp5Roll.id, repository.observeActiveRollId().first())
    }

    @Test
    fun `applyFilmRollsSync clears the active roll when no roll remains available`() = runTest {
        // Sync is merge-only (upsertAll, never deletes) — so unlike a destructive replace, omitting
        // a roll from the payload doesn't remove it. To genuinely have no AVAILABLE fallback left,
        // every roll must be explicitly marked non-available, not merely left out of the payload.
        repository.seedIfEmpty()
        repository.setActiveRoll(DefaultSeedData.portra400Roll.id)

        val completedPortra = DefaultSeedData.portra400Roll.copy(status = RollStatus.COMPLETED)
        val completedHp5 = DefaultSeedData.hp5Roll.copy(status = RollStatus.COMPLETED)
        repository.applyFilmRollsSync(listOf(completedPortra, completedHp5))

        assertNull(repository.observeActiveRollId().first())
    }

    @Test
    fun `updateExposurePhotoStatus only touches the targeted exposure`() = runTest {
        repository.seedIfEmpty()
        val rollId = DefaultSeedData.portra400Roll.id
        val a = repository.saveExposure(draftExposure(rollId))
        val b = repository.saveExposure(draftExposure(rollId))

        repository.updateExposurePhotoStatus(a.id, PhotoStatus.CAPTURED)

        assertEquals(PhotoStatus.CAPTURED, requireNotNull(repository.getExposure(a.id)).referencePhotoStatus)
        assertEquals(PhotoStatus.NONE, requireNotNull(repository.getExposure(b.id)).referencePhotoStatus)
    }

    @Test
    fun `getAllExposuresOnce returns exposures across every roll`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id))
        repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id))

        assertEquals(2, repository.getAllExposuresOnce().size)
    }

    @Test
    fun `pending capture requests round-trip and can be removed`() = runTest {
        repository.enqueuePendingCaptureRequest("exp-1", "roll-1", 3)

        val pending = repository.getPendingCaptureRequests()
        assertEquals(1, pending.size)
        assertEquals("exp-1", pending.single().exposureId)

        repository.removePendingCaptureRequest("exp-1")

        assertTrue(repository.getPendingCaptureRequests().isEmpty())
    }

    @Test
    fun `enqueuing a second request for the same exposure replaces the first`() = runTest {
        repository.enqueuePendingCaptureRequest("exp-1", "roll-1", 3)

        repository.enqueuePendingCaptureRequest("exp-1", "roll-1", 3)

        assertEquals(1, repository.getPendingCaptureRequests().size)
    }

    @Test
    fun `last-used exposure settings are unset before anything is ever saved`() = runTest {
        repository.seedIfEmpty()

        val settings = repository.observeLastUsedExposureSettings().first()

        assertNull(settings.lensId)
        assertNull(settings.shutterSpeed)
        assertNull(settings.aperture)
        assertNull(settings.iso)
        assertNull(settings.zone)
        assertNull(settings.focalLengthMm)
    }

    @Test
    fun `saveExposure records its values as the new last-used settings`() = runTest {
        repository.seedIfEmpty()
        val draft = draftExposure(DefaultSeedData.portra400Roll.id, focalLengthMm = 50).copy(
            lensId = DefaultSeedData.sekor50mmF45.id,
            shutterSpeed = ShutterSpeed.fraction(250),
            aperture = 5.6,
            isoUsed = 800,
        )

        repository.saveExposure(draft)

        val settings = repository.observeLastUsedExposureSettings().first()
        assertEquals(DefaultSeedData.sekor50mmF45.id, settings.lensId)
        assertEquals(ShutterSpeed.fraction(250), settings.shutterSpeed)
        assertEquals(5.6, settings.aperture)
        assertEquals(800, settings.iso)
        assertEquals(50, settings.focalLengthMm)
    }

    @Test
    fun `saving a prime exposure does not wipe out the last zoom focal length`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id, focalLengthMm = 50))

        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id, focalLengthMm = null))

        val settings = repository.observeLastUsedExposureSettings().first()
        assertEquals(50, settings.focalLengthMm)
    }

    @Test
    fun `last-used settings reflect the most recently saved exposure, even on a different roll`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id).copy(isoUsed = 400))

        repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id).copy(isoUsed = 1600))

        assertEquals(1600, repository.observeLastUsedExposureSettings().first().iso)
    }

    @Test
    fun `saveExposure records a chosen zone as the new last-used zone`() = runTest {
        repository.seedIfEmpty()

        repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id, zone = 3))

        assertEquals(3, repository.observeLastUsedExposureSettings().first().zone)
    }

    @Test
    fun `saving an exposure with no zone does not clear a previously recorded last-used zone`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.hp5Roll.id, zone = 7))

        // A roll with no light meter never collects a zone, so this save passes zone = null.
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Roll.id, zone = null))

        assertEquals(7, repository.observeLastUsedExposureSettings().first().zone)
    }
}
