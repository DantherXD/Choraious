package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvFocusTarget

/** Action emphasis (spec §4.6/§6): one filled accent button per screen, glass for the rest. */
enum class TvButtonStyle {
    Primary,
    Secondary,
}

private val ButtonShape = RoundedCornerShape(percent = 50)

/**
 * A pill action button.
 *
 * tv-material3's `Button` is avoided on purpose: it animates its own focus scale, which would
 * multiply with [tvFocusTarget]'s. The secondary style draws the `pill` preset's tint without
 * sampling a backdrop — small controls stay inside the §4.5 glass budget, and the result is
 * identical to what `GlassTier.Solid` would give on an older box.
 */
@Composable
fun TvGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TvButtonStyle = TvButtonStyle.Secondary,
    iconRes: Int? = null,
    enabled: Boolean = true,
    contentDescription: String? = text,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val interactionSource = remember { MutableInteractionSource() }

    val background = when {
        !enabled -> colors.surface1
        style == TvButtonStyle.Primary -> colors.accent
        else -> colors.surface2
    }
    val labelColor = when {
        !enabled -> colors.labelDisabled
        style == TvButtonStyle.Primary -> colors.onAccent
        else -> colors.labelPrimary
    }

    Row(
        modifier = modifier
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = ButtonShape,
                focusedScale = TvDimens.focusScaleButton,
                lift = 0.dp,
                halo = enabled,
            )
            .background(background, ButtonShape)
            .then(
                if (style == TvButtonStyle.Secondary && enabled) {
                    Modifier.border(1.dp, colors.hairline, ButtonShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = TvDimens.buttonHeight)
            .padding(horizontal = TvDimens.space32)
            .alpha(if (enabled) 1f else 0.9f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
    ) {
        if (iconRes != null) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(TvDimens.iconInline),
            )
        }
        Text(
            text = text,
            style = type.button,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
