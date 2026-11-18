package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * A rail or list header: `sectionTitle` plus an optional "See All" control.
 *
 * The action is a focusable pill rather than plain text, because on a TV every actionable thing
 * has to be reachable by D-pad — and the Listen Now "See All" is the only path into the full
 * `HomeLists` grid (plan P2-H).
 */
@Composable
fun TvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = type.sectionTitle,
            color = colors.labelPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        Spacer(Modifier.width(TvDimens.space16))

        if (actionLabel != null && onAction != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val shape = RoundedCornerShape(percent = 50)

            Row(
                modifier = Modifier
                    .background(
                        color = if (isFocused) colors.surface3 else colors.surface1,
                        shape = shape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = actionLabel,
                        onClick = onAction,
                    )
                    .padding(horizontal = TvDimens.space24, vertical = TvDimens.space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvDimens.space8),
            ) {
                Text(
                    text = actionLabel,
                    style = type.caption,
                    color = if (isFocused) colors.labelPrimary else colors.labelSecondary,
                    maxLines = 1,
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.round_chevron_right_24),
                    contentDescription = null,
                    tint = if (isFocused) colors.labelPrimary else colors.labelTertiary,
                    modifier = Modifier.size(TvDimens.space16),
                )
            }
        }
    }
}
