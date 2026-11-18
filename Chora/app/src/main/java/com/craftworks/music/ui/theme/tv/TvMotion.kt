package com.craftworks.music.ui.theme.tv

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens (spec §4.7).
 *
 * Presets live here so screens never hardcode durations; per-screen tuning is allowed but
 * must be commented with the screen name. Under reduced motion [spec] snaps and
 * [duration] returns 0, except fades at or below [ReducedMotionMaxFadeMillis].
 */
@Immutable
data class TvMotion(
    val focusIn: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 400f),
    val focusOut: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 300f),
    val press: FiniteAnimationSpec<Float> = tween(durationMillis = 90),
    val tabContent: FiniteAnimationSpec<Float> =
        tween(durationMillis = 250, easing = FastOutSlowInEasing),
    val push: FiniteAnimationSpec<Float> =
        tween(durationMillis = 350, easing = FastOutSlowInEasing),
    val pop: FiniteAnimationSpec<Float> =
        tween(durationMillis = 250, easing = FastOutSlowInEasing),
    val artworkCrossfade: FiniteAnimationSpec<Float> = tween(durationMillis = 450),
    val artworkScale: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 350f),
    val lyricsLine: FiniteAnimationSpec<Float> = tween(durationMillis = 300),
    val lyricsActiveLine: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 380f),
    val panelIn: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 380f),
    val panelTint: FiniteAnimationSpec<Float> = tween(durationMillis = 200),
    val dialogIn: FiniteAnimationSpec<Float> = tween(durationMillis = 300),
    val dialogOut: FiniteAnimationSpec<Float> = tween(durationMillis = 200),
    val controlsFade: FiniteAnimationSpec<Float> = tween(durationMillis = 250),

    /**
     * Onboarding step change: 350 ms with a cubic in-out, slightly softer than [push] because
     * nothing is being pushed on a navigation stack.
     */
    val stepChange: FiniteAnimationSpec<Float> =
        tween(durationMillis = 350, easing = EaseInOutCubic),

    /** [stepChange] for the horizontal slide of the same transition. */
    val stepSlide: FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = 350, easing = EaseInOutCubic),

    /**
     * The only fade allowed under reduced motion: at [ReducedMotionMaxFadeMillis] it is the
     * longest duration that still counts as an instant change rather than an animation.
     */
    val reducedFade: FiniteAnimationSpec<Float> =
        tween(durationMillis = ReducedMotionMaxFadeMillis),

    /** [panelIn] as an offset spec, for the slide of a panel or the transport bar. */
    val panelSlide: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.85f, stiffness = 380f),
)

val LocalTvMotion = staticCompositionLocalOf { TvMotion() }

/** Snaps the animation under reduced motion instead of animating it. */
@Composable
fun <T> TvMotion.spec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else spec

/** Millis for non-Compose timing (delays, auto-hide); 0 under reduced motion except short fades. */
@Composable
fun TvMotion.duration(millis: Int): Int =
    if (LocalReduceMotion.current && millis > ReducedMotionMaxFadeMillis) 0 else millis
