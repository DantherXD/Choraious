package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.providerType
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.AddRadioDialog
import com.craftworks.music.ui.elements.dialogs.tv.ModifyRadioDialog
import com.craftworks.music.ui.elements.tv.TvRadioCard
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvDpadScrollOnEdge
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import kotlinx.coroutines.launch

/**
 * Internet radio (spec §7): three-column 16:9 grid, one Play per station, long-press to edit or
 * delete.
 *
 * Long-press rather than a per-card button keeps the grid to one focusable per station; the
 * edit/delete dialogs are the existing TV ones.
 */
@Composable
fun TvRadioScreen(
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    viewModel: RadioScreenViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val stations by viewModel.radioStations.collectAsStateWithLifecycle()
    val selectedRadio by viewModel.selectedRadioStation.collectAsStateWithLifecycle()

    var showRadioAddDialog by remember { mutableStateOf(false) }
    var showRadioModifyDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .tvDpadScrollOnEdge(gridState),
        contentPadding = PaddingValues(
            start = TvDimens.screenMarginHorizontal,
            end = TvDimens.screenMarginHorizontal,
            top = topInset + TvDimens.screenMarginTop,
            bottom = TvDimens.screenMarginBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = TvDimens.space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nav_radios),
                    style = type.screenTitle,
                    color = colors.labelPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        items(stations, key = { it.mediaId }) { radio ->
            TvRadioCard(
                name = radio.mediaMetadata.station?.toString()
                    ?: radio.mediaMetadata.title?.toString().orEmpty(),
                artworkUrl = radio.mediaMetadata.artworkUri,
                cacheKey = radio.mediaId,
                onClick = {
                    scope.launch {
                        SongHelper.play(listOf(radio), 0, mediaController)
                        navHostController.navigate(Screen.NowPlayingLandscape) {
                            launchSingleTop = true
                        }
                    }
                },
                onLongClick = {
                    viewModel.selectRadioStation(radio)
                    showRadioModifyDialog = true
                },
            )
        }

        item(key = "add") {
            AddStationTile(onClick = { showRadioAddDialog = true })
        }

        if (stations.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TvEmptyState(
                    title = stringResource(R.string.tv_browse_empty_title),
                    body = stringResource(R.string.radio_add_internet_radio),
                )
            }
        }
    }

    if (showRadioAddDialog) {
        AddRadioDialog(
            setShowDialog = { showRadioAddDialog = it },
            onAdded = { name, url, homePageUrl ->
                viewModel.addRadioStation(name, url, homePageUrl)
            },
        )
    }

    if (showRadioModifyDialog) {
        ModifyRadioDialog(
            setShowDialog = { showRadioModifyDialog = it },
            radio = selectedRadio,
            onModified = { providerId, id, name, url, homepage ->
                viewModel.modifyRadioStation(
                    providerId,
                    selectedRadio?.mediaMetadata?.providerType ?: ProviderType.LOCAL_FOLDER,
                    id,
                    name,
                    url,
                    homepage,
                )
            },
            onDeleted = { providerId, id -> viewModel.deleteRadioStation(providerId, id) },
        )
    }
}

@Composable
private fun AddStationTile(onClick: () -> Unit) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(16.dp)

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .tvFocusTarget(interactionSource = interactionSource, shape = shape)
            .background(colors.surface1, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = stringResource(R.string.radio_add_internet_radio),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.rounded_add_24),
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(TvDimens.space48),
        )
    }
}
