package dev.aether.gui.screens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.gui.GuiScale;
import dev.aether.gui.leaf.CosmeticEntry;
import dev.aether.gui.leaf.PageBar;
import dev.aether.gui.leaf.SelectButton;
import dev.aether.gui.preview.PlayerPreview;
import dev.aether.ui.GuiSection;

/**
 * The Cosmetics screen, ported from Leaf Client's {@code CosmeticSettings} (GPLv3,
 * see docs/GUI_REBUILD.md): the navigation tiles up top; on the left a vertical list
 * of 300x90 cosmetic pills starting at (480, 400) with a 100 pitch - "None" first,
 * exactly like Leaf - paged three at a time by the scrollbar at (945, 400, 32, 400);
 * below the list the category selector in Leaf's SelectButton position (480, 700);
 * and on the right the large player preview at Leaf's spot (entity feet at
 * (1300, 800), 200 units tall) that turns with the mouse, as Leaf's does.
 * <p>
 * Selecting an entry equips it immediately through {@code CosmeticLibrary} - the same
 * source the in-world cosmetic renderer reads - and persists through the client's
 * save path. Categories come from the data: only types that actually own assets are
 * offered, so new cosmetic types need no screen change.
 */
public final class AetherCosmeticScreen extends AetherGuiScreen {

    private static final int LIST_X = 480;
    private static final int LIST_TOP = 400;
    private static final int ENTRY_PITCH = 100;
    private static final int ENTRIES_PER_PAGE = 3;
    private static final int PREVIEW_CENTER_X = 1300;
    private static final int PREVIEW_FEET_Y = 800;
    private static final int PREVIEW_SCALE = 200;

    private final PageBar pageBar = new PageBar(945, LIST_TOP, 32, 400, ENTRIES_PER_PAGE, 0);
    private final PlayerPreview preview = new PlayerPreview(client);
    private CosmeticType selectedType;
    private SelectButton typeButton;
    private String status;
    private long statusAtMillis;

    public AetherCosmeticScreen(dev.aether.AetherClient client) {
        super(client);
        this.selectedType = firstTypeWithAssets();
        this.typeButton = buildTypeButton();
        pageBar.setListSize(entryCount());
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.COSMETICS;
    }

    @Override
    protected void renderContent(double mx, double my) {
        preview.setMouseLook(GuiScale.designWidth() / 2.0D - mx, mouseYFromTop(my));

        int index = pageBar.getIndex();
        List<CosmeticAsset> assets = assetsOfType(selectedType);
        for (int slot = 0; slot < ENTRIES_PER_PAGE; slot++) {
            int listIndex = index + slot;
            int y = LIST_TOP + slot * ENTRY_PITCH;
            if (listIndex == 0) {
                CosmeticEntry none = new CosmeticEntry("None", LIST_X, y, 300, 90,
                    isSlotEmpty(), new Runnable() {
                        public void run() {
                            client.cosmetics().clear(selectedType);
                            saveQuietly();
                        }
                    });
                none.onMouseMove(mx, my);
                none.render();
            } else if (listIndex - 1 < assets.size()) {
                final CosmeticAsset asset = assets.get(listIndex - 1);
                CosmeticEntry entry = new CosmeticEntry(asset.name(), LIST_X, y, 300, 90,
                    isEquipped(asset), new Runnable() {
                        public void run() {
                            client.cosmetics().select(asset.id());
                            saveQuietly();
                            status = asset.name() + " equipped.";
                            statusAtMillis = System.currentTimeMillis();
                        }
                    });
                entry.onMouseMove(mx, my);
                entry.render();
            }
        }
        pageBar.render();
        typeButton.render();

        preview.render();

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            dev.aether.gui.AetherFont.draw(dev.aether.gui.AetherFont.Size.SMALL, status,
                dev.aether.gui.GuiScale.x(770), dev.aether.gui.GuiScale.y(880),
                dev.aether.forge189.AetherUi.TEXT_SECONDARY);
        }
    }

    private double mouseYFromTop(double my) {
        // Leaf's preview measures the mouse offset from the screen centre, with the
        // vertical delta clamped to +-30 design units.
        double dy = 540.0D - my;
        return Math.max(-30.0D, Math.min(30.0D, dy));
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (typeButton.onMouseClick(mx, my, button)) {
            return true;
        }
        int index = pageBar.getIndex();
        List<CosmeticAsset> assets = assetsOfType(selectedType);
        for (int slot = 0; slot < ENTRIES_PER_PAGE; slot++) {
            int listIndex = index + slot;
            int y = LIST_TOP + slot * ENTRY_PITCH;
            if (listIndex == 0) {
                if (new CosmeticEntry("None", LIST_X, y, 300, 90, isSlotEmpty(), null)
                    .onMouseClick(mx, my, button)) {
                    client.cosmetics().clear(selectedType);
                    saveQuietly();
                    return true;
                }
            } else if (listIndex - 1 < assets.size()) {
                final CosmeticAsset asset = assets.get(listIndex - 1);
                if (new CosmeticEntry(asset.name(), LIST_X, y, 300, 90, isEquipped(asset), null)
                    .onMouseClick(mx, my, button)) {
                    client.cosmetics().select(asset.id());
                    saveQuietly();
                    status = asset.name() + " equipped.";
                    statusAtMillis = System.currentTimeMillis();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        // The preview follows the mouse without dragging, so nothing to release.
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        if (delta < 0) {
            pageBar.onScroll();
        } else {
            pageBar.onUnScroll();
        }
        return true;
    }

    @Override
    protected void disposeContent() {
        preview.dispose();
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private SelectButton buildTypeButton() {
        List<String> labels = new ArrayList<String>();
        for (CosmeticType type : availableTypes()) {
            labels.add(displayLabel(type));
        }
        String current = selectedType == null ? "Cape" : displayLabel(selectedType);
        return new SelectButton("Category", LIST_X, 700, 300, 90, labels, current, new Runnable() {
            public void run() {
                String picked = typeButton.current();
                for (CosmeticType type : availableTypes()) {
                    if (displayLabel(type).equals(picked)) {
                        selectedType = type;
                        break;
                    }
                }
                pageBar.setListSize(entryCount());
            }
        });
    }

    private CosmeticType firstTypeWithAssets() {
        for (CosmeticType type : CosmeticType.values()) {
            if (!client.cosmetics().forType(type).isEmpty()) {
                return type;
            }
        }
        return CosmeticType.STATIC_CAPE;
    }

    private List<CosmeticType> availableTypes() {
        Set<CosmeticType> types = new LinkedHashSet<CosmeticType>();
        for (CosmeticType type : CosmeticType.values()) {
            if (!client.cosmetics().forType(type).isEmpty()) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            types.add(CosmeticType.STATIC_CAPE);
        }
        return new ArrayList<CosmeticType>(types);
    }

    private static String displayLabel(CosmeticType type) {
        switch (type) {
            case STATIC_CAPE: return "Cape";
            case ANIMATED_CAPE: return "Animated Cape";
            case WINGS: return "Wings";
            case HALO: return "Halo";
            case HAT: return "Hat";
            default: {
                String raw = type.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
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

    private List<CosmeticAsset> assetsOfType(CosmeticType type) {
        return type == null ? new ArrayList<CosmeticAsset>() : client.cosmetics().forType(type);
    }

    private int entryCount() {
        // "None" plus every asset of the selected type.
        return 1 + assetsOfType(selectedType).size();
    }

    private boolean isSlotEmpty() {
        return selectedType != null && client.cosmetics().isCleared(selectedType);
    }

    private boolean isEquipped(CosmeticAsset asset) {
        CosmeticAsset selected = client.cosmetics().selectedFor(asset.type());
        return selected != null && selected.id().equals(asset.id());
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    public void debugImportCape() {
        try {
            client.cosmetics().importNewestDroppedCape();
            pageBar.setListSize(entryCount());
            status = "Imported.";
            statusAtMillis = System.currentTimeMillis();
            saveQuietly();
        } catch (IOException failed) {
            status = "Import failed: " + failed.getMessage();
            statusAtMillis = System.currentTimeMillis();
        }
    }
}
