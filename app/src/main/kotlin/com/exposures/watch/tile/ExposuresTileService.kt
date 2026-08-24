package com.exposures.watch.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
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
 * Manifest-registered so the system tile picker/renderer can bind to this. A static "Select Film"
 * quick-launcher — not a live view into any film/exposure state (protolayout can't embed the
 * app's own Compose Home screen, so this mirrors its branding/action instead of trying to be a
 * miniature live copy of it). Tapping the tile opens [MainActivity] straight into the film picker,
 * skipping Home — Home would just make you tap "Select Film" a second time.
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
                        MainActivity.EXTRA_START_FILM_MEDIA_SWITCHER,
                        ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build(),
                    )
                    .build(),
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("select-film")
            .setOnClick(openAction)
            .build()

        // Column alone can't vertically center its content (no setVerticalAlignment on
        // Column.Builder, confirmed via javap against the resolved protolayout 1.4.2 AAR — only
        // Box supports both axes), so the title+pill group is wrapped in an outer expand()ed Box
        // and centered there; the inner Column itself just wrap()s its content.
        val content = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.wrap())
            .setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Exposures")
                    .setMaxLines(1)
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                    .build(),
            )
            .addContent(selectFilmPill())
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build())
            .addContent(content)
            .build()
    }

    /**
     * A pill-shaped "Select Film" label, offset below the title by [PILL_TOP_MARGIN_DP] — same
     * fill/text colors as Material 3's baseline primaryContainer/onPrimaryContainer tokens,
     * matching the Home screen's Select Film button's color family. Protolayout has no shared
     * theme object to pull these from directly, so they're literal here.
     */
    private fun selectFilmPill(): LayoutElementBuilders.LayoutElement {
        val pill = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.wrap())
            .setHeight(DimensionBuilders.wrap())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(PRIMARY_CONTAINER_COLOR))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(DimensionBuilders.dp(PILL_CORNER_RADIUS_DP))
                                    .build(),
                            )
                            .build(),
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(DimensionBuilders.dp(PILL_HORIZONTAL_PADDING_DP))
                            .setEnd(DimensionBuilders.dp(PILL_HORIZONTAL_PADDING_DP))
                            .setTop(DimensionBuilders.dp(PILL_VERTICAL_PADDING_DP))
                            .setBottom(DimensionBuilders.dp(PILL_VERTICAL_PADDING_DP))
                            .build(),
                    )
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Select Film")
                    .setMaxLines(1)
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setColor(ColorBuilders.argb(ON_PRIMARY_CONTAINER_COLOR))
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_MEDIUM)
                            .build(),
                    )
                    .build(),
            )
            .build()

        // Plain wrapper purely for the top margin, kept separate from the pill's own (symmetric)
        // internal text padding above so the pill's content doesn't sit off-center inside it.
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.wrap())
            .setHeight(DimensionBuilders.wrap())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setTop(DimensionBuilders.dp(PILL_TOP_MARGIN_DP))
                            .build(),
                    )
                    .build(),
            )
            .addContent(pill)
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val PILL_HORIZONTAL_PADDING_DP = 16f
        private const val PILL_VERTICAL_PADDING_DP = 8f
        private const val PILL_CORNER_RADIUS_DP = 20f
        private const val PILL_TOP_MARGIN_DP = 8f

        // Material 3 baseline light-scheme primaryContainer / onPrimaryContainer, matching the
        // Home screen's Select Film button (bare MaterialTheme(), no custom color scheme).
        private const val PRIMARY_CONTAINER_COLOR = 0xFFEADDFF.toInt()
        private const val ON_PRIMARY_CONTAINER_COLOR = 0xFF21005D.toInt()
    }
}
