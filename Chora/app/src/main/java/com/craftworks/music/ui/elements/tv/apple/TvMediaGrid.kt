package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import kotlinx.coroutines.flow.filter

/**
 * The five-column list grid every library screen shares (spec §7).
 *
 * One header row holds the screen title and its sort/filter controls, so the controls scroll away
 * with the list instead of eating fixed height. Paging is offset-driven: when the last visible
 * index comes within [PagingThreshold] of the end, [onLoadMore] fires once, which is the same
 * rule the drawer-era screens used.
 */
@Composable
fun <T> TvMediaGrid(
    items: List<T>,
    title: String,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    onLoadMore: (() -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    if (onLoadMore != null) {
        LaunchedEffect(gridState, items.size) {
            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: return@snapshotFlow false
                val total = gridState.layoutInfo.totalItemsCount
                lastVisible >= total - PagingThreshold
            }.filter { it }.collect { onLoadMore() }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(5),
        modifier = modifier
            .fillMaxSize()
            .focusGroup(),
        contentPadding = PaddingValues(
            start = TvDimens.screenMarginHorizontal,
            end = TvDimens.screenMarginHorizontal,
            top = topInset + TvDimens.screenMarginTop,
            bottom = TvDimens.screenMarginBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = TvDimens.space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
            ) {
                Text(
                    text = title,
                    style = type.screenTitle,
                    color = colors.labelPrimary,
                    modifier = Modifier.weight(1f),
                )
                headerActions?.invoke(this)
            }
        }

        items(items, key = key) { item ->
            itemContent(item)
        }
    }
}

private const val PagingThreshold = 15
