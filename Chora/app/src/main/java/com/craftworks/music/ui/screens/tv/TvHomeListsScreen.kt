package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset

/**
 * The full grid behind a Listen Now rail's "See All" (plan P2-H).
 *
 * Before this, the TV shell pushed the *phone* `HomeListsScreen`, so a TV user tapping See All got
 * a phone layout with a top app bar inside the tab-bar shell. Same category keys, same album
 * source — only the presentation is TV now.
 *
 * Back returns to Listen Now with focus where it was, because the rail's focus group and the grid
 * are separate composables and the nav entry is restored rather than rebuilt.
 */
@Composable
fun TvHomeListsScreen(
    albums: List<MediaItem>,
    categoryKey: String,
    navHostController: NavHostController,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val titleRes = when (categoryKey) {
        "recently_played" -> R.string.home_recently_played
        "recently_added" -> R.string.home_recently_added
        "most_played" -> R.string.home_most_played
        "random_songs" -> R.string.home_explore_library
        else -> R.string.home_recently_played
    }

    val gridState = rememberLazyGridState()

    if (albums.isEmpty()) {
        TvEmptyState(
            title = stringResource(R.string.tv_browse_empty_title),
            body = stringResource(R.string.tv_browse_empty_body),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset),
        )
        return
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(5),
        modifier = Modifier
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
            Text(
                text = stringResource(titleRes),
                style = type.screenTitle,
                color = colors.labelPrimary,
                modifier = Modifier.padding(bottom = TvDimens.space8),
            )
        }

        items(albums, key = { it.mediaId }) { album ->
            TvMediaTile(
                artworkUrl = album.artworkUrlAt(600),
                cacheKey = album.artworkKey,
                title = album.mediaMetadata.title?.toString().orEmpty(),
                subtitle = album.mediaMetadata.artist?.toString(),
                fillWidth = true,
                onClick = {
                    navHostController.navigate(
                        Screen.AlbumDetails(
                            album.mediaMetadata.id ?: "",
                            album.mediaMetadata.artworkUri.toString(),
                        )
                    ) { launchSingleTop = true }
                },
            )
        }
    }
}
