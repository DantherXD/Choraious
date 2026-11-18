package com.craftworks.music.ui.screens.tv

import android.net.Uri
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
import com.craftworks.music.data.model.favorite
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
import com.craftworks.music.ui.elements.tv.apple.TvGlassIconButton
import com.craftworks.music.ui.elements.tv.apple.TvTrackRow
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import com.craftworks.music.utils.StringUtils
import kotlinx.coroutines.launch

/**
 * Album detail (spec §6): collapsing header, action row, track list.
 *
 * `songsInAlbum` carries the album itself at index 0 and its tracks after it, so the header reads
 * `first()` and the list is `drop(1)`. The collapse fraction comes from the list's first visible
 * offset — no animation spec, as the spec requires for scroll-driven collapse.
 *
 * The playing row is marked from the controller rather than from local state, so it stays correct
 * when playback continues after navigating away and back.
 */
@Composable
fun TvAlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    viewModel: AlbumDetailsViewModel = hiltViewModel(),
) {
    val items by viewModel.songsInAlbum.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val album = items.firstOrNull()
    val songs = remember(items) { items.drop(1) }

    var menuSong by remember { mutableStateOf<MediaItem?>(null) }
    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }
    var isFavorite by remember(album) { mutableStateOf(album?.mediaMetadata?.favorite ?: false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topInset = LocalTvTopInset.current

    // The header collapses over roughly the expanded artwork's height, in pixels.
    val collapseDistancePx = with(LocalDensity.current) { TvDimens.detailArtworkLarge.toPx() }

    LaunchedEffect(selectedAlbumId) { viewModel.loadAlbumDetails(selectedAlbumId) }

    val collapse by remember(listState, collapseDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    val currentMediaId = mediaController?.currentMediaItem?.mediaId
    val artworkUrl = album?.artworkUrlAt(800)
        ?: selectedAlbumImage.takeIf { it != Uri.EMPTY }?.toString()

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TvDimens.screenMarginHorizontal,
                    end = TvDimens.screenMarginHorizontal,
                    top = topInset,
                    bottom = TvDimens.screenMarginBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
            ) {
                item(key = "header") {
                    TvDetailHeader(
                        artworkUrl = artworkUrl,
                        cacheKey = album?.artworkKey ?: selectedAlbumId,
                        title = album?.mediaMetadata?.title?.toString().orEmpty(),
                        subtitle = album?.mediaMetadata?.artist?.toString(),
                        metadata = albumMetadata(songs.size),
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
                                TvGlassIconButton(
                                    iconRes = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24,
                                    contentDescription = stringResource(R.string.action_add_to_favorites),
                                    onClick = {
                                        val id = album?.mediaMetadata?.id ?: return@TvGlassIconButton
                                        if (isFavorite) viewModel.unstarAlbum(id) else viewModel.starAlbum(id)
                                        isFavorite = !isFavorite
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
                title = album?.mediaMetadata?.title?.toString().orEmpty(),
                artworkUrl = artworkUrl,
                cacheKey = album?.artworkKey ?: selectedAlbumId,
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
                viewModel.setSongRating(songId = selectedSong.mediaMetadata.id ?: "", rating = rating)
            },
            onDownload = { viewModel.downloadSong(it) },
            setShowDialog = { showSongDialog = it },
        )
    }
}

@Composable
private fun albumMetadata(songCount: Int): String =
    if (songCount > 0) stringResource(R.string.tv_library_song_count, songCount) else ""
