package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.ui.ControlCenterState;
import dev.aether.ui.ControlCenterSection;
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
        ControlCenterState nav = screen.getNav();
        ControlCenterSection section = nav.section();

        // Keyboard navigation for the control center
        switch (keyCode) {
            case 15: // Tab: cycle sections
                nav.cycleSection(keyCode);
                break;
            case 28: // Enter: end search or toggle module
                if (nav.isSearching()) {
                    nav.endSearch();
                } else {
                    // Toggle the currently selected module's enabled state
                    List<ClientModule> modules = screen.getSearch().results();
                    if (!modules.isEmpty() && nav.selected() < modules.size()) {
                        screen.toggleModule(modules.get(nav.selected()));
                    }
                }
                break;
            case 57: // Space: expand/collapse the selected module
                List<ClientModule> modules = screen.getSearch().results();
                if (!modules.isEmpty() && nav.selected() < modules.size()) {
                    ClientModule module = modules.get(nav.selected());
                    nav.toggleExpanded(module.metadata().id());
                }
                break;
            case 1: // Escape: close search/settings/palettes/keybinds
                if (!nav.focus().isIdle()) {
                    nav.clearFocus();
                } else if (nav.isSearching()) {
                    nav.endSearch();
                } else if (nav.state() == ControlCenterState.MenuState.MODULE_SETTINGS) {
                    nav.closeModuleSettings();
                }
                break;
            case 200: // Up arrow
            case 208: // Down arrow
                if (nav.showsModuleList()) {
                    int size = screen.getSearch().results().size();
                    nav.move(keyCode == 200 ? -1 : 1, size);
                }
                break;
            case 201: // Page Up
                if (nav.showsModuleList()) {
                    int size = screen.getSearch().results().size();
                    nav.move(-10, size);
                }
                break;
            case 209: // Page Down
                if (nav.showsModuleList()) {
                    int size = screen.getSearch().results().size();
                    nav.move(10, size);
                }
                break;
            case 199: // Home
                if (nav.showsModuleList()) {
                    nav.select(0, screen.getSearch().results().size());
                }
                break;
            case 207: // End
                if (nav.showsModuleList()) {
                    nav.select(screen.getSearch().results().size() - 1, screen.getSearch().results().size());
                }
                break;
            case 31: // S key (cosmetics search) - not used
                break;
            default:
                // Handle keybind capture
                if (nav.focus().is(ControlFocus.Kind.KEYBIND)) {
                    Setting<?> setting = nav.focus().setting();
                    if (setting != null) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Setting<Integer> s = (Setting) setting;
                        s.setValue(Integer.valueOf(keyCode));
                        screen.writeLastChange("Keybind '" + setting.label() + "' -> " + keyCode);
                        nav.clearFocus();
                    }
                } else if (nav.focus().is(ControlFocus.Kind.TEXT)) {
                    // Handle text input for text settings
                    Setting<?> setting = nav.focus().setting();
                    if (setting != null) {
                        StringBuilder buffer = new StringBuilder(nav.focus().text());
                        if (keyCode == 14) { // Backspace
                            if (buffer.length() > 0) buffer.deleteCharAt(buffer.length() - 1);
                        } else if (typedChar != 0 && typedChar >= 32 && typedChar <= 126) {
                            buffer.append(typedChar);
                        }
                        nav.setFocus(ControlFocus.text(setting, buffer.toString()));
                        if (keyCode == 28) { // Enter confirms
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            Setting<String> s = (Setting) setting;
                            s.setValue(buffer.toString());
                            screen.writeLastChange("Setting '" + setting.label() + "' -> " + buffer.toString());
                            nav.clearFocus();
                        }
                    }
                } else if (nav.isSearching()) {
                    // Character input — feed it to the search field
                    if (keyCode == 14) { // Backspace
                        String currentQuery = screen.getQuery();
                        if (!currentQuery.isEmpty()) {
                            screen.setQuery(currentQuery.substring(0, currentQuery.length() - 1));
                            screen.getSearch().query(screen.getQuery());
                            screen.syncVisible();
                        }
                    } else if (typedChar != 0 && typedChar >= 32 && typedChar <= 126) {
                        String currentQuery = screen.getQuery();
                        screen.setQuery(currentQuery + typedChar);
                        screen.getSearch().query(screen.getQuery());
                        screen.syncVisible();
                    }
                } else if (section == ControlCenterSection.COSMETICS) {
                    // Cosmetics page shortcuts
                    handleCosmeticsShortcuts(keyCode);
                }
                break;
        }
    }

    /** Cosmetics page keyboard shortcuts: F=favorite, E=equip, U=unequip */
    private void handleCosmeticsShortcuts(int keyCode) {
        // F = Toggle favorite (keyCode 33)
        // E = Equip (keyCode 18)
        // U = Unequip (keyCode 22)
        // These shortcuts would need the cosmetics page to expose the selected item
        // For now, just pass - the cosmetics page handles its own clicks
    }

    /** Called from the screen's mouse wheel handling. */
    public void scroll(float amount) {
        screen.setScroll(screen.getScroll() + amount);
        screen.clampScroll();
    }
}