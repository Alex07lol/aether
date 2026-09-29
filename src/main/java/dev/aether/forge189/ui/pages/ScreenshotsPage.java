package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.screenshot.ScreenshotInfo;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.font.GlyphPageFontRenderer;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherButton;
import dev.aether.forge189.ui.AetherMetrics;
import java.util.List;

/**
 * Screenshots page – gallery grid with thumbnails.
 */
public final class ScreenshotsPage extends Page {

    private final AetherButton captureBtn = new AetherButton("Capture", 0, 0, 96, 22);
    private static final int THUMB_W = 120;
    private static final int THUMB_H = 80;

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        int y = layout.listY + 8;
        AetherUi.textSmooth(font, "SCREENSHOTS", layout.listX + 8, y, AetherUi.TEXT_DISABLED);
        y += 30;

        captureBtn.render(font, mouseX, mouseY, layout.listX + layout.listW - 110, y);
        y += 34;

        List<ScreenshotInfo> shots = screen.getClient().screenshots().store().list();

        if (shots.isEmpty()) {
            // Empty state
            int centerX = layout.listX + layout.listW / 2;
            int centerY = y + 80;
            AetherUi.textSmooth(font, "No screenshots yet", centerX - 60, centerY, AetherUi.TEXT_SECONDARY);
            AetherUi.textSmooth(font, "Press the Capture button or use the Screenshot module", centerX - 130, centerY + 16, AetherUi.TEXT_DISABLED);
            return;
        }

        int cols = Math.max(1, (layout.listW - 20) / (THUMB_W + 8));
        int startX = layout.listX + 8;

        int idx = 0;
        for (ScreenshotInfo shot : shots) {
            int col = idx % cols;
            int row = idx / cols;
            int x = startX + col * (THUMB_W + 8);
            int ty = y + row * (THUMB_H + 8);

            // Card background with hover effect
            boolean cardHover = mouseX >= x && mouseX <= x + THUMB_W &&
                                mouseY >= ty && mouseY <= ty + THUMB_H;
            int bg = cardHover ? AetherUi.lerpColor(AetherUi.PANEL, AetherUi.ACCENT, 0.04f) : AetherUi.PANEL;
            AetherUi.roundRect(x, ty, x + THUMB_W, ty + THUMB_H,
                               AetherMetrics.CORNER_RADIUS_SMALL, bg);

            // Border accent
            if (cardHover) {
                AetherUi.outline(x, ty, x + THUMB_W, ty + THUMB_H, AetherUi.withAlpha(AetherUi.ACCENT, 0x88));
            }

            String shortName = shot.name().length() > 20
                ? shot.name().substring(0, 18) + ".." : shot.name();
            AetherUi.textSmooth(font, shortName, x + 4, ty + 4, AetherUi.TEXT_PRIMARY);

            AetherUi.textSmooth(font, shot.timestamp() + " · " + (shot.sizeBytes() / 1024) + " KB",
                         x + 4, ty + 18, AetherUi.TEXT_DISABLED);

            // OPEN button
            int actionX = x + THUMB_W - 60;
            int btnY = ty + THUMB_H - 24;
            boolean openHover = mouseX >= actionX && mouseX <= actionX + 56 &&
                                mouseY >= btnY && mouseY <= btnY + 18;
            int openBg = openHover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(actionX, btnY, actionX + 56, btnY + 18, openBg);
            AetherUi.textSmooth(font, "OPEN", actionX + 10, btnY + 6,
                         openHover ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);

            // DEL button
            int delX = actionX - 42;
            boolean delHover = mouseX >= delX && mouseX <= delX + 38 &&
                               mouseY >= btnY && mouseY <= btnY + 18;
            int delBg = delHover ? AetherUi.withAlpha(AetherUi.WARN, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(delX, btnY, delX + 38, btnY + 18, delBg);
            AetherUi.textSmooth(font, "DEL", delX + 8, btnY + 6,
                         delHover ? AetherUi.WARN : AetherUi.TEXT_DISABLED);

            idx++;
        }
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        // Handle Capture button
        int btnX = layout.listX + layout.listW - 110;
        int btnY = layout.listY + 8 + 30;
        if (mouseX >= btnX && mouseX <= btnX + 96 &&
            mouseY >= btnY && mouseY <= btnY + 22) {
            screen.takeScreenshot();
            return;
        }

        List<ScreenshotInfo> shots = screen.getClient().screenshots().store().list();
        int cols = Math.max(1, (layout.listW - 20) / (THUMB_W + 8));
        int startX = layout.listX + 8;
        int gridY = layout.listY + 8 + 30 + 34;

        int idx = 0;
        for (ScreenshotInfo shot : shots) {
            int col = idx % cols;
            int row = idx / cols;
            int x = startX + col * (THUMB_W + 8);
            int ty = gridY + row * (THUMB_H + 8);

            // OPEN button hit test
            int actionX = x + THUMB_W - 60;
            int buttonY = ty + THUMB_H - 24;
            if (mouseX >= actionX && mouseX <= actionX + 56 &&
                mouseY >= buttonY && mouseY <= buttonY + 18) {
                openScreenshot(screen, shot);
                return;
            }

            // DEL button hit test
            int delX = actionX - 42;
            if (mouseX >= delX && mouseX <= delX + 38 &&
                mouseY >= buttonY && mouseY <= buttonY + 18) {
                if (screen.getClient().screenshots().store().delete(shot.name())) {
                    screen.writeLastChange("Deleted screenshot: " + shot.name());
                    screen.getToasts().push("Deleted " + shot.name(), AetherUi.WARN);
                }
                return;
            }

            idx++;
        }
    }

    private void openScreenshot(AetherClickGuiScreen screen, ScreenshotInfo shot) {
        try {
            java.awt.Desktop.getDesktop().open(screen.getClient().screenshots().store().safeResolve(shot.name()).toFile());
            screen.writeLastChange("Opened screenshot: " + shot.name());
        } catch (Exception e) {
            screen.writeLastChange("Failed to open screenshot: " + e.getMessage());
            screen.getToasts().push("Failed to open screenshot", AetherUi.WARN);
        }
    }
}