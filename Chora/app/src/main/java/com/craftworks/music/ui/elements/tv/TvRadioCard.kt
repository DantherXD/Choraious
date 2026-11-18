package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.elements.tv.apple.TvArtwork
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvArtworkParallax
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.tv.tvLongPress

/**
 * A radio station tile (spec §7: 3-column 16:9 grid, LIVE chip, focus zoom and parallax).
 *
 * Stations often have no artwork, so the fallback is a canvas fill plus the radio glyph rather
 * than an album placeholder — a grid of identical album placeholders reads as broken content.
 */
@Composable
fun TvRadioCard(
    name: String,
    modifier: Modifier = Modifier,
    artworkUrl: Any? = null,
    cacheKey: String? = null,
    showLiveChip: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(16.dp)

    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .tvFocusTarget(interactionSource = interactionSource, shape = shape)
                .clip(shape)
                .background(colors.bgCanvasTop)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = name,
                    onClick = onClick,
                )
                .tvLongPress(onLongClick),
        ) {
            TvArtwork(
                data = artworkUrl,
                cacheKey = cacheKey,
                modifier = Modifier
                    .fillMaxSize()
                    .tvArtworkParallax(interactionSource),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.bgCanvas.copy(alpha = 0.55f))
                        )
                    )
            )

            if (showLiveChip) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(TvDimens.space16)
                        .background(colors.scrim, RoundedCornerShape(percent = 50))
                        .padding(horizontal = TvDimens.space12, vertical = TvDimens.space4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.space8),
                ) {
                    Box(
                        modifier = Modifier
                            .background(colors.accent, RoundedCornerShape(percent = 50))
                            .padding(horizontal = TvDimens.space8, vertical = 2.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.tv_radio_live),
                            style = type.caption.copy(fontSize = type.caption.fontSize * 0.8f),
                            color = colors.onAccent,
                        )
                    }
                }
            }
        }

        Text(
            text = name,
            style = type.cardTitle,
            color = colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TvDimens.space12),
        )
    }
}
