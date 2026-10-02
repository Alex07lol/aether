package dev.aether.gui;

import dev.aether.AetherClient;
import dev.aether.menu.AetherHudEditorScreen;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.ui.GuiSection;
import dev.aether.forge189.Mc189Compat;

/**
 * Entry points of the Aether GUI: the factory that opens the menu and the one place
 * that knows how sections map to it. Event bridges and module one-shots open the GUI
 * through this class, never by constructing screens directly, so the navigation graph
 * has exactly one definition.
 */
public final class AetherGui {

    private AetherGui() {
    }

    /** Opens the menu on the given section (fresh window state, no stale scroll). */
    public static void open(AetherClient client, GuiSection section) {
        Mc189Compat.displayGuiScreen(create(client, section));
    }

    /** Opens the menu; the section lands preselected. */
    public static AetherMenuScreen menu(AetherClient client, GuiSection section) {
        return new AetherMenuScreen(client, section);
    }

    /** The menu opening on Modules, the most common entry. */
    public static AetherMenuScreen modules(AetherClient client) {
        return menu(client, GuiSection.MODULES);
    }

    /** Opens the menu on a category (the theme selector one-shot lands here). */
    public static AetherMenuScreen modulesForCategory(AetherClient client,
                                                      dev.aether.module.ClientModule.ModuleCategory category) {
        AetherMenuScreen screen = menu(client, GuiSection.MODULES);
        screen.focusCategory(category);
        return screen;
    }

    public static AetherMenuScreen cosmetics(AetherClient client) {
        return menu(client, GuiSection.COSMETICS);
    }

    public static AetherMenuScreen appearance(AetherClient client) {
        return menu(client, GuiSection.APPEARANCE);
    }

    public static AetherMenuScreen profiles(AetherClient client) {
        return menu(client, GuiSection.PROFILES);
    }

    public static AetherMenuScreen settings(AetherClient client) {
        return menu(client, GuiSection.SETTINGS);
    }

    /** The fullscreen HUD editor, opened from the rail's Edit HUD button. */
    public static AetherHudEditorScreen hudEditor(AetherClient client) {
        return new AetherHudEditorScreen(client);
    }

    public static void openHudEditor(AetherClient client) {
        Mc189Compat.displayGuiScreen(hudEditor(client));
    }

    public static AetherMenuScreen create(AetherClient client, GuiSection section) {
        return menu(client, section);
    }
}
