package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.ui.ControlCenterState;
import dev.aether.ui.ControlFocus;
import dev.aether.module.ClientModule;
import dev.aether.module.setting.Setting;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import java.util.List;

/**
 * Handles all mouse / keyboard input for the Control Center.
 * It works directly on the screen's {@link ControlCenterState} and the page objects.
 */
public final class ControlCenterInput {

    private final AetherClickGuiScreen screen;

    public ControlCenterInput(AetherClickGuiScreen screen) {
        this.screen = screen;
    }

    /** Called from the screen's click handler. */
    public void click(int mouseX, int mouseY, int button) {
        Layout layout = screen.getLayout();
        if (layout == null) return;

        // 1. hit-test the sidebar
        if (layout.sidebarW > 0 &&
            mouseX >= layout.sidebarX &&
            mouseX <= layout.sidebarX + layout.sidebarW &&
            mouseY >= layout.sidebarY &&
            mouseY <= layout.sidebarY + layout.sidebarH) {
            screen.handleSidebarClick(mouseX, mouseY, button);
            return;
        }

        // 2. hit-test the spine buttons (only if click is in spine area)
        if (layout.spineW > 0 &&
            mouseX >= layout.spineX &&
            mouseX <= layout.spineX + layout.spineW &&
            mouseY >= layout.spineY &&
            mouseY <= layout.spineY + layout.spineH) {
            screen.handleSpineClick(mouseX, mouseY, button);
            return;
        }

        // 3. hit-test the current page content
        screen.getCurrentPage().handleClick(screen, layout, mouseX, mouseY, button);
    }

    /** Called from the screen's key handler. */
    public void handleKey(char typedChar, int keyCode) {
        // Keyboard navigation for the control center
        switch (keyCode) {
            case 15: // Tab: cycle sections
                screen.getNav().cycleSection(keyCode);
                break;
            case 28: // Enter: end search or toggle module
                if (screen.getNav().isSearching()) {
                    screen.getNav().endSearch();
                } else {
                    // Toggle the currently selected module's enabled state
                    java.util.List<dev.aether.module.ClientModule> modules = screen.getSearch().results();
                    if (!modules.isEmpty() && screen.getNav().selected() < modules.size()) {
                        screen.toggleModule(modules.get(screen.getNav().selected()));
                    }
                }
                break;
            case 57: // Space: expand/collapse the selected module
                java.util.List<dev.aether.module.ClientModule> modules = screen.getSearch().results();
                if (!modules.isEmpty() && screen.getNav().selected() < modules.size()) {
                    ClientModule module = modules.get(screen.getNav().selected());
                    screen.getNav().toggleExpanded(module.metadata().id());
                }
                break;
            case 1: // Escape: close search/settings
                screen.getNav().clearFocus();
                if (screen.getNav().isSearching()) {
                    screen.getNav().endSearch();
                }
                break;
            default:
                // Handle keybind capture
                if (screen.getNav().focus().is(ControlFocus.Kind.KEYBIND)) {
                    dev.aether.module.setting.Setting<?> setting = screen.getNav().focus().setting();
                    if (setting != null) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        dev.aether.module.setting.Setting<Integer> s = (dev.aether.module.setting.Setting) setting;
                        s.setValue(Integer.valueOf(keyCode));
                        screen.writeLastChange("Keybind '" + setting.label() + "' -> " + keyCode);
                        screen.getNav().clearFocus();
                    }
                } else if (screen.getNav().isSearching()) {
                    // Character input — feed it to the search field
                    String currentQuery = screen.getQuery();
                    screen.setQuery(currentQuery + typedChar);
                    screen.getSearch().query(screen.getQuery());
                    screen.syncVisible();
                }
                break;
        }
    }

    /** Called from the screen's mouse wheel handling. */
    public void scroll(float amount) {
        screen.setScroll(screen.getScroll() + amount);
        screen.clampScroll();
    }
}