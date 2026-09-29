package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.ui.pages.ModulesPage;
import dev.aether.module.ClientModule;

/**
 * Computes layout geometry for the Control Center.
 * <p>
 * The deck is a centered card with comfortable margins over a dimmed backdrop (never a
 * near-fullscreen slab), and {@link Layout#maxScroll} is computed from the real content
 * height of the modules page - expanded accordions included - so the list can scroll.
 */
public final class ControlCenterLayout {

    /** Visible height of the module list, matching the page's own drawing constants. */
    static final int LIST_CONTENT_TOP = 90;
    private static final int CARD_GAP = 10;

    public Layout compute(AetherClickGuiScreen screen, int width, int height) {
        // Deck: a compact centered panel, not a fullscreen sheet. Target size follows the
        // screen a little but is capped hard, so it stays a modest window at any resolution.
        int deckW = Math.max(380, Math.min(560, (int) (width * 0.55f)));
        int deckH = Math.max(240, Math.min(360, (int) (height * 0.72f)));
        int deckX = (width - deckW) / 2;
        int deckY = (height - deckH) / 2;

        int headerH = AetherMetrics.HEADER_HEIGHT;
        int footerH = AetherMetrics.FOOTER_HEIGHT;
        int bodyTop = deckY + headerH;
        int footerY = deckY + deckH - footerH;
        int sidebarW = (deckW >= 560) ? AetherMetrics.SIDEBAR_WIDTH : 0;
        int sidebarX = deckX + 8;
        int sidebarY = bodyTop + 6;
        int sidebarH = footerY - sidebarY - 6;
        int listX = (sidebarW > 0) ? sidebarX + sidebarW + 8 : sidebarX;
        int innerW = deckX + deckW - listX - 8;
        boolean showSpine = innerW >= 430 && sidebarW > 0;
        int spineW = showSpine ? Math.min(Math.max((int) (innerW * 0.26f), 132), 200) : 0;
        int spineX = listX + innerW - spineW;
        int spineY = sidebarY;
        int spineH = sidebarH;
        int listW = innerW - spineW;
        int listY = sidebarY;
        int listH = footerY - listY - 6;

        // Real scroll range: the full height of the modules page content. Also mirrored
        // into the screen so the wheel handler's clampScroll() sees the same bound.
        float maxScroll = Math.max(0f, contentHeight(screen) - listH);
        screen.setMaxScroll(maxScroll);
        float scroll = Math.max(0f, Math.min(screen.getScroll(), maxScroll));

        return new Layout(deckX, deckY, deckW, deckH,
                headerH, footerH,
                sidebarW, sidebarX, sidebarY, sidebarH,
                listX, listY, listW, listH,
                spineX, spineY, spineW, spineH,
                footerY,
                width, height,
                scroll, maxScroll);
    }

    /** Height of the module list content in pixels, expanded accordions included. */
    public static int contentHeight(AetherClickGuiScreen screen) {
        int total = LIST_CONTENT_TOP;
        for (ClientModule module : screen.getVisibleModules()) {
            total += ModulesPage.rowHeight(screen, module);
            total += CARD_GAP;
        }
        return total;
    }

    /** Pixels from the top of the list content to the first row. */
    public static int listContentTop() {
        return LIST_CONTENT_TOP;
    }
}
