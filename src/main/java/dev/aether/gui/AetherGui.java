package dev.aether.gui;

import dev.aether.AetherClient;
import dev.aether.ui.GuiSection;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.screens.AetherClientSettingsScreen;
import dev.aether.gui.screens.AetherCosmeticScreen;
import dev.aether.gui.screens.AetherHudEditorScreen;
import dev.aether.gui.screens.AetherModScreen;
import dev.aether.gui.screens.AetherModuleSettingsScreen;
import dev.aether.gui.screens.AetherThemesScreen;

/**
 * Entry points of the Aether GUI: the factory that opens a section and the one place
 * that knows how section names map to screens. Event bridges and module one-shots open
 * the GUI through this class, never by constructing screens directly, so the
 * navigation graph has exactly one definition.
 */
public final class AetherGui {

    private AetherGui() {
    }

    /** Opens the section as a fresh screen (fresh components, no stale scroll state). */
    public static void open(AetherClient client, GuiSection section) {
        Mc189Compat.displayGuiScreen(create(client, section));
    }

    /**
     * Creates the screen for a section without displaying it, for callers that need to
     * seed state first (for example opening Modules pre-filtered to a category).
     */
    public static AetherModScreen modules(AetherClient client) {
        return new AetherModScreen(client);
    }

    public static AetherCosmeticScreen cosmetics(AetherClient client) {
        return new AetherCosmeticScreen(client);
    }

    public static AetherHudEditorScreen hudEditor(AetherClient client) {
        return new AetherHudEditorScreen(client);
    }

    public static AetherClientSettingsScreen settings(AetherClient client) {
        return new AetherClientSettingsScreen(client);
    }

    /** The theme picker (Aether's own section; Leaf has no theme picker). */
    public static AetherThemesScreen themes(AetherClient client) {
        return new AetherThemesScreen(client);
    }

    /** One module's configuration screen (Leaf's ModDetailSettings role). */
    public static AetherModuleSettingsScreen moduleSettings(AetherClient client, String moduleId) {
        return AetherModuleSettingsScreen.forModule(client, moduleId);
    }

    public static dev.aether.gui.screens.AetherGuiScreen create(AetherClient client, GuiSection section) {
        switch (section) {
            case COSMETICS:
                return cosmetics(client);
            case HUD:
                return hudEditor(client);
            case THEMES:
                return themes(client);
            case SETTINGS:
                return settings(client);
            case MODULES:
            default:
                return modules(client);
        }
    }
}
