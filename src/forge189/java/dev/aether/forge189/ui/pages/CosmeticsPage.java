package dev.aether.forge189.ui.pages;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.CosmeticValidationResult;
import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherButton;
import dev.aether.forge189.ui.components.AetherSearchBox;
import dev.aether.forge189.ui.AetherMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cosmetics page - full wardrobe experience with search, filterable grid, and preview.
 */
public final class CosmeticsPage extends Page {

    private static final int CELL_SIZE = 80;
    private static final int CELL_GAP = 8;
    private static final int SEARCH_HEIGHT = 24;
    private static final int FILTER_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 26;

    private final AetherButton importBtn = new AetherButton("Import", 0, 0, 80, 22);
    private final AetherButton reloadBtn = new AetherButton("Reload", 0, 0, 80, 22);
    private final AetherButton equipBtn = new AetherButton("Equip", 0, 0, 80, 22);
    private final AetherButton unequipBtn = new AetherButton("Unequip", 0, 0, 80, 22);
    private final AetherSearchBox searchBox = new AetherSearchBox();

    private CosmeticFilter currentFilter = CosmeticFilter.ALL;
    private String selectedId = null;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean searchFocused = false;
    private final StringBuilder searchBuffer = new StringBuilder();

    // Caches
    private final Map<String, BufferedImage> thumbCache = new HashMap<>();
    private final Set<String> unreadableIds = new HashSet<>();

    public enum CosmeticFilter {
        ALL, CAPES, ANIMATED, FAVORITES, EQUIPPED
    }

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        int gx = layout.listX;
        int gy = layout.listY;
        int gw = layout.listW;
        int gh = layout.listH;

        // Search box
        searchBox.render(font, gx + 8, gy);
        searchBox.setQuery(searchQuery, searchFocused);
        int controlY = gy + SEARCH_HEIGHT + 4;

        // Filter chips
        drawFilterChips(screen, font, gx, controlY, gw, mouseX, mouseY);
        int gridStartY = controlY + FILTER_HEIGHT + 8;
        int gridH = gh - (gridStartY - gy);
        if (gridH < 40) gridH = 40;

        // Grid
        drawGrid(screen, font, mouseX, mouseY, layout, gx, gridStartY, gw, gridH);

        // Bottom buttons
        int btnY = gy + gh - BUTTON_HEIGHT - 6;
        importBtn.render(font, mouseX, mouseY, gx, btnY);
        reloadBtn.render(font, mouseX, mouseY, gx + 88, btnY);
        equipBtn.render(font, mouseX, mouseY, gx, btnY + BUTTON_HEIGHT + 4);
        unequipBtn.render(font, mouseX, mouseY, gx + 88, btnY + BUTTON_HEIGHT + 4);
    }

    private void drawFilterChips(AetherClickGuiScreen screen, Object font,
                                  int gx, int chipY, int gw, int mx, int my) {
        CosmeticFilter[] values = {CosmeticFilter.ALL, CosmeticFilter.CAPES,
                                   CosmeticFilter.ANIMATED, CosmeticFilter.FAVORITES, CosmeticFilter.EQUIPPED};
        String[] labels = {"All", "Capes", "Animated", "Favorites", "Equipped"};
        int cx = gx + 8;
        int cw = 72;
        for (int i = 0; i < labels.length; i++) {
            boolean active = values[i] == currentFilter;
            boolean hover = mx >= cx && mx <= cx + cw && my >= chipY && my <= chipY + FILTER_HEIGHT;
            int bg = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            Mc189Compat.drawRect(cx, chipY, cx + cw, chipY + FILTER_HEIGHT, bg);
            AetherUi.text(font, labels[i], cx + 8, chipY + 5,
                         active ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
            cx += cw + 4;
        }
    }

    private void drawGrid(AetherClickGuiScreen screen, Object font,
                          int mx, int my, Layout layout, int gx, int gy, int gw, int gh) {
        List<CosmeticAsset> all = screen.getClient().cosmetics().all();
        List<CosmeticAsset> filtered = filterAssets(all, screen);

        int cols = Math.max(1, gw / (CELL_SIZE + CELL_GAP));
        int rows = (int) Math.ceil((double) filtered.size() / cols);
        int totalH = rows * (CELL_SIZE + CELL_GAP);
        maxScroll = Math.max(0, totalH - gh + 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        if (filtered.isEmpty()) {
            int cx = gx + gw / 2;
            int cy = gy + gh / 2;
            AetherUi.text(font, "No cosmetics found", cx - 50, cy - 8, AetherUi.TEXT_SECONDARY);
            AetherUi.text(font, "Import a PNG cape to get started", cx - 65, cy + 8, AetherUi.TEXT_DISABLED);
            return;
        }

        int startIdx = (scrollOffset / (CELL_SIZE + CELL_GAP)) * cols;
        int visibleRows = (gh + CELL_SIZE + CELL_GAP - 1) / (CELL_SIZE + CELL_GAP);
        int endIdx = Math.min(startIdx + cols * visibleRows, filtered.size());

        for (int idx = startIdx; idx < endIdx; idx++) {
            CosmeticAsset asset = filtered.get(idx);
            int col = idx % cols;
            int row = idx / cols;
            int cx = gx + 8 + col * (CELL_SIZE + CELL_GAP);
            int cy = gy + row * (CELL_SIZE + CELL_GAP) - scrollOffset;

            if (cy + CELL_SIZE < gy || cy > gy + gh) continue; // culling

            boolean isSelected = selectedId != null && selectedId.equals(asset.id());
            boolean isFav = asset.favorite();
            CosmeticAsset equipped = screen.getClient().cosmetics().effective(asset.type());
            boolean isEquipped = equipped == asset;
            boolean hover = mx >= cx && mx <= cx + CELL_SIZE && my >= cy && my <= cy + CELL_SIZE;

            int bg = isSelected ? AetherUi.withAlpha(AetherUi.ACCENT, 0x26)
                    : isEquipped ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x1A)
                    : hover ? AetherUi.CARD_HOVER : AetherUi.CARD;
            Mc189Compat.drawRect(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, bg);
            if (isSelected) {
                Mc189Compat.drawRect(cx, cy, cx + CELL_SIZE, cy + 2, AetherUi.ACCENT);
                Mc189Compat.drawRect(cx, cy + CELL_SIZE - 2, cx + CELL_SIZE, cy + CELL_SIZE, AetherUi.ACCENT);
            }

            // Thumbnail or placeholder
            BufferedImage thumb = getThumb(asset, screen);
            if (thumb != null) {
                // Simple scaled draw - just use a colored rect as placeholder
                // For real rendering, would need GL texture upload
                int phColor = AetherUi.withAlpha(asset.primaryColor(), 0x66);
                Mc189Compat.drawRect(cx + 4, cy + 4, cx + CELL_SIZE - 4, cy + CELL_SIZE - 20, phColor);
            } else {
                int phColor = AetherUi.withAlpha(asset.primaryColor(), 0x44);
                Mc189Compat.drawRect(cx + 4, cy + 4, cx + CELL_SIZE - 4, cy + CELL_SIZE - 20, phColor);
            }

            // Name
            String shortName = asset.name().length() > 10
                ? asset.name().substring(0, 8) + ".." : asset.name();
            AetherUi.text(font, shortName, cx + 4, cy + CELL_SIZE - 16, AetherUi.TEXT_PRIMARY);

            // Type badge
            AetherUi.drawBadge(asset.type().name(), cx + 4, cy + 4, AetherUi.ACCENT);

            // Star for favorite
            if (isFav) AetherUi.drawStar(cx + CELL_SIZE - 16, cy + 6, AetherUi.STAR);

            // Equipped indicator
            if (isEquipped) {
                Mc189Compat.drawRect(cx + CELL_SIZE - 16, cy + CELL_SIZE - 14,
                                     cx + CELL_SIZE - 6, cy + CELL_SIZE - 4, AetherUi.ACCENT_ON);
            }
        }
    }

    private List<CosmeticAsset> filterAssets(List<CosmeticAsset> all, AetherClickGuiScreen screen) {
        List<CosmeticAsset> result = new ArrayList<>();
        for (CosmeticAsset a : all) {
            if (!searchQuery.isEmpty() && !a.name().toLowerCase().contains(searchQuery.toLowerCase())) {
                continue;
            }
            switch (currentFilter) {
                case CAPES:
                    if (a.type() != CosmeticType.STATIC_CAPE && a.type() != CosmeticType.ANIMATED_CAPE) continue;
                    break;
                case ANIMATED:
                    if (!a.animated()) continue;
                    break;
                case FAVORITES:
                    if (!a.favorite()) continue;
                    break;
                case EQUIPPED:
                    CosmeticAsset eq = screen.getClient().cosmetics().effective(a.type());
                    if (eq != a) continue;
                    break;
                default:
                    break;
            }
            result.add(a);
        }
        return result;
    }

    private BufferedImage getThumb(CosmeticAsset asset, AetherClickGuiScreen screen) {
        if (unreadableIds.contains(asset.id())) return null;
        BufferedImage img = thumbCache.get(asset.id());
        if (img != null) return img;
        try {
            img = screen.getClient().cosmetics().getPreview(asset.id());
            if (img != null) thumbCache.put(asset.id(), img);
        } catch (IOException e) {
            unreadableIds.add(asset.id());
        }
        return img;
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                            int mx, int my, int button) {
        int gx = layout.listX;
        int gy = layout.listY;

        // Search box click
        if (mx >= gx + 8 && mx <= gx + layout.listW - 8 && my >= gy && my <= gy + SEARCH_HEIGHT) {
            searchFocused = true;
            return;
        }
        if (my < gy) searchFocused = false;

        // Filter chips
        int chipY = gy + SEARCH_HEIGHT + 4;
        CosmeticFilter[] values = {CosmeticFilter.ALL, CosmeticFilter.CAPES,
                                   CosmeticFilter.ANIMATED, CosmeticFilter.FAVORITES, CosmeticFilter.EQUIPPED};
        int cx = gx + 8;
        int cw = 72;
        for (int i = 0; i < values.length; i++) {
            if (mx >= cx && mx <= cx + cw && my >= chipY && my <= chipY + FILTER_HEIGHT) {
                currentFilter = values[i];
                selectedId = null;
                scrollOffset = 0;
                return;
            }
            cx += cw + 4;
        }

        // Grid cells
        int gridStartY = chipY + FILTER_HEIGHT + 8;
        List<CosmeticAsset> filtered = filterAssets(screen.getClient().cosmetics().all(), screen);
        int cols = Math.max(1, layout.listW / (CELL_SIZE + CELL_GAP));
        int startIdx = (scrollOffset / (CELL_SIZE + CELL_GAP)) * cols;
        int visibleRows = (layout.listH - (gridStartY - gy) + CELL_SIZE + CELL_GAP - 1) / (CELL_SIZE + CELL_GAP);
        int endIdx = Math.min(startIdx + cols * visibleRows, filtered.size());

        for (int idx = startIdx; idx < endIdx; idx++) {
            CosmeticAsset asset = filtered.get(idx);
            int col = idx % cols;
            int row = idx / cols;
            int cx2 = gx + 8 + col * (CELL_SIZE + CELL_GAP);
            int cy = gridStartY + row * (CELL_SIZE + CELL_GAP) - scrollOffset;

            if (mx >= cx2 && mx <= cx2 + CELL_SIZE && my >= cy && my <= cy + CELL_SIZE) {
                // Star click (favorite)
                if (mx >= cx2 + CELL_SIZE - 16 && my <= cy + 16) {
                    screen.getClient().cosmetics().toggleFavorite(asset.id());
                    String msg = asset.favorite() ? "Favorited " + asset.name() : "Unfavorited " + asset.name();
                    screen.writeLastChange(msg);
                    screen.getToasts().push(msg, asset.favorite() ? AetherUi.STAR : AetherUi.TEXT_DISABLED);
                    return;
                }
                selectedId = asset.id();
                return;
            }
        }

        // Buttons
        int btnY = gy + layout.listH - BUTTON_HEIGHT - 6;
        if (importBtn.contains(mx, my)) {
            importCape(screen);
            return;
        }
        if (reloadBtn.contains(mx, my)) {
            reloadLibrary(screen);
            return;
        }
        if (equipBtn.contains(mx, my) && selectedId != null) {
            CosmeticAsset asset = findAsset(screen, selectedId);
            if (asset != null) {
                screen.getClient().cosmetics().select(selectedId);
                screen.writeLastChange("Equipped " + asset.name());
                screen.getToasts().push("Equipped " + asset.name(), AetherUi.ACCENT_ON);
            }
            return;
        }
        if (unequipBtn.contains(mx, my)) {
            CosmeticAsset asset = findAsset(screen, selectedId);
            if (asset != null) {
                screen.getClient().cosmetics().select(null);
                screen.writeLastChange("Unequipped " + asset.name());
                screen.getToasts().push("Unequipped " + asset.name(), AetherUi.WARN);
            }
            return;
        }
    }

    private CosmeticAsset findAsset(AetherClickGuiScreen screen, String id) {
        for (CosmeticAsset a : screen.getClient().cosmetics().all()) {
            if (a.id().equals(id)) return a;
        }
        return null;
    }

    private void importCape(AetherClickGuiScreen screen) {
        try {
            CosmeticValidationResult result = screen.getClient().cosmetics().importNewestDroppedCape();
            if (result.valid()) {
                screen.writeLastChange("Imported: " + result.message());
                screen.getToasts().push("Imported cape: " + result.message(), AetherUi.ACCENT_ON);
            } else {
                screen.writeLastChange("Import failed: " + result.message());
                screen.getToasts().push("Import failed: " + result.message(), AetherUi.WARN);
            }
        } catch (Exception e) {
            screen.writeLastChange("Import error: " + e.getMessage());
            screen.getToasts().push("Import error", AetherUi.WARN);
        }
    }

    private void reloadLibrary(AetherClickGuiScreen screen) {
        try {
            screen.getClient().cosmetics().load();
            thumbCache.clear();
            unreadableIds.clear();
            screen.writeLastChange("Cosmetic library reloaded");
            screen.getToasts().push("Collection reloaded", AetherUi.ACCENT_ON);
        } catch (Exception e) {
            screen.writeLastChange("Reload failed: " + e.getMessage());
            screen.getToasts().push("Reload failed", AetherUi.WARN);
        }
    }

    // Keyboard shortcuts
    public void handleKey(char typedChar, int keyCode, AetherClickGuiScreen screen, Layout layout) {
        if (keyCode == 1) { // Escape
            searchFocused = false;
            searchQuery = "";
            searchBuffer.setLength(0);
            searchBox.setQuery("", false);
            return;
        }
        if (keyCode == 28 && searchFocused) { // Enter
            searchFocused = false;
            return;
        }
        if (searchFocused) {
            if (keyCode == 14) { // Backspace
                if (searchBuffer.length() > 0) {
                    searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                    searchQuery = searchBuffer.toString();
                    searchBox.setQuery(searchQuery, true);
                }
            } else if (typedChar >= ' ' && typedChar < 127) {
                searchBuffer.append(typedChar);
                searchQuery = searchBuffer.toString();
                searchBox.setQuery(searchQuery, true);
            }
        } else {
            // F = favorite, E = equip, U = unequip
            if (keyCode == 70 && selectedId != null) { // F
                CosmeticAsset a = findAsset(screen, selectedId);
                if (a != null) {
                    screen.getClient().cosmetics().toggleFavorite(selectedId);
                    String msg = a.favorite() ? "Favorited " + a.name() : "Unfavorited " + a.name();
                    screen.writeLastChange(msg);
                    screen.getToasts().push(msg, a.favorite() ? AetherUi.STAR : AetherUi.TEXT_DISABLED);
                }
            } else if (keyCode == 69 && selectedId != null) { // E
                CosmeticAsset a = findAsset(screen, selectedId);
                if (a != null) {
                    screen.getClient().cosmetics().select(selectedId);
                    screen.writeLastChange("Equipped " + a.name());
                    screen.getToasts().push("Equipped " + a.name(), AetherUi.ACCENT_ON);
                }
            } else if (keyCode == 85 && selectedId != null) { // U
                CosmeticAsset a = findAsset(screen, selectedId);
                if (a != null) {
                    screen.getClient().cosmetics().select(null);
                    screen.writeLastChange("Unequipped " + a.name());
                    screen.getToasts().push("Unequipped " + a.name(), AetherUi.WARN);
                }
            }
        }
    }

    public void handleScroll(float amount, Layout layout) {
        int gridStartY = layout.listY + SEARCH_HEIGHT + FILTER_HEIGHT + 16;
        int gridH = layout.listH - (gridStartY - layout.listY);
        int step = CELL_SIZE + CELL_GAP;
        scrollOffset = Math.max(0, Math.min(scrollOffset + (int) (amount * step), maxScroll));
    }
}
