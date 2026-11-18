package com.craftworks.music.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.TvDimens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The one chrome host every TV destination renders inside (spec §5.3).
 *
 * Draws the canvas gradient, records the content into a backdrop layer, and floats
 * [TvTopTabBar] over it — so the bar blurs the screen beneath instead of a flat fill.
 *
 * Content is drawn **edge-to-edge**: it extends under the bar, which is what gives the glass
 * something to sample when a list scrolls. Screens that want their first row to start below the
 * bar add [LocalTvTopInset] to their top padding; until a screen adopts it (P2), that first row
 * sits behind the bar. The alternative — padding the content slot — leaves the bar blurring
 * nothing, which is worse for the Gate 1 check.
 *
 * Back (spec §5.4 rule 4): on a tab root, Back moves focus into the bar instead of leaving the
 * app. Once the bar itself has focus the handler is disabled, so Back falls through to the
 * system and exits — rules 3 and 5 come from the nav host and the system respectively.
 */
@Composable
fun TvAppScaffold(
    currentTab: TvTab?,
    isTabRoot: Boolean,
    onTabSelected: (TvTab) -> Unit,
    modifier: Modifier = Modifier,
    chromeVisible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = LocalTvColors.current

    val backdrop = rememberLayerBackdrop {
        drawRect(colors.bgCanvas)
        drawContent()
    }

    val tabBarRequester = remember { FocusRequester() }
    var tabBarFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isTabRoot && !tabBarFocused) {
        tabBarRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.bgCanvasTop, colors.bgCanvas, colors.bgBase)
                )
            )
    ) {
        CompositionLocalProvider(
            LocalTvTopInset provides (TvDimens.tabBarHeight + TvDimens.tabBarTopInset),
            LocalTvFocusSearchOnEntry provides (currentTab == TvTab.Search),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                content()
            }
        }

        if (chromeVisible) {
            // A wrapper rather than padding on the bar itself: padding there would sit *inside* the
            // glass pill. Here it only caps the width the bar may take and centres what it measures
            // to, so the bar keeps a 120dp gutter on both sides without growing a hollow pill.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = TvDimens.tabBarSidePadding,
                        end = TvDimens.tabBarSidePadding,
                        top = TvDimens.tabBarTopInset,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                TvTopTabBar(
                    backdrop = backdrop,
                    currentTab = currentTab,
                    onTabSelected = onTabSelected,
                    focusRequester = tabBarRequester,
                    modifier = Modifier.onFocusChanged { tabBarFocused = it.hasFocus },
                )
            }
        }
    }
}
