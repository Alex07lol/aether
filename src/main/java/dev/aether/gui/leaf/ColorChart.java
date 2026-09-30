package dev.aether.gui.leaf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.preview.PlayerPreview;
import dev.aether.gui.preview.PlayerPreview.TextureHandle;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.ColorChart} (GPLv3, see
 * docs/GUI_REBUILD.md): a colour swatch with the row label 250 design units to its
 * left; clicking it opens a colour panel (Leaf places the 255x255 palette at
 * (710, 413)) with a drag-to-pick cursor and a column of recently used colours.
 * Leaf samples a palette image; Aether generates an equivalent HSV field
 * procedurally (hue across, saturation down) so no Leaf asset is used.
 */
public final class ColorChart extends UiComponent {

    /** Palette panel placement, Leaf's coordinates. */
    private static final int PANEL_X = 710;
    private static final int PANEL_Y = 413;
    private static final int PANEL_SIZE = 255;
    /** Recent colours kept per session (Leaf persists them; Aether keeps it simple). */
    private static final List<Integer> RECENT = new ArrayList<Integer>();

    private final String rowName;
    private final Runnable onPick;
    private int code;
    private final int swatchW;
    private final int swatchH;
    private boolean hover;
    private boolean panelOpen;
    private boolean picking;

    private TextureHandle palette;

    public ColorChart(String rowName, int x, int y, int swatchW, int swatchH, int initialCode, Runnable onPick) {
        this.rowName = rowName;
        this.swatchW = swatchW;
        this.swatchH = swatchH;
        this.code = initialCode;
        this.onPick = onPick;
        at(x, y).size(swatchW, swatchH);
    }

    public int colorCode() {
        return code;
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();

        if (rowName != null && !rowName.isEmpty()) {
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(250),
                top + (h - labelSize) / 2, AetherUi.TEXT_SECONDARY);
        }

        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(6), code | 0xFF000000);
        AetherUi.outline(left, top, left + w, top + h, hover
            ? AetherUi.withAlpha(AetherUi.ACCENT, 0x99) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x55));

        if (!panelOpen) {
            return;
        }

        int panelX = GuiScale.x(PANEL_X);
        int panelY = GuiScale.y(PANEL_Y);
        int size = GuiScale.w(PANEL_SIZE);
        Mc189Compat.drawRect(panelX - GuiScale.w(10), panelY - GuiScale.w(10),
            panelX + size + GuiScale.w(320), panelY + size + GuiScale.w(10), 0x50000000);

        if (palette == null) {
            palette = PlayerPreview.uploadTexture(buildPalette());
        }
        if (palette != null) {
            Mc189Compat.enableTexture2D();
            Mc189Compat.bindTexture(palette.glId());
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            dev.aether.forge189.Mc189Compat.drawTextureQuad(panelX, panelY, size, size);
        }

        // Recent colour chips to the right of the palette, like Leaf's recent column.
        int chipX = panelX + size + GuiScale.w(20);
        int chipW = GuiScale.w(60);
        int chipH = GuiScale.h(24);
        int recentY = panelY;
        for (int recent : RECENT) {
            Mc189Compat.drawRect(chipX, recentY, chipX + chipW, recentY + chipH, recent | 0xFF000000);
            Mc189Compat.drawRect(chipX, recentY, chipX + chipW, recentY + 1, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x77));
            recentY += chipH + GuiScale.h(10);
        }
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
        if (!panelOpen) {
            return;
        }
        boolean overPanel = mouseX >= PANEL_X && mouseX < PANEL_X + PANEL_SIZE
            && mouseY >= PANEL_Y && mouseY < PANEL_Y + PANEL_SIZE;
        if (overPanel && picking && Mouse0.down()) {
            int px = (int) Math.max(0, Math.min(PANEL_SIZE - 1, (mouseX - PANEL_X)));
            int py = (int) Math.max(0, Math.min(PANEL_SIZE - 1, (mouseY - PANEL_Y)));
            code = paletteColor(px, py) | 0xFF000000;
        } else if (picking && !Mouse0.down()) {
            picking = false;
            remember(code);
            if (onPick != null) {
                onPick.run();
            }
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!panelOpen) {
            if (contains(mouseX, mouseY)) {
                panelOpen = true;
                picking = false;
                return true;
            }
            return false;
        }
        boolean insidePanelArea = mouseX >= PANEL_X - 10 && mouseX < PANEL_X + PANEL_SIZE + 330
            && mouseY >= PANEL_Y - 10 && mouseY < PANEL_Y + PANEL_SIZE + 10;
        if (!insidePanelArea) {
            panelOpen = false;
            return true;
        }
        boolean overPanel = mouseX >= PANEL_X && mouseX < PANEL_X + PANEL_SIZE
            && mouseY >= PANEL_Y && mouseY < PANEL_Y + PANEL_SIZE;
        if (overPanel) {
            picking = true;
            int px = (int) Math.min(PANEL_SIZE - 1, Math.max(0, mouseX - PANEL_X));
            int py = (int) Math.min(PANEL_SIZE - 1, Math.max(0, mouseY - PANEL_Y));
            code = paletteColor(px, py) | 0xFF000000;
            return true;
        }
        // Recent chips
        int chipX = PANEL_X + PANEL_SIZE + 20;
        int chipW = 60;
        int chipH = 24;
        int recentY = PANEL_Y;
        for (int recent : RECENT) {
            if (mouseX >= chipX && mouseX < chipX + chipW && mouseY >= recentY && mouseY < recentY + chipH) {
                code = recent | 0xFF000000;
                if (onPick != null) {
                    onPick.run();
                }
                return true;
            }
            recentY += chipH + 10;
        }
        return true;
    }

    private void remember(int argb) {
        Integer boxed = Integer.valueOf(argb & 0x00FFFFFF);
        RECENT.remove(boxed);
        RECENT.add(0, boxed);
        while (RECENT.size() > 10) {
            RECENT.remove(RECENT.size() - 1);
        }
    }

    /** The generated HSV field: hue across X, saturation down Y, full brightness. */
    private static BufferedImage buildPalette() {
        BufferedImage image = new BufferedImage(255, 255, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 255; x++) {
            float hue = x / 254.0F;
            for (int y = 0; y < 255; y++) {
                float saturation = 1.0F - y / 254.0F;
                image.setRGB(x, y, Color.HSBtoRGB(hue, saturation, 1.0F));
            }
        }
        return image;
    }

    private static int paletteColor(int x, int y) {
        return Color.HSBtoRGB(x / 254.0F, 1.0F - y / 254.0F, 1.0F);
    }

    /** Tiny indirection so the chart can ask for the left button without LWJGL imports leaking. */
    private static final class Mouse0 {
        static boolean down() {
            return org.lwjgl.input.Mouse.isButtonDown(0);
        }
    }
}
