package com.craftworks.music.ui.tv

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import com.craftworks.music.ui.theme.tv.LocalReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvMotion
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * Distance from the top of the screen to where screen content may start: the tab bar's
 * height plus its gap (spec §4.4/§5.3).
 *
 * Screens add this to their own top padding. The shell draws content edge-to-edge underneath
 * the bar so the glass has something to sample while scrolling.
 */
val LocalTvTopInset = compositionLocalOf { TvDimens.tabBarHeight + TvDimens.tabBarTopInset }

/**
 * True while Search is the active tab, so `TvSearchScreen` can put the cursor in the field on
 * arrival (spec §5.1). Provided by the shell rather than by a click handler on the screen.
 */
val LocalTvFocusSearchOnEntry = compositionLocalOf { false }

@Composable
private fun focusAnimationSpec(isFocused: Boolean): FiniteAnimationSpec<Float> {
    val motion = LocalTvMotion.current
    return when {
        LocalReduceMotion.current -> snap()
        isFocused -> motion.focusIn
        else -> motion.focusOut
    }
}

/**
 * The TV focus language for elements that are not tv-material3 surfaces (spec §4.6):
 * scale only, with the 180 ms in / 140 ms out springs, snapped under reduce-motion.
 *
 * tv-material3 components animate their own focus scale, so new code attaches this to a raw
 * `clickable` instead — never both, or the two scales multiply.
 */
@Composable
fun Modifier.tvFocusScale(
    interactionSource: InteractionSource,
    focusedScale: Float = TvDimens.focusScaleTab,
    unfocusedScale: Float = 1f,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else unfocusedScale,
        animationSpec = focusAnimationSpec(isFocused),
        label = "tvFocusScale",
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Spec §4.6 halo: a 3dp ring at white 90% that sits **outside** the element's clip, plus an
 * 8dp glow at white 12% outside the ring.
 *
 * The shape's outline is stroked on a slightly enlarged copy of itself, so the stroke lands
 * outside the clip instead of straddling it (a plain `Modifier.border` draws inside). The
 * canvas scale also scales the stroke width, so each width is divided by its own factor to
 * keep the ring at exactly 3dp.
 *
 * The halo is a draw-time effect and never changes the layout: it can overflow the element's
 * bounds, so the caller needs at least 11dp of empty space around the element (rail gap 32dp
 * and grid gaps cover this).
 */
@Composable
fun Modifier.tvFocusHalo(
    interactionSource: InteractionSource,
    shape: Shape,
): Modifier {
    val colors = LocalTvColors.current
    val isFocused by interactionSource.collectIsFocusedAsState()

    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = focusAnimationSpec(isFocused),
        label = "tvFocusHalo",
    )

    return drawWithContent {
        drawContent()
        if (alpha <= 0.01f) return@drawWithContent

        val shortest = minOf(size.width, size.height)
        if (shortest <= 0f) return@drawWithContent

        val outline = shape.createOutline(Size(size.width, size.height), layoutDirection, this)
        val path = Path().apply { addOutline(outline) }

        val ringWidth = TvDimens.focusRingWidth.toPx()
        val glowWidth = TvDimens.focusGlowWidth.toPx()

        // Centre each stroke at its distance from the original edge, then compensate the width.
        val glowScale = 1f + 2f * (ringWidth + glowWidth / 2f) / shortest
        scale(glowScale, glowScale, center) {
            drawPath(
                path = path,
                color = colors.focusGlow.copy(alpha = colors.focusGlow.alpha * alpha),
                style = Stroke(width = glowWidth / glowScale),
            )
        }

        val ringScale = 1f + ringWidth / shortest
        scale(ringScale, ringScale, center) {
            drawPath(
                path = path,
                color = colors.focusRing.copy(alpha = colors.focusRing.alpha * alpha),
                style = Stroke(width = ringWidth / ringScale),
            )
        }
    }
}

/**
 * The full §4.6 treatment for a non-tv-material surface: scale, lift, and halo.
 *
 * `graphicsLayer` is applied first so the halo scales and lifts with the element — and so the
 * halo, which overflows the bounds on purpose, is not clipped by the layer.
 */
@Composable
fun Modifier.tvFocusTarget(
    interactionSource: InteractionSource,
    shape: Shape,
    focusedScale: Float = TvDimens.focusScaleTile,
    lift: Dp = TvDimens.focusLift,
    halo: Boolean = true,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val liftPx = with(LocalDensity.current) { lift.toPx() }
    val liftFraction by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = focusAnimationSpec(isFocused),
        label = "tvFocusLift",
    )

    var modifier = this
        .graphicsLayer {
            scaleX = 1f
            scaleY = 1f
            translationY = -liftPx * liftFraction
        }
        .tvFocusScale(interactionSource, focusedScale)

    if (halo) modifier = modifier.tvFocusHalo(interactionSource, shape)

    return modifier
}

/**
 * Spec §4.6 artwork parallax: the bitmap inside a focused tile drifts ±[maxShift] opposite to
 * the tile's position relative to the screen centre, clipped by the tile. Only the artwork
 * moves — the tile itself only lifts.
 *
 * Position is read once per placement, not per frame, and the drift is animated, so this adds
 * no per-frame layout work.
 */
@Composable
fun Modifier.tvArtworkParallax(
    interactionSource: InteractionSource,
    maxShift: Dp = TvDimens.focusParallax,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val view = LocalView.current
    val shiftPx = with(LocalDensity.current) { maxShift.toPx() }

    var offsetFraction by remember { mutableFloatStateOf(0f) }

    val drift by animateFloatAsState(
        targetValue = if (isFocused) offsetFraction else 0f,
        animationSpec = focusAnimationSpec(isFocused),
        label = "tvArtworkParallax",
    )

    return this
        .onGloballyPositioned { coordinates ->
            val screenWidth = view.width
            if (screenWidth <= 0) return@onGloballyPositioned
            val centerX = coordinates.positionInWindow().x + coordinates.size.width / 2f
            offsetFraction = ((centerX / screenWidth) - 0.5f) * 2f
        }
        .graphicsLayer { translationX = -drift * shiftPx }
}

/**
 * Requests focus the first time this element is placed — used to land the D-pad on the primary
 * action of a freshly opened screen without fighting the nav host's transition.
 *
 * Lived in the old TV home screen until P2 moved it here, because the now-playing screen needs it
 * too and a screen file is the wrong home for a focus primitive.
 */
@Composable
fun Modifier.tvRequestFocusOnFirstAppearance(): Modifier {
    val requester = remember { FocusRequester() }
    var placed by remember { mutableStateOf(false) }

    LaunchedEffect(placed) {
        if (placed) runCatching { requester.requestFocus() }
    }

    return this
        .focusRequester(requester)
        .onPlaced { placed = true }
}

/** How long OK must be held for a long press on a D-pad. */
private const val LongPressMillis = 550L

/**
 * D-pad long press, used by tiles, track rows and grids to open the context menu.
 *
 * There is no touch long press on a TV, and a held OK arrives as repeated key-downs, so the
 * press is timed here. `onPreviewKeyEvent` is deliberate: preview events travel from the outer
 * modifier chain inward, so this sees the release before `clickable` can turn it into a click —
 * which is what swallowing the closing key-up relies on.
 */
@Composable
fun Modifier.tvLongPress(onLongPress: (() -> Unit)?): Modifier {
    if (onLongPress == null) return this

    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var fired by remember { mutableStateOf(false) }

    return onPreviewKeyEvent { event ->
        val activates = event.key == Key.DirectionCenter || event.key == Key.Enter
        if (!activates) return@onPreviewKeyEvent false

        when (event.type) {
            KeyEventType.KeyDown -> {
                if (event.nativeKeyEvent.repeatCount == 0) {
                    fired = false
                    job?.cancel()
                    job = scope.launch {
                        delay(LongPressMillis)
                        fired = true
                        onLongPress()
                    }
                }
                false
            }

            KeyEventType.KeyUp -> {
                job?.cancel()
                job = null
                if (fired) {
                    fired = false
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }
}
