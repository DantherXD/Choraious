package com.craftworks.music.ui.elements.dialogs.tv.provider

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.craftworks.music.R
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvDialogActions
import com.craftworks.music.ui.elements.tv.apple.TvDialogSurface
import com.craftworks.music.ui.elements.tv.apple.TvDialogTextField
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import kotlinx.coroutines.launch

/**
 * The LRCLIB endpoint editor (spec §6.9/P4-5): a glass overlay with the system IME, inline
 * validation and a red primary action.
 */
@Preview(
    showBackground = false,
    showSystemUi = true,
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun ModifyLrcLibProviderDialog(
    initialUrl: String = "",
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val settingsManager = remember(context) { MediaProviderSettingsManager(context) }
    var url: String by remember { mutableStateOf(initialUrl) }
    val coroutineScope = rememberCoroutineScope()

    fun save() {
        coroutineScope.launch {
            settingsManager.setLrcLibEndpoint(url)
            setShowDialog(false)
        }
    }

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvDialogSurface(
            onDismiss = { setShowDialog(false) },
            title = "LRCLIB",
            subtitle = "lrclib.net",
        ) {
            TvDialogTextField(
                value = url,
                onValueChange = { url = it },
                label = "LRCLIB Url",
                placeholder = "https://lrclib.net",
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
                onDone = { save() },
                isError = url.isBlank(),
            )

            TvDialogActions {
                TvGlassButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { setShowDialog(false) },
                )
                TvGlassButton(
                    text = stringResource(R.string.action_done),
                    style = TvButtonStyle.Primary,
                    enabled = url.isNotBlank(),
                    onClick = { save() },
                )
            }
        }
    }
}
