package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.ui.ControlCenterSection;
import dev.aether.forge189.ui.AetherIcon;
import dev.aether.forge189.ui.pages.ModulesPage;
import dev.aether.forge189.ui.pages.ProfilesPage;
import dev.aether.forge189.ui.pages.ThemesPage;
import dev.aether.forge189.ui.pages.CosmeticsPage;
import dev.aether.forge189.ui.pages.ScreenshotsPage;
import dev.aether.forge189.ui.pages.SettingsPage;
import java.util.List;

/**
 * Central renderer that draws the whole Control Center. It delegates page-specific drawing to
 * the page objects. The method signature matches the original render() call in
 * AetherClickGuiScreen.
 */
public final class ControlCenterRenderer {
    private final ModulesPage modulesPage = new ModulesPage();
    private final ProfilesPage profilesPage = new ProfilesPage();
    private final ThemesPage themesPage = new ThemesPage();
    private final CosmeticsPage cosmeticsPage = new CosmeticsPage();
    private final ScreenshotsPage screenshotsPage = new ScreenshotsPage();
    private final SettingsPage settingsPage = new SettingsPage();

    public void render(AetherClickGuiScreen screen, Object font, int mouseX, int mouseY, Layout layout) {
        // Background
        AetherUi.drawScreen(layout.deckW, layout.deckH);
        // Deck container
        AetherUi.drawRoundRect(layout.deckX, layout.deckY, layout.deckX + layout.deckW, layout.deckY + layout.deckH, 6, AetherUi.DECK_BG);
        // Header
        drawHeader(screen, font, layout);
        // Sidebar
        drawSidebar(screen, font, layout);
        // Page content
        switch (screen.nav().section()) {
            case MODULES: modulesPage.render(screen, font, mouseX, mouseY, layout); break;
            case PROFILES: profilesPage.render(screen, font, mouseX, mouseY, layout); break;
            case THEMES: themesPage.render(screen, font, mouseX, mouseY, layout); break;
            case COSMETICS: cosmeticsPage.render(screen, font, mouseX, mouseY, layout); break;
            case SCREENSHOTS: screenshotsPage.render(screen, font, mouseX, mouseY, layout); break;
            case SETTINGS: settingsPage.render(screen, font, mouseX, mouseY, layout); break;
        }
        // Spine (telemetry, quick buttons)
        drawSpine(screen, font, layout);
        // Footer
        drawFooter(screen, font, layout);
        // Toasts
        AetherToastRenderer.render(font, screen.getToasts().snapshot(), layout.deckW, layout.deckH);
    }

    private void drawHeader(AetherClickGuiScreen screen, Object font, Layout layout) {
        AetherUi.text(font, "AETHER", layout.deckX + 12, layout.deckY + 12, AetherUi.ACCENT);
    }

    private void drawSidebar(AetherClickGuiScreen screen, Object font, Layout layout) {
        for (ControlCenterSection section : ControlCenterSection.ordered()) {
            AetherIcon.HOME.draw(layout.sidebarX + 10, layout.sidebarY + 10 + section.ordinal() * 32, AetherUi.TEXT_PRIMARY);
        }
    }

    private void drawSpine(AetherClickGuiScreen screen, Object font, Layout layout) {
        // Spine implementation placeholder
    }

    private void drawFooter(AetherClickGuiScreen screen, Object font, Layout layout) {
        // Footer implementation placeholder
    }
}