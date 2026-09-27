package dev.aether.forge189.ui.pages;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherButton;
import dev.aether.forge189.ui.AetherMetrics;
import java.util.List;

/**
 * Cosmetics page – full wardrobe experience with equipped panel, search,
 * filterable grid, and preview area.
 */
public final class CosmeticsPage extends Page {

    private static final int EQUIPPED_PANEL_WIDTH = 180;
    private static final int CELL_SIZE = 80;
    private static final int CELL_GAP = 8;

    private final AetherButton importBtn = new AetherButton("Import File", 0, 0, 96, 22);
    private final AetherButton reloadBtn = new AetherButton("Reload", 0, 0, 80, 22);

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        // ===== Equipped Panel (left) =====
        int equipX = layout.listX;
        int equipY = layout.listY;
        int equipW = EQUIPPED_PANEL_WIDTH;
        int equipH = layout.listH;
        AetherUi.panel(equipX, equipY, equipX + equipW, equipY + equipH);
        AetherUi.text(font, "Equipped", equipX + 10, equipY + 10, AetherUi.TEXT_DISABLED);

        int curY = equipY + 36;
        for (CosmeticType type : CosmeticType.values()) {
            CosmeticAsset asset = screen.getClient().cosmetics().selectedFor(type);
            String name = asset == null ? "None" : asset.name();
            boolean isFav = asset != null && asset.favorite();

            AetherUi.text(font, type.name() + ":", equipX + 10, curY, AetherUi.TEXT_SECONDARY);
            curY += 16;
            AetherUi.text(font, name, equipX + 20, curY,
                         isFav ? AetherUi.ACCENT : AetherUi.TEXT_PRIMARY);
            if (isFav) AetherUi.drawStar(equipX + equipW - 20, curY - 4, AetherUi.STAR);
            curY += 22;
        }

        // ===== Search / Filters (top right) =====
        int rightX = equipX + equipW + 12;
        int rightY = layout.listY + 10;
        int rightW = layout.listW - equipW - 20;

        // Filter chips
        String[] filters = {"All", "Capes", "Animated", "Wings", "Hats", "Trails", "Favorites", "Equipped"};
        int chipY = rightY + 30;
        for (int i = 0; i < filters.length; i++) {
            boolean active = false;
            boolean hover = mouseX >= rightX + i * 70 && mouseX <= rightX + i * 70 + 64 &&
                            mouseY >= chipY && mouseY <= chipY + 18;
            int chipX = rightX + i * 70;
            int bg = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            Mc189Compat.drawRect(chipX, chipY, chipX + 64, chipY + 18, bg);
            AetherUi.text(font, filters[i], chipX + 8, chipY + 5,
                         active ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
        }

        // ===== Cosmetic Grid (right) =====
        int gridX = rightX;
        int gridY = chipY + 28;
        int gridW = rightW;
        int gridH = layout.listH - (gridY - layout.listY);

        List<CosmeticAsset> all = screen.getClient().cosmetics().all();
        int cols = Math.max(1, gridW / (CELL_SIZE + CELL_GAP));

        int idx = 0;
        for (CosmeticAsset asset : all) {
            int col = idx % cols;
            int row = idx / cols;
            int cx = gridX + col * (CELL_SIZE + CELL_GAP);
            int cy = gridY + row * (CELL_SIZE + CELL_GAP);

            boolean hover = mouseX >= cx && mouseX <= cx + CELL_SIZE &&
                            mouseY >= cy && mouseY <= cy + CELL_SIZE;
            boolean equipped = screen.getClient().cosmetics().effective(asset.type()) == asset;
            int bg = equipped ? AetherUi.withAlpha(AetherUi.ACCENT, 0x26)
                        : hover ? AetherUi.CARD_HOVER : AetherUi.CARD;
            AetherUi.roundRect(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE,
                               AetherMetrics.CORNER_RADIUS_SMALL, bg);

            String shortName = asset.name().length() > 12 ? asset.name().substring(0, 10) + ".." : asset.name();
            AetherUi.text(font, shortName, cx + 6, cy + CELL_SIZE - 16, AetherUi.TEXT_PRIMARY);

            AetherUi.drawBadge(asset.type().name(), cx + 6, cy + CELL_SIZE - 32, AetherUi.ACCENT);

            if (asset.favorite()) AetherUi.drawStar(cx + CELL_SIZE - 14, cy + 4, AetherUi.STAR);

            if (equipped) {
                AetherUi.drawRect(cx + CELL_SIZE - 18, cy + 4, cx + CELL_SIZE - 4, cy + 18,
                                  AetherUi.ACCENT);
            }

            idx++;
        }

        // ===== Buttons (bottom right) =====
        int btnY = layout.listY + layout.listH - 30;
        importBtn.render(font, mouseX, mouseY, rightX, btnY);
        reloadBtn.render(font, mouseX, mouseY, rightX + 106, btnY);
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
    }
}