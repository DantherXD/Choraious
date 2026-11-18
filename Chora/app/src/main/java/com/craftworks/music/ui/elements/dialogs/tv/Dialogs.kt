package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.craftworks.music.R
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvDialogActions
import com.craftworks.music.ui.elements.tv.apple.TvDialogOptionRow
import com.craftworks.music.ui.elements.tv.apple.TvDialogSurface
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * The shared single-choice list dialog behind every appearance/playback picker.
 *
 * Restyled in P4 to the spec §6.9 shell: `overlay` glass on a scrim, 24dp radius, trapped focus,
 * Back = Cancel, red dot for the current value. The signature is unchanged so the pickers
 * (OLED, transcoding bitrate/format, theme, alignment, …) needed no edits.
 *
 * `leftAligned` is retained only for source compatibility; dialogs are centred on TV.
 */
@Composable
fun <T> GenericListDialog(
    setShowDialog: (Boolean) -> Unit,
    titleRes: Int,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    helperText: (T) -> String = { "" },
    @Suppress("UNUSED_PARAMETER") leftAligned: Boolean = false,
) {
    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvDialogSurface(
            onDismiss = { setShowDialog(false) },
            modifier = modifier,
            title = stringResource(titleRes),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = TvDimens.dialogListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
                contentPadding = PaddingValues(vertical = TvDimens.space4),
            ) {
                items(options) { option ->
                    TvDialogOptionRow(
                        label = label(option),
                        helper = helperText(option).ifBlank { null },
                        selected = option == selectedOption,
                        onClick = {
                            onOptionSelected(option)
                            setShowDialog(false)
                        },
                    )
                }
            }
        }
    }
}

/** The shared multi-choice dialog (navbar/home ordering): checkbox rows plus Reset/Done. */
@Composable
fun <T> GenericCheckDialog(
    setShowDialog: (Boolean) -> Unit,
    titleRes: Int,
    items: List<T>,
    label: @Composable (T) -> String,
    isEnabled: (T) -> Boolean,
    onCheckedChange: (index: Int, checked: Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TvDialogSurface(
            onDismiss = { setShowDialog(false) },
            title = stringResource(titleRes),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = TvDimens.dialogListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(TvDimens.space4),
                contentPadding = PaddingValues(vertical = TvDimens.space4),
            ) {
                itemsIndexed(items) { index, item ->
                    TvDialogOptionRow(
                        label = label(item),
                        selected = isEnabled(item),
                        onClick = { onCheckedChange(index, !isEnabled(item)) },
                    )
                }
            }

            TvDialogActions {
                TvGlassButton(
                    text = stringResource(R.string.action_reset),
                    onClick = {
                        onReset()
                        setShowDialog(false)
                    },
                )
                TvGlassButton(
                    text = stringResource(R.string.action_done),
                    style = TvButtonStyle.Primary,
                    onClick = { setShowDialog(false) },
                )
            }
        }
    }
}
