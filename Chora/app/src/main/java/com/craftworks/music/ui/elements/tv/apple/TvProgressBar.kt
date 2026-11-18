package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvFocusHalo
import com.craftworks.music.utils.StringUtils
import kotlinx.coroutines.delay

/** D-pad seek step on a focused progress bar (spec §6.7). */
private const val SeekStepMs = 10_000L

/** How often the bar re-reads the controller while it is on screen. */
private const val PollIntervalMs = 500L

/**
 * The now-playing progress bar (spec §6.7/§7.3): a 2dp track with a red fill, elapsed and
 * remaining captions, and ±10s D-pad seeking while it holds focus.
 *
 * The controller is polled rather than observed through `Player.Listener`: the bar shows a
 * continuously moving value, so it needs a tick regardless, and one 2Hz read is cheaper than a
 * listener plus a ticker.
 *
 * Under OLED protection the fill is drawn at 80% so the bright red does not sit unchanged in one
 * spot (spec §6.7).
 */
@OptIn(UnstableApi::class)
@Composable
fun TvProgressBar(
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillAlpha: Float = 1f,
    onInteraction: () -> Unit = {},
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = LocalTvShapes.current.pill

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(mediaController) {
        if (mediaController == null) return@LaunchedEffect
        while (true) {
            positionMs = mediaController.currentPosition.coerceAtLeast(0L)
            durationMs = mediaController.duration.coerceAtLeast(0L)
            delay(PollIntervalMs)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + deltaMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(target)
        positionMs = target
        onInteraction()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvDimens.npProgressTouchHeight)
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .tvFocusHalo(interactionSource = interactionSource, shape = shape)
                .onKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            seekBy(-SeekStepMs)
                            true
                        }

                        Key.DirectionRight -> {
                            seekBy(SeekStepMs)
                            true
                        }

                        else -> false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) TvDimens.npProgressHeight * 2 else TvDimens.npProgressHeight)
                    .background(colors.labelPrimary.copy(alpha = 0.14f), shape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(colors.accent.copy(alpha = colors.accent.alpha * fillAlpha), shape),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TvDimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = StringUtils.formatSeconds((positionMs / 1000L).toInt()),
                style = type.caption,
                color = colors.labelTertiary,
            )
            Text(
                text = "-" + StringUtils.formatSeconds(
                    ((durationMs - positionMs).coerceAtLeast(0L) / 1000L).toInt()
                ),
                style = type.caption,
                color = colors.labelTertiary,
            )
        }
    }
}
