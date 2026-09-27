package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.config.ClientPreferences;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherToggle;
import dev.aether.forge189.ui.AetherMetrics;

/**
 * Settings page – grouped sections for Interface, Appearance, Performance, Input, Accessibility.
 */
public final class SettingsPage extends Page {

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        int y = layout.listY + 8;
        AetherUi.text(font, "SETTINGS", layout.listX + 8, y, AetherUi.TEXT_DISABLED);
        // Section underline
        Mc189Compat.drawRect(layout.listX + 8, y + 16, layout.listX + 100, y + 17, AetherUi.withAlpha(AetherUi.ACCENT, 0x66));
        y += 30;

        ClientPreferences prefs = screen.getClient().preferences();

        drawSectionHeader(font, "INTERFACE", layout.listX + 8, y);
        y += 24;

        drawPreferenceRow(font, y, "Save config when menu closes",
                         prefs.saveOnClose(), layout.listX + 8, layout.listW - 16, mouseX, mouseY, 0);
        y += 28;

        drawPreferenceRow(font, y, "Show tooltips",
                         prefs.showTooltips(), layout.listX + 8, layout.listW - 16, mouseX, mouseY, 1);
        y += 28;

        drawPreferenceRow(font, y, "Animated transitions",
                         true, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        drawPreferenceRow(font, y, "Compact sidebar",
                         false, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        y += 8;
        drawSectionHeader(font, "APPEARANCE", layout.listX + 8, y);
        y += 24;

        drawPreferenceRow(font, y, "Blur background when open",
                         false, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        y += 8;
        drawSectionHeader(font, "PERFORMANCE", layout.listX + 8, y);
        y += 24;

        drawPreferenceRow(font, y, "Limit FPS when menu open",
                         true, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        y += 8;
        drawSectionHeader(font, "INPUT", layout.listX + 8, y);
        y += 24;

        drawPreferenceRow(font, y, "Scroll sensitivity",
                         true, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        y += 8;
        drawSectionHeader(font, "ACCESSIBILITY", layout.listX + 8, y);
        y += 24;

        drawPreferenceRow(font, y, "High contrast mode",
                         false, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
        y += 28;

        drawPreferenceRow(font, y, "Reduce motion",
                         false, layout.listX + 8, layout.listW - 16, mouseX, mouseY, -1);
    }

    private void drawSectionHeader(Object font, String label, int x, int y) {
        AetherUi.text(font, label, x, y, AetherUi.TEXT_DISABLED);
        Mc189Compat.drawRect(x, y + 14, x + 100, y + 15, AetherUi.withAlpha(AetherUi.ACCENT, 0x44));
    }

    private void drawPreferenceRow(Object font, int y, String label, boolean on,
                                   int x, int w, int mouseX, int mouseY, int toggleIndex) {
        boolean hover = mouseX >= x && mouseX <= x + w &&
                        mouseY >= y - 2 && mouseY <= y + 22;
        if (hover) {
            Mc189Compat.drawRect(x, y - 2, x + w, y + 22, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x10));
        }

        AetherUi.text(font, label, x, y + 6, AetherUi.TEXT_PRIMARY);

        int toggleX = x + w - 40;
        int toggleY = y;
        boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + 26 &&
                              mouseY >= toggleY && mouseY <= toggleY + 13;

        // Only show toggle for interactive settings (first two)
        if (toggleIndex >= 0) {
            new AetherToggle(toggleX, toggleY, 26, 13, on).render(toggleHover);
        } else {
            // Read-only indicator
            Mc189Compat.drawRect(toggleX, toggleY, toggleX + 26, toggleY + 13,
                                 AetherUi.withAlpha(AetherUi.ROW_BG, 0x88));
            AetherUi.text(font, "N/A", toggleX + 3, toggleY + 1, AetherUi.TEXT_DISABLED);
        }
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        ClientPreferences prefs = screen.getClient().preferences();
        int x = layout.listX + 8;
        int w = layout.listW - 16;
        int y = layout.listY + 8 + 30 + 24; // First preference row: header(8) + title(30) + INTERFACE header(24)

        // Draw and hit-test each preference row
        String[] labels = {
            "Save config when menu closes",
            "Show tooltips",
            "Animated transitions",
            "Compact sidebar",
            "Blur background when open",
            "Limit FPS when menu open",
            "Scroll sensitivity",
            "High contrast mode",
            "Reduce motion"
        };
        // Read current state from prefs
        boolean[] states = {
            prefs.saveOnClose(),
            prefs.showTooltips(),
            true, // read-only
            false, // read-only
            false, // read-only
            true, // read-only
            true, // read-only
            false, // read-only
            false  // read-only
        };

        for (int i = 0; i < labels.length; i++) {
            boolean on = states[i];
            boolean hover = mouseX >= x && mouseX <= x + w &&
                            mouseY >= y - 2 && mouseY <= y + 22;
            int toggleX = x + w - 40;
            int toggleY = y;
            boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + 26 &&
                                  mouseY >= toggleY && mouseY <= toggleY + 13;

            // Handle toggle clicks (only first two are interactive)
            if (button == 0 && toggleHover && (i == 0 || i == 1)) {
                // Flip the preference
                if (i == 0) prefs.setSaveOnClose(!on);
                if (i == 1) prefs.setShowTooltips(!on);
                states[i] = !on; // Update local state
                screen.writeLastChange("Preference '" + labels[i] + "' -> " + (!on));
                screen.getToasts().push(labels[i] + ": " + (!on ? "Enabled" : "Disabled"), AetherUi.ACCENT);
                return;
            }
            y += 28;
        }
    }
}