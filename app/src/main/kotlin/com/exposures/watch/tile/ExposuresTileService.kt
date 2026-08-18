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
import com.exposures.watch.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future

/**
 * Manifest-registered so the system tile picker/renderer can bind to this. A static "Select Roll"
 * quick-launcher — not a live view into any roll/exposure state (protolayout can't embed the
 * app's own Compose Home screen, so this mirrors its branding/action instead of trying to be a
 * miniature live copy of it). Tapping the tile opens [MainActivity] straight into the roll picker,
 * skipping Home — Home would just make you tap "Select Roll" a second time.
 */
class ExposuresTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        serviceScope.future {
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(0)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout()))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        serviceScope.future { ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    private fun layout(): LayoutElementBuilders.LayoutElement {
        val openAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .addKeyToExtraMapping(
                        MainActivity.EXTRA_START_ROLL_SWITCHER,
                        ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build(),
                    )
                    .build(),
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("select-roll")
            .setOnClick(openAction)
            .build()

        return LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build())
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Exposures")
                    .setMaxLines(1)
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                    .setModifiers(textPaddingModifiers())
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Select Roll")
                    .setMaxLines(1)
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                    .setModifiers(textPaddingModifiers())
                    .build(),
            )
            .build()
    }

    private fun textPaddingModifiers(): ModifiersBuilders.Modifiers = ModifiersBuilders.Modifiers.Builder()
        .setPadding(
            ModifiersBuilders.Padding.Builder()
                .setStart(DimensionBuilders.dp(HORIZONTAL_TEXT_PADDING_DP))
                .setEnd(DimensionBuilders.dp(HORIZONTAL_TEXT_PADDING_DP))
                .build(),
        )
        .build()

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val HORIZONTAL_TEXT_PADDING_DP = 8f
    }
}
