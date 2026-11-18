package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.tv.tvArtworkParallax
import com.craftworks.music.ui.tv.tvFocusTarget
import com.craftworks.music.ui.tv.tvLongPress

/** How long OK must be held for a tile long-press (D-pad has no touch long press). */
private const val LongPressMillis = 550L

/**
 * A rail/grid tile (spec §6/§7): square artwork with the title and artist underneath.
 *
 * Only the artwork carries the focus treatment — scale, lift, halo, artwork parallax — so the
 * labels stay put and the row does not jitter while focus moves along it. Long titles ellipsize
 * instead of wrapping, which would change the tile's height and reflow the row.
 *
 * Long-press opens the context menu: on a D-pad a held OK arrives as repeated key-downs, so the
 * press is timed here and the closing key-up is consumed to keep the click from also firing.
 */
@Composable
fun TvMediaTile(
    artworkUrl: Any?,
    cacheKey: String?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    size: Dp = TvDimens.tileSize,
    /**
     * Grid cells: take the cell's width and stay square, instead of a fixed [size].
     *
     * A fixed tile in a five-column grid overflows its cell (the cell is ~154dp on a 960dp canvas),
     * so every grid call site sets this; rails in a `Row` keep the fixed size.
     */
    fillWidth: Boolean = false,
    shape: Shape = LocalTvShapes.current.tile,
    contentDescription: String? = title,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = if (fillWidth) modifier.fillMaxWidth() else modifier.width(size)) {
        Box(
            modifier = (if (fillWidth) Modifier.fillMaxWidth().aspectRatio(1f) else Modifier.size(size))
                .tvFocusTarget(interactionSource = interactionSource, shape = shape)
                .clip(shape)
                .background(colors.surface1)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = title,
                    onClick = onClick,
                )
                .tvLongPress(onLongClick)
                .semantics { contentDescription?.let { this.contentDescription = it } },
        ) {
            TvArtwork(
                data = artworkUrl,
                cacheKey = cacheKey,
                modifier = Modifier
                    .fillMaxSize()
                    // Parallax sits inside the clip, so the drifting bitmap never spills out.
                    .tvArtworkParallax(interactionSource),
                contentScale = ContentScale.Crop,
            )

            // Bottom scrim keeps a title-bearing overlay legible over bright artwork.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))
                        )
                    )
            )
        }

        Text(
            text = title,
            style = type.cardTitle,
            color = if (isFocused) colors.labelPrimary else colors.labelPrimary.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TvDimens.space12, start = TvDimens.space4, end = TvDimens.space4),
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = type.cardSubtitle,
                color = colors.labelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TvDimens.space4, start = TvDimens.space4, end = TvDimens.space4),
            )
        }
    }
}

/** Circular artist tile (spec §4.4: 200dp circle). */
@Composable
fun TvArtistTile(
    artworkUrl: Any?,
    cacheKey: String?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    size: Dp = TvDimens.artistTileSize,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TvMediaTile(
        artworkUrl = artworkUrl,
        cacheKey = cacheKey,
        title = title,
        subtitle = subtitle,
        size = size,
        fillWidth = fillWidth,
        shape = CircleShape,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
