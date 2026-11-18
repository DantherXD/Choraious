package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.id
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.GenericListDialog
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.apple.TvContextMenu
import com.craftworks.music.ui.elements.tv.apple.TvContextMenuItem
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvGlassIconButton
import com.craftworks.music.ui.elements.tv.apple.TvTrackRow
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.launch

/**
 * Songs (spec §7): plain 72dp rows rather than tiles, because a song has no artwork of its own
 * worth a 220dp square.
 *
 * Long-press opens the queue menu; the existing `SongDialog` (rating, download) stays behind the
 * menu's "More" action so the row keeps a single interaction.
 */
@Composable
fun TvSongsScreen(
    mediaController: MediaController? = null,
    navHostController: NavController,
    viewModel: SongsScreenViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }
    var showSongDialog by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var menuSong by remember { mutableStateOf<MediaItem?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TvDimens.screenMarginHorizontal,
                end = TvDimens.screenMarginHorizontal,
                top = topInset + TvDimens.screenMarginTop,
                bottom = TvDimens.screenMarginBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = TvDimens.space16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
                ) {
                    Text(
                        text = stringResource(R.string.nav_songs),
                        style = type.screenTitle,
                        color = colors.labelPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    if (currentProvider?.featureFlags?.contains(ProviderFeature.FAVORITES) == true) {
                        TvGlassIconButton(
                            iconRes = if (showFavoritesOnly) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24,
                            contentDescription = stringResource(R.string.button_toggle_favorites),
                            onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
                        )
                    }

                    if (currentProvider?.supportedSongSort.orEmpty().size > 1) {
                        TvGlassButton(
                            text = songSortLabel(sort),
                            onClick = { showSortDialog = true },
                            iconRes = R.drawable.rounded_sort_24,
                        )
                    }

                    if (currentProvider?.supportSongSortOrder ?: true) {
                        TvGlassIconButton(
                            iconRes = if (sortOrder == SortOrder.ASC) R.drawable.arrow_upward_24px else R.drawable.arrow_downward_24px,
                            contentDescription = stringResource(R.string.button_toggle_sort_order),
                            onClick = { viewModel.setOrder(sortOrder.invert()) },
                        )
                    }
                }
            }

            itemsIndexed(songs, key = { _, song -> song.mediaId }) { index, song ->
                TvTrackRow(
                    index = index + 1,
                    title = song.mediaMetadata.title?.toString().orEmpty(),
                    artist = song.mediaMetadata.artist?.toString(),
                    durationLabel = song.mediaMetadata.durationMs?.let { formatDuration(it) },
                    isPlaying = false,
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

    if (showSortDialog && currentProvider != null) {
        GenericListDialog(
            titleRes = R.string.button_sort_by,
            label = { songSortLabel(it) },
            setShowDialog = { showSortDialog = it },
            options = currentProvider!!.supportedSongSort,
            selectedOption = sort,
            onOptionSelected = { viewModel.setSorting(it) },
            leftAligned = true,
            modifier = Modifier.padding(20.dp).width(280.dp),
        )
    }
}

private val SongSortLabels = mapOf(
    SongListSort.ALBUM to R.string.sort_by_album,
    SongListSort.ALBUM_ARTIST to R.string.sort_by_album_artist,
    SongListSort.ARTIST to R.string.sort_by_artist,
    SongListSort.BPM to R.string.sort_by_bpm,
    SongListSort.CHANNELS to R.string.sort_by_channels,
    SongListSort.COMMENT to R.string.sort_by_comment,
    SongListSort.DURATION to R.string.sort_by_duration,
    SongListSort.EXPLICIT_STATUS to R.string.sort_by_explicit_status,
    SongListSort.FAVORITE to R.string.sort_by_favorite,
    SongListSort.GENRE to R.string.sort_by_genre,
    SongListSort.ID to R.string.sort_by_id,
    SongListSort.NAME to R.string.sort_by_name,
    SongListSort.PLAY_COUNT to R.string.sort_by_play_count,
    SongListSort.RANDOM to R.string.sort_by_random,
    SongListSort.RATING to R.string.sort_by_rating,
    SongListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
    SongListSort.RECENTLY_PLAYED to R.string.sort_by_recently_played,
    SongListSort.RELEASE_DATE to R.string.sort_by_release_date,
    SongListSort.YEAR to R.string.sort_by_year,
)

@Composable
private fun songSortLabel(sort: SongListSort): String =
    SongSortLabels[sort]?.let { stringResource(it) } ?: sort.name

/** `m:ss` for a duration in milliseconds; rows show a short form, not the detail screen's. */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
