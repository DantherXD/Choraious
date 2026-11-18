package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.ui.elements.tv.apple.TvListRow
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.TvSectionHeader
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel

/** One of the four Library categories (spec §7: 440×220 neutral tile, 40dp glyph). */
private data class Category(
    val titleRes: Int,
    val glyphRes: Int,
    val destination: Screen,
)

/**
 * The Library hub (spec §7): a 2×2 block of category tiles, the recently-added rail, and one
 * Settings row.
 *
 * The tiles are neutral `surface.1` on purpose — eight album covers competing for attention in a
 * grid of navigation would look like content. Item counts are omitted: they would mean four more
 * list queries on every visit for a label no one navigates by.
 */
@Composable
fun TvLibraryScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()

    val categories = listOf(
        Category(R.string.tv_library_albums, R.drawable.rounded_library_music_24, Screen.Albums),
        Category(R.string.tv_library_artists, R.drawable.rounded_artist_24, Screen.Artists),
        Category(R.string.tv_library_songs, R.drawable.round_music_note_24, Screen.Songs),
        Category(R.string.tv_library_playlists, R.drawable.rounded_queue_music_24, Screen.Playlists),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TvDimens.screenMarginHorizontal,
            end = TvDimens.screenMarginHorizontal,
            top = topInset + TvDimens.screenMarginTop,
            bottom = TvDimens.screenMarginBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.nav_library),
                style = type.screenTitle,
                color = colors.labelPrimary,
            )
        }

        item(key = "categories") {
            // Two rows of two, so the D-pad moves right within a row and down to the next.
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.railGap)) {
                categories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
                    ) {
                        row.forEach { category ->
                            CategoryTile(
                                title = stringResource(category.titleRes),
                                glyphRes = category.glyphRes,
                                onClick = {
                                    navHostController.navigate(category.destination) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (recentAlbums.isNotEmpty()) {
            item(key = "recent") {
                Column(
                    modifier = Modifier.focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap),
                ) {
                    TvSectionHeader(title = stringResource(R.string.home_recently_added))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
                        contentPadding = PaddingValues(end = TvDimens.space8),
                    ) {
                        items(recentAlbums, key = { it.mediaId }) { album ->
                            RecentAlbum(album = album, onClick = {
                                navHostController.navigate(
                                    Screen.AlbumDetails(
                                        album.mediaMetadata.id ?: "",
                                        album.mediaMetadata.artworkUri.toString(),
                                    )
                                ) { launchSingleTop = true }
                            })
                        }
                    }
                }
            }
        }

        item(key = "settings") {
            TvListRow(
                label = stringResource(R.string.home_settings),
                showChevron = true,
                onClick = {
                    navHostController.navigate(Screen.Settings) { launchSingleTop = true }
                },
            )
        }
    }
}

@Composable
private fun RecentAlbum(album: MediaItem, onClick: () -> Unit) {
    TvMediaTile(
        artworkUrl = album.artworkUrlAt(600),
        cacheKey = album.artworkKey,
        title = album.mediaMetadata.title?.toString().orEmpty(),
        subtitle = album.mediaMetadata.artist?.toString(),
        onClick = onClick,
    )
}

@Composable
private fun CategoryTile(
    title: String,
    glyphRes: Int,
    onClick: () -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = LocalTvShapes.current.tile

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .width(380.dp)
            .height(170.dp)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.03f,
            )
            .background(
                color = if (isFocused) colors.surface2 else colors.surface1,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = title,
                onClick = onClick,
            )
            .padding(TvDimens.space24),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space12),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(glyphRes),
                contentDescription = null,
                tint = colors.labelSecondary,
                modifier = Modifier.size(TvDimens.iconControl),
            )
            Text(
                text = title,
                style = type.sectionTitle,
                color = colors.labelPrimary,
            )
        }
    }
}
