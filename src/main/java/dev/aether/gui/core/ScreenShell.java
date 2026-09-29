package dev.aether.gui.core;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.ui.GuiSection;
import dev.aether.gui.components.TextField;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * The shared chrome of every Aether GUI screen: backdrop, glass panel, branding,
 * the section navigation bar, the optional search slot and the footer.
 * <p>
 * This is the "Shared Shell" of the brief (Leaf's screens all repeat the same header
 * row; here it is one object the base screen drives, so navigation cannot drift into
 * per-screen copies). Clicks are tested here first: the shell consumes nav and search
 * clicks and reports everything else back to the screen's content.
 */
public final class ScreenShell {

    /** Fixed design-unit geometry of the panel chrome. */
    public static final int PANEL_MARGIN_X = 40;
    public static final int PANEL_TOP = 30;
    public static final int PANEL_BOTTOM_MARGIN = 26;
    public static final int HEADER_HEIGHT = 60;
    public static final int FOOTER_HEIGHT = 30;
    public static final int INNER_PADDING = 20;

    public static final int SEARCH_WIDTH = 280;
    public static final int SEARCH_HEIGHT = 34;
    public static final int TAB_WIDTH = 108;
    public static final int TAB_HEIGHT = 30;

    /** Where the content area sits, in design units. */
    public double contentX;
    public double contentY;
    public double contentW;
    public double contentH;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private final int[] tabX = new int[GuiSection.ordered().length];

    /** Navigates to another section; implemented by the base screen. */
    public interface Navigator {
        void navigateTo(GuiSection section);
    }

    /** Recomputes the fixed chrome geometry. Cheap; called on layout. */
    public void layout() {
        double designW = GuiScale.designWidth();
        panelX = PANEL_MARGIN_X;
        panelW = (int) (designW - PANEL_MARGIN_X * 2);
        panelY = PANEL_TOP;
        panelH = GuiScale.DESIGN_HEIGHT - PANEL_TOP - PANEL_BOTTOM_MARGIN;

        int headerBottom = panelY + HEADER_HEIGHT;
        int footerTop = panelY + panelH - FOOTER_HEIGHT;
        contentX = panelX + INNER_PADDING;
        contentY = headerBottom + 12;
        contentW = panelX + panelW - INNER_PADDING - contentX;
        contentH = footerTop - contentY - 10;

        GuiSection[] sections = GuiSection.ordered();
        int tabsTotal = sections.length * TAB_WIDTH + (sections.length - 1) * 6;
        int rightEdge = panelX + panelW - INNER_PADDING;
        int tabsRight = rightEdge; // the search field, when present, sits left of the tabs
        for (int i = sections.length - 1; i >= 0; i--) {
            tabX[i] = tabsRight - TAB_WIDTH;
            tabsRight -= TAB_WIDTH + 6;
        }
    }

    /** Left edge where the caller should place the search field, when it has one. */
    public int searchX() {
        return panelX + panelW - INNER_PADDING - SEARCH_WIDTH
            - GuiSection.ordered().length * (TAB_WIDTH + 6);
    }

    public int searchY() {
        return panelY + (HEADER_HEIGHT - SEARCH_HEIGHT) / 2;
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    /**
     * Draws the full chrome. {@code search} may be null for sections without a search
     * field; the caller positions it with {@link #searchX()}/{@link #searchY()}.
     */
    public void render(GuiSection current, String title, TextField search, double mouseX, double mouseY) {
        int guiW = GuiScale.guiWidth();
        int guiH = GuiScale.guiHeight();

        // Backdrop scrim over the world, top-to-bottom like the old deck's.
        Mc189Compat.drawGradientRectangle(0, 0, guiW, guiH / 2, AetherUi.SCRIM_TOP, AetherUi.blend(AetherUi.SCRIM_TOP, AetherUi.SCRIM_BOTTOM, 0.5F));
        Mc189Compat.drawGradientRectangle(0, guiH / 2, guiW, guiH - guiH / 2,
            AetherUi.blend(AetherUi.SCRIM_TOP, AetherUi.SCRIM_BOTTOM, 0.5F), AetherUi.SCRIM_BOTTOM);

        drawPanel();
        drawHeader(current, title, search, mouseX, mouseY);
        drawFooter();
    }

    private void drawPanel() {
        int left = GuiScale.x(panelX);
        int top = GuiScale.y(panelY);
        int w = GuiScale.w(panelW);
        int h = GuiScale.h(panelH);
        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(14), AetherUi.DECK_BG);
        AetherUi.outline(left, top, left + w, top + h, AetherUi.DECK_EDGE);
    }

    private void drawHeader(GuiSection current, String title, TextField search, double mouseX, double mouseY) {
        int headerTop = panelY;
        int headerH = HEADER_HEIGHT;

        // Branding: the Aether logo texture with the wordmark next to it.
        int logoX = GuiScale.x(panelX + INNER_PADDING - 4);
        int logoY = GuiScale.y(headerTop + (headerH - 34) / 2);
        Mc189Compat.drawTexture("aetherlogo.png", logoX, logoY, GuiScale.h(34), GuiScale.h(34));

        int labelX = logoX + GuiScale.h(34) + GuiScale.w(10);
        AetherFont.drawShadowed(AetherFont.Size.TITLE, "AETHER", labelX,
            GuiScale.y(headerTop + (headerH - 30) / 2), AetherUi.ACCENT);
        int labelWidth = AetherFont.width(AetherFont.Size.TITLE, "AETHER");

        if (title != null && !title.isEmpty()) {
            int titleX = labelX + labelWidth + GuiScale.w(22);
            AetherFont.draw(AetherFont.Size.SECTION, title, titleX,
                GuiScale.y(headerTop + (headerH - 24) / 2), AetherUi.TEXT_SECONDARY);
        }

        // Navigation tabs.
        GuiSection[] sections = GuiSection.ordered();
        for (int i = 0; i < sections.length; i++) {
            GuiSection section = sections[i];
            int tx = tabX[i];
            int ty = headerTop + (headerH - TAB_HEIGHT) / 2;
            boolean active = section == current;
            boolean hover = !active && mouseX >= tx && mouseX < tx + TAB_WIDTH
                && mouseY >= ty && mouseY < ty + TAB_HEIGHT;
            int gx = GuiScale.x(tx);
            int gy = GuiScale.y(ty);
            int gw = GuiScale.w(TAB_WIDTH);
            int gh = GuiScale.h(TAB_HEIGHT);
            int fill = active ? AetherUi.withAlpha(AetherUi.ACCENT, 0x2E)
                : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            AetherUi.drawRoundRect(gx, gy, gx + gw, gy + gh, GuiScale.h(7), fill);
            if (active) {
                AetherUi.outline(gx, gy, gx + gw, gy + gh, AetherUi.withAlpha(AetherUi.ACCENT, 0x77));
            }
            AetherFont.drawCentered(AetherFont.Size.SMALL, section.label(), gx,
                gy + (gh - AetherFont.height(AetherFont.Size.SMALL)) / 2, gw,
                active ? AetherUi.TEXT_PRIMARY : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        }

        if (search != null) {
            search.render();
        }
    }

    private void drawFooter() {
        int footerTextY = panelY + panelH - FOOTER_HEIGHT + (FOOTER_HEIGHT - 15) / 2;
        // The screen draws its own hint on the left; this draws only the version tag.
        AetherFont.draw(AetherFont.Size.CAPTION, "Aether 0.1.0",
            GuiScale.x(panelX + panelW - INNER_PADDING - 80), GuiScale.y(footerTextY), AetherUi.TEXT_DISABLED);
    }

    /** Draws the screen's hint line on the footer's left side. */
    public void drawHint(String hint) {
        if (hint == null || hint.isEmpty()) {
            return;
        }
        int footerTextY = panelY + panelH - FOOTER_HEIGHT + (FOOTER_HEIGHT - 15) / 2;
        AetherFont.draw(AetherFont.Size.CAPTION, hint,
            GuiScale.x(panelX + INNER_PADDING), GuiScale.y(footerTextY), AetherUi.TEXT_DISABLED);
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    /**
     * Gives the shell first crack at a click (design units).
     *
     * @return true when the shell consumed the click (navigation or search)
     */
    public boolean click(double mouseX, double mouseY, int button, GuiSection current,
                         TextField search, Navigator navigator) {
        if (button == 0) {
            GuiSection[] sections = GuiSection.ordered();
            for (int i = 0; i < sections.length; i++) {
                int ty = panelY + (HEADER_HEIGHT - TAB_HEIGHT) / 2;
                if (mouseX >= tabX[i] && mouseX < tabX[i] + TAB_WIDTH
                    && mouseY >= ty && mouseY < ty + TAB_HEIGHT) {
                    if (sections[i] != current && navigator != null) {
                        navigator.navigateTo(sections[i]);
                    }
                    return true;
                }
            }
        }
        if (search != null && search.onMouseClick(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }
}
