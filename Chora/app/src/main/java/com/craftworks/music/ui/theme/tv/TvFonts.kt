package com.craftworks.music.ui.theme.tv

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.craftworks.music.R

/**
 * The TV typeface family (spec §4.3).
 *
 * Display-only family: sizes below 20sp use the same family, relying on the negative
 * tracking in `TvTypography` instead of a Text optical size.
 *
 * The four resource names are the only font names the TV code knows about. Whichever
 * `fonts` flavor is built resolves them from its own source set — the flavor split is the
 * swap mechanism, with no runtime lookup, no resource overlay and no BuildConfig branch:
 *
 *   - `sfpro` → `app/src/sfpro/res/font/chora_display_*.otf` (licensed SF Pro Display)
 *   - `foss`  → `app/src/foss/res/font/chora_display_*.ttf`  (Inter Display, OFL)
 *
 * Nothing outside this file should reference `R.font.chora_display_*`, so a face change
 * never touches layout or screen code.
 */
object TvFonts {
    val Display: FontFamily = FontFamily(
        Font(R.font.chora_display_regular, FontWeight.Normal),
        Font(R.font.chora_display_medium, FontWeight.Medium),
        Font(R.font.chora_display_semibold, FontWeight.SemiBold),
        Font(R.font.chora_display_bold, FontWeight.Bold),
    )
}
