package dev.aether.menu.impl;

import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.menu.comp.UiToggle;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;

/**
 * The Settings category: Aether's own client-wide options as the grammar's 40px rows -
 * icon, title, description, control on the right. Every row is backed by real state:
 * the two persisted preferences, the config-folder shortcut, and the save/screenshot
 * actions. Nothing here is decorative.
 */
public final class SettingsCategory extends MenuCategory {

    private static final float ROW_STEP = 50.0F;
    private static final float ROW_TOP = 36.0F;
    private static final float ROW_H = 40.0F;

    private final UiToggle toggleControl = new UiToggle();
    private String status;
    private long statusAtMillis;

    public SettingsCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
    }

    @Override
    public String title() {
        return "Settings";
    }

    private int rowCount() {
        return 4;
    }

    @Override
    public void draw(double mx, double my) {
        setContentHeight(ROW_TOP + rowCount() * ROW_STEP + 10.0F);
        float x = innerX() + 15.0F;
        float y = contentY() + ROW_TOP - scroll;

        clipContent();
        try {
            drawRow(x, y, UiIcon.REFRESH, "Save on close", "Write the configuration when the menu closes.",
                new RowToggle() {
                    public boolean isOn() {
                        return client.preferences().saveOnClose();
                    }

                    public void flip() {
                        client.preferences().setSaveOnClose(!client.preferences().saveOnClose());
                        saveQuietly();
                    }
                }, mx, my);
            y += ROW_STEP;
            drawRow(x, y, UiIcon.INFO, "Show tooltips", "Explain controls on hover where available.",
                new RowToggle() {
                    public boolean isOn() {
                        return client.preferences().showTooltips();
                    }

                    public void flip() {
                        client.preferences().setShowTooltips(!client.preferences().showTooltips());
                        saveQuietly();
                    }
                }, mx, my);
            y += ROW_STEP;

            // Action rows (no toggle).
            drawActionRow(x, y, UiIcon.REFRESH, "Save configuration", "Write every module, theme and profile now.", mx, my);
            y += ROW_STEP;
            drawActionRow(x, y, UiIcon.OPEN_FOLDER, "Open config folder", "The folder holding client.json.", mx, my);
        } finally {
            UiCanvas.clearScissor();
        }

        int sheet = UiTheme.sheet();
        for (int i = 0; i < 6; i++) {
            int alpha = Math.round(255 * (1.0F - (i + 0.5F) / 6.0F));
            int band = UiTheme.withAlpha(sheet, alpha);
            UiCanvas.roundRect(contentX(), contentY() + i * 2.0F, contentW(), 2.0F, 0.0F, band);
            UiCanvas.roundRect(contentX(), contentY() + contentH() - (i + 1) * 2.0F, contentW(), 2.0F, 0.0F, band);
        }

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(9.0F, status, innerX(), contentY() + contentH() - 16.0F, UiTheme.textSoft());
        }
    }

    private interface RowToggle {
        boolean isOn();

        void flip();
    }

    private void drawRow(float x, float y, char icon, String title, String description,
                         RowToggle toggle, double mx, double my) {
        boolean hot = mx >= x && mx < x + innerW() - 30.0F && my >= y && my < y + ROW_H;
        UiCanvas.roundRect(x, y, innerW(), ROW_H, 8.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 235));
        AetherFont.drawIcon(icon, 14.0F, x + 11.0F, y + 13.0F, UiTheme.textSoft());
        AetherFont.draw(12.5F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, title,
            x + 32.0F, y + 9.0F, UiTheme.text());
        AetherFont.draw(7.5F, description, x + 32.0F, y + 23.0F, UiTheme.textFaint());
        boolean on = toggle.isOn();
        toggleControl.update(on);
        toggleControl.draw(x + innerW() - 30.0F - 34.0F, y + 12.0F, 0.85F, on, hot);
    }

    private void drawActionRow(float x, float y, char icon, String title, String description, double mx, double my) {
        boolean hot = mx >= x && mx < x + innerW() - 30.0F && my >= y && my < y + ROW_H;
        UiCanvas.roundRect(x, y, innerW(), ROW_H, 8.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 235));
        AetherFont.drawIcon(icon, 14.0F, x + 11.0F, y + 13.0F, UiTheme.textSoft());
        AetherFont.draw(12.5F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, title,
            x + 32.0F, y + 9.0F, UiTheme.text());
        AetherFont.draw(7.5F, description, x + 32.0F, y + 23.0F, UiTheme.textFaint());
        AetherFont.drawIcon(UiIcon.CHEVRON_RIGHT, 10.0F, x + innerW() - 30.0F - 10.0F,
            y + 15.0F, hot ? UiTheme.text() : UiTheme.textFaint());
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return true;
        }
        float x = innerX() + 15.0F;
        float y = contentY() + ROW_TOP - scroll;
        int toggleZoneStart = (int) (x + innerW() - 30.0F - 40.0F);

        // Save on close
        if (my >= y && my < y + ROW_H) {
            if (mx >= toggleZoneStart) {
                client.preferences().setSaveOnClose(!client.preferences().saveOnClose());
                saveQuietly();
                return true;
            }
        }
        y += ROW_STEP;
        // Show tooltips
        if (my >= y && my < y + ROW_H) {
            if (mx >= toggleZoneStart) {
                client.preferences().setShowTooltips(!client.preferences().showTooltips());
                saveQuietly();
                return true;
            }
        }
        y += ROW_STEP;
        // Save configuration
        if (my >= y && my < y + ROW_H) {
            saveQuietly();
            flash("Configuration saved.");
            return true;
        }
        y += ROW_STEP;
        // Open config folder
        if (my >= y && my < y + ROW_H) {
            try {
                java.awt.Desktop.getDesktop().open(client.configFile().getParent().toFile());
            } catch (Exception failed) {
                flash("Could not open the folder.");
            }
            return true;
        }
        return true;
    }

    private void flash(String message) {
        status = message;
        statusAtMillis = System.currentTimeMillis();
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }
}
