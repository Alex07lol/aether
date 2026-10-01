package dev.aether.hud;

/**
 * The keystrokes pad's geometry, as arithmetic instead of a scatter of inline sums.
 * <p>
 * The pad is a W key centred over an A-S-D row, with an optional click row and an optional spacebar
 * under it. Every dimension follows from three settings - the key size, the gap and the spacebar
 * height - and the relationships are the ones the reference layout uses (a key row is three keys and
 * two gaps wide; the movement block is two key rows and the gap between them tall; the spacebar spans
 * the whole pad). Keeping them in one place means the renderer, the HUD editor's measurement and the
 * tests cannot disagree about how big the widget is.
 */
public final class KeystrokesLayout {

    private KeystrokesLayout() {
    }

    /** Width of three movement keys plus the two gaps between them. */
    public static int movementWidth(int boxSize, int gap) {
        return boxSize * 3 + gap * 2;
    }

    /** Height of the W row plus the A-S-D row plus the gap: the movement block on its own. */
    public static int movementHeight(int boxSize, int gap) {
        return boxSize * 2 + gap;
    }

    /** Width of one click key: the pad split in two, minus the gap the halves share. */
    public static int clickWidth(int boxSize, int gap) {
        return (movementWidth(boxSize, gap) - gap) / 2;
    }

    /**
     * The height of the whole widget.
     *
     * @param showMovement whether the movement block is drawn
     * @param showClicks whether the mouse row is drawn
     * @param showSpace whether the spacebar is drawn
     */
    public static int totalHeight(int boxSize, int gap, int clickHeight, int spacebarHeight,
                                  boolean showMovement, boolean showClicks, boolean showSpace) {
        int height = 0;
        if (showMovement) {
            height += movementHeight(boxSize, gap) + gap;
        }
        if (showClicks) {
            height += clickHeight + gap;
        }
        if (showSpace) {
            height += spacebarHeight + gap;
        }
        // The gap after the last row is not part of the widget.
        return Math.max(0, height - gap);
    }

    /** The widest row in the widget, which is what the HUD editor measures the element against. */
    public static int totalWidth(int boxSize, int gap) {
        return movementWidth(boxSize, gap);
    }
}
