package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvFocusTarget

/**
 * A settings row with a trailing switch (spec §6.8): the whole row toggles, the switch is a
 * passive indicator so the D-pad has exactly one target per row.
 *
 * The row grows with [heightIn] instead of a fixed height, so Large Text can raise the type scale
 * without clipping the label.
 */
@Composable
fun TvSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TvDimens.settingsRowHeight)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.02f,
                lift = 0.dp,
                halo = enabled,
            )
            .background(colors.surface1, shape)
            .border(TvDimens.hairlineWidth, colors.hairline, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = title,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = TvDimens.space24, vertical = TvDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body,
                color = if (enabled) colors.labelPrimary else colors.labelDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = type.caption,
                    color = colors.labelTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        SwitchIndicator(checked = checked, enabled = enabled)
    }
}

/** The switch itself: a 32×18dp track with a 14dp knob, accent-colored when on. */
@Composable
private fun SwitchIndicator(checked: Boolean, enabled: Boolean) {
    val colors = LocalTvColors.current
    val track = RoundedCornerShape(percent = 50)

    val trackColor = when {
        !enabled -> colors.surface1
        checked -> colors.accent
        else -> colors.surface3
    }

    Box(
        modifier = Modifier
            .size(width = TvDimens.switchWidth, height = TvDimens.switchHeight)
            .background(trackColor, track)
            .border(TvDimens.hairlineWidth, colors.hairline, track)
            .padding(TvDimens.switchKnobPadding),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(TvDimens.switchKnob)
                .background(
                    color = if (enabled) colors.labelPrimary else colors.labelDisabled,
                    shape = track,
                ),
        )
    }
}

/**
 * A settings row whose value is adjusted with D-pad Left/Right (lyrics animation speed).
 * OK nudges the value up, matching the direction of travel; both are one coarse step.
 */
@Composable
fun TvSettingsSliderRow(
    title: String,
    valueLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TvDimens.settingsRowHeight)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.02f,
                lift = 0.dp,
            )
            .background(colors.surface1, shape)
            .border(TvDimens.hairlineWidth, colors.hairline, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = title,
                onClick = onIncrease,
            )
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onDecrease()
                        true
                    }

                    Key.DirectionRight -> {
                        onIncrease()
                        true
                    }

                    else -> false
                }
            }
            .padding(horizontal = TvDimens.space24, vertical = TvDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
    ) {
        Text(
            text = title,
            style = type.body,
            color = colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueLabel,
            style = type.caption,
            color = colors.labelTertiary,
            maxLines = 1,
        )
    }
}

/** A settings section label above a group of rows (spec §6.8). */
@Composable
fun TvSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Text(
        text = title,
        style = type.sectionTitle,
        color = colors.labelSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = TvDimens.space8),
    )
}
