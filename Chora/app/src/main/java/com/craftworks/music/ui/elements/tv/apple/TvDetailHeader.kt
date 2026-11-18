package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import kotlin.math.roundToInt
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * The collapsing detail header (spec §6): artwork 360→120dp, title 56→34sp, both driven by the
 * list's scroll offset rather than an animation.
 *
 * [collapseFraction] is 0 expanded and 1 collapsed; the screen derives it from its
 * `LazyListState` (`firstVisibleItemScrollOffset / collapseDistance`). Passing it in keeps this
 * composable free of scroll state, so the same header serves album, artist and playlist.
 */
@Composable
fun TvDetailHeader(
    artworkUrl: Any?,
    cacheKey: String?,
    title: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    circularArtwork: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shapes = LocalTvShapes.current

    val fraction = collapseFraction.coerceIn(0f, 1f)
    val artworkSize = lerp(TvDimens.detailArtworkLarge, TvDimens.detailArtworkCollapsed, fraction)
    // Interpolates between two scale styles instead of local sp literals, so the header follows the
    // type scale when it changes rather than pinning the old 56/34sp.
    val titleStyle = type.screenTitle.copy(
        fontSize = lerp(type.screenTitle.fontSize, type.sectionTitle.fontSize, fraction),
        lineHeight = lerp(type.screenTitle.lineHeight, type.sectionTitle.lineHeight, fraction),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TvDimens.space32, bottom = TvDimens.space24),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space32),
    ) {
        // The box shrinks continuously with the scroll, but the *request* is quantised into a few
        // steps: one decode per step instead of a fresh bitmap for every scroll frame.
        TvArtwork(
            data = artworkUrl,
            cacheKey = cacheKey,
            requestSize = quantise(artworkSize),
            shape = if (circularArtwork) shapes.pill else shapes.tile,
            modifier = Modifier.size(artworkSize),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .offset(y = (-TvDimens.space32) * fraction),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space12),
        ) {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = type.caption,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = title,
                style = titleStyle,
                color = colors.labelPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!metadata.isNullOrBlank() && fraction < 0.9f) {
                Text(
                    text = metadata,
                    style = type.caption,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (fraction < 0.9f) {
                Box(modifier = Modifier.padding(top = TvDimens.space8)) {
                    actions()
                }
            }
        }
    }
}

/**
 * The pinned mini-header shown once the big header has scrolled away (spec §6: artwork shrinks to
 * 120dp and pins next to a compact title).
 *
 * Rendered by the screen as an overlay above the list, never inside it, so it stays put while
 * rows scroll underneath.
 */
@Composable
fun TvDetailMiniHeader(
    title: String,
    artworkUrl: Any?,
    cacheKey: String?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvDimens.space48)
                .padding(horizontal = TvDimens.screenMarginHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
        ) {
            TvArtwork(
                data = artworkUrl,
                cacheKey = cacheKey,
                modifier = Modifier
                    .size(TvDimens.space48)
                    .clip(LocalTvShapes.current.chip),
            )
            Text(
                text = title,
                style = type.sectionTitle,
                color = colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Rounds a scroll-driven artwork size up to the next 40dp step (96→260), so the collapsing header
 * asks for a handful of decodes over a whole scroll instead of one per frame.
 */
private fun quantise(size: Dp): Dp =
    (((size.value / 40f).roundToInt() * 40f)
        .coerceIn(TvDimens.detailArtworkCollapsed.value, TvDimens.detailArtworkLarge.value)).dp
