package com.craftworks.music.ui.elements.tv.apple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * The shared empty / error block (spec §7: "provider-absent empty state", "error card").
 *
 * `title` carries the reason and `body` the next step, so a provider that cannot filter by genre
 * can say so without a dialog.
 */
@Composable
fun TvEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    iconRes: Int = R.drawable.round_music_note_24,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Column(
        modifier = modifier
            .widthIn(max = TvDimens.emptyStateMaxWidth)
            .padding(TvDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TvDimens.space16),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = colors.labelTertiary,
            modifier = Modifier.size(TvDimens.space48),
        )
        Text(
            text = title,
            style = type.sectionTitle,
            color = colors.labelPrimary,
            textAlign = TextAlign.Center,
        )
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = type.body,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
