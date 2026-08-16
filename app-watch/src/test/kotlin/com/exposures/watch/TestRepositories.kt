package com.exposures.watch

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.database.repository.ExposureRepository

/** An in-memory, seeded repository for ViewModel tests. Room is test-only here — see build.gradle.kts. */
suspend fun createSeededTestRepository(): ExposureRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, ExposuresDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = ExposureRepository(database)
    repository.seedIfEmpty()
    return repository
}
