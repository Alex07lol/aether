package dev.aether.forge189.ui;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

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
                drawSearchIcon(x, y, color);
                break;
            case HOME:
                drawHomeIcon(x, y, color);
                break;
            case ACCOUNT:
                drawAccountIcon(x, y, color);
                break;
            case EDIT:
                drawEditIcon(x, y, color);
                break;
            case HUD:
                drawHudIcon(x, y, color);
                break;
            case GAMEPLAY:
                drawGameplayIcon(x, y, color);
                break;
            case RENDER:
                drawRenderIcon(x, y, color);
                break;
            case PERFORMANCE:
                drawPerformanceIcon(x, y, color);
                break;
            case COSMETICS:
                drawCosmeticsIcon(x, y, color);
                break;
            case CAPE:
                drawCapeIcon(x, y, color);
                break;
            case WING:
                drawWingIcon(x, y, color);
                break;
            case HALO:
                drawHaloIcon(x, y, color);
                break;
            case HAT:
                drawHatIcon(x, y, color);
                break;
            case TRAIL:
                drawTrailIcon(x, y, color);
                break;
            case FAVORITE:
                drawStarIcon(x, y, color);
                break;
            case BACK:
                drawBackIcon(x, y, color);
                break;
            case CLOSE:
                drawCloseIcon(x, y, color);
                break;
            case EXPAND:
                drawExpandIcon(x, y, color);
                break;
            case COLLAPSE:
                drawCollapseIcon(x, y, color);
                break;
        }
    }

    private static void drawSearchIcon(int x, int y, int color) {
        // Magnifying glass
        AetherUi.drawCircle(x + 4, y + 4, 3, color);
        Mc189Compat.drawRect(x + 3, y + 4, x + 5, y + 5, AetherUi.SEARCH); // Clear inside
        Mc189Compat.drawRect(x + 7, y + 7, x + 9, y + 8, color);
        Mc189Compat.drawRect(x + 8, y + 8, x + 10, y + 9, color);
    }

    private static void drawHomeIcon(int x, int y, int color) {
        // House icon
        Mc189Compat.drawRect(x + 2, y + 5, x + 10, y + 11, color); // base
        Mc189Compat.drawRect(x + 1, y + 4, x + 11, y + 5, color); // roof slant
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color); // roof top
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, AetherUi.PANEL); // door
    }

    private static void drawAccountIcon(int x, int y, int color) {
        // Person icon
        AetherUi.drawCircle(x + 6, y + 4, 2, color); // head
        Mc189Compat.drawRect(x + 2, y + 7, x + 10, y + 8, color); // shoulders
        Mc189Compat.drawRect(x + 4, y + 8, x + 8, y + 11, color); // body
    }

    private static void drawEditIcon(int x, int y, int color) {
        // Pencil icon
        Mc189Compat.drawRect(x + 4, y + 2, x + 8, y + 9, color); // body
        Mc189Compat.drawRect(x + 3, y + 9, x + 9, y + 10, color); // tip base
        Mc189Compat.drawRect(x + 5, y + 10, x + 7, y + 11, color); // tip
        Mc189Compat.drawRect(x + 4, y + 1, x + 8, y + 2, color); // eraser
    }

    private static void drawHudIcon(int x, int y, int color) {
        // Crosshair icon
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 3, color);
        Mc189Compat.drawRect(x + 2, y + 9, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 2, y + 2, x + 3, y + 10, color);
        Mc189Compat.drawRect(x + 9, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 5, color);
    }

    private static void drawGameplayIcon(int x, int y, int color) {
        // Sword icon
        Mc189Compat.drawRect(x + 5, y + 2, x + 7, y + 10, color); // hilt
        Mc189Compat.drawRect(x + 3, y + 4, x + 9, y + 5, color); // guard
        Mc189Compat.drawRect(x + 6, y + 1, x + 7, y + 2, color); // tip
    }

    private static void drawRenderIcon(int x, int y, int color) {
        // Eye icon
        AetherUi.drawCircle(x + 6, y + 6, 4, color);
        AetherUi.drawCircle(x + 6, y + 6, 3, AetherUi.PANEL);
        AetherUi.drawCircle(x + 6, y + 6, 1, color);
    }

    private static void drawPerformanceIcon(int x, int y, int color) {
        // Bolt icon
        Mc189Compat.drawRect(x + 5, y + 1, x + 7, y + 4, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 8, y + 7, color);
        Mc189Compat.drawRect(x + 5, y + 7, x + 7, y + 11, color);
    }

    private static void drawCosmeticsIcon(int x, int y, int color) {
        // Shirt icon
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 10, color);
        Mc189Compat.drawRect(x + 4, y, x + 8, y + 2, color);
        Mc189Compat.drawRect(x, y + 2, x + 2, y + 5, color);
        Mc189Compat.drawRect(x + 10, y + 2, x + 12, y + 5, color);
    }

    private static void drawCapeIcon(int x, int y, int color) {
        // Cape icon
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 6, color);
        Mc189Compat.drawRect(x + 4, y + 1, x + 8, y + 7, color);
        Mc189Compat.drawRect(x + 6, y, x + 6, y + 8, color);
    }

    private static void drawWingIcon(int x, int y, int color) {
        // Wing icon
        Mc189Compat.drawRect(x + 4, y + 1, x + 10, y + 5, color);
        Mc189Compat.drawRect(x + 4, y + 5, x + 10, y + 9, color);
        Mc189Compat.drawRect(x + 1, y + 3, x + 5, y + 7, color);
        Mc189Compat.drawRect(x + 1, y + 7, x + 5, y + 11, color);
    }

    private static void drawHaloIcon(int x, int y, int color) {
        // Halo icon
        AetherUi.drawCircle(x + 6, y + 6, 4, color);
        AetherUi.drawCircle(x + 6, y + 6, 2, AetherUi.PANEL);
    }

    private static void drawHatIcon(int x, int y, int color) {
        // Hat icon
        Mc189Compat.drawRect(x + 2, y + 2, x + 10, y + 6, color);
        Mc189Compat.drawRect(x + 4, y, x + 8, y + 2, color);
        Mc189Compat.drawRect(x + 6, y + 6, x + 8, y + 8, color);
    }

    private static void drawTrailIcon(int x, int y, int color) {
        // Trail icon
        for (int i = 0; i < 3; i++) {
            Mc189Compat.drawRect(x + i * 4, y, x + i * 4 + 3, y + 3, color);
        }
    }

    private static void drawStarIcon(int x, int y, int color) {
        // Star icon
        Mc189Compat.drawRect(x + 4, y, x + 5, y + 8, color);
        Mc189Compat.drawRect(x, y + 2, x + 9, y + 4, color);
        Mc189Compat.drawRect(x + 1, y + 4, x + 8, y + 6, color);
        Mc189Compat.drawRect(x + 2, y + 6, x + 7, y + 8, color);
    }

    private static void drawBackIcon(int x, int y, int color) {
        // Back arrow
        Mc189Compat.drawRect(x + 8, y + 4, x + 10, y + 6, color);
        Mc189Compat.drawRect(x + 6, y + 3, x + 8, y + 7, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 6, y + 6, color);
    }

    private static void drawCloseIcon(int x, int y, int color) {
        // Close icon
        Mc189Compat.drawRect(x, y, x + 8, y + 1, color);
        Mc189Compat.drawRect(x + 7, y, x + 8, y + 8, color);
        Mc189Compat.drawRect(x, y + 7, x + 8, y + 8, color);
        Mc189Compat.drawRect(x, y, x + 1, y + 8, color);
    }

    private static void drawExpandIcon(int x, int y, int color) {
        // Expand arrow
        Mc189Compat.drawRect(x + 4, y + 1, x + 6, y + 3, color);
        Mc189Compat.drawRect(x + 3, y + 2, x + 7, y + 4, color);
        Mc189Compat.drawRect(x + 2, y + 3, x + 8, y + 5, color);
    }

    private static void drawCollapseIcon(int x, int y, int color) {
        // Collapse arrow
        Mc189Compat.drawRect(x + 4, y + 3, x + 6, y + 5, color);
        Mc189Compat.drawRect(x + 3, y + 2, x + 7, y + 4, color);
        Mc189Compat.drawRect(x + 2, y + 1, x + 8, y + 3, color);
    }
}