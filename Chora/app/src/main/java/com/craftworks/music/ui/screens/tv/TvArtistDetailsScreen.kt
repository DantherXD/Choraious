package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvDetailHeader
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import kotlinx.coroutines.launch

/**
 * Artist detail (spec §6): circular hero, Play, and the discography.
 *
 * "Play" flattens every album's tracks, which is the same behaviour the drawer-era screen had —
 * providers expose no artist-level song list, so an artist's songs can only be assembled from its
 * albums.
 */
@Composable
fun TvArtistDetailsScreen(
    selectedArtistId: String? = null,
    selectedArtistImage: String? = null,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: ArtistsScreenViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val artist by viewModel.selectedArtist.collectAsStateWithLifecycle()
    val artistAlbums by viewModel.artistAlbums.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val collapseDistancePx = with(LocalDensity.current) { TvDimens.detailArtworkLarge.toPx() }

    LaunchedEffect(selectedArtistId) {
        if (selectedArtistId != null) viewModel.loadArtistDetails(selectedArtistId)
    }

    val collapse by remember(gridState, collapseDistancePx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) 1f
            else (gridState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    val artworkUrl = artist?.imageUrl
        ?: selectedArtistId?.let { id ->
            currentProvider?.getImageUrl(id, LibraryType.ALBUM_ARTIST)
        }
        ?: selectedArtistImage

    if (artist == null && artistAlbums.isEmpty()) {
        TvEmptyState(
            title = stringResource(R.string.tv_browse_empty_title),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset),
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            contentPadding = PaddingValues(
                start = TvDimens.screenMarginHorizontal,
                end = TvDimens.screenMarginHorizontal,
                top = topInset,
                bottom = TvDimens.screenMarginBottom,
            ),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TvDetailHeader(
                    artworkUrl = artworkUrl,
                    cacheKey = selectedArtistId,
                    title = artist?.name.orEmpty(),
                    subtitle = artist?.biography
                        ?.split("<a target")
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() },
                    metadata = artist?.albumCount?.let {
                        stringResource(R.string.tv_library_album_count, it)
                    },
                    circularArtwork = true,
                    collapseFraction = collapse,
                    actions = {
                        TvGlassButton(
                            text = stringResource(R.string.action_play),
                            style = TvButtonStyle.Primary,
                            iconRes = R.drawable.round_play_arrow_24,
                            onClick = {
                                scope.launch {
                                    val songs: List<MediaItem> = artistAlbums.map { album ->
                                        viewModel.getAlbum(album.mediaMetadata.id ?: "")
                                            .drop(1)
                                    }.flatten()

                                    if (songs.isNotEmpty()) {
                                        SongHelper.play(songs, 0, mediaController)
                                        navHostController.navigate(Screen.NowPlayingLandscape) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            },
                        )
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.artist_details_discography),
                    style = type.sectionTitle,
                    color = colors.labelPrimary,
                )
            }

            items(artistAlbums, key = { it.mediaId }) { album ->
                TvMediaTile(
                    artworkUrl = album.artworkUrlAt(600),
                    cacheKey = album.artworkKey,
                    title = album.mediaMetadata.title?.toString().orEmpty(),
                    subtitle = album.mediaMetadata.recordingYear?.toString(),
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
