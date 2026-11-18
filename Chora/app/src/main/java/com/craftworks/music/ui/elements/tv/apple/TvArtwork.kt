package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.media3.common.MediaItem
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.craftworks.music.R
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id

/**
 * One artwork surface for every TV tile, hero and header (spec §4.8/§7).
 *
 * Requests are always sized explicitly — full resolution for the hero/now-playing art, a
 * downsampled bitmap (tile size × density) for rails — because a full-resolution bitmap decoded
 * per tile is the single biggest cost on a 4K TV.
 *
 * The memory/disk cache key is the item id, so the same album shown at two sizes reuses one
 * decode and a back navigation repaints from memory instead of hitting the provider.
 */
@Composable
fun TvArtwork(
    data: Any?,
    cacheKey: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    size: Dp? = null,
    requestSize: Dp? = null,
    shape: Shape? = null,
    contentDescription: String? = null,
    crossfade: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val decodeSize = requestSize ?: size
    val targetPx = decodeSize?.let { with(density) { it.roundToPx() } }

    // `ImageRequest` is not a data class, so rebuilding it on every recomposition would look like
    // a new model to Coil and restart the load. It is keyed on the inputs that matter instead.
    val request = remember(data, cacheKey, targetPx, crossfade) {
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (targetPx != null) size(targetPx, targetPx)
                if (cacheKey != null) {
                    memoryCacheKey(cacheKey)
                    placeholderMemoryCacheKey(cacheKey)
                    diskCacheKey(cacheKey)
                }
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfade)
            .build()
    }

    var imageModifier = modifier
    if (size != null) imageModifier = imageModifier.size(size)
    if (shape != null) imageModifier = imageModifier.clip(shape)

    AsyncImage(
        model = request,
        fallback = painterResource(R.drawable.placeholder),
        error = painterResource(R.drawable.placeholder),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = imageModifier,
    )
}

/**
 * The artwork URL to request for [size] pixels, rebuilt from the provider when the item carries
 * an image id and falling back to whatever URI the item was built with.
 *
 * `toMediaItem()` bakes a 300px URL into `artworkUri`, which is fine for lists of lists but soft
 * on a focused 220dp tile at 2x, so tiles ask the provider for the resolution they need.
 */
fun MediaItem.artworkUrlAt(size: Int, type: LibraryType = LibraryType.ALBUM): String? {
    val imageId = mediaMetadata.extras?.getString("imageId")
    val fromProvider = imageId?.let { mediaMetadata.getProvider()?.getImageUrl(it, type, size) }
    return fromProvider ?: mediaMetadata.artworkUri?.toString()
}

/** Cache key for artwork requests: the catalogue id, not the URL, so size changes still hit. */
val MediaItem.artworkKey: String
    get() = mediaMetadata.id ?: mediaId

@Composable
fun TvArtworkPlaceholder(modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Image(
        painter = painterResource(R.drawable.placeholder),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}
