package com.janhorak.shutterdeck.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ImmersiveScreenMode(
    useDarkSystemBarIcons: Boolean,
) {
    KeepScreenOn()
    ImmersiveSystemBarsEffect()
    SystemBarAppearanceEffect(useDarkSystemBarIcons = useDarkSystemBarIcons)
}

@Composable
fun ReferenceDisplayMode(
    useDarkSystemBarIcons: Boolean,
) {
    ImmersiveScreenMode(useDarkSystemBarIcons = useDarkSystemBarIcons)
    MaxScreenBrightnessEffect()
}

@Composable
private fun ImmersiveSystemBarsEffect() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, view) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousBrightness = window.attributes.screenBrightness
            val previousBehavior = controller.systemBarsBehavior

            window.attributes = window.attributes.also { attributes ->
                attributes.screenBrightness = 1.0f
            }
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
                window.attributes = window.attributes.also { attributes ->
                    attributes.screenBrightness = previousBrightness
                }
            }
        }
    }
}

@Composable
private fun MaxScreenBrightnessEffect() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val previousBrightness = window.attributes.screenBrightness

            window.attributes = window.attributes.also { attributes ->
                attributes.screenBrightness = 1.0f
            }

            onDispose {
                window.attributes = window.attributes.also { attributes ->
                    attributes.screenBrightness = previousBrightness
                }
            }
        }
    }
}

@Composable
private fun SystemBarAppearanceEffect(
    useDarkSystemBarIcons: Boolean,
) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, view, useDarkSystemBarIcons) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousLightStatusBars = controller.isAppearanceLightStatusBars
            val previousLightNavigationBars = controller.isAppearanceLightNavigationBars

            controller.isAppearanceLightStatusBars = useDarkSystemBarIcons
            controller.isAppearanceLightNavigationBars = useDarkSystemBarIcons

            onDispose {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
                controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }
}
