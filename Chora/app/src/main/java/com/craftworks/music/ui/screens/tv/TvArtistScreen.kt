package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.dialogs.tv.GenericListDialog
import com.craftworks.music.ui.elements.tv.apple.TvArtistTile
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvGlassIconButton
import com.craftworks.music.ui.elements.tv.apple.TvMediaGrid
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel

/** The Artists list (spec §7): circular 200dp tiles in the same five-column grid. */
@Composable
fun TvArtistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: ArtistsScreenViewModel = hiltViewModel(),
) {
    val artists by viewModel.allArtists.collectAsStateWithLifecycle()
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    var showSortDialog by remember { mutableStateOf(false) }

    TvMediaGrid(
        items = artists,
        title = stringResource(R.string.nav_artists),
        key = { it.id },
        modifier = Modifier.fillMaxSize(),
        onLoadMore = viewModel::getMoreArtists,
        headerActions = {
            if (currentProvider?.featureFlags?.contains(ProviderFeature.FAVORITES) == true) {
                TvGlassIconButton(
                    iconRes = if (showFavoritesOnly) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24,
                    contentDescription = stringResource(R.string.button_toggle_favorites),
                    onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
                )
            }

            if (currentProvider?.supportedAlbumArtistSort.orEmpty().size > 1) {
                TvGlassButton(
                    text = sortLabel(sort),
                    onClick = { showSortDialog = true },
                    iconRes = R.drawable.rounded_sort_24,
                )
            }

            if (currentProvider?.supportAlbumArtistSortOrder ?: true) {
                TvGlassIconButton(
                    iconRes = if (sortOrder == SortOrder.ASC) R.drawable.arrow_upward_24px else R.drawable.arrow_downward_24px,
                    contentDescription = stringResource(R.string.button_toggle_sort_order),
                    onClick = { viewModel.setOrder(sortOrder.invert()) },
                )
            }
        },
    ) { artist ->
        TvArtistTile(
            artworkUrl = artworkUrl(artist),
            cacheKey = artist.id,
            title = artist.name,
            subtitle = artist.albumCount?.let { count ->
                stringResource(R.string.tv_library_album_count, count)
            },
            fillWidth = true,
            onClick = {
                navHostController.navigate(
                    Screen.ArtistDetails(artist.id, artist.imageUrl)
                ) { launchSingleTop = true }
            },
        )
    }

    if (showSortDialog && currentProvider != null) {
        GenericListDialog(
            titleRes = R.string.button_sort_by,
            label = { sortLabel(it) },
            setShowDialog = { showSortDialog = it },
            options = currentProvider!!.supportedAlbumArtistSort,
            selectedOption = sort,
            onOptionSelected = { viewModel.setSorting(it) },
            leftAligned = true,
            modifier = Modifier.padding(20.dp).width(280.dp),
        )
    }
}

private fun artworkUrl(artist: MediaModel.Artist): String? =
    artist.imageUrl
        ?: artist.imageId?.let { artist.getProvider()?.getImageUrl(it, LibraryType.ARTIST, 400) }

private val ArtistSortLabels = mapOf(
    AlbumArtistListSort.ALBUM_COUNT to R.string.sort_by_album_count,
    AlbumArtistListSort.FAVORITE to R.string.sort_by_favorite,
    AlbumArtistListSort.NAME to R.string.sort_by_name,
    AlbumArtistListSort.PLAY_COUNT to R.string.sort_by_play_count,
    AlbumArtistListSort.RANDOM to R.string.sort_by_random,
    AlbumArtistListSort.RATING to R.string.sort_by_rating,
    AlbumArtistListSort.SONG_COUNT to R.string.sort_by_song_count,
)

@Composable
private fun sortLabel(sort: AlbumArtistListSort): String =
    ArtistSortLabels[sort]?.let { stringResource(it) } ?: sort.name
