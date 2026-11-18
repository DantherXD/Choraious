package com.craftworks.music.ui.elements.tv.apple

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens

/** One row of a [TvContextMenu]. */
data class TvContextMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * The long-press action menu (spec §6: Play Next / Add to Queue / Go to Artist).
 *
 * Rendered inside the screen's own tree rather than in a `Dialog` window, so it samples the same
 * backdrop as the screen and so Back can dismiss it (spec §5.4 rule 2) without touching the
 * navigation stack. `exit` is cancelled and Back is handled here, which is the whole focus trap:
 * the D-pad cannot wander out into the list underneath while the menu is open.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvContextMenu(
    items: List<TvContextMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    if (items.isEmpty()) return

    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = LocalTvShapes.current.panel

    val firstItem = remember { FocusRequester() }
    LaunchedEffect(items) { firstItem.requestFocus() }

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(TvDimens.menuWidth)
                .heightIn(max = TvDimens.menuMaxHeight)
                .focusRequester(firstItem)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup()
                .background(colors.surfacePressed, shape)
                .background(colors.bgCanvas.copy(alpha = 0.94f), shape)
                .border(1.dp, colors.hairline, shape)
                .padding(TvDimens.space12),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = type.caption,
                    color = colors.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = TvDimens.space24,
                        end = TvDimens.space24,
                        top = TvDimens.space12,
                        bottom = TvDimens.space8,
                    ),
                )
            }

            items.forEach { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val rowShape = RoundedCornerShape(TvDimens.listRowShape)

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isFocused) colors.surface3 else colors.surfacePressed.copy(alpha = 0f),
                            shape = rowShape,
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClickLabel = item.label,
                            onClick = {
                                onDismiss()
                                item.onClick()
                            },
                        )
                        .padding(horizontal = TvDimens.space24, vertical = TvDimens.space16),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = item.label,
                        style = type.body,
                        color = colors.labelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
