package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.tv.tvLongPress

/**
 * A settings row (spec §6: 76dp, `body` label, trailing value or chevron).
 *
 * Focus is a white-8% fill plus the ring, with no scale beyond 1.02 — a settings list that grows
 * and lifts as focus moves reads as unstable next to a rail of tiles.
 */
@Composable
fun TvListRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var rowModifier = modifier
        .fillMaxWidth()
        .height(TvDimens.settingsRowHeight)
        .tvFocusTarget(
            interactionSource = interactionSource,
            shape = shape,
            focusedScale = 1.02f,
            lift = 0.dp,
            halo = onClick != null,
        )
        .background(
            color = if (isFocused) colors.surface2 else colors.surface1,
            shape = shape,
        )

    if (onClick != null) {
        rowModifier = rowModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClickLabel = label,
            onClick = onClick,
        )
    }

    Row(
        modifier = rowModifier.padding(horizontal = TvDimens.space24),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
    ) {
        Text(
            text = label,
            style = type.body,
            color = colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = type.caption,
                color = colors.labelTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showChevron) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_chevron_right_24),
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(TvDimens.space24),
            )
        }
    }
}

/**
 * A track row (spec §6: 72dp, index or equalizer, title, artist, duration right-aligned).
 *
 * The playing row is tinted with `selectionTint` and shows the equalizer instead of the index, so
 * "which track is playing" survives focus moving away from it.
 */
@Composable
fun TvTrackRow(
    index: Int,
    title: String,
    modifier: Modifier = Modifier,
    artist: String? = null,
    durationLabel: String? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background = when {
        isPlaying -> colors.selectionTint
        isFocused -> colors.surface2
        else -> colors.surface1.copy(alpha = 0.04f)
    }
    val titleColor = when {
        isPlaying -> colors.accent
        else -> colors.labelPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.trackRowHeight)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.02f,
                lift = 0.dp,
                halo = true,
            )
            .background(background, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = title,
                onClick = onClick,
            )
            .tvLongPress(onLongClick)
            .padding(horizontal = TvDimens.space24),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
    ) {
        Box(
            modifier = Modifier.width(TvDimens.space32),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                TvEqualizerGlyph(height = TvDimens.space16, barWidth = 2.dp)
            } else {
                Text(
                    text = index.toString(),
                    style = type.caption,
                    color = colors.labelTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!artist.isNullOrBlank()) {
                Text(
                    text = artist,
                    style = type.caption,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!durationLabel.isNullOrBlank()) {
            Text(
                text = durationLabel,
                style = type.caption,
                color = colors.labelTertiary,
            )
        }
    }
}
