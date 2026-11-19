package com.craftworks.music.ui.elements.dialogs.tv.provider

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvDialogActions
import com.craftworks.music.ui.elements.tv.apple.TvDialogOptionRow
import com.craftworks.music.ui.elements.tv.apple.TvDialogSurface
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton

/**
 * Step 1 of the provider wizard (spec §6.9/P4-5): pick a provider type, glass overlay, focus
 * trapped, Back = Cancel.
 */
@Preview(
    showBackground = false,
    showSystemUi = true,
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun SelectProviderType(
    setShowDialog: (Boolean) -> Unit = { },
    selectProvider: (ProviderType) -> Unit = { },
) {
    // A picker needs a visible choice: the rows used to hardcode `selected = false` and the Next
    // button was permanently disabled, so picking Navidrome gave no feedback at all.
    var selected by remember { mutableStateOf<ProviderType?>(null) }

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvDialogSurface(
            onDismiss = { setShowDialog(false) },
            title = stringResource(R.string.settings_media_providers),
            subtitle = stringResource(R.string.add_media_provider_login),
        ) {
            TvDialogOptionRow(
                label = "OpenSubsonic/Navidrome",
                selected = selected == ProviderType.SUBSONIC,
                leadingIconRes = R.drawable.s_m_opensubsonic_bw,
                onClick = { selected = ProviderType.SUBSONIC },
            )
            TvDialogOptionRow(
                label = stringResource(R.string.source_local_folder),
                selected = selected == ProviderType.LOCAL_FOLDER,
                leadingIconRes = R.drawable.s_m_local_filled,
                onClick = { selected = ProviderType.LOCAL_FOLDER },
            )

            TvDialogActions {
                TvGlassButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { setShowDialog(false) },
                )
                TvGlassButton(
                    text = stringResource(R.string.action_next),
                    style = TvButtonStyle.Primary,
                    enabled = selected != null,
                    onClick = { selected?.let(selectProvider) },
                )
            }
        }
    }
}
