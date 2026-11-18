package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.ui.elements.dialogs.playlistToDelete
import com.craftworks.music.ui.elements.dialogs.showDeletePlaylistDialog
import com.craftworks.music.ui.elements.dialogs.tv.PlaylistDeletionConfirmationDialog
import com.craftworks.music.ui.elements.tv.apple.TvMediaGrid
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * The Playlists list (spec §7).
 *
 * Long-press still opens the deletion confirmation — that dialog is already TV-shaped, so it is
 * reused rather than reimplemented. The tab row the drawer era had here is gone: "favourites" is
 * not a playlist filter, and the tab bar already owns screen-level navigation.
 */
@Composable
fun TvPlaylistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: PlaylistScreenViewModel = hiltViewModel(),
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    TvMediaGrid(
        items = playlists,
        title = stringResource(R.string.nav_playlists),
        key = { it.mediaId },
        modifier = Modifier.fillMaxSize(),
    ) { playlist ->
        TvMediaTile(
            artworkUrl = playlist.artworkUrlAt(600),
            cacheKey = playlist.artworkKey,
            title = playlist.mediaMetadata.title?.toString().orEmpty(),
            subtitle = playlist.mediaMetadata.artist?.toString(),
            fillWidth = true,
            onClick = {
                navHostController.navigate(
                    Screen.PlaylistDetails(
                        playlist.mediaMetadata.id ?: "",
                        playlist.mediaMetadata.artworkUri.toString(),
                    )
                ) { launchSingleTop = true }
            },
            onLongClick = {
                playlist.mediaMetadata.id?.let {
                    playlistToDelete.value = it
                    showDeletePlaylistDialog.value = true
                }
            },
        )
    }

    if (showDeletePlaylistDialog.value) {
        PlaylistDeletionConfirmationDialog(
            setShowDialog = { showDeletePlaylistDialog.value = it },
            viewModel,
        )
    }
}
