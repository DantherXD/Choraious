package com.craftworks.music.ui.elements.tv.apple

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.theme.tv.TvGlassPresets
import com.craftworks.music.ui.theme.tv.tvGlass
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.tv.tvLongPress
import com.craftworks.music.ui.tv.tvRequestFocusOnFirstAppearance
import com.craftworks.music.utils.StringUtils
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

/** Provider queue sync is debounced so a burst of edits becomes one write (spec §6.7). */
private const val QueueSyncDebounceMs = 1_000L

/** The item a context menu was opened for, with the index it had when it opened. */
private data class QueueMenuTarget(val index: Int, val item: MediaItem)

/**
 * The "Up Next" panel (spec §6.7/P3-5): the controller's queue, not a copy of it.
 *
 * The queue list is read from the `MediaController` and re-synced on timeline changes, so edits
 * made elsewhere (another screen, the service) appear here without a second source of truth.
 * Reordering goes through `moveMediaItem`, which is the same API the phone queue uses.
 *
 * Provider sync is best-effort: `savePlayQueue` is abstract on every provider but not all of them
 * implement it (the Subsonic/Navidrome implementations still `TODO()`), so the write is wrapped and
 * logged by exception type only. Playback must never depend on it, and a failure is not surfaced.
 *
 * Long-press OK or the row's `⋯` opens the action menu. Back closes the menu, then the panel.
 */
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvQueuePanel(
    backdrop: Backdrop,
    mediaController: MediaController?,
    onClose: () -> Unit,
    onAddFromLibrary: () -> Unit,
    onGoToAlbum: (albumId: String, imageUri: String) -> Unit,
    onGoToArtist: (artistId: String, imageUri: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    // Docked to the right edge, so only the leading corners are rounded.
    val shape = RoundedCornerShape(
        topStart = TvDimens.npPanelCorner,
        bottomStart = TvDimens.npPanelCorner,
    )

    val items = remember { mutableStateListOf<MediaItem>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var menuTarget by remember { mutableStateOf<QueueMenuTarget?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    BackHandler { onClose() }

    DisposableEffect(mediaController) {
        val controller = mediaController

        if (controller != null) {
            val sync = {
                items.clear()
                repeat(controller.mediaItemCount) { index ->
                    items.add(controller.getMediaItemAt(index))
                }
                currentIndex = controller.currentMediaItemIndex.coerceAtLeast(0)
            }

            val listener = object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    sync()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentIndex = controller.currentMediaItemIndex.coerceAtLeast(0)
                }
            }

            sync()
            controller.addListener(listener)

            onDispose { controller.removeListener(listener) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Debounced provider sync: one write per burst of edits, never blocking playback.
    LaunchedEffect(revision) {
        if (revision == 0) return@LaunchedEffect
        delay(QueueSyncDebounceMs)
        val controller = mediaController ?: return@LaunchedEffect
        val provider = MediaProviderManager.currentProvider.value ?: return@LaunchedEffect
        val songIds = List(controller.mediaItemCount) { controller.getMediaItemAt(it).mediaId }
        runCatching {
            provider.savePlayQueue(
                songs = songIds,
                currentIndex = controller.currentMediaItemIndex,
                positionMs = controller.currentPosition.toInt(),
            )
        }.onFailure { error ->
            // Silent by design (spec §6.7): offline playback keeps working either way.
            Log.d("TV-QUEUE", "Queue sync skipped (${error::class.simpleName})")
        }
    }

    fun edited() {
        revision += 1
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .tvGlass(backdrop = backdrop, preset = TvGlassPresets.panel, shape = shape)
            .padding(
                start = TvDimens.space24,
                end = TvDimens.space24,
                top = TvDimens.space24,
                bottom = TvDimens.space16,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.np_up_next),
                style = type.sectionTitle,
                color = colors.labelPrimary,
                modifier = Modifier.weight(1f),
            )

            if (items.size > currentIndex + 1) {
                TvGlassButton(
                    text = stringResource(R.string.np_clear),
                    onClick = { confirmClear = true },
                )
                Spacer(modifier = Modifier.width(TvDimens.space12))
            }

            TvGlassButton(
                text = stringResource(R.string.action_done),
                onClick = onClose,
            )
        }

        Spacer(modifier = Modifier.height(TvDimens.space12))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.np_queue_empty),
                    style = type.body,
                    color = colors.labelSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(TvDimens.space16))
                TvGlassButton(
                    text = stringResource(R.string.np_add_from_library),
                    onClick = onAddFromLibrary,
                    style = TvButtonStyle.Primary,
                    modifier = Modifier.tvRequestFocusOnFirstAppearance(),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                            onClose()
                            true
                        } else {
                            false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "$index-${item.mediaId}" },
                ) { index, item ->
                    QueueRow(
                        item = item,
                        index = index,
                        isCurrent = index == currentIndex,
                        onClick = {
                            mediaController?.seekTo(index, 0L)
                            currentIndex = index
                        },
                        onOpenMenu = { menuTarget = QueueMenuTarget(index, item) },
                    )
                }
            }
        }
    }

    val target = menuTarget
    if (target != null) {
        val metadata = target.item.mediaMetadata
        val albumId = metadata.extras?.getString("albumId")
        val artistId = metadata.extras?.getString("artistId")
        val artworkUri = metadata.artworkUri?.toString()?.takeIf { it.isNotBlank() }

        val playNextLabel = stringResource(R.string.action_play_next)
        val moveUpLabel = stringResource(R.string.np_move_up)
        val moveDownLabel = stringResource(R.string.np_move_down)
        val removeLabel = stringResource(R.string.action_remove)
        val goToAlbumLabel = stringResource(R.string.action_go_to_album)
        val goToArtistLabel = stringResource(R.string.np_go_to_artist)

        TvContextMenu(
            items = buildList {
                add(
                    TvContextMenuItem(playNextLabel) {
                        val controller = mediaController
                        val destination = (currentIndex + 1).coerceAtMost(items.lastIndex)
                        if (controller != null && target.index != currentIndex && target.index != destination) {
                            controller.moveMediaItem(target.index, destination)
                            edited()
                        }
                    }
                )
                if (target.index > 0) {
                    add(
                        TvContextMenuItem(moveUpLabel) {
                            mediaController?.moveMediaItem(target.index, target.index - 1)
                            edited()
                        }
                    )
                }
                if (target.index < items.lastIndex) {
                    add(
                        TvContextMenuItem(moveDownLabel) {
                            mediaController?.moveMediaItem(target.index, target.index + 1)
                            edited()
                        }
                    )
                }
                add(
                    TvContextMenuItem(removeLabel) {
                        mediaController?.removeMediaItem(target.index)
                        edited()
                    }
                )
                if (!albumId.isNullOrBlank()) {
                    add(
                        TvContextMenuItem(goToAlbumLabel) {
                            onGoToAlbum(albumId, artworkUri.orEmpty())
                        }
                    )
                }
                if (!artistId.isNullOrBlank()) {
                    add(TvContextMenuItem(goToArtistLabel) { onGoToArtist(artistId, artworkUri) })
                }
            },
            onDismiss = { menuTarget = null },
            title = target.item.mediaMetadata.title?.toString(),
        )
    }

    if (confirmClear) {
        ClearQueueConfirm(
            backdrop = backdrop,
            onDismiss = { confirmClear = false },
            onConfirm = {
                mediaController?.let { controller ->
                    val from = controller.currentMediaItemIndex + 1
                    val count = controller.mediaItemCount - from
                    if (count > 0) {
                        controller.removeMediaItems(from, count)
                        edited()
                    }
                }
                confirmClear = false
            },
        )
    }
}

@Composable
private fun QueueRow(
    item: MediaItem,
    index: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val metadata = item.mediaMetadata
    val background = when {
        isCurrent -> colors.selectionTint
        isFocused -> colors.surface2
        else -> colors.surface1.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.npQueueRowHeight)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.02f,
                lift = 0.dp,
                halo = true,
            )
            .background(background, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = metadata.title?.toString(),
                onClick = onClick,
            )
            .tvLongPress(onOpenMenu)
            .padding(horizontal = TvDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
    ) {
        Box(
            modifier = Modifier.width(TvDimens.space24),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                TvEqualizerGlyph(height = TvDimens.space16, barWidth = 2.dp)
            } else {
                Text(
                    text = (index + 1).toString(),
                    style = type.caption,
                    color = colors.labelTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        TvArtwork(
            data = metadata.artworkUri,
            cacheKey = item.artworkKey,
            size = TvDimens.npQueueArtworkSize,
            shape = LocalTvShapes.current.chip,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title?.toString().orEmpty(),
                style = type.body,
                color = if (isCurrent) colors.accent else colors.labelPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata.artist?.toString().orEmpty(),
                style = type.caption,
                color = colors.labelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        metadata.durationMs?.takeIf { it > 0 }?.let { durationMs ->
            Text(
                text = StringUtils.formatSeconds((durationMs / 1000L).toInt()),
                style = type.caption,
                color = colors.labelTertiary,
                maxLines = 1,
            )
        }

        TvGlassIconButton(
            iconRes = R.drawable.round_more_horiz_24,
            contentDescription = stringResource(R.string.np_more_actions),
            onClick = onOpenMenu,
            size = TvDimens.npQueueActionSize,
            iconSize = TvDimens.iconInline,
        )
    }
}

/** Destructive confirmation for "Clear" (spec §6.7): overlay glass on a scrim. */
@Composable
private fun ClearQueueConfirm(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(TvDimens.menuWidth)
                .tvGlass(
                    backdrop = backdrop,
                    preset = TvGlassPresets.overlay,
                    shape = LocalTvShapes.current.panel,
                )
                .padding(TvDimens.space24),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space16),
        ) {
            Text(
                text = stringResource(R.string.np_clear_queue_title),
                style = type.sectionTitle,
                color = colors.labelPrimary,
            )
            Text(
                text = stringResource(R.string.np_clear_queue_confirm),
                style = type.body,
                color = colors.labelSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TvGlassButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                )
                TvGlassButton(
                    text = stringResource(R.string.np_clear),
                    onClick = onConfirm,
                    style = TvButtonStyle.Primary,
                    modifier = Modifier.tvRequestFocusOnFirstAppearance(),
                )
            }
        }
    }
}
