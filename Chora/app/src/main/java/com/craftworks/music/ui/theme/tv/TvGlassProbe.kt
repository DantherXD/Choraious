package com.craftworks.music.ui.theme.tv

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Launch-argument names for the temporary glass probe (removed in P5). */
object TvGlassProbeArgs {
    const val EXTRA_ENABLED = "glassProbe"
    const val EXTRA_TIER = "glassTier"
}

/**
 * Temporary P0 debug probe (plan P0-DEBUG): a `chrome` bar, a `panel` and three `pill`s over
 * a scrolling artwork list, so blur + lens can be validated on-device before P1/P2 exist.
 *
 * Reachable only from the TV branch of `MainActivity` when `BuildConfig.DEBUG` is set and the
 * launch intent carries `glassProbe=true`. `glassTier=Solid|BlurOnly|Full` forces a tier for
 * the fallback check. Deleted in P5.
 *
 * Text is hardcoded on purpose: this is a developer surface, never shown to users.
 */
@Composable
fun TvGlassProbe() {
    val forcedTier = rememberForcedTier()
    val tier = forcedTier ?: LocalGlassTier.current

    CompositionLocalProvider(LocalGlassTier provides tier) {
        val colors = LocalTvColors.current
        val type = LocalTvTypography.current
        val backdrop = rememberLayerBackdrop {
            drawRect(colors.bgCanvas)
            drawContent()
        }

        Box(Modifier.fillMaxSize().background(colors.bgBase)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = TvDimens.screenMarginHorizontal,
                    end = TvDimens.screenMarginHorizontal,
                    top = 150.dp,
                    bottom = 220.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space24),
            ) {
                items(PROBE_ARTWORK.size) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(probeArtwork(index))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TvDimens.tabBarTopInset)
                    .fillMaxWidth(0.86f)
                    .height(TvDimens.tabBarHeight)
                    .tvGlass(backdrop, TvGlassPresets.chrome)
                    .padding(horizontal = TvDimens.space32),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvDimens.tabBarItemGap),
            ) {
                Text("Listen Now", style = type.tab, color = colors.labelPrimary)
                Text("Browse", style = type.tab, color = colors.labelSecondary)
                Text("Radio", style = type.tab, color = colors.labelSecondary)
                Spacer(Modifier.weight(1f))
                Text("tier: ${tier.name}", style = type.caption, color = colors.labelSecondary)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = TvDimens.screenMarginBottom)
                    .fillMaxWidth(0.7f)
                    .tvGlass(backdrop, TvGlassPresets.panel)
                    .padding(TvDimens.space32),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space12),
            ) {
                Text("Glass probe", style = type.sectionTitle, color = colors.labelPrimary)
                Text(
                    "chrome + panel over a scrolling list. Force a tier with --es glassTier Solid|BlurOnly|Full.",
                    style = type.caption,
                    color = colors.labelSecondary,
                )
                Text(
                    "blur=${tier.supportsBlur}  lens=${tier.supportsLens}  frozen=${LocalGlassFrozen.current}",
                    style = type.caption,
                    color = colors.labelTertiary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.space16)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(56.dp)
                                .tvGlass(backdrop, TvGlassPresets.pill)
                                .padding(horizontal = TvDimens.space16),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("pill ${index + 1}", style = type.caption, color = colors.labelPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberForcedTier(): GlassTier? {
    val context = LocalContext.current
    return remember(context) {
        (context as? Activity)
            ?.intent
            ?.getStringExtra(TvGlassProbeArgs.EXTRA_TIER)
            ?.let { raw -> GlassTier.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
    }
}

private val PROBE_ARTWORK = listOf(
    Color(0xFF3A1C71) to Color(0xFFD76D77),
    Color(0xFF0F2027) to Color(0xFF2C5364),
    Color(0xFF42275A) to Color(0xFF734B6D),
    Color(0xFF1D976C) to Color(0xFF93F9B9),
    Color(0xFF870000) to Color(0xFF190A05),
    Color(0xFF232526) to Color(0xFF414345),
    Color(0xFF005AA7) to Color(0xFFFFFDE4),
    Color(0xFF603813) to Color(0xFFB29F94),
    Color(0xFF136A8A) to Color(0xFF267871),
    Color(0xFF4B134F) to Color(0xFFC94B4B),
)

private fun probeArtwork(index: Int): Brush {
    val (start, end) = PROBE_ARTWORK[index % PROBE_ARTWORK.size]
    return Brush.linearGradient(listOf(start, end))
}
