package com.janhorak.shutterdeck.utilities.domain

enum class GrayCardReference(
    val label: String,
    val summary: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val useDarkSystemBarIcons: Boolean,
) {
    // 18% linear reflectance gamma-encodes to roughly 0.461 in sRGB -> 118/255 per channel.
    GRAY_18(
        label = "18% Gray",
        summary = "Neutral midtone reference for metering and white-balance checks.",
        red = 0x76,
        green = 0x76,
        blue = 0x76,
        useDarkSystemBarIcons = false,
    ),
    WHITE(
        label = "White",
        summary = "Bright white reference for white-balance and highlight checks.",
        red = 0xFF,
        green = 0xFF,
        blue = 0xFF,
        useDarkSystemBarIcons = true,
    ),
    BLACK(
        label = "Black",
        summary = "Deep black reference for flare and shadow checks.",
        red = 0x00,
        green = 0x00,
        blue = 0x00,
        useDarkSystemBarIcons = false,
    ),
}
