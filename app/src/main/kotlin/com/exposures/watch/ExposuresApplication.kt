package com.exposures.watch

import android.app.Application
import androidx.wear.tiles.TileService
import com.exposures.watch.tile.ExposuresTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExposuresApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        applicationScope.launch {
            container.repository.ensureAppStateInitialized()
        }
        // The system doesn't re-invoke a TileService's onTileRequest just because the app was
        // updated — an already-bound tile keeps showing whatever it last rendered until something
        // explicitly asks for a refresh. Nudge it here so a new app version's tile content doesn't
        // require the user to manually remove/re-add the tile to see it.
        TileService.getUpdater(this).requestUpdate(ExposuresTileService::class.java)
    }
}
