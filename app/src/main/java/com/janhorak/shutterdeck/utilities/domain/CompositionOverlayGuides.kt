package com.janhorak.shutterdeck.utilities.domain

private const val GOLDEN_RATIO = 1.618033988749895
private const val GOLDEN_SECTION = (1.0 - (1.0 / GOLDEN_RATIO)).toFloat()

enum class CompositionOverlayMode(
    val label: String,
    val summary: String,
) {
    RULE_OF_THIRDS(
        label = "Rule of thirds",
        summary = "Classic 3x3 guide for balancing subjects and horizons.",
    ),
    GOLDEN_RATIO(
        label = "Golden ratio",
        summary = "Phi-based guide with lines at the golden sections of the frame.",
    ),
    DIAGONALS(
        label = "Diagonals",
        summary = "Corner-to-corner X guide for dynamic framing and leading lines.",
    ),
}

data class CompositionGuideLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

fun compositionOverlayGuides(mode: CompositionOverlayMode): List<CompositionGuideLine> = when (mode) {
    CompositionOverlayMode.RULE_OF_THIRDS -> listOf(
        verticalGuide(1f / 3f),
        verticalGuide(2f / 3f),
        horizontalGuide(1f / 3f),
        horizontalGuide(2f / 3f),
    )
    CompositionOverlayMode.GOLDEN_RATIO -> listOf(
        verticalGuide(GOLDEN_SECTION),
        verticalGuide(1f - GOLDEN_SECTION),
        horizontalGuide(GOLDEN_SECTION),
        horizontalGuide(1f - GOLDEN_SECTION),
    )
    CompositionOverlayMode.DIAGONALS -> listOf(
        CompositionGuideLine(startX = 0f, startY = 0f, endX = 1f, endY = 1f),
        CompositionGuideLine(startX = 1f, startY = 0f, endX = 0f, endY = 1f),
    )
}

private fun verticalGuide(x: Float): CompositionGuideLine {
    return CompositionGuideLine(startX = x, startY = 0f, endX = x, endY = 1f)
}

private fun horizontalGuide(y: Float): CompositionGuideLine {
    return CompositionGuideLine(startX = 0f, startY = y, endX = 1f, endY = y)
}
