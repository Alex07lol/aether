package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.font.GlyphPageFontRenderer;
import dev.aether.forge189.Mc189Compat;
import dev.aether.ui.ControlCenterSection;
import dev.aether.forge189.ui.pages.ModulesPage;
import dev.aether.forge189.ui.pages.ProfilesPage;
import dev.aether.forge189.ui.pages.ThemesPage;
import dev.aether.forge189.ui.pages.CosmeticsPage;
import dev.aether.forge189.ui.pages.ScreenshotsPage;
import dev.aether.forge189.ui.pages.SettingsPage;

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
        drawBackground(screen, font, layout);
        drawDeck(screen, font, layout);
        drawHeader(screen, font, layout, mouseX, mouseY);
        drawSidebar(screen, font, layout, mouseX, mouseY);
        drawPage(screen, font, mouseX, mouseY, layout);
        drawToasts(font, screen, layout);
    }

    private void drawBackground(AetherClickGuiScreen screen, Object font, Layout layout) {
        int w = layout.screenW;
        int h = layout.screenH;
        if (w <= 0 || h <= 0) return;

        // Dark gradient backdrop
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            int top = i * h / steps;
            int bottom = (i + 1) * h / steps + 1;
            float t = (float) i / steps;
            int color = AetherUi.lerpColor(0xFF0A0C12, 0xFF05070A, t);
            Mc189Compat.drawRect(0, top, w, bottom, color);
        }

        // Twinkling particles
        long time = System.currentTimeMillis() / 40L;
        for (int i = 0; i < 14; i++) {
            int x = (int) ((i * 137L + time) % Math.max(1L, (long) w + 80L)) - 40;
            int y = (i * 53) % Math.max(1, h);
            int size = 1 + (i % 3);
            Mc189Compat.drawRect(x, y, x + size, y + size, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        }
    }

    private void drawDeck(AetherClickGuiScreen screen, Object font, Layout layout) {
        // Shadow layer
        drawShadow(layout.deckX, layout.deckY,
                layout.deckX + layout.deckW, layout.deckY + layout.deckH,
                12, 6);
        // Deck container
        AetherUi.drawRoundRect(layout.deckX, layout.deckY,
                layout.deckX + layout.deckW, layout.deckY + layout.deckH,
                12, AetherUi.DECK_BG);
        AetherUi.outline(layout.deckX, layout.deckY,
                layout.deckX + layout.deckW, layout.deckY + layout.deckH,
                AetherUi.DECK_EDGE);
    }

    private void drawHeader(AetherClickGuiScreen screen, Object font, Layout layout,
                            int mouseX, int mouseY) {
        int headerY = layout.deckY;
        int headerH = layout.headerH;

        // Header background strip
        int headerBg = AetherUi.withAlpha(AetherUi.PANEL, 0xCC);
        Mc189Compat.drawRect(layout.deckX + 1, headerY,
                layout.deckX + layout.deckW - 1, headerY + headerH - 2, headerBg);

        // Top accent line
        Mc189Compat.drawRect(layout.deckX + 1, headerY,
                layout.deckX + layout.deckW - 1, headerY + 2, AetherUi.withAlpha(AetherUi.ACCENT, 0x44));

        // Branding on left
        AetherUi.textSmooth(font, "AETHER", layout.deckX + 16, headerY + 14, AetherUi.ACCENT);

        // Page title centered
        ControlCenterSection section = screen.nav().section();
        String pageTitle = section != null ? section.label() : "";
        int centerX = layout.deckX + layout.deckW / 2;
        AetherUi.centeredSmooth(font, pageTitle, centerX, headerY + 14, layout.deckW, AetherUi.TEXT_PRIMARY);

        // Search icon on right
        int searchIconX = layout.deckX + layout.deckW - 40;
        int searchIconY = headerY + 14;
        boolean searchHover = mouseX >= searchIconX && mouseX <= searchIconX + 24
                && mouseY >= searchIconY && mouseY <= searchIconY + 24;
        if (searchHover) {
            Mc189Compat.drawRect(searchIconX - 2, searchIconY - 2,
                    searchIconX + 26, searchIconY + 26,
                    AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x0D));
        }
        AetherIcon.SEARCH.draw(searchIconX, searchIconY,
                searchHover ? AetherUi.ACCENT_ON : AetherUi.TEXT_SECONDARY);
    }

    private void drawSidebar(AetherClickGuiScreen screen, Object font, Layout layout,
                             int mouseX, int mouseY) {
        int sidebarW = layout.sidebarW;
        if (sidebarW <= 0) return;

        // Subtle sidebar background
        AetherUi.drawRoundRect(layout.sidebarX, layout.sidebarY,
                layout.sidebarX + sidebarW, layout.sidebarY + layout.sidebarH,
                8, AetherUi.withAlpha(AetherUi.PANEL, 0x88));

        ControlCenterSection activeSection = screen.nav().section();

        for (ControlCenterSection section : ControlCenterSection.ordered()) {
            int itemY = layout.sidebarY + 8 + section.ordinal() * AetherMetrics.SIDEBAR_ITEM_HEIGHT;
            int itemH = AetherMetrics.SIDEBAR_ITEM_HEIGHT;

            boolean isHover = mouseX >= layout.sidebarX + 8 && mouseX <= layout.sidebarX + sidebarW - 8
                    && mouseY >= itemY && mouseY <= itemY + itemH;
            boolean isActive = section == activeSection;

            // Active indicator bar
            if (isActive) {
                Mc189Compat.drawRect(layout.sidebarX + 2, itemY + 2,
                        layout.sidebarX + 5, itemY + itemH - 2,
                        AetherUi.ACCENT);
            }

            // Hover background
            if (isHover) {
                Mc189Compat.drawRect(layout.sidebarX + 4, itemY,
                        layout.sidebarX + sidebarW - 4, itemY + itemH,
                        AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x10));
            }

            // Icon
            AetherIcon icon = sectionToIcon(section);
            int iconColor = isActive ? AetherUi.ACCENT_ON : isHover ? AetherUi.TEXT_PRIMARY
                    : AetherUi.TEXT_SECONDARY;
            icon.draw(layout.sidebarX + 10, itemY + 9, iconColor);

            // Label
            String label = section.label();
            int labelX = layout.sidebarX + sidebarW / 2;
            int labelY = itemY + 8;
            AetherUi.textSmooth(font, label, labelX, labelY,
                    isActive ? AetherUi.ACCENT_ON : isHover ? AetherUi.TEXT_PRIMARY
                            : AetherUi.TEXT_SECONDARY);
        }
    }

    private void drawPage(AetherClickGuiScreen screen, Object font, int mouseX, int mouseY,
                          Layout layout) {
        ControlCenterSection section = screen.nav().section();
        switch (section) {
            case MODULES: modulesPage.render(screen, font, mouseX, mouseY, layout); break;
            case PROFILES: profilesPage.render(screen, font, mouseX, mouseY, layout); break;
            case THEMES: themesPage.render(screen, font, mouseX, mouseY, layout); break;
            case COSMETICS: cosmeticsPage.render(screen, font, mouseX, mouseY, layout); break;
            case SCREENSHOTS: screenshotsPage.render(screen, font, mouseX, mouseY, layout); break;
            case SETTINGS: settingsPage.render(screen, font, mouseX, mouseY, layout); break;
            default: break;
        }
    }

    private void drawToasts(Object font, AetherClickGuiScreen screen, Layout layout) {
        AetherToastRenderer.render(font, screen.getToasts().snapshot(), layout.deckW, layout.deckH);
    }

    // Package-private shadow helper (mirrors AetherUi.drawShadow)
    static void drawShadow(int left, int top, int right, int bottom, int radius, int spread) {
        for (int i = 0; i < spread; i++) {
            int alpha = 40 - (i * (40 / spread));
            int shadowColor = (alpha << 24) | (AetherUi.SHADOW & 0x00FFFFFF);
            AetherUi.drawRoundRect(left - i, top - i, right + i, bottom + i, radius + i, shadowColor);
        }
    }

    private static AetherIcon sectionToIcon(ControlCenterSection section) {
        switch (section) {
            case MODULES:    return AetherIcon.HOME;
            case PROFILES:   return AetherIcon.EDIT;
            case THEMES:     return AetherIcon.RENDER;
            case COSMETICS:  return AetherIcon.COSMETICS;
            case SCREENSHOTS:return AetherIcon.HUD;
            case SETTINGS:   return AetherIcon.GAMEPLAY;
            default:         return AetherIcon.HOME;
        }
    }
}