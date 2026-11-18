package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvFocusTarget

/**
 * A circular glass icon control (transport side actions, the `⋯` menu, header back button).
 *
 * Same reasoning as [TvGlassButton]: raw `clickable` plus the shared focus treatment, so the
 * button is not double-scaled by tv-material's internal focus animation.
 */
@Composable
fun TvGlassIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = TvDimens.iconButtonSize,
    iconSize: Dp = TvDimens.iconControl,
) {
    val colors = LocalTvColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = CircleShape,
                focusedScale = TvDimens.focusScaleButton,
                lift = 0.dp,
                halo = enabled,
            )
            .background(colors.surface2, CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) colors.labelPrimary else colors.labelDisabled,
            modifier = Modifier.size(iconSize),
        )
    }
}
