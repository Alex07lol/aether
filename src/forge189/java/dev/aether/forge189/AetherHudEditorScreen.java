package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.hud.HudElement;
import net.minecraft.client.gui.GuiScreen;

import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class AetherHudEditorScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final AetherClient client;
    private final ForgeHudRenderer renderer;
    private final Map<String, Dimension> elementDimensions = new HashMap<>();
    private HudElement draggingElement;
    private int dragOffsetX;
    private int dragOffsetY;

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
        for (HudElement element : getAllElements()) {
            if (renderer.enabled(element.id())) {
                Dimension dim = renderer.getDimensions(element.id(), font, mc);
                elementDimensions.put(element.id(), dim);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGrid();
        renderer.render();

        for (HudElement element : getAllElements()) {
            if (!renderer.enabled(element.id())) {
                continue;
            }
            Dimension dim = elementDimensions.get(element.id());
            if (dim == null) continue;

            int x1 = element.x() - 2;
            int y1 = element.y() - 2;
            int x2 = element.x() + dim.width + 2;
            int y2 = element.y() + dim.height + 2;

            int color = 0x55FFFFFF;
            if (draggingElement == element) {
                color = 0xAA387DFF; // Aether accent color
            } else if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                color = 0x88FFFFFF;
            }
            Mc189Compat.drawRect(x1, y1, x2, y1 + 1, color); // Top
            Mc189Compat.drawRect(x1, y2 - 1, x2, y2, color); // Bottom
            Mc189Compat.drawRect(x1, y1, x1 + 1, y2, color); // Left
            Mc189Compat.drawRect(x2 - 1, y1, x2, y2, color); // Right
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        for (HudElement element : getAllElements()) {
            if (!renderer.enabled(element.id())) continue;
            Dimension dim = elementDimensions.get(element.id());
            if (dim == null) continue;

            if (mouseX >= element.x() && mouseX <= element.x() + dim.width &&
                mouseY >= element.y() && mouseY <= element.y() + dim.height) {
                draggingElement = element;
                dragOffsetX = mouseX - element.x();
                dragOffsetY = mouseY - element.y();
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingElement != null) {
            int snap = getSnap();
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            
            try {
                // This reflection is necessary because HudElement is part of the core API,
                // and we want to avoid modifying it directly for this feature.
                Field xField = HudElement.class.getDeclaredField("x");
                xField.setAccessible(true);
                xField.setInt(draggingElement, Math.round((float) newX / snap) * snap);

                Field yField = HudElement.class.getDeclaredField("y");
                yField.setAccessible(true);
                yField.setInt(draggingElement, Math.round((float) newY / snap) * snap);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // Fail silently in the editor if reflection fails.
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
        }
    }

    private void drawGrid() {
        int snap = getSnap();
        for (int x = 0; x < width; x += snap) {
            Mc189Compat.drawRect(x, 0, x + 1, height, 0x1AFFFFFF);
        }
        for (int y = 0; y < height; y += snap) {
            Mc189Compat.drawRect(0, y, width, y + 1, 0x1AFFFFFF);
        }
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Collection<HudElement> getAllElements() {
        try {
            Field elementsField = client.hudLayout().getClass().getDeclaredField("elements");
            elementsField.setAccessible(true);
            Map<String, HudElement> elements = (Map<String, HudElement>) elementsField.get(client.hudLayout());
            return elements.values();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return java.util.Collections.emptyList();
        }
    }

    private int getSnap() {
        try {
            Field snapField = client.hudLayout().getClass().getDeclaredField("snap");
            snapField.setAccessible(true);
            return snapField.getInt(client.hudLayout());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return 4; // Fallback to default
        }
    }
}