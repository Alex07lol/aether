package dev.aether.gui.screens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.Button;
import dev.aether.gui.core.ScrollContainer;
import dev.aether.gui.core.UiComponent;
import dev.aether.gui.preview.PlayerPreview;
import dev.aether.ui.GuiSection;

/**
 * The Cosmetics screen - the equivalent of Leaf Client's {@code CosmeticSettings}
 * (see docs/GUI_REBUILD.md): the shared navigation on top, a cosmetic category
 * selector and a scrollable list on the left, and a large rotating player preview on
 * the right that shows whatever is currently equipped.
 * <p>
 * Categories come from the data: only {@link CosmeticType}s that actually have assets
 * are offered, so adding a cosmetic of a new type needs no screen change and no fake
 * category is ever shown. Selecting an entry equips it immediately through
 * {@code CosmeticLibrary} (the same source the in-world renderer reads), updates the
 * renderer state on the next frame and persists through the normal save path - the
 * screen holds no cosmetic state of its own and never touches storage or services
 * directly beyond that.
 */
public final class AetherCosmeticScreen extends AetherGuiScreen {

    private static final int ROW_GAP = 8;

    private final ScrollContainer entryList = new ScrollContainer();
    private final PlayerPreview preview;
    private final Button importButton = new Button("Import Cape", Button.Style.QUIET, new Runnable() {
        public void run() {
            importCape();
        }
    });
    private final Button unequipButton = new Button("Wear nothing", Button.Style.QUIET, new Runnable() {
        public void run() {
            client.cosmetics().clear(selectedType);
            saveQuietly();
            rebuildEntries();
        }
    });

    private CosmeticType selectedType;
    private String status;
    private long statusAtMillis;
    private double listW;
    private double listX;

    public AetherCosmeticScreen(dev.aether.AetherClient client) {
        super(client);
        this.preview = new PlayerPreview(client);
        this.selectedType = firstTypeWithAssets();
        rebuildEntries();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.COSMETICS;
    }

    @Override
    protected String title() {
        return "Cosmetics";
    }

    @Override
    protected dev.aether.gui.components.TextField searchField() {
        return null;
    }

    @Override
    protected String footerHint() {
        return "Drag the preview to rotate  |  Wheel zooms  |  Import takes a PNG from the cosmetics drop folder";
    }

    @Override
    protected void layout(double contentX, double contentY, double contentW, double contentH) {
        int chipsHeight = 26;
        int buttonY = (int) contentY + chipsHeight + 10;
        double buttonW = Math.min(190.0D, contentW * 0.3D);
        importButton.at((int) contentX, buttonY).size((int) buttonW, 26);
        unequipButton.at((int) (contentX + buttonW + 8), buttonY).size((int) buttonW, 26);

        double listTop = buttonY + 26 + 10;
        listW = Math.min(380.0D, contentW * 0.42D);
        listX = contentX;
        entryList.at((int) listX, (int) listTop).size((int) listW, (int) (contentH - (listTop - contentY)));
        preview.at((int) (contentX + listW + 16), (int) listTop)
            .size((int) (contentW - listW - 16), (int) (contentH - (listTop - contentY)));
        rebuildEntries();
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private CosmeticType firstTypeWithAssets() {
        for (CosmeticType type : CosmeticType.values()) {
            if (!client.cosmetics().forType(type).isEmpty()) {
                return type;
            }
        }
        return null;
    }

    /** Types with at least one asset, in enum order - the extensible category row. */
    private List<CosmeticType> availableTypes() {
        Set<CosmeticType> types = new LinkedHashSet<CosmeticType>();
        for (CosmeticType type : CosmeticType.values()) {
            if (!client.cosmetics().forType(type).isEmpty()) {
                types.add(type);
            }
        }
        return new ArrayList<CosmeticType>(types);
    }

    private static String label(CosmeticType type) {
        switch (type) {
            case STATIC_CAPE: return "Cape";
            case ANIMATED_CAPE: return "Animated Cape";
            case WINGS: return "Wings";
            case HALO: return "Halo";
            case HAT: return "Hat";
            default: {
                String raw = type.name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
                StringBuilder out = new StringBuilder(raw.length());
                boolean capitalize = true;
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (c == ' ') {
                        capitalize = true;
                        out.append(c);
                    } else if (capitalize) {
                        out.append(Character.toUpperCase(c));
                        capitalize = false;
                    } else {
                        out.append(c);
                    }
                }
                return out.toString();
            }
        }
    }

    private void rebuildEntries() {
        entryList.clear();
        if (selectedType == null) {
            entryList.clampOffset();
            return;
        }
        double y = 0;
        int entryWidth = Math.max(60, entryList.getWidth() - 12);
        for (CosmeticAsset asset : client.cosmetics().forType(selectedType)) {
            CosmeticEntry entry = new CosmeticEntry(asset);
            entry.at(0, (int) y).size(entryWidth, 48);
            entryList.add(entry);
            y += 48 + ROW_GAP;
        }
        entryList.clampOffset();
    }

    private void importCape() {
        try {
            dev.aether.cosmetic.CosmeticValidationResult result = client.cosmetics().importNewestDroppedCape();
            status = result.message();
            if (selectedType == null || selectedType != CosmeticType.STATIC_CAPE) {
                selectedType = CosmeticType.STATIC_CAPE;
            }
            rebuildEntries();
        } catch (IOException failed) {
            status = "Import failed: " + failed.getMessage();
        }
        statusAtMillis = System.currentTimeMillis();
        saveQuietly();
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        drawCategoryChips(mx, my);
        importButton.render();
        unequipButton.render();

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(AetherFont.Size.SMALL, status,
                GuiScale.x(entryList.getX()), GuiScale.y(entryList.getY() - 16), AetherUi.TEXT_SECONDARY);
        }

        entryList.render();
        preview.onMouseMove(mx, my);
        preview.render();
    }

    private void drawCategoryChips(double mx, double my) {
        List<CosmeticType> types = availableTypes();
        int x = (int) shell.contentX;
        int y = (int) shell.contentY;
        for (CosmeticType type : types) {
            String name = label(type);
            int width = AetherFont.width(AetherFont.Size.SMALL, name) + GuiScale.w(18);
            boolean active = type == selectedType;
            boolean hover = mx >= x && mx < x + GuiScale.mouseX(width) && my >= y && my < y + 26;
            int fill = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            AetherUi.drawRoundRect(GuiScale.x(x), GuiScale.y(y), GuiScale.x(x) + width, GuiScale.y(y) + GuiScale.h(26),
                GuiScale.h(7), fill);
            AetherFont.drawCentered(AetherFont.Size.SMALL, name, GuiScale.x(x),
                GuiScale.y(y) + (GuiScale.h(26) - AetherFont.height(AetherFont.Size.SMALL)) / 2, width,
                active ? AetherUi.readableOn(AetherUi.ACCENT) : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
            x += (int) (GuiScale.mouseX(width) + 8);
        }
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        // Category chips sit above everything in the content area.
        List<CosmeticType> types = availableTypes();
        int x = (int) shell.contentX;
        int y = (int) shell.contentY;
        for (CosmeticType type : types) {
            String name = label(type);
            int width = AetherFont.width(AetherFont.Size.SMALL, name) + GuiScale.w(18);
            int chipW = (int) (GuiScale.mouseX(width) + 8);
            if (mx >= x && mx < x + chipW && my >= y && my < y + 26) {
                if (selectedType != type) {
                    selectedType = type;
                    rebuildEntries();
                }
                return true;
            }
            x += chipW;
        }
        if (importButton.onMouseClick(mx, my, button) || unequipButton.onMouseClick(mx, my, button)) {
            return true;
        }
        if (preview.onMouseClick(mx, my, button)) {
            return true;
        }
        entryList.onMouseMove(mx, my);
        return entryList.onMouseClick(mx, my, button);
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        entryList.onMouseRelease(mx, my, button);
        preview.onMouseRelease(mx, my, button);
        importButton.onMouseRelease(mx, my, button);
        unequipButton.onMouseRelease(mx, my, button);
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        return false;
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        return entryList.onWheel(mx, my, delta) || preview.onWheel(mx, my, delta);
    }

    @Override
    protected void disposeContent() {
        entryList.dispose();
        preview.dispose();
    }

    /* ── the cosmetic entry ─────────────────────────────────────────────── */

    /** One list row: colour swatch, name, equipped state and a favorite star. */
    private final class CosmeticEntry extends UiComponent {

        private final CosmeticAsset asset;
        private boolean hover;

        CosmeticEntry(CosmeticAsset asset) {
            this.asset = asset;
        }

        private boolean equipped() {
            String equippedId = client.cosmetics().selectedFor(asset.type()) == null
                ? null : client.cosmetics().selectedFor(asset.type()).id();
            return asset.id().equals(equippedId);
        }

        private boolean inStar(double mx, double my) {
            double x = this.x + this.width - 14 - 18;
            return mx >= x && mx < x + 18 && my >= this.y && my < this.y + this.height;
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            boolean isEquipped = equipped();

            int base = isEquipped ? AetherUi.ROW_SELECTED : hover ? AetherUi.CARD_HOVER : AetherUi.CARD;
            AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(8), base);
            AetherUi.outline(left, top, left + w, top + h,
                isEquipped ? AetherUi.withAlpha(AetherUi.ACCENT, 0x66) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));

            int pad = GuiScale.w(12);
            int swatchSize = GuiScale.h(20);
            int swatchY = top + (h - swatchSize) / 2;
            AetherUi.drawRoundRect(left + pad, swatchY, left + pad + swatchSize, swatchY + swatchSize, GuiScale.h(5),
                asset.primaryColor() | 0xFF000000);
            AetherUi.drawRoundRect(left + pad + swatchSize / 3, swatchY + swatchSize / 3,
                left + pad + swatchSize - swatchSize / 6, swatchY + swatchSize - swatchSize / 6, GuiScale.h(3),
                asset.secondaryColor() | 0xFF000000);

            int textX = left + pad + swatchSize + GuiScale.w(10);
            AetherFont.draw(AetherFont.Size.BODY, asset.name(), textX, top + GuiScale.h(7),
                isEquipped ? AetherUi.TEXT_PRIMARY : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
            String subtitle = asset.builtIn() ? "Built-in" : "Imported";
            AetherFont.draw(AetherFont.Size.CAPTION, subtitle, textX, top + GuiScale.h(27), AetherUi.TEXT_DISABLED);

            if (isEquipped) {
                int markX = left + w - pad - GuiScale.w(40);
                int markY = top + h / 2 - GuiScale.h(5);
                Mc189Compat.drawRect(markX, markY + GuiScale.h(4), markX + GuiScale.w(10), markY + GuiScale.h(6), AetherUi.ACCENT_ON);
                Mc189Compat.drawRect(markX + GuiScale.w(2), markY + GuiScale.h(2), markX + GuiScale.w(8), markY + GuiScale.h(8), AetherUi.ACCENT_ON);
                Mc189Compat.drawRect(markX + GuiScale.w(4), markY, markX + GuiScale.w(6), markY + GuiScale.h(10), AetherUi.ACCENT_ON);
                AetherFont.draw(AetherFont.Size.CAPTION, "Worn", markX + GuiScale.w(14), top + h / 2 - AetherFont.height(AetherFont.Size.CAPTION) / 2,
                    AetherUi.ACCENT_ON);
            }

            boolean fav = client.cosmetics().isFavorite(asset.id());
            int starColor = fav ? AetherUi.STAR : AetherUi.TEXT_DISABLED;
            int starX = left + w - pad - GuiScale.w(14);
            int starY = top + h / 2 - GuiScale.h(7);
            int s = GuiScale.h(14);
            Mc189Compat.drawRect(starX + s / 2 - Math.max(1, GuiScale.w(1)), starY, starX + s / 2 + Math.max(1, GuiScale.w(1)), starY + s, starColor);
            Mc189Compat.drawRect(starX, starY + s / 3, starX + s, starY + s / 3 + GuiScale.h(3), starColor);
            AetherUi.drawCircle(starX + s / 2, starY + s / 2, s / 4, starColor);
        }

        @Override
        public void onMouseMove(double mx, double my) {
            hover = contains(mx, my);
        }

        @Override
        public boolean onMouseClick(double mx, double my, int button) {
            if (button != 0 || !contains(mx, my)) {
                return false;
            }
            if (inStar(mx, my)) {
                client.cosmetics().toggleFavorite(asset.id());
                saveQuietly();
                return true;
            }
            client.cosmetics().select(asset.id());
            saveQuietly();
            return true;
        }
    }
}
