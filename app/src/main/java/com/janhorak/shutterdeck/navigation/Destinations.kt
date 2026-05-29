package com.janhorak.shutterdeck.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes used by the app. */
object Routes {
    const val TOOLS = "tools"
    const val PLANNER = "planner"
    const val GEAR = "gear"
    const val MORE = "more"

    const val LIGHT_METER = "lightmeter"
    const val DEPTH_OF_FIELD = "depthoffield"
    const val ND_FILTER = "ndfilter"
    const val FIELD_OF_VIEW = "fieldofview"
    const val ASTRO_SHUTTER = "astroshutter"
    const val PRINT_SIZE = "printsize"
    const val FOCUS_STACK = "focusstack"
    const val SUNNY_16 = "sunny16"
    const val GUIDE_NUMBER = "guidenumber"
    const val EQUIVALENT_EXPOSURE = "equivalentexposure"
    const val MACRO = "macro"
    const val DIFFRACTION = "diffraction"
    const val SUN_TIMES = "suntimes"
}

/** The destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    TOOLS(Routes.TOOLS, "Tools", Icons.Filled.Home),
    PLANNER(Routes.PLANNER, "Planner", Icons.Filled.DateRange),
    GEAR(Routes.GEAR, "Gear", Icons.AutoMirrored.Filled.List),
    MORE(Routes.MORE, "More", Icons.Filled.Settings),
}

/** Human-readable title for the app bar, given the current route. */
fun titleForRoute(route: String?): String = when (route) {
    Routes.PLANNER -> "Planner"
    Routes.GEAR -> "Gear"
    Routes.MORE -> "More"
    Routes.LIGHT_METER -> "Light Meter"
    Routes.DEPTH_OF_FIELD -> "Depth of Field"
    Routes.ND_FILTER -> "ND Filter"
    Routes.FIELD_OF_VIEW -> "Field of View"
    Routes.ASTRO_SHUTTER -> "Astro Shutter"
    Routes.PRINT_SIZE -> "Print Size"
    Routes.FOCUS_STACK -> "Focus Stacking"
    Routes.SUNNY_16 -> "Sunny 16"
    Routes.GUIDE_NUMBER -> "Guide Number"
    Routes.EQUIVALENT_EXPOSURE -> "Equivalent Exposure"
    Routes.MACRO -> "Macro / Extension"
    Routes.DIFFRACTION -> "Diffraction Limit"
    Routes.SUN_TIMES -> "Golden Hour"
    else -> "ShutterDeck"
}
