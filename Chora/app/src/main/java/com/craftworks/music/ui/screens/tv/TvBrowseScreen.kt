package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.TvSectionHeader
import com.craftworks.music.ui.elements.tv.apple.TvSkeleton
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvDpadScrollOnEdge
import com.craftworks.music.ui.viewmodels.BrowseViewModel

/**
 * Browse (spec §7): curated rails plus genre chips.
 *
 * The genre row only appears when the provider can actually list genres — every provider currently
 * stubs that endpoint (see `GenreRepository`), so in practice this screen is curated rails today
 * and gains chips the day a provider implements the call. Selected genres filter through the album
 * query; selecting the same chip again clears the filter.
 */
@Composable
fun TvBrowseScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val newReleases by viewModel.newReleases.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val randomPicks by viewModel.randomPicks.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val genreAlbums by viewModel.genreAlbums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .tvDpadScrollOnEdge(listState),
        contentPadding = PaddingValues(
            start = TvDimens.screenMarginHorizontal,
            end = TvDimens.screenMarginHorizontal,
            top = topInset + TvDimens.screenMarginTop,
            bottom = TvDimens.screenMarginBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.nav_browse),
                style = type.screenTitle,
                color = colors.labelPrimary,
            )
        }

        if (!genres.isNullOrEmpty()) {
            item(key = "genres") {
                Column(
                    modifier = Modifier.focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap),
                ) {
                    TvSectionHeader(title = stringResource(R.string.tv_browse_genres))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(TvDimens.space12)) {
                        items(genres.orEmpty(), key = { it.name }) { genre ->
                            GenreChip(
                                name = genre.name,
                                selected = genre.name == selectedGenre,
                                onClick = { viewModel.selectGenre(genre.name) },
                            )
                        }
                    }
                }
            }
        }

        if (selectedGenre != null && genreAlbums.isNotEmpty()) {
            item(key = "genre-results") {
                Rail(title = selectedGenre.orEmpty(), albums = genreAlbums, navHostController)
            }
        }

        if (newReleases.isNotEmpty()) {
            item(key = "new") {
                Rail(
                    title = stringResource(R.string.tv_browse_new_releases),
                    albums = newReleases,
                    navHostController = navHostController,
                    onSeeAll = {
                        navHostController.navigate(Screen.HomeLists("recently_added")) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                Rail(
                    title = stringResource(R.string.home_most_played),
                    albums = mostPlayed,
                    navHostController = navHostController,
                    onSeeAll = {
                        navHostController.navigate(Screen.HomeLists("most_played")) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        if (randomPicks.isNotEmpty()) {
            item(key = "picks") {
                Rail(
                    title = stringResource(R.string.home_explore_library),
                    albums = randomPicks,
                    navHostController = navHostController,
                )
            }
        }

        if (isLoading && newReleases.isEmpty() && mostPlayed.isEmpty()) {
            item(key = "skeletons") {
                Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space32)) {
                    repeat(2) {
                        Column(verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap)) {
                            TvSkeleton(
                                modifier = Modifier
                                    .width(TvDimens.tileSize)
                                    .height(TvDimens.space24)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap)) {
                                repeat(5) {
                                    TvSkeleton(modifier = Modifier.size(TvDimens.tileSize))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isEmpty && !isLoading) {
            item(key = "empty") {
                TvEmptyState(
                    title = stringResource(R.string.tv_browse_empty_title),
                    body = stringResource(R.string.tv_browse_empty_body),
                )
            }
        }
    }
}

@Composable
private fun Rail(
    title: String,
    albums: List<MediaItem>,
    navHostController: NavHostController,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap),
    ) {
        TvSectionHeader(
            title = title,
            actionLabel = if (onSeeAll != null) stringResource(R.string.action_see_all) else null,
            onAction = onSeeAll,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
            contentPadding = PaddingValues(end = TvDimens.space8),
        ) {
            items(albums, key = { it.mediaId }) { album ->
                TvMediaTile(
                    artworkUrl = album.artworkUrlAt(600),
                    cacheKey = album.artworkKey,
                    title = album.mediaMetadata.title?.toString().orEmpty(),
                    subtitle = album.mediaMetadata.artist?.toString(),
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
}

@Composable
private fun GenreChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(12.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .background(
                color = when {
                    selected -> colors.accent
                    isFocused -> colors.surface3
                    else -> colors.surface1
                },
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = name,
                onClick = onClick,
            )
            .padding(horizontal = TvDimens.space24, vertical = TvDimens.space12),
    ) {
        Text(
            text = name,
            style = type.caption,
            color = if (selected) colors.onAccent else colors.labelPrimary,
            maxLines = 1,
        )
    }
}
