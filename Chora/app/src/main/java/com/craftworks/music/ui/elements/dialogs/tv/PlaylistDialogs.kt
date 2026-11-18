package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.craftworks.music.R
import com.craftworks.music.ui.elements.dialogs.playlistToDelete
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvDialogActions
import com.craftworks.music.ui.elements.tv.apple.TvDialogSurface
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel

/** Destructive confirmation in the shared TV dialog shell (spec §6.9). */
@Composable
fun PlaylistDeletionConfirmationDialog(
    setShowDialog: (Boolean) -> Unit,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvDialogSurface(
            onDismiss = { setShowDialog(false) },
            title = stringResource(R.string.playlist_delete_playlist),
            subtitle = stringResource(R.string.delete_playlist_are_you_sure),
        ) {
            TvDialogActions {
                TvGlassButton(
                    text = stringResource(R.string.action_close),
                    onClick = { setShowDialog(false) },
                )
                TvGlassButton(
                    text = stringResource(R.string.playlist_delete_playlist),
                    style = TvButtonStyle.Primary,
                    onClick = {
                        viewModel.deletePlaylist(playlistToDelete.value)
                        setShowDialog(false)
                    },
                )
            }
        }
    }
}
