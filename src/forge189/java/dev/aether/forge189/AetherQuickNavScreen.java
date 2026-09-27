package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Quick Navigation Overlay — triggered by Right-Shift.
 *
 * Layout: one dark card (rect) with a gradient accent bar at the top,
 * two filled circles as buttons, labels and hints below each.
 */
public final class AetherQuickNavScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_RSHIFT = 54;

    // card
    private static final int CW = 300;
    private static final int CH = 160;

    // circles
    private static final int RADIUS = 32;

    private final AetherClient client;
    private final GuiScreen    parent;

    public AetherQuickNavScreen(AetherClient client) {
        this(client, null);
    }

    public AetherQuickNavScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    @Override public void initGui()  { super.initGui(); }
    public void func_73866_w_()      { initGui(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        renderScreen(mouseX, mouseY, partialTicks);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        renderScreen(mouseX, mouseY, partialTicks);
    }

    private void renderScreen(int mouseX, int mouseY, float partialTicks) {
        AetherUi.syncTheme();
        int sw = Mc189Compat.screenWidth(this);
        int sh = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);

        // Every colour here comes from the shared theme tokens, so the quick nav follows the
        // active theme module exactly like the click deck does.
        int panelEdge = AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40);
        int accentLight = AetherUi.blend(AetherUi.ACCENT, 0xFFFFFFFF, 0.35F);

        // ── scrim ──────────────────────────────────────────────────────
        Mc189Compat.drawRect(0, 0, sw, sh, AetherUi.withAlpha(AetherUi.SURFACE, 0xAA));

        // ── card ───────────────────────────────────────────────────────
        int cx = (sw - CW) / 2;
        int cy = (sh - CH) / 2;

        // outer shadow (3 layers, progressively lighter)
        Mc189Compat.drawRect(cx - 3, cy - 3, cx + CW + 3, cy + CH + 3, AetherUi.withAlpha(AetherUi.SHADOW, 0x28));
        Mc189Compat.drawRect(cx - 2, cy - 2, cx + CW + 2, cy + CH + 2, AetherUi.withAlpha(AetherUi.SHADOW, 0x38));
        Mc189Compat.drawRect(cx - 1, cy - 1, cx + CW + 1, cy + CH + 1, AetherUi.withAlpha(AetherUi.SHADOW, 0x50));

        // card body
        Mc189Compat.drawRect(cx, cy, cx + CW, cy + CH, AetherUi.SURFACE);

        // top accent bar: solid accent 3px, then 1px lighter line
        Mc189Compat.drawRect(cx,      cy,     cx + CW, cy + 3, AetherUi.ACCENT);
        Mc189Compat.drawRect(cx,      cy + 3, cx + CW, cy + 4, accentLight);

        // border: 1px on the 3 other sides
        Mc189Compat.drawRect(cx,          cy + 4, cx + 1,      cy + CH, panelEdge);
        Mc189Compat.drawRect(cx + CW - 1, cy + 4, cx + CW,     cy + CH, panelEdge);
        Mc189Compat.drawRect(cx,          cy + CH - 1, cx + CW, cy + CH, panelEdge);

        // ── header text ────────────────────────────────────────────────
        AetherUi.centered(font, "AETHER", cx, cy + 9,  CW, AetherUi.TEXT_PRIMARY);
        AetherUi.centered(font, "Quick Navigation", cx, cy + 20, CW, AetherUi.TEXT_DISABLED);

        // thin separator
        int sepY = cy + 33;
        Mc189Compat.drawRect(cx + 40, sepY, cx + CW - 40, sepY + 1, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));

        // ── circle button positions ─────────────────────────────────────
        int leftCX  = cx + CW / 4;
        int rightCX = cx + CW * 3 / 4;
        int btnCY   = cy + 83;          // vertical centre of circles

        boolean hL = dist(mouseX, mouseY, leftCX,  btnCY) <= RADIUS;
        boolean hR = dist(mouseX, mouseY, rightCX, btnCY) <= RADIUS;

        // ── left button: Mod Menu ───────────────────────────────────────
        // outer glow ring on hover (radius+4, very transparent)
        int idleFill = AetherUi.withAlpha(AetherUi.ACCENT_DARK, 0xE0);
        int idleRing = AetherUi.withAlpha(AetherUi.ACCENT, 0x88);
        int idleIcon = AetherUi.withAlpha(AetherUi.TEXT_PRIMARY, 0xAA);

        if (hL) {
            drawSolidCircle(leftCX, btnCY, RADIUS + 5, AetherUi.withAlpha(AetherUi.ACCENT, 0x1A));
            drawSolidCircle(leftCX, btnCY, RADIUS + 3, AetherUi.withAlpha(AetherUi.ACCENT, 0x2A));
        }
        // filled circle
        drawSolidCircle(leftCX, btnCY, RADIUS, hL ? AetherUi.ACCENT : idleFill);
        // 1-px outline ring
        drawCircleOutline(leftCX, btnCY, RADIUS, hL ? accentLight : idleRing);

        // icon (12×12 gear, centred in circle)
        int iconColor = hL ? AetherUi.readableOn(AetherUi.ACCENT) : idleIcon;
        AetherUi.drawClientIcon(leftCX - 6, btnCY - 10, iconColor);

        // label inside circle, below icon
        AetherUi.centered(font, "Mods", leftCX - RADIUS, btnCY + 4, RADIUS * 2,
            hL ? AetherUi.readableOn(AetherUi.ACCENT) : AetherUi.TEXT_PRIMARY);

        // label below circle
        AetherUi.centered(font, "Mod Menu", leftCX - 40, btnCY + RADIUS + 7, 80,
            hL ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);

        // ── right button: HUD Editor ────────────────────────────────────
        if (hR) {
            drawSolidCircle(rightCX, btnCY, RADIUS + 5, AetherUi.withAlpha(AetherUi.ACCENT, 0x1A));
            drawSolidCircle(rightCX, btnCY, RADIUS + 3, AetherUi.withAlpha(AetherUi.ACCENT, 0x2A));
        }
        drawSolidCircle(rightCX, btnCY, RADIUS, hR ? AetherUi.ACCENT : idleFill);
        drawCircleOutline(rightCX, btnCY, RADIUS, hR ? accentLight : idleRing);

        int iconColorR = hR ? AetherUi.readableOn(AetherUi.ACCENT) : idleIcon;
        AetherUi.drawHudIcon(rightCX - 6, btnCY - 10, iconColorR);

        AetherUi.centered(font, "HUD", rightCX - RADIUS, btnCY + 4, RADIUS * 2,
            hR ? AetherUi.readableOn(AetherUi.ACCENT) : AetherUi.TEXT_PRIMARY);

        AetherUi.centered(font, "HUD Editor", rightCX - 40, btnCY + RADIUS + 7, 80,
            hR ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);

        // ── footer ─────────────────────────────────────────────────────
        int[] legacy = legacyChip(cx, cy, font);
        boolean legacyHover = mouseX >= legacy[0] && mouseX <= legacy[0] + legacy[2]
                && mouseY >= legacy[1] && mouseY <= legacy[1] + legacy[3];
        Mc189Compat.drawRect(legacy[0], legacy[1], legacy[0] + legacy[2], legacy[1] + legacy[3],
                legacyHover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.ROW_BG);
        AetherUi.centered(font, "Classic list view", legacy[0], legacy[1] + 4, legacy[2],
                legacyHover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
        AetherUi.centered(font, "ESC or Right Shift to close", cx, cy + CH - 10, CW, AetherUi.TEXT_DISABLED);
    }

    /** Footer chip that keeps the older three-panel manager one click away. */
    private static int[] legacyChip(int cx, int cy, Object font) {
        String label = "Classic list view";
        int w = Mc189Compat.stringWidth(font, label) + 16;
        return new int[] {cx + CW / 2 - w / 2, cy + CH - 34, w, 16};
    }

    // ── filled circle via scanlines (guaranteed to work with drawRect) ───
    private static void drawSolidCircle(int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            if (hw > 0) Mc189Compat.drawRect(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
        }
    }

    // ── 1-px outline ring ────────────────────────────────────────────────
    private static void drawCircleOutline(int cx, int cy, int r, int color) {
        int inner = r - 1;
        for (int dy = -r; dy <= r; dy++) {
            int outerHW = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            int innerHW = (Math.abs(dy) <= inner)
                    ? (int) Math.round(Math.sqrt((double) inner * inner - (double) dy * dy))
                    : 0;
            if (outerHW > innerHW) {
                Mc189Compat.drawRect(cx - outerHW, cy + dy, cx - innerHW, cy + dy + 1, color);
                Mc189Compat.drawRect(cx + innerHW, cy + dy, cx + outerHW, cy + dy + 1, color);
            }
        }
    }

    private static double dist(int mx, int my, int cx, int cy) {
        int dx = mx - cx;
        int dy = my - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ── input ────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleClick(mouseX, mouseY, mouseButton);
    }

    protected void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleClick(mouseX, mouseY, mouseButton);
    }

    private void handleClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        int sw = Mc189Compat.screenWidth(this);
        int sh = Mc189Compat.screenHeight(this);
        int cx = (sw - CW) / 2;
        int cy = (sh - CH) / 2;
        int leftCX  = cx + CW / 4;
        int rightCX = cx + CW * 3 / 4;
        int btnCY   = cy + 83;

        if (dist(mouseX, mouseY, leftCX, btnCY) <= RADIUS) {
            Mc189Compat.displayGuiScreen(new AetherClickGuiScreen(client, this));
            return;
        }
        int[] legacy = legacyChip(cx, cy, Mc189Compat.screenFontRenderer(this));
        if (mouseX >= legacy[0] && mouseX <= legacy[0] + legacy[2]
                && mouseY >= legacy[1] && mouseY <= legacy[1] + legacy[3]) {
            Mc189Compat.displayGuiScreen(new AetherModMenuScreen(client, this));
            return;
        }
        if (dist(mouseX, mouseY, rightCX, btnCY) <= RADIUS) {
            Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE || keyCode == KEY_RSHIFT) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE || keyCode == KEY_RSHIFT) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }
    public boolean func_73868_f() { return false; }
}
