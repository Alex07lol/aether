package dev.aether.ui.theme;

public final class UiColor {
    private final int alpha;
    private final int red;
    private final int green;
    private final int blue;

    private UiColor(int alpha, int red, int green, int blue) {
        this.alpha = channel(alpha, "alpha");
        this.red = channel(red, "red");
        this.green = channel(green, "green");
        this.blue = channel(blue, "blue");
    }

    public static UiColor rgb(int red, int green, int blue) {
        return new UiColor(255, red, green, blue);
    }

    public static UiColor argb(int alpha, int red, int green, int blue) {
        return new UiColor(alpha, red, green, blue);
    }

    public static UiColor packed(int argb) {
        return new UiColor(
            (argb >>> 24) & 0xFF,
            (argb >>> 16) & 0xFF,
            (argb >>> 8) & 0xFF,
            argb & 0xFF
        );
    }

    public UiColor withAlpha(int alpha) {
        return new UiColor(alpha, red, green, blue);
    }

    public int alpha() {
        return alpha;
    }

    public int red() {
        return red;
    }

    public int green() {
        return green;
    }

    public int blue() {
        return blue;
    }

    public int toArgb() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public String hex() {
        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue);
    }

    private static int channel(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Color " + name + " channel must be between 0 and 255.");
        }
        return value;
    }
}
