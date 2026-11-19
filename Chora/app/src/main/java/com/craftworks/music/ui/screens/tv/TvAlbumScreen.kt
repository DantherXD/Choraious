package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.id
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.GenericListDialog
import com.craftworks.music.ui.elements.tv.apple.TvContextMenu
import com.craftworks.music.ui.elements.tv.apple.TvContextMenuItem
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvGlassIconButton
import com.craftworks.music.ui.elements.tv.apple.TvMediaGrid
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import kotlinx.coroutines.launch

/**
 * The Albums list (spec §7): five-column grid, sort and favourites controls in the header.
 *
 * The provider decides which controls exist — a provider that cannot sort by an album field or
 * cannot fetch favourites simply gets no button, rather than one that silently does nothing.
 */
@Composable
fun TvAlbumScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: AlbumScreenViewModel = hiltViewModel(),
    mediaController: MediaController? = null,
) {
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }
    var menuAlbum by remember { mutableStateOf<MediaItem?>(null) }

    val scope = rememberCoroutineScope()

    fun queue(album: MediaItem, playNext: Boolean) {
        scope.launch {
            // Provider/network failure must not escape this coroutine and crash the screen.
            val songs = runCatching { viewModel.getAlbum(album.mediaMetadata.id.orEmpty()) }
                .onFailure { android.util.Log.e("TV-ALBUMS", "Could not load album", it) }
                .getOrNull()
                ?.drop(1)
                .orEmpty()
            if (songs.isEmpty()) return@launch
            if (playNext) SongHelper.playNext(songs, mediaController)
            else SongHelper.enqueue(songs, mediaController)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TvMediaGrid(
            items = albums,
            title = stringResource(R.string.nav_albums),
            key = { it.mediaId },
            onLoadMore = { viewModel.getMoreAlbums(50) },
            headerActions = {
                if (currentProvider?.featureFlags?.contains(ProviderFeature.FAVORITES) == true) {
                    TvGlassIconButton(
                        iconRes = if (showFavoritesOnly) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24,
                        contentDescription = stringResource(R.string.button_toggle_favorites),
                        onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
                    )
                }

                if (currentProvider?.supportedAlbumSort.orEmpty().size > 1) {
                    TvGlassButton(
                        text = sortLabel(sort),
                        onClick = { showSortDialog = true },
                        iconRes = R.drawable.rounded_sort_24,
                    )
                }

                if (currentProvider?.supportAlbumSortOrder ?: true) {
                    TvGlassIconButton(
                        iconRes = if (sortOrder == SortOrder.ASC) R.drawable.arrow_upward_24px else R.drawable.arrow_downward_24px,
                        contentDescription = stringResource(R.string.button_toggle_sort_order),
                        onClick = { viewModel.setOrder(sortOrder.invert()) },
                    )
                }
            },
        ) { album ->
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
                onLongClick = { menuAlbum = album },
            )
        }

        menuAlbum?.let { album ->
            TvContextMenu(
                title = album.mediaMetadata.title?.toString().orEmpty(),
                items = listOf(
                    TvContextMenuItem(stringResource(R.string.action_play_next)) {
                        queue(album, playNext = true)
                    },
                    TvContextMenuItem(stringResource(R.string.action_add_to_queue)) {
                        queue(album, playNext = false)
                    },
                ),
                onDismiss = { menuAlbum = null },
            )
        }
    }

    if (showSortDialog && currentProvider != null) {
        GenericListDialog(
            titleRes = R.string.button_sort_by,
            label = { sortLabel(it) },
            setShowDialog = { showSortDialog = it },
            options = currentProvider!!.supportedAlbumSort,
            selectedOption = sort,
            onOptionSelected = { viewModel.setSorting(it) },
            leftAligned = true,
            modifier = Modifier.padding(20.dp).width(280.dp),
        )
    }
}

/**
 * Sort labels as a map rather than an exhaustive `when`: the enum has values the app has no
 * string for (`COMMUNITY_RATING`, `CRITIC_RATING`), and a provider is free to offer them.
 */
private val AlbumSortLabels = mapOf(
    AlbumListSort.ALBUM_ARTIST to R.string.sort_by_album_artist,
    AlbumListSort.ARTIST to R.string.sort_by_artist,
    AlbumListSort.DURATION to R.string.sort_by_duration,
    AlbumListSort.EXPLICIT_STATUS to R.string.sort_by_explicit_status,
    AlbumListSort.FAVORITE to R.string.sort_by_favorite,
    AlbumListSort.NAME to R.string.sort_by_name,
    AlbumListSort.PLAY_COUNT to R.string.sort_by_play_count,
    AlbumListSort.RANDOM to R.string.sort_by_random,
    AlbumListSort.RATING to R.string.sort_by_rating,
    AlbumListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
    AlbumListSort.RECENTLY_PLAYED to R.string.sort_by_recently_played,
    AlbumListSort.RELEASE_DATE to R.string.sort_by_release_date,
    AlbumListSort.SONG_COUNT to R.string.sort_by_song_count,
    AlbumListSort.YEAR to R.string.sort_by_year,
)

@Composable
private fun sortLabel(sort: AlbumListSort): String =
    AlbumSortLabels[sort]?.let { stringResource(it) } ?: sort.name
