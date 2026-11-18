package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.craftworks.music.ui.theme.tv.LocalTvColors

/**
 * The blurred-artwork wash behind a detail header or now-playing screen (spec §6/§7).
 *
 * Two deliberate choices:
 *
 * - The image is requested at 48px and scaled up, not blurred at full resolution. That decode is
 *   one small bitmap from Coil's memory cache keyed by `${cacheKey}-blur`, so a header never
 *   re-blurs per frame and the cost does not grow with screen size.
 * - `Modifier.blur` is `RenderEffect`, which is a no-op below API 31. The upscaled 48px bitmap is
 *   still soft there, so the fallback is a plausible tint rather than a missing background; the
 *   extra darkening below keeps text legible in both tiers.
 */
@Composable
fun TvArtworkTint(
    data: Any?,
    cacheKey: String?,
    modifier: Modifier = Modifier,
    dim: Float = 0.65f,
    blurRadius: Int = 40,
) {
    val context = LocalContext.current
    val colors = LocalTvColors.current

    val request = ImageRequest.Builder(context)
        .data(data)
        .size(48, 48)
        .apply {
            if (cacheKey != null) {
                // Distinct from the crisp key: same item id, different decode size.
                memoryCacheKey("$cacheKey-blur")
                diskCacheKey("$cacheKey-blur")
            }
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()

    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp)
                .graphicsLayer { alpha = 0.85f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dim))
        )
        // Bottom-up fade so the header's text block sits on near-solid canvas.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.bgCanvas.copy(alpha = 0.85f))
                    )
                )
        )
    }
}
