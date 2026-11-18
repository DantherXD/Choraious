@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.OLEDProtectionMode
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.ui.elements.tv.apple.TvArtwork
import com.craftworks.music.ui.elements.tv.apple.TvArtworkTint
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.elements.tv.apple.TvLyricsPanel
import com.craftworks.music.ui.elements.tv.apple.TvProgressBar
import com.craftworks.music.ui.elements.tv.apple.TvQueuePanel
import com.craftworks.music.ui.theme.tv.LocalReduceMotion
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvMotion
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.theme.tv.TvGlassPresets
import com.craftworks.music.ui.theme.tv.tvGlass
import com.craftworks.music.ui.tv.tvFocusTarget
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlin.time.Duration.Companion.milliseconds

/** Which side panel is open; only one at a time (spec §4.5 glass budget). */
private enum class NpPanel { None, Lyrics, Queue }

/** Controls stay up for this long after the last D-pad input (spec §6.7 keeps the 5 s timeout). */
private const val ControlsTimeoutMs = 5_000L

/**
 * The TV now-playing screen (spec §6.7).
 *
 * Full-bleed and chrome-less: the shell hides the tab bar on this route, so this screen paints its
 * own black base, the blurred-artwork wash, the artwork/title block, the transport and the two
 * side panels.
 *
 * Deliberate choices:
 *
 * - The wash is [TvArtworkTint] — a pre-blurred 48px bitmap — not a Backdrop layer. Backdrop is for
 *   chrome; a full-screen backdrop would re-sample the whole composition every frame.
 * - Panels *displace* the artwork (18% left, 0.94 scale) instead of overlaying it, per spec §6.7.
 * - Auto-hide is off under reduced motion (controls stay visible) and while a panel is open, and
 *   the timeout constant lives here, not in three places.
 * - OLED protection keeps its two existing TV behaviours: "lyrics only" opens the lyrics panel and
 *   dims the wash, "minimal" drops the wash entirely. The red progress fill is softened to 80%.
 */
@OptIn(FlowPreview::class)
@Composable
fun TvNowPlaying(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    navController: NavHostController? = null,
    onRefreshLyrics: () -> Unit = {},
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shapes = LocalTvShapes.current
    val motion = LocalTvMotion.current
    val reduceMotion = LocalReduceMotion.current
    val context = LocalContext.current

    val oledMode by AppearanceSettingsManager(context)
        .oledProtectionMode
        .collectAsStateWithLifecycle(OLEDProtectionMode.OFF)
    val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

    val player: Player? = mediaController ?: ChoraMediaLibraryService.getInstance()?.player

    var panel by remember { mutableStateOf(NpPanel.None) }
    var lastPanel by remember { mutableStateOf(NpPanel.None) }

    // "Lyrics only" OLED mode always has the panel up; the previous TV screen did the same.
    LaunchedEffect(oledMode) {
        if (oledMode == OLEDProtectionMode.LYRICS_ONLY && panel == NpPanel.None) {
            panel = NpPanel.Lyrics
            lastPanel = NpPanel.Lyrics
        }
    }

    BackHandler(enabled = panel != NpPanel.None) {
        panel = NpPanel.None
    }

    // ---- Controls visibility -----------------------------------------------------------------
    // Auto-hide must never take the focused element away (P3-3), so the timer only hides when no
    // transport control holds focus; when it does hide, focus moves to the screen root, which is
    // focusable precisely so the next D-pad press can bring the controls back.
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsFocused by remember { mutableStateOf(false) }
    var pendingPlayFocus by remember { mutableStateOf(false) }
    val interaction = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val currentPanel by rememberUpdatedState(panel)
    val controlsFocusedState by rememberUpdatedState(controlsFocused)

    val rootInteraction = remember { MutableInteractionSource() }
    val rootRequester = remember { FocusRequester() }
    val playRequester = remember { FocusRequester() }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            controlsVisible = true
            return@LaunchedEffect
        }
        interaction
            .debounce(ControlsTimeoutMs.milliseconds)
            .collect {
                if (currentPanel == NpPanel.None && !controlsFocusedState) {
                    controlsVisible = false
                    runCatching { rootRequester.requestFocus() }
                }
            }
    }

    fun revealControls() {
        controlsVisible = true
        if (!reduceMotion) interaction.tryEmit(Unit)
    }

    // Re-focusing has to wait for the transport to be composed by the visibility change.
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible || !pendingPlayFocus) return@LaunchedEffect
        withFrameNanos { }
        pendingPlayFocus = false
        runCatching { playRequester.requestFocus() }
    }

    // ---- Playback position (drives the lyrics highlight and the panel time) -------------------
    var positionMs by remember { mutableIntStateOf(0) }
    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L).toInt()
            delay(500L)
        }
    }

    // ---- Background wash drift (spec §6.7: ±12 spec points, 20 s cycle) ----------------------
    val driftPx = with(LocalDensity.current) { TvDimens.npBackgroundDrift.toPx() }
    val driftTransition = rememberInfiniteTransition(label = "npDrift")
    val animatedDrift by driftTransition.animateFloat(
        initialValue = if (reduceMotion) 0f else -driftPx,
        targetValue = if (reduceMotion) 0f else driftPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "npDriftValue",
    )

    val displace by animateFloatAsState(
        targetValue = if (panel == NpPanel.None) 0f else 1f,
        animationSpec = if (reduceMotion) snap() else motion.panelIn,
        label = "npDisplace",
    )

    val lyricsRequester = remember { FocusRequester() }
    val queueRequester = remember { FocusRequester() }

    // Closing a panel puts focus back on the pill that opened it (spec §6.7/P3-6).
    LaunchedEffect(panel) {
        if (panel != NpPanel.None) return@LaunchedEffect
        when (lastPanel) {
            NpPanel.Lyrics -> runCatching { lyricsRequester.requestFocus() }
            NpPanel.Queue -> runCatching { queueRequester.requestFocus() }
            NpPanel.None -> {}
        }
    }

    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootRequester)
            .focusable(interactionSource = rootInteraction)
            // Any D-pad input reveals the controls and restarts the timer; the event still reaches
            // the focused control, because this only observes it.
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (!controlsVisible) pendingPlayFocus = true
                    revealControls()
                }
                false
            },
    ) {
        val panelWidth = (maxWidth * TvDimens.npPanelWidthFraction)
            .coerceAtMost(TvDimens.npPanelMaxWidth)
        val queuePanelWidth = (maxWidth * TvDimens.npQueuePanelWidthFraction)
            .coerceAtMost(TvDimens.npQueuePanelMaxWidth)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            if (oledMode != OLEDProtectionMode.MINIMAL) {
                TvArtworkTint(
                    data = metadata?.artworkUri,
                    cacheKey = metadata?.id,
                    dim = if (oledMode == OLEDProtectionMode.OFF) 0.65f else 0.88f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = animatedDrift
                            scaleX = 1.06f
                            scaleY = 1.06f
                        },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -size.width * TvDimens.npDisplaceFraction * displace
                        val scale = 1f - (1f - TvDimens.npDisplaceScale) * displace
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = TvDimens.screenMarginHorizontal,
                            vertical = TvDimens.screenMarginTop,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.space32),
                ) {
                    Box(
                        modifier = Modifier
                            .size(TvDimens.npArtworkSize)
                            .shadow(TvDimens.npArtworkShadow, shapes.tile, clip = false),
                    ) {
                        AnimatedContent(
                            targetState = metadata?.artworkUri,
                            transitionSpec = {
                                val fade = if (reduceMotion) {
                                    motion.reducedFade
                                } else {
                                    motion.artworkCrossfade
                                }
                                (fadeIn(animationSpec = fade) + scaleIn(
                                    initialScale = if (reduceMotion) 1f else 0.97f,
                                    animationSpec = fade,
                                )) togetherWith fadeOut(
                                    animationSpec = if (reduceMotion) motion.reducedFade else motion.panelTint
                                )
                            },
                            label = "npArtwork",
                        ) { artworkUri ->
                            TvArtwork(
                                data = artworkUri,
                                cacheKey = metadata?.id,
                                size = TvDimens.npArtworkSize,
                                requestSize = TvDimens.npArtworkSize,
                                shape = shapes.tile,
                                contentDescription = metadata?.title?.toString(),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = TvDimens.npInfoWidth)
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(TvDimens.space8),
                    ) {
                        Text(
                            text = metadata?.title?.toString().orEmpty(),
                            style = type.npTitle,
                            color = colors.labelPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            text = metadata?.artist?.toString().orEmpty(),
                            style = type.npArtist,
                            color = colors.labelSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val album = metadata?.albumTitle?.toString()
                        val year = metadata?.recordingYear?.takeIf { it > 0 }
                        val format = metadata?.extras?.getString("format")
                        val metaLine = listOfNotNull(album, year?.toString(), format)
                            .joinToString(" • ")
                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = type.caption,
                                color = colors.labelTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Right-edge panel triggers (spec §6.7). They sit under the panels when one is open.
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TvDimens.npActionEdge),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space12),
                horizontalAlignment = Alignment.End,
            ) {
                TvGlassButton(
                    text = stringResource(R.string.np_lyrics),
                    iconRes = R.drawable.lyrics_active,
                    onClick = {
                        panel = NpPanel.Lyrics
                        lastPanel = NpPanel.Lyrics
                        revealControls()
                    },
                    modifier = Modifier.focusRequester(lyricsRequester),
                )
                TvGlassButton(
                    text = stringResource(R.string.np_up_next),
                    iconRes = R.drawable.rounded_queue_music_24,
                    onClick = {
                        panel = NpPanel.Queue
                        lastPanel = NpPanel.Queue
                        revealControls()
                    },
                    modifier = Modifier.focusRequester(queueRequester),
                )
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(
                    animationSpec = if (reduceMotion) motion.reducedFade else motion.controlsFade
                ) + if (reduceMotion) {
                    EnterTransition.None
                } else {
                    slideInVertically(
                        animationSpec = motion.panelSlide,
                        initialOffsetY = { it / 2 },
                    )
                },
                exit = fadeOut(
                    animationSpec = if (reduceMotion) motion.reducedFade else motion.controlsFade
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = TvDimens.screenMarginHorizontal)
                    .padding(bottom = TvDimens.space24),
            ) {
                Column(
                    modifier = Modifier
                        .width(TvDimens.npTransportWidth)
                        .onFocusChanged { controlsFocused = it.hasFocus },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TvProgressBar(
                        mediaController = mediaController,
                        enabled = metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION,
                        fillAlpha = if (oledMode != OLEDProtectionMode.OFF) 0.8f else 1f,
                        onInteraction = { revealControls() },
                    )

                    Spacer(modifier = Modifier.height(TvDimens.space12))

                    Row(
                        modifier = Modifier
                            .height(TvDimens.npTransportHeight)
                            .tvGlass(
                                backdrop = backdrop,
                                preset = TvGlassPresets.pill,
                                shape = shapes.pill,
                            )
                            .padding(horizontal = TvDimens.space12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        if (player != null) {
                            TransportControls(
                                player = player,
                                playFocusRequester = playRequester,
                            )
                        }
                    }
                }
            }
        }

        val panelEnter = if (reduceMotion) {
            EnterTransition.None
        } else {
            slideInHorizontally(
                animationSpec = motion.panelSlide,
                initialOffsetX = { it },
            ) + fadeIn(animationSpec = motion.panelTint)
        }
        val panelExit = if (reduceMotion) {
            ExitTransition.None
        } else {
            slideOutHorizontally(
                animationSpec = motion.panelSlide,
                targetOffsetX = { it },
            ) + fadeOut(animationSpec = motion.panelTint)
        }
        // Fill height only: the container must wrap the panel's width, otherwise the child would
        // start at the container's left edge instead of docking to the right.
        val panelModifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(
                end = TvDimens.npPanelGap,
                top = TvDimens.space16,
                bottom = TvDimens.space16,
            )
            .fillMaxHeight()

        AnimatedVisibility(
            visible = panel == NpPanel.Lyrics,
            enter = panelEnter,
            exit = panelExit,
            modifier = panelModifier,
        ) {
            TvLyricsPanel(
                backdrop = backdrop,
                lyrics = lyrics,
                positionMs = positionMs,
                onClose = { panel = NpPanel.None },
                onRefresh = onRefreshLyrics,
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight(),
            )
        }

        AnimatedVisibility(
            visible = panel == NpPanel.Queue,
            enter = panelEnter,
            exit = panelExit,
            modifier = panelModifier,
        ) {
            TvQueuePanel(
                backdrop = backdrop,
                mediaController = mediaController,
                onClose = { panel = NpPanel.None },
                onAddFromLibrary = {
                    panel = NpPanel.None
                    navController?.navigate(Screen.Library) {
                        popUpTo<Screen.NowPlayingLandscape> { inclusive = true }
                    }
                },
                onGoToAlbum = { albumId, imageUri ->
                    panel = NpPanel.None
                    navController?.navigate(Screen.AlbumDetails(albumId, imageUri)) {
                        popUpTo<Screen.NowPlayingLandscape> { inclusive = true }
                    }
                },
                onGoToArtist = { artistId, imageUri ->
                    panel = NpPanel.None
                    navController?.navigate(Screen.ArtistDetails(artistId, imageUri)) {
                        popUpTo<Screen.NowPlayingLandscape> { inclusive = true }
                    }
                },
                modifier = Modifier
                    .width(queuePanelWidth)
                    .fillMaxHeight(),
            )
        }
    }
}

/** Transport row: shuffle, previous, play/pause, next, repeat (spec §6.7). */
@Composable
private fun TransportControls(player: Player, playFocusRequester: FocusRequester) {
    val colors = LocalTvColors.current

    val shuffleState = rememberShuffleButtonState(player)
    val previousTitle = stringResource(R.string.np_previous)
    val playTitle = stringResource(R.string.np_play)
    val pauseTitle = stringResource(R.string.np_pause)
    val nextTitle = stringResource(R.string.np_next)
    val shuffleTitle = stringResource(R.string.np_shuffle)
    val repeatTitle = stringResource(R.string.np_repeat)

    TransportButton(
        iconRes = R.drawable.round_shuffle_28,
        contentDescription = shuffleTitle,
        onClick = shuffleState::onClick,
        enabled = shuffleState.isEnabled,
        tint = if (shuffleState.shuffleOn) colors.accent else colors.labelPrimary,
        scaleSpec = TvDimens.focusScaleButton,
    )

    TransportButton(
        iconRes = R.drawable.media3_notification_seek_to_previous,
        contentDescription = previousTitle,
        onClick = { player.seekToPreviousMediaItem() },
        enabled = player.hasPreviousMediaItem(),
        scaleSpec = TvDimens.focusScaleButton,
    )

    PlayPauseButton(
        player = player,
        playTitle = playTitle,
        pauseTitle = pauseTitle,
        focusRequester = playFocusRequester,
    )

    TransportButton(
        iconRes = R.drawable.media3_notification_seek_to_next,
        contentDescription = nextTitle,
        onClick = { player.seekToNextMediaItem() },
        enabled = player.hasNextMediaItem(),
        scaleSpec = TvDimens.focusScaleButton,
    )

    val repeatState = rememberRepeatButtonState(player)
    TransportButton(
        iconRes = if (repeatState.repeatModeState == Player.REPEAT_MODE_ONE) {
            R.drawable.rounded_repeat1_24
        } else {
            R.drawable.rounded_repeat_24
        },
        contentDescription = repeatTitle,
        onClick = repeatState::onClick,
        enabled = repeatState.isEnabled,
        tint = if (repeatState.repeatModeState != Player.REPEAT_MODE_OFF) {
            colors.accent
        } else {
            colors.labelPrimary
        },
        scaleSpec = TvDimens.focusScaleButton,
    )
}

@Composable
private fun TransportButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    scaleSpec: Float,
    tint: Color = LocalTvColors.current.labelPrimary,
) {
    val colors = LocalTvColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(TvDimens.iconButtonSize)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = CircleShape,
                focusedScale = scaleSpec,
                lift = 0.dp,
                halo = enabled,
            )
            .background(
                color = if (isFocused) colors.surface3 else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.labelDisabled,
            modifier = Modifier.size(TvDimens.iconTransport),
        )
    }
}

/** The white filled play/pause circle from the spec: white disc, black glyph. */
@Composable
private fun PlayPauseButton(
    player: Player,
    playTitle: String,
    pauseTitle: String,
    focusRequester: FocusRequester,
) {
    val state = rememberPlayPauseButtonState(player)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .size(TvDimens.npPlayButtonSize)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = CircleShape,
                focusedScale = TvDimens.focusScaleButton,
                lift = 0.dp,
            )
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = state.isEnabled,
                onClickLabel = if (state.showPlay) playTitle else pauseTitle,
                onClick = state::onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (state.showPlay) {
                ImageVector.vectorResource(R.drawable.round_play_arrow_24)
            } else {
                ImageVector.vectorResource(R.drawable.media3_notification_pause)
            },
            contentDescription = if (state.showPlay) playTitle else pauseTitle,
            tint = Color.Black,
            modifier = Modifier.size(TvDimens.iconTransport),
        )
    }
}
