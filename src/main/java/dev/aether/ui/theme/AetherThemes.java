package dev.aether.ui.theme;

public final class AetherThemes {
    private AetherThemes() {
    }

    public static AetherUiTheme defaultDark() {
        return new AetherUiTheme(
            "Aether Dark",
            UiColor.rgb(11, 14, 20),
            UiColor.argb(238, 16, 20, 27),
            UiColor.rgb(25, 30, 41),
            UiColor.rgb(35, 40, 52),
            UiColor.rgb(56, 125, 255),
            UiColor.argb(85, 56, 125, 255),
            UiColor.argb(34, 255, 255, 255),
            UiColor.argb(80, 0, 0, 0),
            UiColor.rgb(234, 239, 251),
            UiColor.rgb(138, 153, 181),
            UiColor.rgb(86, 99, 124),
            UiColor.rgb(56, 125, 255),
            UiColor.rgb(52, 58, 73),
            UiColor.argb(70, 56, 125, 255),
            new UiSpacing(2, 4, 8, 12, 16, 24, 32),
            new UiRadii(0, 4, 6, 8, 12, 999),
            new UiDurations(0, 90, 160, 260, 120, 140, 80, 180)
        );
    }
}
