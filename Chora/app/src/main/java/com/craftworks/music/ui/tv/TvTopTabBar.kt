package com.craftworks.music.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.theme.tv.TvGlassPresets
import com.craftworks.music.ui.theme.tv.tvGlass
import com.kyant.backdrop.Backdrop

/**
 * The floating tab bar (spec §5.1).
 *
 * Five tabs in fixed order, each an icon over a label, with the active red dot underneath. Active =
 * 100% white label + accent icon + dot; focused = white-14% pill, 1.04 scale and a ring. The pill is
 * `TvShapes.pill`; the surface behind it is the `chrome` glass preset, sampling whatever [backdrop]
 * recorded.
 *
 * Icons were added at Gate 1: text-only labels read as a phone nav bar however small they are, and
 * on a TV an icon is the primary recognition cue from three metres away. The label stays — it is
 * the accessible name and the fallback if a glyph is ambiguous.
 *
 * Tint is always `chrome` in P1 — the 10%-to-chrome scroll ramp needs a scroll offset from each
 * screen, which arrives with the P2 screens. The ring is drawn as an inset border for now; the
 * true outside-the-clip halo is part of the P2 primitive components (the bar is clipped by its own
 * pill, so an outside ring would need the halo primitive applied to it).
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvTopTabBar(
    backdrop: Backdrop,
    currentTab: TvTab?,
    onTabSelected: (TvTab) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val pill = LocalTvShapes.current.pill

    val tabs = TvTab.entries
    val tabRequesters = remember { List(tabs.size) { FocusRequester() } }
    val activeIndex = currentTab?.let { tabs.indexOf(it) }?.takeIf { it >= 0 } ?: 0

    Row(
        modifier = modifier
            .focusRequester(focusRequester)
            // Entering the bar always lands on the active tab, not whichever tab is leftmost.
            .focusProperties { enter = { tabRequesters[activeIndex] } }
            .tvGlass(backdrop = backdrop, preset = TvGlassPresets.chrome, shape = pill)
            .height(TvDimens.tabBarHeight)
            .padding(horizontal = TvDimens.space16)
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.tabBarItemGap),
    ) {
        tabs.forEachIndexed { index, tab ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val isActive = tab == currentTab

            Column(
                modifier = Modifier
                    .focusRequester(tabRequesters[index])
                    .tvFocusScale(interactionSource)
                    .clip(pill)
                    .background(
                        color = if (isFocused) colors.surface3 else Color.Transparent,
                        shape = pill,
                    )
                    .then(
                        if (isFocused) {
                            Modifier.border(
                                width = TvDimens.focusRingWidth / 2,
                                color = colors.focusRing,
                                shape = pill,
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onTabSelected(tab) }
                    .padding(horizontal = TvDimens.space16, vertical = TvDimens.space8)
                    .semantics { selected = isActive },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(tab.iconRes),
                    contentDescription = null,
                    tint = when {
                        isActive -> colors.accent
                        isFocused -> colors.labelPrimary
                        else -> colors.labelSecondary
                    },
                    modifier = Modifier.size(TvDimens.tabIconSize),
                )
                Box(Modifier.height(TvDimens.space4))
                Text(
                    text = stringResource(tab.labelRes),
                    style = type.tab,
                    color = if (isActive) colors.labelPrimary else colors.labelSecondary,
                    maxLines = 1,
                )
                Box(Modifier.height(TvDimens.space4))
                Box(
                    modifier = Modifier
                        .size(TvDimens.tabDotSize)
                        .background(
                            color = if (isActive) colors.accent else Color.Transparent,
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}
