package com.craftworks.music.ui.theme.tv

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.managers.settings.AppearanceSettingsManager

/**
 * Reduced motion (spec §4.7): system animator scale 0 **or** the in-app toggle.
 *
 * The system scale is re-read on every resume because it can be changed from developer
 * options while the app is in the background; the in-app flag is a DataStore flow.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** Opacity crossfades at or below this stay enabled under reduced motion. */
const val ReducedMotionMaxFadeMillis = 120

@androidx.compose.runtime.Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var systemScaleIsZero by remember(context) { mutableStateOf(readAnimatorScaleIsZero(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemScaleIsZero = readAnimatorScaleIsZero(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val inAppFlag by remember(context) {
        AppearanceSettingsManager(context).reduceMotionFlow
    }.collectAsStateWithLifecycle(initialValue = false)

    return systemScaleIsZero || inAppFlag
}

private fun readAnimatorScaleIsZero(context: Context): Boolean = try {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f
} catch (e: Exception) {
    false
}
