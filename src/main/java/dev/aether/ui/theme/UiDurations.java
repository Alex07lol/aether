package dev.aether.ui.theme;

public final class UiDurations {
    private final int instantMillis;
    private final int fastMillis;
    private final int normalMillis;
    private final int slowMillis;
    private final int hoverMillis;
    private final int focusMillis;
    private final int pressedMillis;
    private final int selectionMillis;

    public UiDurations(
        int instantMillis,
        int fastMillis,
        int normalMillis,
        int slowMillis,
        int hoverMillis,
        int focusMillis,
        int pressedMillis,
        int selectionMillis
    ) {
        this.instantMillis = nonNegative(instantMillis, "instantMillis");
        this.fastMillis = nonNegative(fastMillis, "fastMillis");
        this.normalMillis = nonNegative(normalMillis, "normalMillis");
        this.slowMillis = nonNegative(slowMillis, "slowMillis");
        this.hoverMillis = nonNegative(hoverMillis, "hoverMillis");
        this.focusMillis = nonNegative(focusMillis, "focusMillis");
        this.pressedMillis = nonNegative(pressedMillis, "pressedMillis");
        this.selectionMillis = nonNegative(selectionMillis, "selectionMillis");
    }

    public int instantMillis() {
        return instantMillis;
    }

    public int fastMillis() {
        return fastMillis;
    }

    public int normalMillis() {
        return normalMillis;
    }

    public int slowMillis() {
        return slowMillis;
    }

    public int hoverMillis() {
        return hoverMillis;
    }

    public int focusMillis() {
        return focusMillis;
    }

    public int pressedMillis() {
        return pressedMillis;
    }

    public int selectionMillis() {
        return selectionMillis;
    }

    private static int nonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("Duration " + name + " cannot be negative.");
        }
        return value;
    }
}
