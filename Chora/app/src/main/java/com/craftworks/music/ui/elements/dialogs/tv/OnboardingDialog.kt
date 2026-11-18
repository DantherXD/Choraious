package com.craftworks.music.ui.elements.dialogs.tv

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.dialogs.OnboardingStep
import com.craftworks.music.ui.elements.dialogs.tv.provider.TvCreateMediaProviderDialog
import com.craftworks.music.ui.elements.tv.TvProviderCard
import com.craftworks.music.ui.elements.tv.apple.TvButtonStyle
import com.craftworks.music.ui.elements.tv.apple.TvGlassButton
import com.craftworks.music.ui.theme.tv.LocalTvColors
import com.craftworks.music.ui.theme.tv.LocalTvTypography
import com.craftworks.music.ui.theme.tv.TvDimens

/**
 * First-run onboarding, restyled without adding steps (spec §6.9/P4-5): the Chora wordmark, one
 * line of value copy, a red pill per step, and the provider list using the restyled cards.
 */
@Preview(
    showBackground = false,
    showSystemUi = true,
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun OnboardingDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    val colors = LocalTvColors.current
    var step by remember { mutableStateOf(OnboardingStep.OVERVIEW) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler {
            val previous = step.ordinal - 1
            if (previous >= 0) step = OnboardingStep.entries[previous]
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgCanvas)
                .padding(horizontal = TvDimens.screenMarginHorizontal, vertical = TvDimens.screenMarginTop),
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    slideInHorizontally(
                        animationSpec = tween(350, easing = EaseInOutCubic),
                        initialOffsetX = { if (forward) it / 4 else -it / 4 },
                    ) + fadeIn(tween(350)) togetherWith slideOutHorizontally(
                        animationSpec = tween(350, easing = EaseInOutCubic),
                        targetOffsetX = { if (forward) -it / 4 else it / 4 },
                    ) + fadeOut(tween(350))
                },
                label = "onboardingStep",
            ) { currentStep ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TvDimens.space24, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (currentStep) {
                        OnboardingStep.OVERVIEW -> OnboardingOverviewStep {
                            step = OnboardingStep.PROVIDER_SELECTION
                        }

                        OnboardingStep.PROVIDER_SELECTION -> OnboardingSetupProviders(
                            onNext = { step = OnboardingStep.DONE },
                            showAddProviderDialog = { showAddProviderDialog = true },
                        )

                        OnboardingStep.DONE -> OnboardingDoneScreen {
                            setShowDialog(false)
                        }
                    }
                }
            }

            // Step dots, drawn with tokens instead of a tv-material carousel indicator.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = TvDimens.space8),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.space8),
            ) {
                OnboardingStep.entries.forEachIndexed { index, entry ->
                    Box(
                        modifier = Modifier
                            .size(
                                if (entry == step) TvDimens.space8 else TvDimens.space4,
                            )
                            .background(
                                color = if (entry == step) colors.accent else colors.surface3,
                                shape = RoundedCornerShape(percent = 50),
                            ),
                    )
                }
            }
        }
    }

    if (showAddProviderDialog) {
        TvCreateMediaProviderDialog(setShowDialog = { showAddProviderDialog = it })
    }
}

@Composable
private fun OnboardingOverviewStep(onNext: () -> Unit) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Text(
        text = "Chora",
        style = type.displayHero,
        color = colors.labelPrimary,
    )
    Text(
        text = stringResource(R.string.onboarding_no_provider_splash),
        style = type.body,
        color = colors.labelSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = TvDimens.emptyStateMaxWidth),
    )
    TvGlassButton(
        text = stringResource(R.string.action_next),
        style = TvButtonStyle.Primary,
        onClick = onNext,
    )
}

@Composable
private fun OnboardingSetupProviders(
    onNext: () -> Unit,
    showAddProviderDialog: () -> Unit,
) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current
    val providers by MediaProviderManager.allProviders.collectAsStateWithLifecycle()

    Text(
        text = stringResource(R.string.media_providers_media_source),
        style = type.sectionTitle,
        color = colors.labelPrimary,
        textAlign = TextAlign.Center,
    )

    LazyColumn(
        modifier = Modifier.widthIn(max = TvDimens.npInfoWidth),
        contentPadding = PaddingValues(vertical = TvDimens.space8),
        verticalArrangement = Arrangement.spacedBy(TvDimens.space8),
    ) {
        items(providers, key = { it.id }) { provider ->
            TvProviderCard(provider)
        }

        item {
            TvGlassButton(
                text = stringResource(R.string.action_add),
                onClick = showAddProviderDialog,
            )
        }
    }

    TvGlassButton(
        text = stringResource(R.string.action_next),
        style = TvButtonStyle.Primary,
        onClick = onNext,
    )
}

@Composable
private fun OnboardingDoneScreen(onFinished: () -> Unit) {
    val colors = LocalTvColors.current
    val type = LocalTvTypography.current

    Text(
        text = stringResource(R.string.onboarding_all_done),
        style = type.screenTitle,
        color = colors.labelPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = TvDimens.emptyStateMaxWidth),
    )
    TvGlassButton(
        text = stringResource(R.string.onboarding_lets_go),
        style = TvButtonStyle.Primary,
        onClick = onFinished,
    )
}
