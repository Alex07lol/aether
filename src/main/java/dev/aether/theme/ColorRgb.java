package dev.aether.theme;

public final class ColorRgb {
    private final int red;
    private final int green;
    private final int blue;

    private ColorRgb(int red, int green, int blue) {
        this.red = channel(red);
        this.green = channel(green);
        this.blue = channel(blue);
    }

    public static ColorRgb of(int red, int green, int blue) {
        return new ColorRgb(red, green, blue);
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

    public String hex() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private static int channel(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Color channel must be between 0 and 255.");
        }
        return value;
    }
}

