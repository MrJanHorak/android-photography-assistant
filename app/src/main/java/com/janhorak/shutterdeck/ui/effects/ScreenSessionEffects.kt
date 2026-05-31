package com.janhorak.shutterdeck.ui.effects

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun KeepScreenOn() {
    val view = LocalView.current

    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }
}

@Composable
fun LockPortraitOrientation() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onDispose {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
}

@Composable
fun LockLandscapeOrientation() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
