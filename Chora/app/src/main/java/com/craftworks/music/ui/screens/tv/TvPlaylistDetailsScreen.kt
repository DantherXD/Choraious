package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvContextMenu
import com.craftworks.music.ui.elements.tv.apple.TvContextMenuItem
import com.craftworks.music.ui.elements.tv.apple.TvDetailHeader
import com.craftworks.music.ui.elements.tv.apple.TvDetailMiniHeader
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvTrackRow
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvDpadScrollOnEdge
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.utils.StringUtils
import kotlinx.coroutines.launch

/**
 * Playlist detail (spec §6): same shell as album detail — collapsing header, Play / Shuffle, track
 * rows — but the tracks come from `selectedPlaylistSongs`, which has no leading playlist item.
 */
@Composable
fun TvPlaylistDetails(
    selectedPlaylistId: String? = null,
    selectedPlaylistImage: String? = null,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: PlaylistScreenViewModel = hiltViewModel(),
) {
    val playlistMetadata = viewModel.selectedPlaylist.collectAsStateWithLifecycle().value?.mediaMetadata
    val songs by viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topInset = LocalTvTopInset.current
    val collapseDistancePx = with(LocalDensity.current) { TvDimens.detailArtworkLarge.toPx() }

    var menuSong by remember { mutableStateOf<MediaItem?>(null) }
    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlaylistId) {
        if (selectedPlaylistId != null) viewModel.loadPlaylistDetails(selectedPlaylistId)
    }

    val collapse by remember(listState, collapseDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    val title = playlistMetadata?.title?.toString().orEmpty()
    val artworkUrl = playlistMetadata?.artworkUri?.toString() ?: selectedPlaylistImage
    val currentMediaId = mediaController?.currentMediaItem?.mediaId

    Box(modifier = Modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            TvEmptyState(
                title = stringResource(R.string.tv_browse_empty_title),
                body = if (isLoading) null else stringResource(R.string.tv_browse_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .tvDpadScrollOnEdge(listState),
                contentPadding = PaddingValues(
                    start = TvDimens.screenMarginHorizontal,
                    end = TvDimens.screenMarginHorizontal,
                    top = topInset,
                    bottom = TvDimens.screenMarginBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
            ) {
                item(key = "header") {
                    val totalMs = remember(songs) {
                        songs.sumOf { it.mediaMetadata.durationMs ?: 0L }
                    }

                    TvDetailHeader(
                        artworkUrl = artworkUrl,
                        cacheKey = selectedPlaylistId,
                        title = title,
                        subtitle = playlistMetadata?.artist?.toString(),
                        metadata = stringResource(
                            R.string.tv_library_song_count,
                            songs.size,
                        ) + " • " + StringUtils.formatSeconds((totalMs / 1000).toInt()),
                        collapseFraction = collapse,
                        actions = {
                            Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.space16)) {
                                TvGlassButton(
                                    text = stringResource(R.string.action_play),
                                    style = TvButtonStyle.Primary,
                                    iconRes = R.drawable.round_play_arrow_24,
                                    onClick = {
                                        scope.launch {
                                            SongHelper.play(songs, 0, mediaController)
                                            navHostController.navigate(Screen.NowPlayingLandscape) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                )
                                TvGlassButton(
                                    text = stringResource(R.string.action_shuffle),
                                    iconRes = R.drawable.round_shuffle_28,
                                    onClick = {
                                        scope.launch {
                                            SongHelper.play(songs.shuffled(), 0, mediaController)
                                            navHostController.navigate(Screen.NowPlayingLandscape) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    )
                }

                itemsIndexed(songs, key = { _, song -> song.mediaId }) { index, song ->
                    TvTrackRow(
                        index = index + 1,
                        title = song.mediaMetadata.title?.toString().orEmpty(),
                        artist = song.mediaMetadata.artist?.toString(),
                        durationLabel = song.mediaMetadata.durationMs
                            ?.let { StringUtils.formatSeconds((it / 1000).toInt()) },
                        isPlaying = currentMediaId != null && song.mediaId == currentMediaId,
                        onClick = {
                            scope.launch {
                                SongHelper.play(songs, index, mediaController)
                                navHostController.navigate(Screen.NowPlayingLandscape) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLongClick = { menuSong = song },
                    )
                }
            }

            TvDetailMiniHeader(
                title = title,
                artworkUrl = artworkUrl,
                cacheKey = selectedPlaylistId,
                visible = collapse > 0.9f,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset),
            )
        }

        menuSong?.let { song ->
            TvContextMenu(
                title = song.mediaMetadata.title?.toString().orEmpty(),
                items = listOf(
                    TvContextMenuItem(stringResource(R.string.action_play_next)) {
                        SongHelper.playNext(listOf(song), mediaController)
                    },
                    TvContextMenuItem(stringResource(R.string.action_add_to_queue)) {
                        SongHelper.enqueue(listOf(song), mediaController)
                    },
                    TvContextMenuItem(stringResource(R.string.action_more)) {
                        selectedSong = song
                        showSongDialog = true
                    },
                ),
                onDismiss = { menuSong = null },
            )
        }
    }

    if (showSongDialog) {
        SongDialog(
            song = selectedSong,
            onSetRating = { rating ->
                selectedSong.mediaMetadata.id?.let { id ->
                    viewModel.setSongRating(id, rating)
                }
            },
            onDownload = { viewModel.downloadSong(it) },
            setShowDialog = { showSongDialog = it },
        )
    }
}
