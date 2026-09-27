package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.screenshot.ScreenshotInfo;
import dev.aether.forge189.AetherUi;
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
        AetherUi.text(font, "SCREENSHOTS", layout.listX + 8, y, AetherUi.TEXT_DISABLED);
        y += 30;

        captureBtn.render(font, mouseX, mouseY, layout.listX + layout.listW - 110, y);
        y += 34;

        List<ScreenshotInfo> shots = screen.getClient().screenshots().store().list();
        int cols = Math.max(1, (layout.listW - 20) / (THUMB_W + 8));
        int startX = layout.listX + 8;

        int idx = 0;
        for (ScreenshotInfo shot : shots) {
            int col = idx % cols;
            int row = idx / cols;
            int x = startX + col * (THUMB_W + 8);
            int ty = y + row * (THUMB_H + 8);

            AetherUi.roundRect(x, ty, x + THUMB_W, ty + THUMB_H,
                               AetherMetrics.CORNER_RADIUS_SMALL, AetherUi.PANEL);

            String shortName = shot.name().length() > 20
                ? shot.name().substring(0, 18) + ".." : shot.name();
            AetherUi.text(font, shortName, x + 4, ty + 4, AetherUi.TEXT_PRIMARY);

            AetherUi.text(font, shot.timestamp() + " · " + (shot.sizeBytes() / 1024) + " KB",
                         x + 4, ty + 18, AetherUi.TEXT_DISABLED);

            int actionX = x + THUMB_W - 60;
            boolean openHover = mouseX >= actionX && mouseX <= actionX + 56 &&
                                mouseY >= ty + THUMB_H - 24 && mouseY <= ty + THUMB_H - 6;
            int openBg = openHover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(actionX, ty + THUMB_H - 24, actionX + 56, ty + THUMB_H - 6, openBg);
            AetherUi.text(font, "OPEN", actionX + 10, ty + THUMB_H - 18,
                         openHover ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);

            int delX = actionX - 42;
            boolean delHover = mouseX >= delX && mouseX <= delX + 38 &&
                               mouseY >= ty + THUMB_H - 24 && mouseY <= ty + THUMB_H - 6;
            int delBg = delHover ? AetherUi.withAlpha(AetherUi.WARN, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(delX, ty + THUMB_H - 24, delX + 38, ty + THUMB_H - 6, delBg);
            AetherUi.text(font, "DEL", delX + 8, ty + THUMB_H - 18,
                         delHover ? AetherUi.WARN : AetherUi.TEXT_DISABLED);

            idx++;
        }
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
    }
}