package com.janhorak.shutterdeck.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes used by the app. */
object Routes {
    const val TOOLS = "tools"
    const val PLANNER = "planner"
    const val GEAR = "gear"
    const val FILM = "film"
    const val MORE = "more"

    const val LIGHT_METER = "lightmeter"
    const val EV_LUX = "evlux"
    const val COLOR_TEMPERATURE = "colortemperature"
    const val UNIT_CONVERTER = "unitconverter"
    const val DEPTH_OF_FIELD = "depthoffield"
    const val ND_FILTER = "ndfilter"
    const val FIELD_OF_VIEW = "fieldofview"
    const val ASTRO_SHUTTER = "astroshutter"
    const val PRINT_SIZE = "printsize"
    const val FOCUS_STACK = "focusstack"
    const val SUNNY_16 = "sunny16"
    const val GUIDE_NUMBER = "guidenumber"
    const val EQUIVALENT_EXPOSURE = "equivalentexposure"
    const val INTERVALOMETER = "intervalometer"
    const val DIGITAL_SLATE = "digitalslate"
    const val SHOT_NOTES = "shotnotes"
    const val LIGHTING_SETUP = "lightingsetup"
    const val SPIRIT_LEVEL = "spiritlevel"
    const val GRAY_CARD = "graycard"
    const val COMPOSITION_OVERLAYS = "compositionoverlays"
    const val LIVE_HISTOGRAM = "livehistogram"
    const val MACRO = "macro"
    const val DIFFRACTION = "diffraction"
    const val SUN_TIMES = "suntimes"
    const val SUN_MOON_POSITION = "sunmoonposition"
    const val LOCATIONS = "locations"
    const val SHOOTS = "shoots"
    const val FILM_STOCKS = "filmstocks"
    const val FILM_ROLLS = "filmrolls"
    const val FILM_DEVELOPMENT = "filmdevelopment"
    const val FILM_PUSH_PULL = "filmpushpull"
    const val FILM_RECIPROCITY = "filmreciprocity"
    const val FILM_ROLL_ID_ARG = "filmRollId"
    const val FILM_ROLL_DETAIL = "filmroll/{$FILM_ROLL_ID_ARG}"

    fun filmRollDetailRoute(rollId: Long): String = "filmroll/$rollId"

    const val SHOOT_ID_ARG = "shootId"
    const val SHOOT_DETAIL = "shoot/{$SHOOT_ID_ARG}"

    fun shootDetail(shootId: Long): String = "shoot/$shootId"
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
    FILM(Routes.FILM, "Film", Icons.Filled.Edit),
    MORE(Routes.MORE, "More", Icons.Filled.Settings),
}

/** Human-readable title for the app bar, given the current route. */
fun titleForRoute(route: String?): String = when {
    route == null -> "ShutterDeck"
    route.startsWith("shoot/") -> "Shoot"
    else -> when (route) {
        Routes.PLANNER -> "Planner"
        Routes.GEAR -> "Gear"
        Routes.FILM -> "Film"
        Routes.MORE -> "Settings & Help"
        Routes.LIGHT_METER -> "Light Meter"
        Routes.EV_LUX -> "EV / Lux"
        Routes.COLOR_TEMPERATURE -> "Color Temperature"
        Routes.UNIT_CONVERTER -> "Unit Converter"
        Routes.DEPTH_OF_FIELD -> "Depth of Field"
        Routes.ND_FILTER -> "ND Filter"
        Routes.FIELD_OF_VIEW -> "Field of View"
        Routes.ASTRO_SHUTTER -> "Astro Shutter"
        Routes.PRINT_SIZE -> "Print Size"
        Routes.FOCUS_STACK -> "Focus Stacking"
        Routes.SUNNY_16 -> "Sunny 16"
        Routes.GUIDE_NUMBER -> "Guide Number"
        Routes.EQUIVALENT_EXPOSURE -> "Equivalent Exposure"
        Routes.INTERVALOMETER -> "Intervalometer"
        Routes.DIGITAL_SLATE -> "Digital Slate"
        Routes.SHOT_NOTES -> "Shot Notes"
        Routes.LIGHTING_SETUP -> "Lighting Setup"
        Routes.SPIRIT_LEVEL -> "Spirit Level"
        Routes.GRAY_CARD -> "Gray Card"
        Routes.COMPOSITION_OVERLAYS -> "Composition Overlays"
        Routes.LIVE_HISTOGRAM -> "Live Histogram & Zebra"
        Routes.MACRO -> "Macro / Extension"
        Routes.DIFFRACTION -> "Diffraction Limit"
        Routes.SUN_TIMES -> "Golden Hour"
        Routes.SUN_MOON_POSITION -> "Sun & Moon Position"
        Routes.LOCATIONS -> "Scouting Locations"
        Routes.SHOOTS -> "Shoots"
        Routes.FILM_STOCKS -> "Film Stocks"
        Routes.FILM_ROLLS -> "Roll Logger"
        Routes.FILM_DEVELOPMENT -> "Development Timer"
        Routes.FILM_PUSH_PULL -> "Push / Pull Helper"
        Routes.FILM_RECIPROCITY -> "Reciprocity Assistant"
        Routes.FILM_ROLL_DETAIL -> "Roll Log"
        else -> "ShutterDeck"
    }
}
