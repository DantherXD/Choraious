package com.craftworks.music.ui.screens.tv.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.OLEDProtectionMode
import com.craftworks.music.ui.elements.dialogs.tv.OledProtectionModeDialog
import com.craftworks.music.ui.elements.tv.apple.TvListRow
import com.craftworks.music.ui.elements.tv.apple.TvSettingsSection
import com.craftworks.music.ui.elements.tv.apple.TvSettingsSliderRow
import com.craftworks.music.ui.elements.tv.apple.TvSwitchRow
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import kotlinx.coroutines.launch

/**
 * TV appearance settings, pruned to the options that exist on a TV (spec §6.8/P4-2):
 *
 * - OLED protection, screen standby
 * - Reduce Motion, Large Text (new; the DataStore flags were wired in P0)
 * - lyrics autoscroll, recenter and animation speed
 * - provider logo
 *
 * Removed on TV: theme and background style (dark-only, fixed wash), navbar/home item ordering
 * (the tab bar is fixed), lyrics alignment and blur (fixed by the panel), more info, refresh
 * animation and track numbers.
 *
 * Every value is written straight to `AppearanceSettingsManager`, and Reduce Motion/Large Text
 * take effect immediately because `TvTheme` reads the same flows.
 */
@Preview(
    device = "id:tv_1080p",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun TvS_AppearanceScreen() {
    val context = LocalContext.current
    val type = LocalTvTypography.current
    val manager = remember(context) { AppearanceSettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var showOledDialog by remember { mutableStateOf(false) }

    val oledMode by manager.oledProtectionMode.collectAsStateWithLifecycle(OLEDProtectionMode.OFF)
    val standby by manager.disableScreenStandby.collectAsStateWithLifecycle(false)
    val reduceMotion by manager.reduceMotionFlow.collectAsStateWithLifecycle(false)
    val largeText by manager.largeTextFlow.collectAsStateWithLifecycle(false)
    val providerLogo by manager.showNavidromeLogoFlow.collectAsStateWithLifecycle(true)
    val autoScroll by manager.lyricsAutoScroll.collectAsStateWithLifecycle(true)
    val recenter by manager.lyricsRecenterAfterScroll.collectAsStateWithLifecycle(true)
    val speed by manager.lyricsAnimationSpeedFlow.collectAsStateWithLifecycle(660)

    val oledLabels = mapOf(
        OLEDProtectionMode.OFF to R.string.oled_protection_mode_off,
        OLEDProtectionMode.LYRICS_ONLY to R.string.oled_protection_mode_lyrics_only,
        OLEDProtectionMode.MINIMAL to R.string.oled_protection_mode_minimal,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                text = stringResource(R.string.settings_appearance),
                style = type.screenTitle,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space8)) {
                TvSettingsSection(stringResource(R.string.settings_section_display))

                TvListRow(
                    label = stringResource(R.string.appearance_oled_mode),
                    value = stringResource(oledLabels[oledMode] ?: R.string.oled_protection_mode_off),
                    showChevron = true,
                    onClick = { showOledDialog = true },
                )

                TvSwitchRow(
                    title = stringResource(R.string.appearance_screen_standby),
                    checked = standby,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setDisableScreenStandby(enabled) }
                    },
                )

                TvSwitchRow(
                    title = stringResource(R.string.settings_reduce_motion),
                    subtitle = stringResource(R.string.settings_reduce_motion_hint),
                    checked = reduceMotion,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setReduceMotion(enabled) }
                    },
                )

                TvSwitchRow(
                    title = stringResource(R.string.settings_large_text),
                    subtitle = stringResource(R.string.settings_large_text_hint),
                    checked = largeText,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setLargeText(enabled) }
                    },
                )

                TvSwitchRow(
                    title = stringResource(R.string.appearance_provider_logo),
                    checked = providerLogo,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setShowNavidromeLogo(enabled) }
                    },
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space8)) {
                TvSettingsSection(stringResource(R.string.settings_section_lyrics))

                TvSwitchRow(
                    title = stringResource(R.string.appearance_lyrics_auto_scroll),
                    checked = autoScroll,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setLyricsAutoScroll(enabled) }
                    },
                )

                TvSwitchRow(
                    title = stringResource(R.string.appearance_lyrics_recenter),
                    checked = recenter,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch { manager.setLyricsRecenterAfterScroll(enabled) }
                    },
                )

                // The stored value is a duration: 600 ms is the fastest, 2400 ms the slowest.
                TvSettingsSliderRow(
                    title = stringResource(R.string.appearance_lyrics_animation_speed),
                    valueLabel = stringResource(R.string.settings_lyrics_speed_value, speed),
                    onDecrease = {
                        coroutineScope.launch {
                            manager.setLyricsAnimationSpeed((speed - 300).coerceAtLeast(600))
                        }
                    },
                    onIncrease = {
                        coroutineScope.launch {
                            manager.setLyricsAnimationSpeed((speed + 300).coerceAtMost(2400))
                        }
                    },
                )
            }
        }
    }

    if (showOledDialog) {
        OledProtectionModeDialog(setShowDialog = { showOledDialog = it })
    }
}
