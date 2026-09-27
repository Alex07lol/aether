package dev.aether.forge189.ui;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import java.util.List;

public final class AetherToastRenderer {
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 8;
    private static final int WIDTH = 260;
    private static final int HEIGHT = 20;

    public static void render(Object font, List<AetherToast> toasts, int screenWidth, int screenHeight) {
        int y = screenHeight - PADDING_Y - HEIGHT;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            AetherToast toast = toasts.get(i);
            float vis = toast.visibility();
            int alpha = (int) (vis * 255);
            int bg = AetherUi.withAlpha(toast.accent(), alpha);
            int txt = AetherUi.readableOn(bg);
            int x = screenWidth - PADDING_X - WIDTH;
            int slide = (int) ((1f - vis) * 30);
            AetherUi.roundRect(x + slide, y, x + WIDTH + slide, y + HEIGHT, 4, bg);
            AetherUi.text(font, toast.text(), x + slide + 8, y + 6, txt);
            y -= HEIGHT + 4;
        }
    }
}