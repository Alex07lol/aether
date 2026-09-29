package dev.aether.gui.components;

import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;

/**
 * The pill toggle of the Aether GUI, standing in for Leaf Client's
 * {@code ToggleButton} concept with Aether's own procedural rendering
 * (see docs/GUI_REBUILD.md). The pill is only the switch; the row around it is the
 * caller's layout.
 */
public final class Toggle extends UiComponent {

    /** Reads the current state; the GUI must not own the truth. */
    public interface StateProvider {
        boolean isOn();
    }

    private final StateProvider state;
    private final Runnable onChange;
    private boolean hover;

    public Toggle(StateProvider state, Runnable onChange) {
        this.state = state;
        this.onChange = onChange;
        this.width = 46;
        this.height = 24;
    }

    public boolean isOn() {
        return state.isOn();
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        boolean on = state.isOn();
        int radius = h / 2;
        int track = on ? AetherUi.ACCENT_ON : hover ? AetherUi.withAlpha(AetherUi.TOGGLE_BG, 0xFF) : AetherUi.withAlpha(AetherUi.TOGGLE_BG, 0xCC);
        AetherUi.drawRoundRect(left, top, left + w, top + h, radius, track);
        AetherUi.outline(left, top, left + w, top + h, on
            ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x66)
            : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x44));
        int knob = h - GuiScale.h(6);
        int knobX = on ? left + w - knob - GuiScale.h(3) : left + GuiScale.h(3);
        int knobY = top + (h - knob) / 2;
        AetherUi.drawRoundRect(knobX, knobY, knobX + knob, knobY + knob, knob / 2,
            on ? 0xFF0B1210 : 0xFFE8E9F0);
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        if (onChange != null) {
            onChange.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
