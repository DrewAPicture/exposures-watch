package com.exposures.watch

import android.app.Application
import com.exposures.database.ExposuresDatabase
import com.exposures.database.ExposuresDatabaseProvider
import com.exposures.database.repository.ExposureRepository

/**
 * Hand-rolled DI container. A single repository behind one Room database doesn't earn a DI
 * framework yet — revisit once app-phone and cross-module wiring in later phases make Hilt's
 * ceremony worth it.
 */
interface AppContainer {
    val repository: ExposureRepository
}

class DefaultAppContainer(application: Application) : AppContainer {

    private val database: ExposuresDatabase by lazy { ExposuresDatabaseProvider.create(application) }

    override val repository: ExposureRepository by lazy { ExposureRepository(database) }
}
