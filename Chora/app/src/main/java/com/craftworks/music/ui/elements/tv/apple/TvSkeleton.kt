package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.craftworks.music.ui.theme.tv.LocalTvColors

/**
 * A loading placeholder block (spec §7: skeletons on Listen Now, Browse and lists).
 *
 * Deliberately still. It used to sweep a gradient forever, which meant the home screen was
 * permanently animating while data loaded — the motion is gone, the two-tone fill is not.
 */
@Composable
fun TvSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val colors = LocalTvColors.current

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(listOf(colors.surface1, colors.surface2)),
            shape = shape,
        )
    )
}
