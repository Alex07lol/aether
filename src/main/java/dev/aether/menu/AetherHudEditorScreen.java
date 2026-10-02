package dev.aether.menu;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import dev.aether.forge189.ForgeHudRenderer;
import dev.aether.gui.AetherFont;
import dev.aether.gui.core.AetherUiScreen;
import dev.aether.hud.HudElement;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * The HUD editor, the menu's Edit HUD destination: every HUD element renders in place
 * with a bounding box, drags move with magnetic snapping (screen edges, centre lines,
 * element-to-element), the wheel scales the selection, Shift+wheel sets opacity, R
 * resets, and ESC saves and returns. Styled in the menu's language - dim backdrop, a
 * dark guide grid, accent guides while snapping - but fullscreen, because the elements
 * live wherever the player put them.
 * <p>
 * Element coordinates stay in GUI-scale pixels (that is the space the HUD renderer
 * draws in); positions write straight to {@code HudLayout} and persist with the next
 * client save.
 */
public final class AetherHudEditorScreen extends GuiScreen implements AetherUiScreen {

    private static final int KEY_R = 19;
    private static final int KEY_MINUS = 12;
    private static final int KEY_EQUALS = 13;
    private static final int KEY_LBRACKET = 26;
    private static final int SNAP_THRESHOLD = 6;

    private final dev.aether.AetherClient client;
    private ForgeHudRenderer renderer;
    private final Map<String, Dimension> elementDimensions = new HashMap<String, Dimension>();
    private boolean dimensionsCalculated;

    private HudElement selectedElement;
    private HudElement draggingElement;
    private int dragOffsetX;
    private int dragOffsetY;

    private final List<Integer> snapLinesVertical = new ArrayList<Integer>();
    private final List<Integer> snapLinesHorizontal = new ArrayList<Integer>();
    private HudElement lastSnappedElement;
    private int lastSnapMouseX = Integer.MIN_VALUE;
    private int lastSnapMouseY = Integer.MIN_VALUE;

    public AetherHudEditorScreen(dev.aether.AetherClient client) {
        this.client = client;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private ForgeHudRenderer renderer() {
        if (renderer == null) {
            renderer = new ForgeHudRenderer(client);
        }
        return renderer;
    }

    private void calculateDimensions() {
        elementDimensions.clear();
        Object font = dev.aether.forge189.Mc189Compat.screenFontRenderer(this);
        Object minecraft = dev.aether.forge189.Mc189Compat.minecraft();
        for (HudElement element : client.hudLayout().elements()) {
            Dimension dim = renderer().getDimensions(element.id(), font, minecraft);
            int scaledWidth = Math.round(dim.width * element.scale());
            int scaledHeight = Math.round(dim.height * element.scale());
            elementDimensions.put(element.id(), new Dimension(Math.max(16, scaledWidth), Math.max(10, scaledHeight)));
        }
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!dimensionsCalculated) {
            dimensionsCalculated = true;
            calculateDimensions();
        }
        UiTheme.apply(client.theme().palette());
        int w = this.width;
        int h = this.height;

        // Dim backdrop so the grid and guides read over any world.
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tessellator.getWorldRenderer();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        wr.pos(0, h, 0).color(0, 0, 0, 110).endVertex();
        wr.pos(w, h, 0).color(0, 0, 0, 110).endVertex();
        wr.pos(w, 0, 0).color(0, 0, 0, 110).endVertex();
        wr.pos(0, 0, 0).color(0, 0, 0, 110).endVertex();
        tessellator.draw();
        GL11.glPopAttrib();

        UiCanvas.beginRaw();
        try {
            drawGrid(w, h);
            renderer().renderForEditor();

            if (draggingElement != null) {
                updateDraggingPosition(mouseX, mouseY);
            } else {
                snapLinesVertical.clear();
                snapLinesHorizontal.clear();
                lastSnappedElement = null;
            }

            int guide = UiTheme.withAlpha(UiTheme.accent(), 200);
            int guideFade = UiTheme.withAlpha(UiTheme.accent(), 0);
            for (int xLine : snapLinesVertical) {
                UiCanvas.gradientRoundRect(xLine, 0, 1, h, 0, guide, guideFade);
            }
            for (int yLine : snapLinesHorizontal) {
                UiCanvas.roundRect(0, yLine, w, 1, 0, guide);
            }

            for (HudElement element : client.hudLayout().elements()) {
                Dimension dim = elementDimensions.get(element.id());
                if (dim == null) {
                    continue;
                }
                boolean enabled = renderer().enabled(element.id());
                int x1 = element.x() - 2;
                int y1 = element.y() - 2;
                int x2 = element.x() + dim.width + 2;
                int y2 = element.y() + dim.height + 2;

                int boxColor = enabled
                    ? UiTheme.withAlpha(UiTheme.text(), 70)
                    : UiTheme.withAlpha(UiTheme.textFaint(), 90);
                if (element == selectedElement || element == draggingElement) {
                    boxColor = UiTheme.accent();
                } else if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                    boxColor = enabled ? UiTheme.withAlpha(UiTheme.text(), 150) : UiTheme.withAlpha(UiTheme.textFaint(), 170);
                }
                UiCanvas.roundRect(x1, y1, x2 - x1, y2 - y1, 3.0F, UiTheme.withAlpha(boxColor, 40));
                UiCanvas.outline(x1, y1, x2 - x1, y2 - y1, 3.0F, boxColor, 1.0F);
                if (element == selectedElement) {
                    drawHandle(x1, y1);
                    drawHandle(x2 - 3, y1);
                    drawHandle(x1, y2 - 3);
                    drawHandle(x2 - 3, y2 - 3);
                }
            }

            if (selectedElement != null) {
                drawControlBar(selectedElement, mouseX, mouseY);
            }
            drawHelpBar(w, h);
        } finally {
            UiCanvas.end();
        }
    }

    private void drawHandle(int x, int y) {
        UiCanvas.roundRect(x - 1, y - 1, 5, 5, 1.5F, UiTheme.accent());
    }

    private void drawGrid(int w, int h) {
        int snap = 16;
        for (int x = 0; x < w; x += snap) {
            UiCanvas.roundRect(x, 0, 1, h, 0, UiTheme.withAlpha(UiTheme.text(), 10));
        }
        for (int y = 0; y < h; y += snap) {
            UiCanvas.roundRect(0, y, w, 1, 0, UiTheme.withAlpha(UiTheme.text(), 10));
        }
    }

    private void updateDraggingPosition(int mouseX, int mouseY) {
        if (draggingElement == lastSnappedElement && mouseX == lastSnapMouseX && mouseY == lastSnapMouseY) {
            return;
        }
        lastSnappedElement = draggingElement;
        lastSnapMouseX = mouseX;
        lastSnapMouseY = mouseY;
        snapLinesVertical.clear();
        snapLinesHorizontal.clear();

        int targetX = mouseX - dragOffsetX;
        int targetY = mouseY - dragOffsetY;
        Dimension targetDim = elementDimensions.get(draggingElement.id());
        int targetW = targetDim != null ? targetDim.width : 50;
        int targetH = targetDim != null ? targetDim.height : 12;
        int snappedX = targetX;
        int snappedY = targetY;
        int w = this.width;
        int h = this.height;
        int centerX = w / 2;
        int centerY = h / 2;

        if (Math.abs(targetX) < SNAP_THRESHOLD) {
            snappedX = 0;
            snapLinesVertical.add(0);
        } else if (Math.abs((targetX + targetW) - w) < SNAP_THRESHOLD) {
            snappedX = w - targetW;
            snapLinesVertical.add(w - 1);
        } else if (Math.abs((targetX + targetW / 2) - centerX) < SNAP_THRESHOLD) {
            snappedX = centerX - targetW / 2;
            snapLinesVertical.add(centerX);
        }
        if (Math.abs(targetY) < SNAP_THRESHOLD) {
            snappedY = 0;
            snapLinesHorizontal.add(0);
        } else if (Math.abs((targetY + targetH) - h) < SNAP_THRESHOLD) {
            snappedY = h - targetH;
            snapLinesHorizontal.add(h - 1);
        } else if (Math.abs((targetY + targetH / 2) - centerY) < SNAP_THRESHOLD) {
            snappedY = centerY - targetH / 2;
            snapLinesHorizontal.add(centerY);
        }

        for (HudElement other : client.hudLayout().elements()) {
            if (other == draggingElement || !renderer().enabled(other.id())) {
                continue;
            }
            Dimension otherDim = elementDimensions.get(other.id());
            if (otherDim == null) {
                continue;
            }
            int ox = other.x();
            int oy = other.y();
            int ow = otherDim.width;
            int oh = otherDim.height;
            if (Math.abs(targetX - ox) < SNAP_THRESHOLD) {
                snappedX = ox;
                snapLinesVertical.add(ox);
            } else if (Math.abs((targetX + targetW) - (ox + ow)) < SNAP_THRESHOLD) {
                snappedX = ox + ow - targetW;
                snapLinesVertical.add(ox + ow);
            } else if (Math.abs(targetX - (ox + ow)) < SNAP_THRESHOLD) {
                snappedX = ox + ow;
                snapLinesVertical.add(ox + ow);
            }
            if (Math.abs(targetY - oy) < SNAP_THRESHOLD) {
                snappedY = oy;
                snapLinesHorizontal.add(oy);
            } else if (Math.abs((targetY + targetH) - (oy + oh)) < SNAP_THRESHOLD) {
                snappedY = oy + oh - targetH;
                snapLinesHorizontal.add(oy + oh);
            } else if (Math.abs(targetY - (oy + oh)) < SNAP_THRESHOLD) {
                snappedY = oy + oh;
                snapLinesHorizontal.add(oy + oh);
            }
        }
        draggingElement.moveTo(snappedX, snappedY);
    }

    private void drawControlBar(HudElement element, int mouseX, int mouseY) {
        Dimension dim = elementDimensions.get(element.id());
        int elementW = dim != null ? dim.width : 50;
        int barW = 270;
        int barH = 26;
        int barX = Math.max(8, Math.min(this.width - barW - 8, element.x() + (elementW - barW) / 2));
        int barY = element.y() - barH - 8;
        if (barY < 8) {
            barY = element.y() + (dim != null ? dim.height : 12) + 8;
        }
        UiCanvas.roundRect(barX, barY, barW, barH, 6.0F, UiTheme.withAlpha(0x101116, 245));
        UiCanvas.gradientRoundRect(barX + 2, barY, barW - 4, 2, 1.0F, UiTheme.accent(), UiTheme.accentDeep());

        String name = element.id();
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < name.length() - 1) {
            name = name.substring(lastDot + 1).toUpperCase(Locale.ENGLISH);
        }
        AetherFont.draw(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, name, barX + 8.0F,
            barY + (barH - AetherFont.height(9.0F)) / 2.0F, UiTheme.text());

        boolean enabled = renderer().enabled(element.id());
        int toggleX = barX + 110;
        int toggleY = barY + 4;
        boolean toggleHot = mouseX >= toggleX && mouseX < toggleX + 32 && mouseY >= toggleY && mouseY < toggleY + 18;
        int toggleColor = enabled
            ? UiTheme.withAlpha(UiTheme.accent(), toggleHot ? 255 : 220)
            : UiTheme.withAlpha(0xFFFF7A6B, toggleHot ? 255 : 200);
        UiCanvas.roundRect(toggleX, toggleY, 32, 18, 4.0F, toggleColor);
        AetherFont.drawCentered(8.0F, enabled ? "ON" : "OFF", toggleX, toggleY + (18.0F - AetherFont.height(8.0F)) / 2.0F,
            32.0F, enabled ? UiTheme.readableOn(UiTheme.accent()) : 0xFFFFFFFF);

        int resetX = barX + barW - 38;
        int resetY = barY + 4;
        boolean resetHot = mouseX >= resetX && mouseX < resetX + 32 && mouseY >= resetY && mouseY < resetY + 18;
        UiCanvas.roundRect(resetX, resetY, 32, 18, 4.0F,
            UiTheme.withAlpha(0xFFFF7A6B, resetHot ? 200 : 70));
        AetherFont.drawCentered(8.0F, "Reset", resetX, resetY + (18.0F - AetherFont.height(8.0F)) / 2.0F,
            32.0F, UiTheme.text());
    }

    private void drawHelpBar(int w, int h) {
        String help = "[Drag] Move  |  [Scroll] Scale  |  [Shift+Scroll] Opacity  |  [R] Reset  |  [ESC] Save & Exit";
        int textW = AetherFont.width(8.0F, help);
        UiCanvas.roundRect((w - textW - 16) / 2.0F, h - 28, textW + 16, 20, 6.0F, UiTheme.withAlpha(0x101116, 235));
        AetherFont.draw(8.0F, help, (w - textW - 16) / 2.0F + 8.0F, h - 24, UiTheme.accent());
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) {
            return;
        }
        if (selectedElement != null) {
            Dimension dim = elementDimensions.get(selectedElement.id());
            int elementW = dim != null ? dim.width : 50;
            int barW = 270;
            int barH = 26;
            int barX = Math.max(8, Math.min(this.width - barW - 8, selectedElement.x() + (elementW - barW) / 2));
            int barY = selectedElement.y() - barH - 8;
            if (barY < 8) {
                barY = selectedElement.y() + (dim != null ? dim.height : 12) + 8;
            }
            int toggleX = barX + 110;
            int toggleY = barY + 4;
            if (mouseX >= toggleX && mouseX < toggleX + 32 && mouseY >= toggleY && mouseY < toggleY + 18) {
                boolean currentState = renderer().enabled(selectedElement.id());
                client.modules().setEnabled(selectedElement.id(), !currentState);
                saveQuietly();
                return;
            }
            int resetX = barX + barW - 38;
            int resetY = barY + 4;
            if (mouseX >= resetX && mouseX < resetX + 32 && mouseY >= resetY && mouseY < resetY + 18) {
                selectedElement.setScale(1.0F);
                selectedElement.setOpacity(1.0F);
                calculateDimensions();
                return;
            }
        }
        for (HudElement element : client.hudLayout().elements()) {
            Dimension dim = elementDimensions.get(element.id());
            if (dim == null) {
                continue;
            }
            if (mouseX >= element.x() - 2 && mouseX <= element.x() + dim.width + 2
                && mouseY >= element.y() - 2 && mouseY <= element.y() + dim.height + 2) {
                selectedElement = element;
                draggingElement = element;
                dragOffsetX = mouseX - element.x();
                dragOffsetY = mouseY - element.y();
                return;
            }
        }
        selectedElement = null;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (draggingElement != null) {
            updateDraggingPosition(mouseX, mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = org.lwjgl.input.Mouse.getX() * this.width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = this.height - org.lwjgl.input.Mouse.getY() * this.height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        HudElement target = selectedElement;
        if (target == null) {
            for (HudElement element : client.hudLayout().elements()) {
                if (!renderer().enabled(element.id())) {
                    continue;
                }
                Dimension dim = elementDimensions.get(element.id());
                if (dim != null && mouseX >= element.x() && mouseX <= element.x() + dim.width
                    && mouseY >= element.y() && mouseY <= element.y() + dim.height) {
                    target = element;
                    break;
                }
            }
        }
        if (target == null) {
            return;
        }
        float amount = wheel > 0 ? 0.05F : -0.05F;
        if (isShiftKeyDown()) {
            target.setOpacity(Math.max(0.10F, Math.min(1.0F, target.opacity() + amount)));
        } else {
            target.setScale(Math.max(0.50F, Math.min(2.50F, target.scale() + amount)));
            calculateDimensions();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingElement != null) {
            draggingElement = null;
            saveQuietly();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC saves and exits
            saveQuietly();
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        if (selectedElement != null) {
            if (keyCode == KEY_R) {
                selectedElement.setScale(1.0F);
                selectedElement.setOpacity(1.0F);
                calculateDimensions();
            } else if (keyCode == KEY_EQUALS) {
                selectedElement.setScale(Math.min(2.50F, selectedElement.scale() + 0.05F));
                calculateDimensions();
            } else if (keyCode == KEY_MINUS) {
                selectedElement.setScale(Math.max(0.50F, selectedElement.scale() - 0.05F));
            } else if (keyCode == KEY_LBRACKET) {
                selectedElement.setOpacity(Math.max(0.10F, selectedElement.opacity() - 0.05F));
            }
        }
    }

    @Override
    public void onGuiClosed() {
        saveQuietly();
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }
}
