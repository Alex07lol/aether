package dev.aether.forge189.ui;

import dev.aether.forge189.AetherUi;

public enum AetherIcon {
    SEARCH,
    HOME,
    ACCOUNT,
    EDIT,
    HUD,
    GAMEPLAY,
    RENDER,
    PERFORMANCE,
    COSMETICS,
    CAPE,
    WING,
    HALO,
    HAT,
    TRAIL,
    FAVORITE,
    BACK,
    CLOSE,
    EXPAND,
    COLLAPSE;

    public void draw(int x, int y, int color) {
        switch (this) {
            case SEARCH:
                AetherUi.drawSearchGlyph(x, y, color);
                break;
            case HOME:
                AetherUi.drawHomeIcon(x, y, color);
                break;
            case ACCOUNT:
                AetherUi.drawAccountIcon(x, y, color);
                break;
            case EDIT:
                AetherUi.drawEditIcon(x, y, color);
                break;
            case HUD:
                AetherUi.drawHudIcon(x, y, color);
                break;
            case GAMEPLAY:
                AetherUi.drawGameplayIcon(x, y, color);
                break;
            case RENDER:
                AetherUi.drawRenderIcon(x, y, color);
                break;
            case PERFORMANCE:
                AetherUi.drawPerformanceIcon(x, y, color);
                break;
            case COSMETICS:
                AetherUi.drawCosmeticsIcon(x, y, color);
                break;
            case CAPE:
                AetherUi.drawMark(x, y + 2, color);
                break;
            case WING:
                AetherUi.drawStar(x, y, color);
                AetherUi.drawStar(x + 6, y, color);
                break;
            case HALO:
                AetherUi.drawCircle(x + 4, y + 4, 6, color);
                break;
            case HAT:
                AetherUi.drawRect(x, y, x + 10, y + 6, color);
                break;
            case TRAIL:
                for (int i = 0; i < 3; i++) {
                    AetherUi.drawRect(x + i * 4, y, x + i * 4 + 3, y + 3, color);
                }
                break;
            case FAVORITE:
                AetherUi.drawStar(x, y, color);
                break;
            case BACK:
                AetherUi.drawChevron(x + 8, y + 4, -1, color);
                break;
            case CLOSE:
                AetherUi.drawRect(x, y, x + 8, y + 1, color);
                AetherUi.drawRect(x + 7, y, x + 8, y + 8, color);
                AetherUi.drawRect(x, y + 7, x + 8, y + 8, color);
                AetherUi.drawRect(x, y, x + 1, y + 8, color);
                break;
            case EXPAND:
                AetherUi.drawChevron(x, y, 1, color);
                break;
            case COLLAPSE:
                AetherUi.drawChevron(x, y, -1, color);
                break;
        }
    }
}