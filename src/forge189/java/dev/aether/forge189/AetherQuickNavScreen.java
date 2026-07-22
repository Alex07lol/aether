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
        int sw = Mc189Compat.screenWidth(this);
        int sh = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);

        // ── scrim ──────────────────────────────────────────────────────
        Mc189Compat.drawRect(0, 0, sw, sh, 0xAA050810);

        // ── card ───────────────────────────────────────────────────────
        int cx = (sw - CW) / 2;
        int cy = (sh - CH) / 2;

        // outer shadow (3 layers, progressively lighter)
        Mc189Compat.drawRect(cx - 3, cy - 3, cx + CW + 3, cy + CH + 3, 0x28000000);
        Mc189Compat.drawRect(cx - 2, cy - 2, cx + CW + 2, cy + CH + 2, 0x38000000);
        Mc189Compat.drawRect(cx - 1, cy - 1, cx + CW + 1, cy + CH + 1, 0x50000000);

        // card body
        Mc189Compat.drawRect(cx, cy, cx + CW, cy + CH, 0xFF0E1220);

        // top accent bar: solid blue 3px, then 1px lighter line
        Mc189Compat.drawRect(cx,      cy,     cx + CW, cy + 3, 0xFF2563EB);
        Mc189Compat.drawRect(cx,      cy + 3, cx + CW, cy + 4, 0xFF3B82F6);

        // border: 1px on the 3 other sides
        Mc189Compat.drawRect(cx,          cy + 4, cx + 1,      cy + CH, 0x40FFFFFF);
        Mc189Compat.drawRect(cx + CW - 1, cy + 4, cx + CW,     cy + CH, 0x40FFFFFF);
        Mc189Compat.drawRect(cx,          cy + CH - 1, cx + CW, cy + CH, 0x40FFFFFF);

        // ── header text ────────────────────────────────────────────────
        // "AETHER" in bright white, centred
        AetherUi.centered(font, "AETHER", cx, cy + 9,  CW, 0xFFFFFFFF);
        // small subtitle
        AetherUi.centered(font, "Quick Navigation", cx, cy + 20, CW, 0xFF4A6FA5);

        // thin separator
        int sepY = cy + 33;
        Mc189Compat.drawRect(cx + 40, sepY, cx + CW - 40, sepY + 1, 0x30FFFFFF);

        // ── circle button positions ─────────────────────────────────────
        int leftCX  = cx + CW / 4;
        int rightCX = cx + CW * 3 / 4;
        int btnCY   = cy + 83;          // vertical centre of circles

        boolean hL = dist(mouseX, mouseY, leftCX,  btnCY) <= RADIUS;
        boolean hR = dist(mouseX, mouseY, rightCX, btnCY) <= RADIUS;

        // ── left button: Mod Menu ───────────────────────────────────────
        // outer glow ring on hover (radius+4, very transparent)
        if (hL) {
            drawSolidCircle(leftCX, btnCY, RADIUS + 5, 0x1A2563EB);
            drawSolidCircle(leftCX, btnCY, RADIUS + 3, 0x2A3B82F6);
        }
        // filled circle
        drawSolidCircle(leftCX, btnCY, RADIUS, hL ? 0xFF1D4ED8 : 0xFF162040);
        // 1-px outline ring
        drawCircleOutline(leftCX, btnCY, RADIUS, hL ? 0xFF60A5FA : 0xFF1E3A6E);

        // icon (12×12 gear, centred in circle)
        int iconColor = hL ? 0xFFFFFFFF : 0xFF6EA3DE;
        AetherUi.drawClientIcon(leftCX - 6, btnCY - 10, iconColor);

        // label inside circle, below icon
        AetherUi.centered(font, "Mods", leftCX - RADIUS, btnCY + 4, RADIUS * 2, hL ? 0xFFFFFFFF : 0xFFACC4E8);

        // label below circle
        AetherUi.centered(font, "Mod Menu", leftCX - 40, btnCY + RADIUS + 7, 80, hL ? 0xFFDDE4F5 : 0xFF3D547A);

        // ── right button: HUD Editor ────────────────────────────────────
        if (hR) {
            drawSolidCircle(rightCX, btnCY, RADIUS + 5, 0x1A0891B2);
            drawSolidCircle(rightCX, btnCY, RADIUS + 3, 0x2A22D3EE);
        }
        drawSolidCircle(rightCX, btnCY, RADIUS, hR ? 0xFF0E7490 : 0xFF0D2030);
        drawCircleOutline(rightCX, btnCY, RADIUS, hR ? 0xFF38BDF8 : 0xFF0F4060);

        int iconColorR = hR ? 0xFFFFFFFF : 0xFF5BB8D4;
        AetherUi.drawHudIcon(rightCX - 6, btnCY - 10, iconColorR);

        AetherUi.centered(font, "HUD", rightCX - RADIUS, btnCY + 4, RADIUS * 2, hR ? 0xFFFFFFFF : 0xFF7DD3FC);

        AetherUi.centered(font, "HUD Editor", rightCX - 40, btnCY + RADIUS + 7, 80, hR ? 0xFFDDE4F5 : 0xFF254A60);

        // ── footer ─────────────────────────────────────────────────────
        AetherUi.centered(font, "ESC or Right Shift to close", cx, cy + CH - 8, CW, 0xFF1E2D45);
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
