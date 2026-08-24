package com.exposures.database.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.database.mapper.toEntity
import com.exposures.database.seed.DefaultSeedData
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.FilmMediumStatus
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
        filmMediumId: String,
        notes: String? = null,
        zone: Int? = null,
        focalLengthMm: Int? = null,
    ) = Exposure(
        id = java.util.UUID.randomUUID().toString(),
        filmMediumId = filmMediumId,
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
    fun `seedIfEmpty populates default equipment and film media`() = runTest {
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.cameraBodies.toSet(), repository.observeCameraBodies().first().toSet())
        assertEquals(DefaultSeedData.lenses.toSet(), repository.observeLenses().first().toSet())
        assertEquals(DefaultSeedData.lightMeters.toSet(), repository.observeLightMeters().first().toSet())
        assertEquals(DefaultSeedData.filmBacks.toSet(), repository.observeFilmBacks().first().toSet())
        assertEquals(DefaultSeedData.filmMedia.toSet(), repository.observeAvailableFilmMedia().first().toSet())
    }

    @Test
    fun `seedIfEmpty does not duplicate rows when called twice`() = runTest {
        repository.seedIfEmpty()
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.filmMedia.size, repository.observeAvailableFilmMedia().first().size)
    }

    @Test
    fun `available film media excludes completed and archived film media`() = runTest {
        repository.seedIfEmpty()
        val completedMedium = DefaultSeedData.portra400Medium.copy(
            id = "extra-completed-medium",
            status = FilmMediumStatus.COMPLETED,
        )
        database.filmMediumDao().upsertAll(listOf(completedMedium.toEntity()))

        val available = repository.observeAvailableFilmMedia().first()

        assertTrue(available.none { it.id == completedMedium.id })
    }

    @Test
    fun `switcher film media include available and completed, sorted available-first`() = runTest {
        repository.seedIfEmpty()
        val completedMedium = DefaultSeedData.portra400Medium.copy(id = "extra-completed-medium", status = FilmMediumStatus.COMPLETED)
        val archivedMedium = DefaultSeedData.portra400Medium.copy(id = "extra-archived-medium", status = FilmMediumStatus.ARCHIVED)
        database.filmMediumDao().upsertAll(listOf(completedMedium.toEntity(), archivedMedium.toEntity()))

        val switcherFilmMedia = repository.observeSwitcherFilmMedia().first()

        assertTrue(switcherFilmMedia.none { it.id == archivedMedium.id })
        assertTrue(switcherFilmMedia.any { it.id == completedMedium.id })
        val lastAvailableIndex = switcherFilmMedia.indexOfLast { it.status == FilmMediumStatus.AVAILABLE }
        val firstCompletedIndex = switcherFilmMedia.indexOfFirst { it.status == FilmMediumStatus.COMPLETED }
        assertTrue(lastAvailableIndex < firstCompletedIndex)
    }

    @Test
    fun `markFilmMediumCompletedLocally flips the film medium's status immediately`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.filmMedia.first().id

        repository.markFilmMediumCompletedLocally(filmMediumId)

        assertEquals(FilmMediumStatus.COMPLETED, repository.getFilmMedium(filmMediumId)?.status)
    }

    @Test
    fun `saving the first exposure on a film medium assigns frame number 1`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id

        val saved = repository.saveExposure(draftExposure(filmMediumId))

        assertEquals(1, saved.frameNumber)
    }

    @Test
    fun `saving successive exposures increments the frame number`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id

        repository.saveExposure(draftExposure(filmMediumId, notes = "first"))
        repository.saveExposure(draftExposure(filmMediumId, notes = "second"))
        val third = repository.saveExposure(draftExposure(filmMediumId, notes = "third"))

        assertEquals(3, third.frameNumber)
        assertEquals(3, repository.observeExposures(filmMediumId).first().size)
    }

    @Test
    fun `frame numbering is independent per film medium`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id))

        val firstOnOtherMedium = repository.saveExposure(draftExposure(DefaultSeedData.hp5Medium.id))

        assertEquals(1, firstOnOtherMedium.frameNumber)
    }

    @Test
    fun `observeExposures returns frames most-recent-first`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        repository.saveExposure(draftExposure(filmMediumId, notes = "one"))
        repository.saveExposure(draftExposure(filmMediumId, notes = "two"))
        repository.saveExposure(draftExposure(filmMediumId, notes = "three"))

        val frameNumbers = repository.observeExposures(filmMediumId).first().map { it.frameNumber }

        assertEquals(listOf(3, 2, 1), frameNumbers)
    }

    @Test
    fun `updateExposure persists field changes and marks the exposure pending sync`() = runTest {
        repository.seedIfEmpty()
        val saved = repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id))

        repository.updateExposure(saved.copy(isoUsed = 800, aperture = 11.0, syncStatus = SyncStatus.SYNCED))

        val updated = repository.getExposure(saved.id)
        assertEquals(800, updated?.isoUsed)
        assertEquals(11.0, updated?.aperture)
        assertEquals(SyncStatus.PENDING_SYNC, updated?.syncStatus)
    }

    @Test
    fun `updateExposure does not change the frame number`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        repository.saveExposure(draftExposure(filmMediumId))
        val second = repository.saveExposure(draftExposure(filmMediumId))

        repository.updateExposure(second.copy(isoUsed = 1600))

        assertEquals(2, repository.getExposure(second.id)?.frameNumber)
    }

    @Test
    fun `updateExposure does not change the last-used-settings defaults`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        val first = repository.saveExposure(draftExposure(filmMediumId))
        repository.saveExposure(draftExposure(filmMediumId))
        val lastUsedBeforeEdit = repository.observeLastUsedExposureSettings().first()

        repository.updateExposure(first.copy(isoUsed = 3200, aperture = 22.0))

        assertEquals(lastUsedBeforeEdit, repository.observeLastUsedExposureSettings().first())
    }

    @Test
    fun `toggleFavorite flips the flag and round-trips both directions`() = runTest {
        repository.seedIfEmpty()
        val saved = repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id))
        assertEquals(false, saved.isFavorite)

        repository.toggleFavorite(saved.id, true)
        assertEquals(true, repository.getExposure(saved.id)?.isFavorite)

        repository.toggleFavorite(saved.id, false)
        assertEquals(false, repository.getExposure(saved.id)?.isFavorite)
    }

    @Test
    fun `toggleFavorite does not change the last-used-settings defaults`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        val saved = repository.saveExposure(draftExposure(filmMediumId))
        repository.saveExposure(draftExposure(filmMediumId))
        val lastUsedBeforeToggle = repository.observeLastUsedExposureSettings().first()

        repository.toggleFavorite(saved.id, true)

        assertEquals(lastUsedBeforeToggle, repository.observeLastUsedExposureSettings().first())
    }

    @Test
    fun `exposures survive a fresh repository over the same underlying database`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        val saved = repository.saveExposure(draftExposure(filmMediumId, notes = "restart check"))

        val reopened = ExposureRepository(database)
        val persisted = reopened.getExposure(saved.id)

        assertEquals(saved, persisted)
    }

    @Test
    fun `getFilmMedium returns null for an unknown id`() = runTest {
        assertNull(repository.getFilmMedium("does-not-exist"))
    }

    @Test
    fun `seedIfEmpty defaults the active film medium to the first seed film medium`() = runTest {
        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.filmMedia.first().id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `setActiveFilmMedium updates the observed active film medium id`() = runTest {
        repository.seedIfEmpty()

        repository.setActiveFilmMedium(DefaultSeedData.hp5Medium.id)

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `seedIfEmpty does not overwrite an already-selected active film medium`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.hp5Medium.id)

        repository.seedIfEmpty()

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `ensureAppStateInitialized starts with no active film medium and no seeded equipment`() = runTest {
        repository.ensureAppStateInitialized()

        assertNull(repository.observeActiveFilmMediumId().first())
        assertTrue(repository.observeCameraBodies().first().isEmpty())
        assertTrue(repository.observeLenses().first().isEmpty())
        assertTrue(repository.observeAvailableFilmMedia().first().isEmpty())
    }

    @Test
    fun `ensureAppStateInitialized does not overwrite an already-selected active film medium`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.hp5Medium.id)

        repository.ensureAppStateInitialized()

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
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
            draftExposure(DefaultSeedData.portra400Medium.id).copy(lensId = DefaultSeedData.sekor50mmF45.id),
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
    fun `applyFilmMediaSync keeps the active film medium when it still exists`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.hp5Medium.id)

        repository.applyFilmMediaSync(DefaultSeedData.filmMedia)

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `applyFilmMediaSync keeps active film medium when payload omits it`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.hp5Medium.id)

        repository.applyFilmMediaSync(listOf(DefaultSeedData.portra400Medium)) // partial payload update only

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `applyFilmMediaSync keeps active film medium when payload is empty`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.portra400Medium.id)

        repository.applyFilmMediaSync(emptyList())

        assertEquals(DefaultSeedData.portra400Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `applyFilmMediaSync does not delete referenced historical film media`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id))
        repository.setActiveFilmMedium(DefaultSeedData.portra400Medium.id)

        repository.applyFilmMediaSync(emptyList())

        val remaining = repository.observeFilmMedium(DefaultSeedData.portra400Medium.id).first()
        assertEquals(DefaultSeedData.portra400Medium.id, remaining?.id)
        assertEquals(DefaultSeedData.portra400Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `applyFilmMediaSync falls back to another film medium when the active one is completed, not just removed`() = runTest {
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.portra400Medium.id)

        // The film medium is still present in the sync — just no longer AVAILABLE (e.g. just completed).
        val completed = DefaultSeedData.portra400Medium.copy(status = FilmMediumStatus.COMPLETED)
        repository.applyFilmMediaSync(listOf(completed, DefaultSeedData.hp5Medium))

        assertEquals(DefaultSeedData.hp5Medium.id, repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `applyFilmMediaSync clears the active film medium when no film medium remains available`() = runTest {
        // Sync is merge-only (upsertAll, never deletes) — so unlike a destructive replace, omitting
        // a film medium from the payload doesn't remove it. To genuinely have no AVAILABLE fallback
        // left, every film medium must be explicitly marked non-available, not merely left out of
        // the payload.
        repository.seedIfEmpty()
        repository.setActiveFilmMedium(DefaultSeedData.portra400Medium.id)

        val completedPortra = DefaultSeedData.portra400Medium.copy(status = FilmMediumStatus.COMPLETED)
        val completedHp5 = DefaultSeedData.hp5Medium.copy(status = FilmMediumStatus.COMPLETED)
        repository.applyFilmMediaSync(listOf(completedPortra, completedHp5))

        assertNull(repository.observeActiveFilmMediumId().first())
    }

    @Test
    fun `updateExposurePhotoStatus only touches the targeted exposure`() = runTest {
        repository.seedIfEmpty()
        val filmMediumId = DefaultSeedData.portra400Medium.id
        val a = repository.saveExposure(draftExposure(filmMediumId))
        val b = repository.saveExposure(draftExposure(filmMediumId))

        repository.updateExposurePhotoStatus(a.id, PhotoStatus.CAPTURED)

        assertEquals(PhotoStatus.CAPTURED, requireNotNull(repository.getExposure(a.id)).referencePhotoStatus)
        assertEquals(PhotoStatus.NONE, requireNotNull(repository.getExposure(b.id)).referencePhotoStatus)
    }

    @Test
    fun `getAllExposuresOnce returns exposures across every film medium`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id))
        repository.saveExposure(draftExposure(DefaultSeedData.hp5Medium.id))

        assertEquals(2, repository.getAllExposuresOnce().size)
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
        val draft = draftExposure(DefaultSeedData.portra400Medium.id, focalLengthMm = 50).copy(
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
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id, focalLengthMm = 50))

        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id, focalLengthMm = null))

        val settings = repository.observeLastUsedExposureSettings().first()
        assertEquals(50, settings.focalLengthMm)
    }

    @Test
    fun `last-used settings reflect the most recently saved exposure, even on a different film medium`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id).copy(isoUsed = 400))

        repository.saveExposure(draftExposure(DefaultSeedData.hp5Medium.id).copy(isoUsed = 1600))

        assertEquals(1600, repository.observeLastUsedExposureSettings().first().iso)
    }

    @Test
    fun `saveExposure records a chosen zone as the new last-used zone`() = runTest {
        repository.seedIfEmpty()

        repository.saveExposure(draftExposure(DefaultSeedData.hp5Medium.id, zone = 3))

        assertEquals(3, repository.observeLastUsedExposureSettings().first().zone)
    }

    @Test
    fun `saving an exposure with no zone does not clear a previously recorded last-used zone`() = runTest {
        repository.seedIfEmpty()
        repository.saveExposure(draftExposure(DefaultSeedData.hp5Medium.id, zone = 7))

        // A film medium with no light meter never collects a zone, so this save passes zone = null.
        repository.saveExposure(draftExposure(DefaultSeedData.portra400Medium.id, zone = null))

        assertEquals(7, repository.observeLastUsedExposureSettings().first().zone)
    }
}
