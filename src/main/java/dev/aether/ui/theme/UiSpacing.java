package dev.aether.ui.theme;

public final class UiSpacing {
    private final int xxs;
    private final int xs;
    private final int sm;
    private final int md;
    private final int lg;
    private final int xl;
    private final int xxl;

    public UiSpacing(int xxs, int xs, int sm, int md, int lg, int xl, int xxl) {
        this.xxs = nonNegative(xxs, "xxs");
        this.xs = nonNegative(xs, "xs");
        this.sm = nonNegative(sm, "sm");
        this.md = nonNegative(md, "md");
        this.lg = nonNegative(lg, "lg");
        this.xl = nonNegative(xl, "xl");
        this.xxl = nonNegative(xxl, "xxl");
    }

    public int xxs() {
        return xxs;
    }

    public int xs() {
        return xs;
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

    public int xxl() {
        return xxl;
    }

    private static int nonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("Spacing " + name + " cannot be negative.");
        }
        return value;
    }
}
