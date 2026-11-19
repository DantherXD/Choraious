package com.craftworks.music.ui.screens.tv

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.craftworks.music.BuildConfig
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.dialogs.tv.NameDialog
import com.craftworks.music.ui.elements.tv.apple.TvListRow
import com.craftworks.music.ui.elements.tv.apple.TvSettingsSection
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvDpadScrollOnEdge

/**
 * The TV settings hub (spec §6.8/P4-1).
 *
 * Grouped list, not a stack of wide buttons: Account, Playback, the two provider screens,
 * Appearance and About, with 56dp rows on the shared `surface.1` fill and a hairline. Rows are
 * `TvListRow`, so focus is a white fill plus ring and no lift — a settings list that grows as
 * focus moves reads as unstable next to a rail.
 *
 * Account edits the provider username inline (`NameDialog`); About shows the version and nothing
 * that would navigate into phone-only UI.
 */
@Preview(
    showSystemUi = true,
    showBackground = true,
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun TvSettingScreen(
    navHostController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val type = LocalTvTypography.current
    val username by remember(context) {
        AppearanceSettingsManager(context).usernameFlow
    }.collectAsStateWithLifecycle(initialValue = "Username")

    var showNameDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer()
            .tvDpadScrollOnEdge(listState),
        contentPadding = PaddingValues(
            start = TvDimens.screenMarginHorizontal,
            end = TvDimens.screenMarginHorizontal,
            top = LocalTvTopInset.current + TvDimens.space24,
            bottom = TvDimens.screenMarginBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space24),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings),
                style = type.screenTitle,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space8)) {
                TvSettingsSection(stringResource(R.string.settings_section_account))
                TvListRow(
                    label = stringResource(R.string.appearance_username),
                    value = username,
                    showChevron = true,
                    onClick = { showNameDialog = true },
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space8)) {
                TvSettingsSection(stringResource(R.string.settings_section_general))
                TvListRow(
                    label = stringResource(R.string.settings_playback),
                    showChevron = true,
                    onClick = { navHostController.navigate(Screen.S_Playback) { launchSingleTop = true } },
                )
                TvListRow(
                    label = stringResource(R.string.settings_media_providers),
                    showChevron = true,
                    onClick = { navHostController.navigate(Screen.S_Media_Providers) { launchSingleTop = true } },
                )
                TvListRow(
                    label = stringResource(R.string.settings_lyrics_providers),
                    showChevron = true,
                    onClick = { navHostController.navigate(Screen.S_Lyrics_Providers) { launchSingleTop = true } },
                )
                TvListRow(
                    label = stringResource(R.string.settings_appearance),
                    showChevron = true,
                    onClick = { navHostController.navigate(Screen.S_Appearance) { launchSingleTop = true } },
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space8)) {
                TvSettingsSection(stringResource(R.string.settings_section_about))
                TvListRow(
                    label = stringResource(R.string.settings_version),
                    value = BuildConfig.VERSION_NAME,
                )
            }
        }
    }

    if (showNameDialog) {
        NameDialog(setShowDialog = { showNameDialog = it })
    }
}
