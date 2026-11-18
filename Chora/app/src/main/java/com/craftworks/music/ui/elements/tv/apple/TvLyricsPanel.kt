package com.craftworks.music.ui.elements.tv.apple

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.LyricsRole
import com.craftworks.music.data.model.SyncType
import com.craftworks.music.ui.theme.tv.LocalReduceMotion
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.theme.tv.TvGlassPresets
import com.craftworks.music.ui.theme.tv.tvGlass
import com.kyant.backdrop.Backdrop

/**
 * The lyrics panel (spec §6.7/P3-4): a right-hand `panel` glass surface over the now-playing
 * artwork, with synced, word-synced and plain lyrics.
 *
 * It deliberately does **not** reuse `LyricsView`: that composable is phone Material3, reads the
 * phone alignment/blur/bounce settings and is built around pull-to-refresh. The data (`LyricsState`,
 * `LyricsRepository`) is reused unchanged — this file only renders it with TV tokens.
 *
 * Word sync: the active line's words are coloured as the track passes them, so a word-synced track
 * shows progress inside the line instead of jumping only per line. Plain (unsynced) lyrics are one
 * scrollable block, as the spec asks.
 *
 * Focus: the list takes focus on open, D-pad Left returns to the now-playing surface (the caller
 * restores focus to the trigger), and Back closes the panel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvLyricsPanel(
    backdrop: Backdrop,
    lyrics: Lyrics?,
    positionMs: Int,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val reduceMotion = LocalReduceMotion.current

    // Rounded on the leading edge only: the panel is docked to the right screen edge.
    val shape = RoundedCornerShape(
        topStart = TvDimens.npPanelCorner,
        bottomStart = TvDimens.npPanelCorner,
    )

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    BackHandler { onClose() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val synced = lyrics != null &&
        lyrics.syncType != SyncType.NONE &&
        lyrics.lines.any { it.startMs >= 0 }

    val activeIndex = if (!synced) {
        -1
    } else {
        lyrics!!.lines.indexOfLast { it.startMs >= 0 && it.startMs <= positionMs }
    }

    // Centres the active line: the item is scrolled half a viewport up from the top edge.
    LaunchedEffect(activeIndex) {
        if (activeIndex < 0) return@LaunchedEffect
        val target = activeIndex.coerceAtLeast(0)
        val offset = -listState.layoutInfo.viewportSize.height / 2
        if (reduceMotion) {
            listState.scrollToItem(target, offset)
        } else {
            listState.animateScrollToItem(target, offset)
        }
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
                text = stringResource(R.string.np_lyrics),
                style = type.sectionTitle,
                color = colors.labelPrimary,
                modifier = Modifier.weight(1f),
            )
            TvGlassButton(
                text = stringResource(R.string.action_done),
                onClick = onClose,
            )
        }

        Spacer(modifier = Modifier.height(TvDimens.space16))

        when {
            lyrics == null || lyrics.lines.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusProperties { canFocus = false },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.np_lyrics_unavailable),
                        style = type.body,
                        color = colors.labelSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(TvDimens.space16))
                    TvGlassButton(
                        text = stringResource(R.string.np_lyrics_refresh),
                        onClick = onRefresh,
                    )
                }
            }

            !synced -> {
                // Plain lyrics: one scrollable block, no active line to follow (spec §6.7).
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    item {
                        Text(
                            text = lyrics.lines.joinToString("\n\n") { line ->
                                line.lines.joinToString("\n") { it.text }
                            },
                            style = type.body,
                            color = colors.labelSecondary,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(TvDimens.space12),
                ) {
                    // Leading and trailing room so the first and last lines can be centred.
                    item { Spacer(modifier = Modifier.height(TvDimens.npPanelScrollPadding)) }
                    itemsIndexed(
                        items = lyrics.lines,
                        key = { index, line -> "$index-${line.startMs}" },
                    ) { index, line ->
                        LyricsLineRow(
                            line = line,
                            isActive = index == activeIndex,
                            positionMs = positionMs,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(TvDimens.npPanelScrollPadding)) }
                }
            }
        }
    }
}

@Composable
private fun LyricsLineRow(
    line: LyricsLine,
    isActive: Boolean,
    positionMs: Int,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The active line's red leading bar (spec §6.7).
        Box(
            modifier = Modifier
                .width(TvDimens.npLyricsLeadBarWidth)
                .height(TvDimens.space24)
                .background(
                    color = if (isActive) colors.accent else Color.Transparent,
                    shape = RoundedCornerShape(percent = 50),
                ),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
        ) {
            line.lines.forEach { lyric ->
                LyricText(
                    lyric = lyric,
                    isActive = isActive,
                    positionMs = positionMs,
                )
            }
        }
    }
}

@Composable
private fun LyricText(
    lyric: Lyric,
    isActive: Boolean,
    positionMs: Int,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    val isBackground = lyric.role == LyricsRole.BG
    val style = when {
        isBackground -> type.caption
        isActive -> type.lyricsActive
        else -> type.lyricsInactive
    }
    val baseColor = when {
        isBackground -> colors.labelTertiary
        isActive -> colors.labelPrimary
        else -> colors.labelSecondary
    }

    val words = lyric.words
    val text: AnnotatedString = if (isActive && !words.isNullOrEmpty()) {
        buildAnnotatedString {
            words.forEach { word ->
                val passed = word.startMs <= positionMs
                withStyle(
                    SpanStyle(color = if (passed) colors.labelPrimary else colors.labelTertiary)
                ) {
                    append(word.text)
                }
            }
        }
    } else {
        AnnotatedString(lyric.text)
    }

    Text(
        text = text,
        style = style,
        color = baseColor,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
