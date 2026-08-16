package com.exposures.watch.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.exposures.watch.ExposuresApplication
import com.exposures.watch.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

/**
 * Manifest-registered so the system tile picker/renderer can bind to this. Kept thin and
 * mechanical — the actual "what to show" decision lives in the unit-tested [TileContentResolver];
 * nothing in this class can be meaningfully tested outside a real Wear OS device/tile renderer.
 * Tapping the tile opens [MainActivity] straight into exposure entry for the active roll.
 */
class ExposuresTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository get() = (application as ExposuresApplication).container.repository

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        serviceScope.future {
            val activeRollId = repository.observeActiveRollId().first()
            val activeRoll = activeRollId?.let { repository.getRoll(it) }
            val exposureCount = activeRoll?.let { repository.observeExposures(it.id).first().size } ?: 0
            val content = TileContentResolver.resolve(activeRoll, exposureCount)

            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(0)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layoutFor(content)))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        serviceScope.future { ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    private fun layoutFor(content: TileContent): LayoutElementBuilders.LayoutElement {
        val openAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .addKeyToExtraMapping(
                        MainActivity.EXTRA_ROLL_ID,
                        ActionBuilders.AndroidStringExtra.Builder().setValue(content.rollId.orEmpty()).build(),
                    )
                    .build(),
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("open-exposure-entry")
            .setOnClick(openAction)
            .build()

        return LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(content.headline).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(content.subline).build())
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
