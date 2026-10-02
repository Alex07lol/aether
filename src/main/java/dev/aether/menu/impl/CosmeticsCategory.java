package dev.aether.menu.impl;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * The Cosmetics category: chip tabs over a card grid of 88x135 tiles, four per row,
 * with the selected slot carrying an accent ring - the reference grammar's card
 * system driven entirely by Aether's {@code CosmeticLibrary}.
 * <p>
 * The first card is always "None" (wear nothing of this type), favourites get a star,
 * and the folder button in the header opens a small popover with the three import
 * actions: import the newest dropped PNG, open the cosmetics folder, refresh the
 * library. Equipping happens immediately through {@code CosmeticLibrary.select},
 * which is the same source the in-world renderer reads.
 */
public final class CosmeticsCategory extends MenuCategory {

    private static final float CARD_W = 88.0F;
    private static final float CARD_H = 135.0F;
    private static final float STEP_X = 100.0F;
    private static final float STEP_Y = 147.0F;
    private static final float TOP = 36.0F;

    /** One filter tab: either a cosmetic type, the favourites view or the custom view. */
    private enum Filter {
        ALL("All", null),
        CAPES("Capes", CosmeticType.STATIC_CAPE),
        ANIMATED("Animated", CosmeticType.ANIMATED_CAPE),
        WINGS("Wings", CosmeticType.WINGS),
        HATS("Hats", CosmeticType.HAT),
        HALOS("Halos", CosmeticType.HALO),
        TRAILS("Trails", CosmeticType.TRAIL),
        FAVORITES("Favorites", null),
        CUSTOM("Custom", null);

        final String label;
        final CosmeticType type;

        Filter(String label, CosmeticType type) {
            this.label = label;
            this.type = type;
        }
    }

    private Filter filter = Filter.ALL;
    private String query = "";
    /** Popover actions visibility + which type the import targets. */
    private boolean popoverOpen;

    /** Uploaded thumbnails keyed by asset id: {-1, w, h} until rendered once. */
    private final Map<String, int[]> thumbnails = new HashMap<String, int[]>();

    public CosmeticsCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
    }

    @Override
    public String title() {
        return "Cosmetics";
    }

    @Override
    public boolean hasSearch() {
        return true;
    }

    @Override
    public void onShow() {
        super.onShow();
        popoverOpen = false;
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private CosmeticType activeType() {
        if (filter == Filter.CUSTOM) {
            return null; // custom shows every file-backed asset
        }
        return filter.type;
    }

    private List<CosmeticAsset> visibleAssets() {
        List<CosmeticAsset> assets = new ArrayList<CosmeticAsset>();
        for (CosmeticAsset asset : client.cosmetics().all()) {
            if (filter == Filter.FAVORITES) {
                if (!client.cosmetics().isFavorite(asset.id())) {
                    continue;
                }
            } else if (filter == Filter.CUSTOM) {
                if (asset.localFile() == null) {
                    continue;
                }
            } else if (filter.type != null && asset.type() != filter.type) {
                continue;
            }
            if (!query.isEmpty()
                && !asset.name().toLowerCase(java.util.Locale.ENGLISH).contains(query)) {
                continue;
            }
            assets.add(asset);
        }
        return assets;
    }

    /** Filters that actually have content are offered; empty ones hide (no fake tabs). */
    private List<Filter> visibleFilters() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(Filter.ALL);
        List<CosmeticType> typesWithAssets = new ArrayList<CosmeticType>();
        for (CosmeticType type : CosmeticType.values()) {
            if (!client.cosmetics().forType(type).isEmpty()) {
                typesWithAssets.add(type);
            }
        }
        for (Filter candidate : Filter.values()) {
            if (candidate == Filter.ALL) {
                continue;
            }
            if (candidate.type != null && !typesWithAssets.contains(candidate.type)) {
                continue;
            }
            if (candidate == Filter.FAVORITES && client.cosmetics().favorites().isEmpty()) {
                continue;
            }
            if (candidate == Filter.CUSTOM) {
                boolean anyFileBacked = false;
                for (CosmeticAsset asset : client.cosmetics().all()) {
                    if (asset.localFile() != null) {
                        anyFileBacked = true;
                        break;
                    }
                }
                if (!anyFileBacked) {
                    continue;
                }
            }
            filters.add(candidate);
        }
        return filters;
    }

    /* ── drawing ────────────────────────────────────────────────────────── */

    @Override
    public void draw(double mx, double my) {
        float x = innerX();
        float y = contentY() + 13.0F;

        // Filter chips.
        List<Filter> filters = visibleFilters();
        float chipX = x;
        float chipY = y - scroll;
        for (Filter candidate : filters) {
            float chipW = AetherFont.width(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, candidate.label) + 28.0F;
            boolean activeChip = candidate == filter;
            boolean hot = !activeChip && mx >= chipX && mx < chipX + chipW && my >= chipY && my < chipY + 16.0F;
            if (activeChip) {
                UiCanvas.gradientRoundRect(chipX, chipY, chipW, 16.0F, 6.0F,
                    UiTheme.withAlpha(UiTheme.accent(), 235), UiTheme.withAlpha(UiTheme.accentDeep(), 235));
            } else {
                UiCanvas.roundRect(chipX, chipY, chipW, 16.0F, 6.0F,
                    UiTheme.withAlpha(UiTheme.card(), hot ? 255 : 220));
            }
            AetherFont.drawCentered(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, candidate.label,
                chipX, chipY + (16.0F - AetherFont.height(9.0F)) / 2.0F, chipW,
                activeChip ? UiTheme.readableOn(UiTheme.accent()) : hot ? UiTheme.text() : UiTheme.textSoft());
            chipX += chipW + 6.0F;
        }

        // Card grid.
        List<CosmeticAsset> assets = visibleAssets();
        int columns = Math.max(1, (int) ((innerW() + (STEP_X - CARD_W)) / STEP_X));
        int rows = (assets.size() + columns) / columns + 1; // +1: the "None" card
        setContentHeight(TOP + rows * STEP_Y + 10.0F);

        clipContent();
        try {
            // "None" card first.
            drawNoneCard(x + 15.0F, contentY() + TOP - scroll, mx, my);
            for (int i = 0; i < assets.size(); i++) {
                int slot = i + 1; // after None
                int col = slot % columns;
                int row = slot / columns;
                float cardX = x + 15.0F + col * STEP_X;
                float cardY = contentY() + TOP - scroll + row * STEP_Y;
                if (cardY > contentY() + contentH() || cardY + CARD_H < contentY()) {
                    continue;
                }
                drawAssetCard(assets.get(i), cardX, cardY, mx, my);
            }
        } finally {
            UiCanvas.clearScissor();
        }

        // Edge fades.
        int sheet = UiTheme.sheet();
        for (int i = 0; i < 6; i++) {
            int alpha = Math.round(255 * (1.0F - (i + 0.5F) / 6.0F));
            int band = UiTheme.withAlpha(sheet, alpha);
            UiCanvas.roundRect(contentX(), contentY() + i * 2.0F, contentW(), 2.0F, 0.0F, band);
            UiCanvas.roundRect(contentX(), contentY() + contentH() - (i + 1) * 2.0F, contentW(), 2.0F, 0.0F, band);
        }

        drawPopover(mx, my);
    }

    private void drawNoneCard(float x, float y, double mx, double my) {
        CosmeticType type = activeType() == null ? CosmeticType.STATIC_CAPE : activeType();
        boolean cleared = client.cosmetics().isCleared(type);
        boolean hot = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
        if (cleared) {
            UiCanvas.roundRect(x - 2.0F, y - 2.0F, CARD_W + 4.0F, CARD_H + 4.0F, 8.5F, UiTheme.accent());
        }
        UiCanvas.roundRect(x, y, CARD_W, CARD_H, 8.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 240));

        // Empty-slot glyph: a ring with a cross, centred in the thumbnail well.
        int well = 70;
        float wellX = x + (CARD_W - well) / 2.0F;
        float wellY = y + 9.0F;
        float cx = wellX + well / 2.0F;
        float cy = wellY + well / 2.0F;
        int glyphColor = cleared ? UiTheme.accent() : UiTheme.textFaint();
        UiCanvas.outline(cx - 14.0F, cy - 14.0F, 28.0F, 28.0F, 14.0F, glyphColor, 1.5F);
        for (int i = -8; i <= 8; i++) {
            UiCanvas.roundRect(cx + i, cy + i, 1.5F, 1.5F, 0.0F, glyphColor);
            UiCanvas.roundRect(cx - i, cy + i, 1.5F, 1.5F, 0.0F, glyphColor);
        }
        AetherFont.drawCentered(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, "None",
            x, y + 114.5F, CARD_W, cleared ? UiTheme.accent() : UiTheme.textSoft());
        if (cleared) {
            AetherFont.drawCentered(7.5F, "CLEARED", x, y + 108.0F, CARD_W, UiTheme.textFaint());
        }
    }

    private void drawAssetCard(CosmeticAsset asset, float x, float y, double mx, double my) {
        boolean selected = isSelected(asset);
        boolean hot = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
        if (selected) {
            UiCanvas.roundRect(x - 2.0F, y - 2.0F, CARD_W + 4.0F, CARD_H + 4.0F, 8.5F, UiTheme.accent());
        }
        UiCanvas.roundRect(x, y, CARD_W, CARD_H, 8.0F, hot && !selected ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 240));

        // Thumbnail: the asset's preview image letterboxed into a rounded well.
        int well = 70;
        float wellX = x + (CARD_W - well) / 2.0F;
        float wellY = y + 9.0F;
        UiCanvas.roundRect(wellX, wellY, well, well, 8.0F, UiTheme.withAlpha(UiTheme.sheet(), 160));
        int[] thumb = thumbnailFor(asset);
        if (thumb != null && thumb[0] > 0) {
            double scale = Math.min((well - 8.0D) / thumb[1], (well - 8.0D) / thumb[2]);
            int drawW = (int) Math.round(thumb[1] * scale);
            int drawH = (int) Math.round(thumb[2] * scale);
            float drawX = wellX + (well - drawW) / 2.0F;
            float drawY = wellY + (well - drawH) / 2.0F;
            UiCanvas.scissor(wellX + 1.0F, wellY + 1.0F, well - 2.0F, well - 2.0F);
            Mc189CompatBridge.bindTexture(thumb[0]);
            Mc189CompatBridge.drawQuad(drawX, drawY, drawW, drawH);
            UiCanvas.clearScissor();
        } else {
            char glyph = typeGlyph(asset.type());
            AetherFont.drawIcon(glyph, 22.0F,
                wellX + (well - AetherFont.iconWidth(glyph, 22.0F)) / 2.0F,
                wellY + (well - AetherFont.height(22.0F)) / 2.0F,
                UiTheme.withAlpha(asset.primaryColor(), 235));
        }

        // Favourite star.
        boolean favorite = client.cosmetics().isFavorite(asset.id());
        boolean starHot = mx >= x + CARD_W - 20.0F && mx < x + CARD_W - 4.0F && my >= y + 4.0F && my < y + 20.0F;
        AetherFont.drawIcon(UiIcon.STAR, 9.0F, x + CARD_W - 18.0F, y + 6.0F,
            favorite ? 0xFFFFD166 : starHot ? UiTheme.text() : UiTheme.textFaint());

        // Name.
        AetherFont.drawCentered(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
            AetherFont.trim(10.0F, asset.name(), CARD_W - 8.0F),
            x, y + 114.5F, CARD_W, selected ? UiTheme.accent() : hot ? UiTheme.text() : UiTheme.textSoft());
    }

    private boolean isSelected(CosmeticAsset asset) {
        CosmeticAsset selected = client.cosmetics().selectedFor(asset.type());
        return selected != null && selected.id().equals(asset.id());
    }

    private char typeGlyph(CosmeticType type) {
        switch (type) {
            case WINGS: return UiIcon.WINGS_GLYPH;
            case HAT: return UiIcon.HAT_GLYPH;
            case HALO: return '\uF2BF'; // "cup"-like ring glyph
            case TRAIL: return '\uF47B'; // sparkle trail glyph
            default: return UiIcon.CAPE_GLYPH;
        }
    }

    /** Uploads the asset's preview image once and caches {glId, w, h}; null while unavailable. */
    private int[] thumbnailFor(CosmeticAsset asset) {
        int[] cached = thumbnails.get(asset.id());
        if (cached != null) {
            return cached[0] > 0 || cached[1] == -2 ? cached : null;
        }
        try {
            BufferedImage image = client.cosmetics().getPreview(asset.id());
            if (image == null) {
                thumbnails.put(asset.id(), new int[] {0, -2, 0}); // latched "no image"
                return null;
            }
            dev.aether.ui.UiTextures.Handle handle =
                dev.aether.ui.UiTextures.upload(image);
            if (handle == null) {
                thumbnails.put(asset.id(), new int[] {0, -2, 0});
                return null;
            }
            int[] uploaded = {handle.glId(), image.getWidth(), image.getHeight()};
            thumbnails.put(asset.id(), uploaded);
            return uploaded;
        } catch (Exception failed) {
            thumbnails.put(asset.id(), new int[] {0, -2, 0});
            return null;
        }
    }

    /* ── the import popover ─────────────────────────────────────────────── */

    private void drawPopover(double mx, double my) {
        if (!popoverOpen) {
            return;
        }
        float x = contentX() + contentW() - 175.0F - 40.0F;
        float y = windowY() + 26.0F;
        float w = 130.0F;
        UiCanvas.roundRect(x, y, w, 84.0F, 8.0F, UiTheme.withAlpha(0x101116, 250));
        UiCanvas.outline(x, y, w, 84.0F, 8.0F, UiTheme.edge(), 1.0F);

        String[] labels = {"Import newest PNG", "Open cosmetics folder", "Refresh"};
        for (int i = 0; i < labels.length; i++) {
            float rowY = y + 8.0F + i * 23.0F;
            boolean hot = popoverHot(x, rowY, w, mx, my);
            if (hot) {
                UiCanvas.roundRect(x + 4.0F, rowY, w - 8.0F, 18.0F, 5.0F, UiTheme.withAlpha(UiTheme.cardHover(), 255));
            }
            AetherFont.draw(9.0F, labels[i], x + 10.0F, rowY + (18.0F - AetherFont.height(9.0F)) / 2.0F,
                hot ? UiTheme.text() : UiTheme.textSoft());
        }
    }

    private boolean popoverHot(float x, float rowY, float w, double mx, double my) {
        return mx >= x + 4.0F && mx < x + w - 4.0F && my >= rowY && my < rowY + 18.0F;
    }

    private boolean clickPopover(double mx, double my, int button) {
        if (!popoverOpen || button != 0) {
            return false;
        }
        float x = contentX() + contentW() - 175.0F - 40.0F;
        float y = windowY() + 26.0F;
        float w = 130.0F;
        for (int i = 0; i < 3; i++) {
            float rowY = y + 8.0F + i * 23.0F;
            if (mx >= x + 4.0F && mx < x + w - 4.0F && my >= rowY && my < rowY + 18.0F) {
                popoverOpen = false;
                runImportAction(i);
                return true;
            }
        }
        popoverOpen = false;
        return true;
    }

    private void runImportAction(int action) {
        try {
            if (action == 0) {
                CosmeticType target = activeType() == null ? CosmeticType.STATIC_CAPE : activeType();
                dev.aether.cosmetic.CosmeticValidationResult result =
                    target == CosmeticType.STATIC_CAPE
                        ? client.cosmetics().importNewestDroppedCape()
                        : client.cosmetics().importNewestDroppedCosmetic(target);
                if (result.valid()) {
                    Minecraft.getMinecraft().getSoundHandler().playSound(
                        PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                }
            } else if (action == 1) {
                CosmeticType target = activeType() == null ? CosmeticType.STATIC_CAPE : activeType();
                java.awt.Desktop.getDesktop().open(client.cosmetics().typeDirectory(target).toFile());
            } else {
                client.cosmetics().rescan();
                thumbnails.clear();
            }
            saveQuietly();
        } catch (Exception failed) {
            System.out.println("[Aether] Cosmetic action failed: " + failed);
        }
    }

    /* ── header extras: the folder button ───────────────────────────────── */

    @Override
    public void drawHeaderExtras(double mx, double my) {
        float x = contentX() + contentW() - 198.0F;
        float y = windowY() + 6.5F;
        boolean hot = mx >= x && mx < x + 18.0F && my >= y && my < y + 18.0F;
        UiCanvas.roundRect(x, y, 18.0F, 18.0F, 6.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 235));
        AetherFont.drawIcon(popoverOpen ? UiIcon.DISMISS : UiIcon.FOLDER, 11.0F,
            x + (18.0F - AetherFont.iconWidth(popoverOpen ? UiIcon.DISMISS : UiIcon.FOLDER, 11.0F)) / 2.0F,
            y + (18.0F - AetherFont.height(11.0F)) / 2.0F, UiTheme.textSoft());

        searchBoxProxy.draw(x + 202.0F - 160.0F + 23.0F, y, mx, my);
    }

    private final dev.aether.menu.comp.UiSearchBox searchBoxProxy = new dev.aether.menu.comp.UiSearchBox(new Runnable() {
        public void run() {
            query = searchBoxProxy.text().toLowerCase(java.util.Locale.ENGLISH);
        }
    });

    @Override
    public boolean clickHeaderExtras(double mx, double my, int button) {
        float x = contentX() + contentW() - 198.0F;
        float y = windowY() + 6.5F;
        if (button == 0 && mx >= x && mx < x + 18.0F && my >= y && my < y + 18.0F) {
            popoverOpen = !popoverOpen;
            return true;
        }
        if (popoverOpen && clickPopover(mx, my, button)) {
            return true;
        }
        return searchBoxProxy.click(x + 65.0F, y, mx, my, button);
    }

    @Override
    public boolean keyHeaderExtras(char typedChar, int keyCode) {
        return searchBoxProxy.key(typedChar, keyCode);
    }

    @Override
    public boolean capturesKeyboard() {
        return searchBoxProxy.focused();
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return true;
        }
        // chips
        List<Filter> filters = visibleFilters();
        float chipX = innerX();
        float chipY = contentY() + 13.0F - scroll;
        for (Filter candidate : filters) {
            float chipW = AetherFont.width(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, candidate.label) + 28.0F;
            if (mx >= chipX && mx < chipX + chipW && my >= chipY && my < chipY + 16.0F) {
                filter = candidate;
                clampScroll();
                return true;
            }
            chipX += chipW + 6.0F;
        }
        // cards
        List<CosmeticAsset> assets = visibleAssets();
        int columns = Math.max(1, (int) ((innerW() + (STEP_X - CARD_W)) / STEP_X));
        float x = innerX() + 15.0F;
        float y = contentY() + TOP - scroll;
        // None card
        if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) {
            CosmeticType type = activeType() == null ? CosmeticType.STATIC_CAPE : activeType();
            client.cosmetics().clear(type);
            saveQuietly();
            Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            return true;
        }
        for (int i = 0; i < assets.size(); i++) {
            int slot = i + 1;
            int col = slot % columns;
            int row = slot / columns;
            float cardX = x + col * STEP_X;
            float cardY = contentY() + TOP - scroll + row * STEP_Y;
            if (mx >= cardX && mx < cardX + CARD_W && my >= cardY && my < cardY + CARD_H) {
                CosmeticAsset asset = assets.get(i);
                boolean starHot = mx >= cardX + CARD_W - 20.0F && mx < cardX + CARD_W - 4.0F
                    && my >= cardY + 4.0F && my < cardY + 20.0F;
                if (starHot) {
                    client.cosmetics().toggleFavorite(asset.id());
                } else {
                    client.cosmetics().select(asset.id());
                    Minecraft.getMinecraft().getSoundHandler().playSound(
                        PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                }
                saveQuietly();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean stepBack() {
        if (popoverOpen) {
            popoverOpen = false;
            return true;
        }
        if (searchBoxProxy.focused()) {
            searchBoxProxy.setFocused(false);
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        for (int[] thumb : thumbnails.values()) {
            // GL textures live in the shared upload cache; the preview owns deletion.
        }
    }

    /* ── debug hooks ────────────────────────────────────────────────────── */

    public void debugSearch(String query) {
        searchBoxProxy.setText(query);
        this.query = query.toLowerCase(java.util.Locale.ENGLISH);
    }

    public void debugSelectFilter(int index) {
        List<Filter> filters = visibleFilters();
        if (index >= 0 && index < filters.size()) {
            filter = filters.get(index);
        }
    }

    public void debugSelectCard(int index) {
        List<CosmeticAsset> assets = visibleAssets();
        int slot = index;
        if (slot == 0) {
            CosmeticType type = activeType() == null ? CosmeticType.STATIC_CAPE : activeType();
            client.cosmetics().clear(type);
            return;
        }
        if (slot - 1 < assets.size()) {
            client.cosmetics().select(assets.get(slot - 1).id());
        }
    }

    public void debugToggleImportMenu() {
        popoverOpen = !popoverOpen;
    }

    public void debugNextPage() {
        onWheel(0, 0, -2);
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }

    /** Bridge shim so the card thumbnails can draw GL textures without new imports. */
    private static final class Mc189CompatBridge {
        static void bindTexture(int glId) {
            dev.aether.forge189.Mc189Compat.bindTexture(glId);
        }

        static void drawQuad(float x, float y, float w, float h) {
            org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
            org.lwjgl.opengl.GL11.glTexCoord2f(0.0F, 1.0F);
            org.lwjgl.opengl.GL11.glVertex2f(x, y + h);
            org.lwjgl.opengl.GL11.glTexCoord2f(1.0F, 1.0F);
            org.lwjgl.opengl.GL11.glVertex2f(x + w, y + h);
            org.lwjgl.opengl.GL11.glTexCoord2f(1.0F, 0.0F);
            org.lwjgl.opengl.GL11.glVertex2f(x + w, y);
            org.lwjgl.opengl.GL11.glTexCoord2f(0.0F, 0.0F);
            org.lwjgl.opengl.GL11.glVertex2f(x, y);
            org.lwjgl.opengl.GL11.glEnd();
        }
    }
}
