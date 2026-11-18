package com.craftworks.music.ui.elements.tv.apple

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvShapes
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens
import com.craftworks.music.ui.theme.tv.TvGlassPresets
import com.craftworks.music.ui.tv.tvFocusTarget

/**
 * The shared shell for every TV dialog (spec §6.9): 45% scrim, `overlay` glass, 24dp radius,
 * 720dp max width (halved to [TvDimens.dialogMaxWidth]), focus trapped inside, Back = Cancel.
 *
 * It is an overlay inside the dialog window rather than a Backdrop surface: a dialog window has
 * its own composition and cannot sample the app's backdrop layer, so the `overlay` preset's tint
 * plus hairline is used directly — the same result `GlassTier.Solid` produces.
 *
 * The panel swallows taps without becoming a focus target, so tapping the panel body does not
 * dismiss the dialog and D-pad focus still enters the first action.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvDialogSurface(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    maxWidth: Dp = TvDimens.dialogMaxWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shapes = LocalTvShapes.current

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .focusGroup()
                .focusProperties { exit = { FocusRequester.Cancel } }
                .background(TvGlassPresets.overlay.tint, shapes.panel)
                .border(TvDimens.hairlineWidth, colors.hairline, shapes.panel)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(TvDimens.space24),
            verticalArrangement = Arrangement.spacedBy(TvDimens.space16),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = type.screenTitle,
                    color = colors.labelPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = type.body,
                    color = colors.labelSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content()
        }
    }
}

/** The dialog's action row: right-aligned pills, secondary first. */
@Composable
fun TvDialogActions(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f))
        content()
    }
}

/**
 * One selectable row in a list dialog: label, optional glyph, and a red dot for the current
 * value. Focus uses the shared halo/lift treatment so it matches the settings rows behind it.
 */
@Composable
fun TvDialogOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconRes: Int? = null,
    helper: String? = null,
    enabled: Boolean = true,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val shape = RoundedCornerShape(TvDimens.listRowShape)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TvDimens.settingsRowHeight)
            .tvFocusTarget(
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.02f,
                lift = 0.dp,
                halo = enabled,
            )
            .background(colors.surface1, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(horizontal = TvDimens.space16, vertical = TvDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.space12),
    ) {
        if (leadingIconRes != null) {
            Icon(
                imageVector = ImageVector.vectorResource(leadingIconRes),
                contentDescription = null,
                tint = if (enabled) colors.labelSecondary else colors.labelDisabled,
                modifier = Modifier.padding(end = TvDimens.space4),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.body,
                color = if (enabled) colors.labelPrimary else colors.labelDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!helper.isNullOrBlank()) {
                Text(
                    text = helper,
                    style = type.caption,
                    color = colors.labelTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .background(colors.accent, RoundedCornerShape(percent = 50))
                    .padding(TvDimens.space4),
            )
        }
    }
}

/**
 * A text field for dialogs (provider credentials, URLs): the system IME is used, so only the
 * frame and colors are restyled to the TV palette.
 */
@Composable
fun TvDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label, style = type.caption, color = colors.labelSecondary) },
        placeholder = placeholder?.let {
            { Text(text = it, style = type.body, color = colors.labelTertiary) }
        },
        textStyle = type.body,
        singleLine = true,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.hairline,
            errorBorderColor = colors.destructive,
            cursorColor = colors.accent,
            focusedTextColor = colors.labelPrimary,
            unfocusedTextColor = colors.labelPrimary,
            errorTextColor = colors.destructive,
        ),
    )
}
