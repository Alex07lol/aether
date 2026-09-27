package dev.aether.ui;

import dev.aether.module.setting.Setting;

/**
 * Which control currently owns the keyboard or the mouse drag, plus the transient payload that
 * control needs to finish.
 * <p>
 * The Control Center used to keep four independent nullable references - a slider drag, a colour
 * palette popover, a keybind capture and a text editor - which allowed combinations that were never
 * intentional (a capture running with a text editor still open, a palette floating over a slider
 * drag) and meant every input path had to check all four. One focus object with a kind makes those
 * combinations unrepresentable: an input is either idle, dragging, picking a colour, capturing a key
 * or editing text.
 * <p>
 * The payload fields are also per-kind on purpose. A slider remembers its track, a palette its
 * anchor point, a text editor its buffer - and nothing remembers anything it does not own.
 */
public final class ControlFocus {
    /** What the Control Center is currently doing with the mouse and keyboard. */
    public enum Kind {
        /** No control has focus; input goes to the list. */
        NONE,
        /** A number slider is being dragged. */
        SLIDER,
        /** A colour palette popover is open. */
        PALETTE,
        /** A keybind pill is waiting for a key press. */
        KEYBIND,
        /** A text field is being edited. */
        TEXT
    }

    private static final ControlFocus IDLE = new ControlFocus();

    private Kind kind = Kind.NONE;
    private Setting<?> setting;
    private String text = "";
    private int trackX;
    private int trackW;
    private int anchorX;
    private int anchorY;

    private ControlFocus() {
    }

    /** @return the shared idle instance; it is immutable because nothing writes to it again. */
    public static ControlFocus idle() {
        return IDLE;
    }

    public static ControlFocus slider(Setting<?> setting, int trackX, int trackW) {
        ControlFocus focus = new ControlFocus();
        focus.kind = Kind.SLIDER;
        focus.setting = setting;
        focus.trackX = trackX;
        focus.trackW = trackW;
        return focus;
    }

    public static ControlFocus palette(Setting<?> setting, int anchorX, int anchorY) {
        ControlFocus focus = new ControlFocus();
        focus.kind = Kind.PALETTE;
        focus.setting = setting;
        focus.anchorX = anchorX;
        focus.anchorY = anchorY;
        return focus;
    }

    public static ControlFocus keybind(Setting<?> setting) {
        ControlFocus focus = new ControlFocus();
        focus.kind = Kind.KEYBIND;
        focus.setting = setting;
        return focus;
    }

    public static ControlFocus text(Setting<?> setting, String initial) {
        ControlFocus focus = new ControlFocus();
        focus.kind = Kind.TEXT;
        focus.setting = setting;
        focus.text = initial == null ? "" : initial;
        return focus;
    }

    public Kind kind() {
        return this.kind;
    }

    public boolean isIdle() {
        return this.kind == Kind.NONE;
    }

    public boolean is(Kind other) {
        return this.kind == other;
    }

    /** The setting being dragged, edited, captured or recoloured; {@code null} while idle. */
    public Setting<?> setting() {
        return this.setting;
    }

    /** @return true when {@code candidate} is the control this focus belongs to. */
    public boolean targets(Setting<?> candidate) {
        return this.setting != null && this.setting == candidate;
    }

    public int trackX() {
        return this.trackX;
    }

    public int trackW() {
        return this.trackW;
    }

    public int anchorX() {
        return this.anchorX;
    }

    public int anchorY() {
        return this.anchorY;
    }

    /** The text editor buffer; empty for every other kind. */
    public String text() {
        return this.text;
    }

    public void append(char typed) {
        this.text = this.text + typed;
    }

    public void backspace() {
        if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
        }
    }
}
