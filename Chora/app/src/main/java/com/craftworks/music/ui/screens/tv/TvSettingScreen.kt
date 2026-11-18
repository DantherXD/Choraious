package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.tv.material3.WideButton
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset


@Preview(
    showSystemUi = false, showBackground = true, device = "id:tv_1080p"
)
@Composable
fun TvSettingScreen(
    navHostController: NavHostController = rememberNavController()
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Content is drawn under the tab bar, so the first row starts below it.
    val topInset = LocalTvTopInset.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = TvDimens.screenMarginHorizontal,
                end = TvDimens.screenMarginHorizontal,
                top = topInset + TvDimens.space24,
                bottom = TvDimens.space24,
            )
            .focusRestorer(focusRequester)
            .focusRequester(focusRequester)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsButton(
            Screen.S_Appearance,
            R.drawable.s_a_palette,
            R.string.settings_appearance,
            navHostController,
            Modifier.onFocusChanged {
                focusRequester.saveFocusedChild()
            }
        )

        SettingsButton(
            Screen.S_Media_Providers,
            R.drawable.s_m_media_providers,
            R.string.settings_media_providers,
            navHostController,
            Modifier.onFocusChanged {
                focusRequester.saveFocusedChild()
            }
        )

        SettingsButton(
            Screen.S_Playback,
            R.drawable.s_m_playback,
            R.string.settings_playback,
            navHostController,
            Modifier.onFocusChanged {
                focusRequester.saveFocusedChild()
            }
        )
    }
}

@Composable
private fun SettingsButton(
    route: Screen,
    icon: Int,
    text: Int,
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
    WideButton(
        modifier = modifier,
        onClick = {
            navHostController.navigate(route) {
                launchSingleTop = true
            }
        },
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        title = {
            Text(
                text = stringResource(text), modifier = Modifier
            )
        }
    )
}