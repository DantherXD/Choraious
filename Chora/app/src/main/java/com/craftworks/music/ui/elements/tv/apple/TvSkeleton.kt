package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.craftworks.music.ui.theme.tv.LocalReduceMotion
import com.craftworks.music.ui.theme.tv.LocalTvColors

/**
 * A loading placeholder block (spec §7: skeletons on Listen Now, Browse and lists).
 *
 * One translating gradient per skeleton rather than a per-item animation, and no animation at
 * all under reduced motion — where it simply sits at its resting fill.
 */
@Composable
fun TvSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val colors = LocalTvColors.current

    if (LocalReduceMotion.current) {
        Box(modifier = modifier.background(colors.surface1, shape))
        return
    }

    val transition = rememberInfiniteTransition(label = "tvSkeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tvSkeletonSweep",
    )

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(colors.surface1, colors.surface2, colors.surface1),
                start = Offset(x = -600f + progress * 2400f, y = 0f),
                end = Offset(x = progress * 2400f, y = 600f),
            ),
            shape = shape,
        )
    )
}
