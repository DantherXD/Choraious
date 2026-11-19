package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.apple.TvArtwork
import com.craftworks.music.ui.elements.tv.apple.TvContextMenu
import com.craftworks.music.ui.elements.tv.apple.TvContextMenuItem
import com.craftworks.music.ui.elements.tv.apple.TvEmptyState
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvMediaTile
import com.craftworks.music.ui.elements.tv.apple.TvSectionHeader
import com.craftworks.music.ui.elements.tv.apple.TvSkeleton
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.artworkKey
import com.craftworks.music.ui.elements.tv.apple.artworkUrlAt
import com.craftworks.music.ui.screens.HomeItem
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.LocalTvTopInset
import com.craftworks.music.ui.tv.tvDpadScrollOnEdge
import com.craftworks.music.ui.tv.tvRequestFocusOnFirstAppearance
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch

/**
 * Listen Now (spec §6): one hero and a stack of rails.
 *
 * The hero is the most recent album; each rail is a `focusGroup` so the D-pad moves along it and
 * Down leaves for the next one. Rails are built from the user's Home Items setting and skip empty
 * sections, so a provider that only returns one list does not show three empty headers.
 *
 * "See All" is the only route into the TV `HomeLists` grid (plan P2-H) — activating it pushes the
 * same category the rail is showing.
 */
@Composable
fun TvHomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val topInset = LocalTvTopInset.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recentlyPlayed by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val orderedHomeItems = AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
        initial = listOf(
            HomeItem("recently_played", true),
            HomeItem("recently_added", true),
            HomeItem("most_played", true),
        )
    ).value

    val railDefs = remember(orderedHomeItems, recentlyPlayed, recentAlbums, mostPlayedAlbums) {
        orderedHomeItems.filter { it.enabled }.mapNotNull { item ->
            val albums = when (item.key) {
                "recently_played" -> recentlyPlayed
                "recently_added" -> recentAlbums
                "most_played" -> mostPlayedAlbums
                else -> emptyList()
            }
            if (albums.isEmpty()) null else Rail(item.key, titleResFor(item.key), albums)
        }
    }

    val hero = recentlyPlayed.firstOrNull() ?: recentAlbums.firstOrNull()

    val listState = rememberLazyListState()
    val heroActionRequester = remember { FocusRequester() }
    var menuTarget by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(hero?.mediaId) {
        if (hero != null) {
            // The hero is item 0, so it is composed on first frame; a missing focus target just
            // means focus lands on the first rail instead.
            runCatching { heroActionRequester.requestFocus() }
        }
    }

    fun playAlbum(album: MediaItem, shuffled: Boolean) {
        scope.launch {
            val albumId = album.mediaMetadata.id ?: return@launch
            // getAlbumSongs returns the album item first, so the tracks start at index 1.
            // The lookup is a provider/network call: if it throws, the exception used to escape
            // into this coroutine and crash the app the moment Play was pressed.
            val songs = runCatching { viewModel.getAlbumSongs(albumId) }
                .onFailure { Log.e("TV-HOME", "Could not load album $albumId", it) }
                .getOrNull()
                ?.drop(1)
                .orEmpty()
            if (songs.isEmpty()) return@launch
            SongHelper.play(
                mediaItems = if (shuffled) songs.shuffled() else songs,
                index = 0,
                mediaController = mediaController,
            )
            navHostController.navigate(Screen.NowPlayingLandscape) { launchSingleTop = true }
        }
    }

    fun queueAlbum(album: MediaItem, playNext: Boolean) {
        scope.launch {
            val albumId = album.mediaMetadata.id ?: return@launch
            val songs = runCatching { viewModel.getAlbumSongs(albumId) }
                .onFailure { Log.e("TV-HOME", "Could not load album $albumId", it) }
                .getOrNull()
                ?.drop(1)
                .orEmpty()
            if (songs.isEmpty()) return@launch
            if (playNext) SongHelper.playNext(songs, mediaController)
            else SongHelper.enqueue(songs, mediaController)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .tvDpadScrollOnEdge(listState),
            contentPadding = PaddingValues(
                start = TvDimens.screenMarginHorizontal,
                end = TvDimens.screenMarginHorizontal,
                top = topInset,
                bottom = TvDimens.screenMarginBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space32),
        ) {
            if (hero != null) {
                item(key = "hero") {
                    Hero(
                        album = hero,
                        onPlay = { playAlbum(hero, shuffled = false) },
                        onShuffle = { playAlbum(hero, shuffled = true) },
                        playFocusRequester = heroActionRequester,
                    )
                }
            }

            if (isLoading && railDefs.isEmpty() && hero == null) {
                item(key = "skeletons") { RailSkeletons() }
            }

            items(railDefs, key = { it.key }) { rail ->
                RailRow(
                    rail = rail,
                    onSeeAll = {
                        navHostController.navigate(Screen.HomeLists(rail.key)) {
                            launchSingleTop = true
                        }
                    },
                    onAlbumClick = { album ->
                        navHostController.navigate(
                            Screen.AlbumDetails(
                                album.mediaMetadata.id ?: "",
                                album.mediaMetadata.artworkUri.toString(),
                            )
                        ) { launchSingleTop = true }
                    },
                    onAlbumLongClick = { album -> menuTarget = album },
                )
            }

            if (!isLoading && railDefs.isEmpty() && hero == null) {
                item(key = "empty") {
                    TvEmptyState(
                        title = stringResource(R.string.tv_browse_empty_title),
                        body = stringResource(R.string.tv_browse_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        menuTarget?.let { album ->
            val title = album.mediaMetadata.title?.toString().orEmpty()
            TvContextMenu(
                title = title,
                items = listOf(
                    TvContextMenuItem(stringResource(R.string.action_play_next)) {
                        queueAlbum(album, playNext = true)
                    },
                    TvContextMenuItem(stringResource(R.string.action_add_to_queue)) {
                        queueAlbum(album, playNext = false)
                    },
                    TvContextMenuItem(stringResource(R.string.action_go_to_album)) {
                        navHostController.navigate(
                            Screen.AlbumDetails(
                                album.mediaMetadata.id ?: "",
                                album.mediaMetadata.artworkUri.toString(),
                            )
                        ) { launchSingleTop = true }
                    },
                ),
                onDismiss = { menuTarget = null },
            )
        }
    }
}

private data class Rail(
    val key: String,
    val titleRes: Int,
    val albums: List<MediaItem>,
)

/** Resolved outside composition: the rail list is built in a `remember`. */
private fun titleResFor(key: String): Int = when (key) {
    "recently_played" -> R.string.home_recently_played
    "recently_added" -> R.string.home_recently_added
    "most_played" -> R.string.home_most_played
    else -> R.string.home_explore_library
}

/**
 * The hero (spec §6): 960×540 artwork in a full-width band, a bottom-up scrim, `displayHero`
 * title and the Play / Shuffle pair.
 */
@Composable
private fun Hero(
    album: MediaItem,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    playFocusRequester: FocusRequester,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    val title = album.mediaMetadata.title?.toString().orEmpty()
    val artist = album.mediaMetadata.artist?.toString().orEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.heroHeight)
            .background(colors.bgCanvasTop),
    ) {
        TvArtwork(
            data = album.artworkUrlAt(1280),
            cacheKey = album.artworkKey,
            // No crossfade: the hero repaints on every return to Listen Now, and the fade-in read
            // as the home screen animating itself rather than as artwork loading.
            crossfade = false,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        0.75f to colors.bgCanvas.copy(alpha = 0.72f),
                        1f to colors.bgCanvas,
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.bgCanvas.copy(alpha = 0.86f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(TvDimens.space48)
                .width(TvDimens.heroTextWidth),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space16),
        ) {
            Text(
                text = title,
                style = type.displayHero,
                color = colors.labelPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = type.body,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.padding(top = TvDimens.space8),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.space16),
            ) {
                TvGlassButton(
                    text = stringResource(R.string.action_play),
                    onClick = onPlay,
                    style = TvButtonStyle.Primary,
                    iconRes = R.drawable.round_play_arrow_24,
                    modifier = Modifier
                        .focusRequester(playFocusRequester)
                        .tvRequestFocusOnFirstAppearance(),
                )
                TvGlassButton(
                    text = stringResource(R.string.action_shuffle),
                    onClick = onShuffle,
                    iconRes = R.drawable.round_shuffle_28,
                )
            }
        }
    }
}

@Composable
private fun RailRow(
    rail: Rail,
    onSeeAll: () -> Unit,
    onAlbumClick: (MediaItem) -> Unit,
    onAlbumLongClick: (MediaItem) -> Unit,
) {
    Column(
        modifier = Modifier.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap),
    ) {
        TvSectionHeader(
            title = stringResource(rail.titleRes),
            actionLabel = stringResource(R.string.action_see_all),
            onAction = onSeeAll,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap),
            // Rails sit inside the 64dp screen margin, so the row itself adds none; the trailing
            // gap keeps the last tile's halo off the edge.
            contentPadding = PaddingValues(end = TvDimens.space8),
        ) {
            items(rail.albums, key = { it.mediaId }) { album ->
                TvMediaTile(
                    artworkUrl = album.artworkUrlAt(600),
                    cacheKey = album.artworkKey,
                    title = album.mediaMetadata.title?.toString().orEmpty(),
                    subtitle = album.mediaMetadata.artist?.toString(),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) },
                )
            }
        }
    }
}

@Composable
private fun RailSkeletons() {
    Column(verticalArrangement = Arrangement.spacedBy(TvDimens.space32)) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.railTitleGap)) {
                TvSkeleton(modifier = Modifier.width(TvDimens.tileSize).height(TvDimens.space24))
                Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.railGap)) {
                    repeat(5) {
                        TvSkeleton(
                            modifier = Modifier
                                .width(TvDimens.tileSize)
                                .height(TvDimens.tileSize)
                        )
                    }
                }
            }
        }
    }
}
