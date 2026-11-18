package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.craftworks.music.ui.theme.tv.LocalReduceMotion
import com.craftworks.music.ui.theme.tv.LocalTvColors

/**
 * The now-playing indicator: four bars, staggered so they never move in lockstep.
 *
 * Under reduced motion the transition is not started at all and the bars render at their
 * resting heights — spec §4.7 asks for no looping animation, not for a shorter loop.
 */
@Composable
fun TvEqualizerGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalTvColors.current.accent,
    height: Dp = 20.dp,
    barWidth: Dp = 3.dp,
) {
    val bars = 4
    val reduceMotion = LocalReduceMotion.current

    if (reduceMotion) {
        EqualizerBars(
            fractions = EqualizerRestingFractions,
            color = color,
            height = height,
            barWidth = barWidth,
            modifier = modifier,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "tvEqualizer")
    val fractions = List(bars) { index ->
        val (low, high) = when (index) {
            0 -> 0.35f to 0.95f
            1 -> 0.55f to 1f
            2 -> 0.25f to 0.7f
            else -> 0.45f to 0.85f
        }
        val duration = 620 + index * 130
        val animated by transition.animateFloat(
            initialValue = low,
            targetValue = high,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "tvEqualizerBar$index",
        )
        animated
    }

    EqualizerBars(
        fractions = fractions,
        color = color,
        height = height,
        barWidth = barWidth,
        modifier = modifier,
    )
}

private val EqualizerRestingFractions = listOf(0.45f, 0.75f, 0.35f, 0.6f)

@Composable
private fun EqualizerBars(
    fractions: List<Float>,
    color: Color,
    height: Dp,
    barWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(barWidth / 2),
    ) {
        fractions.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction.coerceIn(0.15f, 1f))
                    .background(color, shape)
            )
        }
    }
}
