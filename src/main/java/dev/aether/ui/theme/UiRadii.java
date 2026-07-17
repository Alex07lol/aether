package dev.aether.ui.theme;

public final class UiRadii {
    private final int none;
    private final int sm;
    private final int md;
    private final int lg;
    private final int xl;
    private final int pill;

    public UiRadii(int none, int sm, int md, int lg, int xl, int pill) {
        this.none = nonNegative(none, "none");
        this.sm = nonNegative(sm, "sm");
        this.md = nonNegative(md, "md");
        this.lg = nonNegative(lg, "lg");
        this.xl = nonNegative(xl, "xl");
        this.pill = nonNegative(pill, "pill");
    }

    public int none() {
        return none;
    }

    public int sm() {
        return sm;
    }

    public int md() {
        return md;
    }

    public int lg() {
        return lg;
    }

    public int xl() {
        return xl;
    }

    public int pill() {
        return pill;
    }

    private static int nonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("Radius " + name + " cannot be negative.");
        }
        return value;
    }
}
