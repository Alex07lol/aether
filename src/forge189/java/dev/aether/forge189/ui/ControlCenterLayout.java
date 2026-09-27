package dev.aether.forge189.ui;

import dev.aether.forge189.AetherClickGuiScreen;

/**
 * Computes layout geometry for the Control Center.
 */
public final class ControlCenterLayout {
    public Layout compute(AetherClickGuiScreen screen, int width, int height) {
        int margin = Math.min(Math.max(width / 24, 6), 16);
        int deckW = Math.min(1040, width - margin * 2);
        int deckH = height - margin * 2;
        int deckX = (width - deckW) / 2;
        int deckY = margin;
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

        float maxScroll = 0f;
        float scroll = Math.max(0f, Math.min(screen.getScroll(), maxScroll));

        return new Layout(deckX, deckY, deckW, deckH,
                headerH, footerH,
                sidebarW, sidebarX, sidebarY, sidebarH,
                listX, listY, listW, listH,
                spineX, spineY, spineW, spineH,
                footerY,
                scroll, maxScroll);
    }
}