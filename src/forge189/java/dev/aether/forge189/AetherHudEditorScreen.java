package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.hud.HudElement;
import net.minecraft.client.gui.GuiScreen;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Direct-Manipulation HUD Layout Editor Screen.
 * Supports real-time dragging, magnetic edge & element snapping,
 * mouse scroll & hotkey scaling, shift-scroll opacity adjustments,
 * and a floating control bar.
 */
public final class AetherHudEditorScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_R = 19;
    private static final int KEY_MINUS = 12;
    private static final int KEY_EQUALS = 13;
    private static final int KEY_LBRACKET = 26;
    private static final int KEY_RBRACKET = 27;

    private static final int SNAP_THRESHOLD = 6;
    private static final int ACCENT_COLOR = 0xFF52BEEB;
    private static final int GUIDE_LINE_COLOR = 0xCC52BEEB;

    private final AetherClient client;
    private final ForgeHudRenderer renderer;
    private final Map<String, Dimension> elementDimensions = new HashMap<>();

    private HudElement selectedElement;
    private HudElement draggingElement;
    private int dragOffsetX;
    private int dragOffsetY;

    // Active snap guide lines for rendering
    private final List<Integer> activeVerticalSnapLines = new ArrayList<>();
    private final List<Integer> activeHorizontalSnapLines = new ArrayList<>();

    public AetherHudEditorScreen(AetherClient client) {
        this.client = client;
        this.renderer = new ForgeHudRenderer(client);
    }

    @Override
    public void initGui() {
        super.initGui();
        calculateDimensions();
    }

    @Override
    public void onGuiClosed() {
        saveQuietly();
    }

    private void calculateDimensions() {
        elementDimensions.clear();
        Object font = Mc189Compat.screenFontRenderer(this);
        Object mc = Mc189Compat.minecraft();
        for (HudElement element : client.hudLayout().elements()) {
            if (renderer.enabled(element.id())) {
                Dimension dim = renderer.getDimensions(element.id(), font, mc);
                int scaledWidth = Math.round(dim.width * element.scale());
                int scaledHeight = Math.round(dim.height * element.scale());
                elementDimensions.put(element.id(), new Dimension(Math.max(12, scaledWidth), Math.max(8, scaledHeight)));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGrid();
        renderer.render();

        activeVerticalSnapLines.clear();
        activeHorizontalSnapLines.clear();

        // If currently dragging, update position with snapping
        if (draggingElement != null) {
            updateDraggingPosition(mouseX, mouseY);
        }

        // Draw active snap guide lines
        for (int xLine : activeVerticalSnapLines) {
            Mc189Compat.drawRect(xLine, 0, xLine + 1, height, GUIDE_LINE_COLOR);
        }
        for (int yLine : activeHorizontalSnapLines) {
            Mc189Compat.drawRect(0, yLine, width, yLine + 1, GUIDE_LINE_COLOR);
        }

        // Render element selection bounding boxes and handles
        for (HudElement element : client.hudLayout().elements()) {
            if (!renderer.enabled(element.id())) {
                continue;
            }
            Dimension dim = elementDimensions.get(element.id());
            if (dim == null) continue;

            int x1 = element.x() - 2;
            int y1 = element.y() - 2;
            int x2 = element.x() + dim.width + 2;
            int y2 = element.y() + dim.height + 2;

            int boxColor = 0x44FFFFFF;
            if (element == selectedElement || element == draggingElement) {
                boxColor = ACCENT_COLOR;
            } else if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                boxColor = 0x99FFFFFF;
            }

            // Outline
            Mc189Compat.drawRect(x1, y1, x2, y1 + 1, boxColor);
            Mc189Compat.drawRect(x1, y2 - 1, x2, y2, boxColor);
            Mc189Compat.drawRect(x1, y1, x1 + 1, y2, boxColor);
            Mc189Compat.drawRect(x2 - 1, y1, x2, y2, boxColor);

            // Selection Corner Handles for selected element
            if (element == selectedElement) {
                drawHandle(x1, y1);
                drawHandle(x2 - 3, y1);
                drawHandle(x1, y2 - 3);
                drawHandle(x2 - 3, y2 - 3);
            }
        }

        // Render Floating Control Bar for Selected Element
        if (selectedElement != null && renderer.enabled(selectedElement.id())) {
            drawFloatingControlBar(selectedElement, mouseX, mouseY);
        }

        // Bottom Help Bar
        drawHelpBar();
    }

    private void drawHandle(int x, int y) {
        Mc189Compat.drawRect(x - 1, y - 1, x + 4, y + 4, ACCENT_COLOR);
    }

    private void updateDraggingPosition(int mouseX, int mouseY) {
        int targetX = mouseX - dragOffsetX;
        int targetY = mouseY - dragOffsetY;

        Dimension targetDim = elementDimensions.get(draggingElement.id());
        int targetW = targetDim != null ? targetDim.width : 50;
        int targetH = targetDim != null ? targetDim.height : 12;

        int snappedX = targetX;
        int snappedY = targetY;

        // 1. Screen Edge Snapping
        int screenCenterX = width / 2;
        int screenCenterY = height / 2;

        if (Math.abs(targetX) < SNAP_THRESHOLD) {
            snappedX = 0;
            activeVerticalSnapLines.add(0);
        } else if (Math.abs((targetX + targetW) - width) < SNAP_THRESHOLD) {
            snappedX = width - targetW;
            activeVerticalSnapLines.add(width - 1);
        } else if (Math.abs((targetX + targetW / 2) - screenCenterX) < SNAP_THRESHOLD) {
            snappedX = screenCenterX - targetW / 2;
            activeVerticalSnapLines.add(screenCenterX);
        }

        if (Math.abs(targetY) < SNAP_THRESHOLD) {
            snappedY = 0;
            activeHorizontalSnapLines.add(0);
        } else if (Math.abs((targetY + targetH) - height) < SNAP_THRESHOLD) {
            snappedY = height - targetH;
            activeHorizontalSnapLines.add(height - 1);
        } else if (Math.abs((targetY + targetH / 2) - screenCenterY) < SNAP_THRESHOLD) {
            snappedY = screenCenterY - targetH / 2;
            activeHorizontalSnapLines.add(screenCenterY);
        }

        // 2. Magnetic Element-to-Element Snapping
        for (HudElement other : client.hudLayout().elements()) {
            if (other == draggingElement || !renderer.enabled(other.id())) continue;
            Dimension otherDim = elementDimensions.get(other.id());
            if (otherDim == null) continue;

            int ox = other.x();
            int oy = other.y();
            int ow = otherDim.width;
            int oh = otherDim.height;

            // Left to Left
            if (Math.abs(targetX - ox) < SNAP_THRESHOLD) {
                snappedX = ox;
                activeVerticalSnapLines.add(ox);
            }
            // Right to Right
            else if (Math.abs((targetX + targetW) - (ox + ow)) < SNAP_THRESHOLD) {
                snappedX = ox + ow - targetW;
                activeVerticalSnapLines.add(ox + ow);
            }
            // Left to Right
            else if (Math.abs(targetX - (ox + ow)) < SNAP_THRESHOLD) {
                snappedX = ox + ow;
                activeVerticalSnapLines.add(ox + ow);
            }

            // Top to Top
            if (Math.abs(targetY - oy) < SNAP_THRESHOLD) {
                snappedY = oy;
                activeHorizontalSnapLines.add(oy);
            }
            // Bottom to Bottom
            else if (Math.abs((targetY + targetH) - (oy + oh)) < SNAP_THRESHOLD) {
                snappedY = oy + oh - targetH;
                activeHorizontalSnapLines.add(oy + oh);
            }
            // Top to Bottom
            else if (Math.abs(targetY - (oy + oh)) < SNAP_THRESHOLD) {
                snappedY = oy + oh;
                activeHorizontalSnapLines.add(oy + oh);
            }
        }

        draggingElement.moveTo(snappedX, snappedY);
    }

    private void drawFloatingControlBar(HudElement element, int mouseX, int mouseY) {
        Object font = Mc189Compat.screenFontRenderer(this);
        Dimension dim = elementDimensions.get(element.id());
        int elementW = dim != null ? dim.width : 50;

        int barW = 220;
        int barH = 26;
        int barX = Math.max(8, Math.min(width - barW - 8, element.x() + (elementW - barW) / 2));
        int barY = element.y() - barH - 8;
        if (barY < 8) {
            barY = element.y() + (dim != null ? dim.height : 12) + 8;
        }

        // Background card
        Mc189Compat.drawRect(barX, barY, barX + barW, barY + barH, 0xF010141B);
        Mc189Compat.drawRect(barX, barY, barX + barW, barY + 1, ACCENT_COLOR);

        // Name
        String name = element.id();
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < name.length() - 1) {
            name = name.substring(lastDot + 1).toUpperCase(Locale.ENGLISH);
        }
        Mc189Compat.drawStringWithShadow(font, name, barX + 6, barY + 4, 0xFFFFFFFF);

        // Scale Control
        int scalePct = Math.round(element.scale() * 100.0F);
        String scaleText = "Scale: " + scalePct + "%";
        Mc189Compat.drawStringWithShadow(font, scaleText, barX + 80, barY + 4, 0xFF52BEEB);

        // Opacity Control
        int opacityPct = Math.round(element.opacity() * 100.0F);
        String opacityText = "Alpha: " + opacityPct + "%";
        Mc189Compat.drawStringWithShadow(font, opacityText, barX + 145, barY + 4, 0xFFA0AEC0);

        // Reset Button
        int btnX = barX + barW - 38;
        int btnY = barY + 3;
        boolean hoverReset = mouseX >= btnX && mouseX <= btnX + 34 && mouseY >= btnY && mouseY <= btnY + 18;
        Mc189Compat.drawRect(btnX, btnY, btnX + 34, btnY + 18, hoverReset ? 0xCCFF5555 : 0x44FF5555);
        Mc189Compat.drawStringWithShadow(font, "Reset", btnX + 4, btnY + 5, 0xFFFFFFFF);
    }

    private void drawHelpBar() {
        Object font = Mc189Compat.screenFontRenderer(this);
        String help = "[Drag] Move  |  [Scroll] Scale  |  [Shift+Scroll] Opacity  |  [R] Reset  |  [ESC] Save & Exit";
        int textW = Mc189Compat.stringWidth(font, help);
        int barX = (width - textW - 16) / 2;
        int barY = height - 22;

        Mc189Compat.drawRect(barX, barY, barX + textW + 16, barY + 18, 0xDD10141B);
        Mc189Compat.drawStringWithShadow(font, help, barX + 8, barY + 5, ACCENT_COLOR);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        // Check floating control bar buttons first
        if (selectedElement != null) {
            Dimension dim = elementDimensions.get(selectedElement.id());
            int elementW = dim != null ? dim.width : 50;
            int barW = 220;
            int barH = 26;
            int barX = Math.max(8, Math.min(width - barW - 8, selectedElement.x() + (elementW - barW) / 2));
            int barY = selectedElement.y() - barH - 8;
            if (barY < 8) {
                barY = selectedElement.y() + (dim != null ? dim.height : 12) + 8;
            }

            int btnX = barX + barW - 38;
            int btnY = barY + 3;
            if (mouseX >= btnX && mouseX <= btnX + 34 && mouseY >= btnY && mouseY <= btnY + 18) {
                selectedElement.setScale(1.0F);
                selectedElement.setOpacity(1.0F);
                calculateDimensions();
                return;
            }
        }

        // Check HUD elements
        for (HudElement element : client.hudLayout().elements()) {
            if (!renderer.enabled(element.id())) continue;
            Dimension dim = elementDimensions.get(element.id());
            if (dim == null) continue;

            if (mouseX >= element.x() - 2 && mouseX <= element.x() + dim.width + 2 &&
                mouseY >= element.y() - 2 && mouseY <= element.y() + dim.height + 2) {
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
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mc189Compat.mouseWheelDelta();
        if (dWheel != 0) {
            int mouseX = Mc189Compat.mouseX() * width / Mc189Compat.displayWidth(Mc189Compat.minecraft());
            int mouseY = height - Mc189Compat.mouseY() * height / Mc189Compat.displayHeight(Mc189Compat.minecraft()) - 1;

            HudElement target = selectedElement;
            if (target == null) {
                for (HudElement element : client.hudLayout().elements()) {
                    if (!renderer.enabled(element.id())) continue;
                    Dimension dim = elementDimensions.get(element.id());
                    if (dim != null && mouseX >= element.x() && mouseX <= element.x() + dim.width &&
                        mouseY >= element.y() && mouseY <= element.y() + dim.height) {
                        target = element;
                        break;
                    }
                }
            }

            if (target != null) {
                boolean shift = isShiftKeyDown();
                if (shift) {
                    // Adjust Opacity
                    float delta = dWheel > 0 ? 0.05F : -0.05F;
                    float newOpacity = Math.max(0.10F, Math.min(1.0F, target.opacity() + delta));
                    target.setOpacity(newOpacity);
                } else {
                    // Adjust Scale
                    float delta = dWheel > 0 ? 0.05F : -0.05F;
                    float newScale = Math.max(0.50F, Math.min(2.50F, target.scale() + delta));
                    target.setScale(newScale);
                    calculateDimensions();
                }
            }
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
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(null);
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
                calculateDimensions();
            } else if (keyCode == KEY_RBRACKET) {
                selectedElement.setOpacity(Math.min(1.00F, selectedElement.opacity() + 0.05F));
            } else if (keyCode == KEY_LBRACKET) {
                selectedElement.setOpacity(Math.max(0.10F, selectedElement.opacity() - 0.05F));
            }
        }
    }

    private void drawGrid() {
        int snap = 16;
        for (int x = 0; x < width; x += snap) {
            Mc189Compat.drawRect(x, 0, x + 1, height, 0x0DFFFFFF);
        }
        for (int y = 0; y < height; y += snap) {
            Mc189Compat.drawRect(0, y, width, y + 1, 0x0DFFFFFF);
        }
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (IOException ignored) {
        }
    }
}