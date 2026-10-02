package dev.aether.gui.screens;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.aether.animation.Anim;
import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.CosmeticValidationResult;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.ChipBar;
import dev.aether.gui.components.CosmeticCard;
import dev.aether.gui.components.ScrollView;
import dev.aether.gui.components.SearchBox;
import dev.aether.gui.preview.PlayerPreview;
import dev.aether.runtime.FolderOpener;
import dev.aether.ui.GuiSection;

/**
 * The Cosmetics screen: a real library, not a settings page.
 * <p>
 * The Leaf pill list is gone (see docs/GUI_REBUILD.md for what it was). In its place is the
 * modern cosmetics-manager composition the brief asks for - chip filters (All / Capes /
 * Animated / Wings / Hats / Halos / Trails / Favorites / Custom), a search field, a
 * scissored grid of rounded cards with thumbnail, name, type, an EQUIPPED badge and a
 * favourite star, all fed from {@code CosmeticLibrary} - with the rotating player preview
 * on the right showing what is actually worn.
 * <p>
 * Everything on this screen delegates: equipping goes through {@code CosmeticLibrary.select},
 * favourites through {@code toggleFavorite}, folders through {@link FolderOpener}, and the
 * import popover covers the three ways a PNG gets in (drop folder, type folder + rescan,
 * native file dialog). The screen never touches module or GL state beyond the shared
 * components, and the preview restores its own GL state (it always has).
 */
public final class AetherCosmeticScreen extends AetherGuiScreen {

    private static final int SEARCH_Y = 386;
    private static final int SEARCH_W = 300;
    private static final int BTN_Y = 386;
    private static final int BTN_H = 40;
    private static final int CHIPS_Y = 440;
    private static final int LIST_X = 430;
    private static final int LIST_W = CosmeticCard.CARD_W * 3 + 2 * (CosmeticCard.PITCH_X - CosmeticCard.CARD_W);
    private static final int LIST_TOP = 494;
    private static final int LIST_H = 372;
    private static final int COLS = 3;
    private static final int STATUS_Y = 876;
    private static final int BUTTON_GAP = 8;
    private static final int POPOVER_GAP = 6;
    private static final int POPOVER_W = 240;
    private static final int POPOVER_ITEM_H = 30;
    private static final int POPOVER_PAD = 8;

    /** The chip filters, in the brief's order. */
    private enum Filter {
        ALL, CAPES, ANIMATED, WINGS, HATS, HALOS, TRAILS, FAVORITES, CUSTOM
    }

    private static final String[] FILTER_LABELS = {
        "All", "Capes", "Animated", "Wings", "Hats", "Halos", "Trails", "Favorites", "Custom"
    };

    /** One row of the import popover: a label and what running it does. */
    private static final class PopItem {
        final String label;
        final Runnable action;

        PopItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    /** An uploaded thumbnail with its source dimensions, for letterboxing. */
    private static final class Thumb {
        final int glId;
        final int width;
        final int height;

        Thumb(int glId, int width, int height) {
            this.glId = glId;
            this.width = width;
            this.height = height;
        }
    }

    private final List<CosmeticAsset> visible = new ArrayList<CosmeticAsset>();
    /** Parallel to {@link #cards}: the asset each card shows, null for the "None" card. */
    private final List<CosmeticAsset> entries = new ArrayList<CosmeticAsset>();
    private final List<CosmeticCard> cards = new ArrayList<CosmeticCard>();
    private final Map<String, Thumb> thumbs = new HashMap<String, Thumb>();

    private final ScrollView scroll = new ScrollView().bounds(LIST_X, LIST_TOP, LIST_W, LIST_H);
    private final SearchBox search = new SearchBox("Search cosmetics", new Runnable() {
        public void run() {
            refresh();
        }
    }).place(LIST_X, SEARCH_Y, SEARCH_W, 40);
    private final ChipBar filters = new ChipBar(new Runnable() {
        public void run() {
            filter = filterAt(filters.selected());
            refresh();
        }
    });
    private final PlayerPreview preview = new PlayerPreview(client);

    private Filter filter = Filter.ALL;
    private String lastSignature = "";

    private String status;
    private long statusAtMillis;

    private boolean popoverOpen;
    private final Anim popoverAnim = new Anim(0.0F, 160.0F, Easing.EASE_OUT_CUBIC);
    private final Anim importHover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);
    private final Anim folderHover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);
    private final Anim refreshHover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);

    /** Button rectangles in design units, recomputed each frame (and before hit tests). */
    private int btnImportX;
    private int btnImportW;
    private int btnFolderX;
    private int btnFolderW;
    private int btnRefreshX;
    private int btnRefreshW;

    public AetherCosmeticScreen(dev.aether.AetherClient client) {
        super(client);
        List<String> labels = new ArrayList<String>();
        Collections.addAll(labels, FILTER_LABELS);
        filters.labels(labels);
        refresh();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.COSMETICS;
    }

    @Override
    protected void renderContent(double mx, double my) {
        layoutButtons();
        search.update();
        filters.update();
        scroll.update();
        popoverAnim.target(popoverOpen ? 1.0F : 0.0F);
        popoverAnim.update();
        importHover.target(inside(btnImportX, btnImportW, mx, my) ? 1.0F : 0.0F);
        folderHover.target(inside(btnFolderX, btnFolderW, mx, my) ? 1.0F : 0.0F);
        refreshHover.target(inside(btnRefreshX, btnRefreshW, mx, my) ? 1.0F : 0.0F);
        importHover.update();
        folderHover.update();
        refreshHover.update();

        syncCards();

        search.render();
        layoutFilters();
        filters.render();
        drawButtons();

        double contentMouseY = my + scroll.offset();
        Mc189Compat.pushScissor(GuiScale.x(LIST_X), GuiScale.y(LIST_TOP),
            GuiScale.w(LIST_W + 12), GuiScale.h(LIST_H));
        try {
            for (CosmeticCard card : cards) {
                card.renderOffset(0.0D, -scroll.offset());
                card.onMouseMove(mx, contentMouseY);
                card.update();
                card.render();
            }
            if (cards.isEmpty()) {
                drawEmptyState();
            }
        } finally {
            Mc189Compat.popScissor();
        }
        scroll.render();

        // The preview follows the mouse the way Leaf's does: yaw from the horizontal offset
        // from screen centre, pitch from the clamped vertical offset.
        preview.setMouseLook(GuiScale.designWidth() / 2.0D - mx, mouseYFromTop(my));
        preview.render();

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(AetherFont.Size.CAPTION, status, GuiScale.x(LIST_X), GuiScale.y(STATUS_Y),
                AetherUi.TEXT_SECONDARY);
        }

        if (popoverAnim.value() > 0.01F) {
            drawPopover(mx, my);
        }
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        layoutButtons();
        if (search.onMouseClick(mx, my, button)) {
            return true;
        }
        if (filters.onMouseClick(mx, my, button)) {
            return true;
        }
        if (popoverOpen) {
            int item = popoverItemAt(mx, my);
            if (item >= 0) {
                PopItem chosen = popoverItems().get(item);
                popoverOpen = false;
                chosen.action.run();
                return true;
            }
            if (popoverRectContains(mx, my)) {
                return true; // clicks inside the popover do nothing else
            }
            popoverOpen = false; // a click outside dismisses it, then falls through
        }
        if (inside(btnImportX, btnImportW, mx, my)) {
            popoverOpen = !popoverOpen;
            return true;
        }
        if (inside(btnFolderX, btnFolderW, mx, my)) {
            openFolder(client.cosmetics().storageDirectory(), "cosmetics folder");
            return true;
        }
        if (inside(btnRefreshX, btnRefreshW, mx, my)) {
            rescan();
            return true;
        }

        double contentMouseY = my + scroll.offset();
        if (scroll.onMouseClick(mx, my, button)) {
            return true;
        }
        for (CosmeticCard card : cards) {
            if (card.onMouseClick(mx, contentMouseY, button)) {
                return true;
            }
        }
        search.blur();
        return true; // the backdrop swallows everything else, like Leaf's fullscreen texture
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        double contentMouseY = my + scroll.offset();
        for (CosmeticCard card : cards) {
            card.onMouseRelease(mx, contentMouseY, button);
        }
        scroll.onMouseRelease();
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        if (search.onKeyTyped(typedChar, keyCode)) {
            return true;
        }
        if (keyCode == 1 && popoverOpen) {
            popoverOpen = false;
            return true;
        }
        return false;
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        if (popoverOpen) {
            popoverOpen = false;
        }
        scroll.wheel(delta);
        return true;
    }

    @Override
    protected void disposeContent() {
        for (CosmeticCard card : cards) {
            card.dispose();
        }
        preview.dispose();
        thumbs.clear();
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    /** Rebuilds the filtered, searched, sorted card list from the library. */
    private void refresh() {
        String query = search.text().trim().toLowerCase(Locale.ENGLISH);
        List<CosmeticAsset> results = new ArrayList<CosmeticAsset>();
        for (CosmeticAsset asset : client.cosmetics().all()) {
            if (matches(asset) && matchesQuery(asset, query)) {
                results.add(asset);
            }
        }
        Collections.sort(results, new Comparator<CosmeticAsset>() {
            public int compare(CosmeticAsset a, CosmeticAsset b) {
                // The built-in starter cape first, then alphabetical - the order a library
                // wants, not the order a hash map happened to hand over.
                if (a.builtIn() != b.builtIn()) {
                    return a.builtIn() ? -1 : 1;
                }
                return a.name().compareToIgnoreCase(b.name());
            }
        });
        visible.clear();
        visible.addAll(results);

        String signature = filter + "|" + query + "|" + visible.size();
        boolean animated = !signature.equals(lastSignature);
        lastSignature = signature;

        rebuildCards(animated);
    }

    private boolean matches(CosmeticAsset asset) {
        switch (filter) {
            case CAPES:
                return asset.type() == CosmeticType.STATIC_CAPE || asset.type() == CosmeticType.ANIMATED_CAPE;
            case ANIMATED:
                return asset.animated();
            case WINGS:
                return asset.type() == CosmeticType.WINGS;
            case HATS:
                return asset.type() == CosmeticType.HAT;
            case HALOS:
                return asset.type() == CosmeticType.HALO;
            case TRAILS:
                return asset.type() == CosmeticType.TRAIL;
            case FAVORITES:
                return asset.favorite();
            case CUSTOM:
                return !asset.builtIn();
            case ALL:
            default:
                return true;
        }
    }

    private boolean matchesQuery(CosmeticAsset asset, String query) {
        if (query.isEmpty()) {
            return true;
        }
        if (asset.name().toLowerCase(Locale.ENGLISH).contains(query)) {
            return true;
        }
        if (asset.type().displayName().toLowerCase(Locale.ENGLISH).contains(query)) {
            return true;
        }
        return asset.builtIn() ? "built-in".contains(query) : "custom".contains(query);
    }

    private void rebuildCards(boolean animated) {
        for (CosmeticCard card : cards) {
            card.dispose();
        }
        cards.clear();
        entries.clear();

        CosmeticType clearedSlot = clearedSlotFor(filter);
        if (clearedSlot != null) {
            // The "None" card: how a player takes this kind of cosmetic off, in the same
            // grid as everything else.
            CosmeticCard none = new CosmeticCard("None", "Clear this slot", true, new Runnable() {
                public void run() {
                    clearSlot();
                }
            }, null);
            cards.add(none);
            entries.add(null);
        }

        for (final CosmeticAsset asset : visible) {
            CosmeticCard card = new CosmeticCard(asset.name(), typeLabel(asset), false,
                new Runnable() {
                    public void run() {
                        equip(asset);
                    }
                }, new Runnable() {
                    public void run() {
                        toggleFavorite(asset);
                    }
                });
            cards.add(card);
            entries.add(asset);
        }

        for (int i = 0; i < cards.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            cards.get(i).place(LIST_X + col * CosmeticCard.PITCH_X,
                LIST_TOP + row * CosmeticCard.PITCH_Y, i, animated);
        }
        int rows = (cards.size() + COLS - 1) / COLS;
        scroll.content(Math.max(0, rows * CosmeticCard.PITCH_Y - (CosmeticCard.PITCH_Y - CosmeticCard.CARD_H)));
        if (scroll.offset() > scroll.maxOffset()) {
            scroll.setOffset(scroll.maxOffset());
        }
    }

    /** Pushes the live selection/favourite state into the cards, once per frame. */
    private void syncCards() {
        for (int i = 0; i < cards.size(); i++) {
            CosmeticCard card = cards.get(i);
            CosmeticAsset asset = entries.get(i);
            if (asset == null) {
                CosmeticType slot = clearedSlotFor(filter);
                card.setEquipped(slot != null && client.cosmetics().isCleared(slot));
                card.setThumb(-1, 0, 0);
            } else {
                CosmeticAsset selected = client.cosmetics().selectedFor(asset.type());
                card.setEquipped(selected != null && selected.id().equals(asset.id()));
                card.setFavorite(asset.favorite());
                syncThumb(card, asset);
            }
        }
    }

    /** Uploads an asset's preview image once and hands the texture to its card. */
    private void syncThumb(CosmeticCard card, CosmeticAsset asset) {
        if (asset.localFile() == null) {
            card.setThumb(-1, 0, 0); // built-ins have no image behind them
            return;
        }
        Thumb thumb = thumbs.get(asset.id());
        if (thumb == null) {
            try {
                BufferedImage image = client.cosmetics().getPreview(asset.id());
                if (image != null) {
                    PlayerPreview.TextureHandle handle = PlayerPreview.uploadTexture(image);
                    if (handle != null) {
                        thumb = new Thumb(handle.glId(), image.getWidth(), image.getHeight());
                        thumbs.put(asset.id(), thumb);
                    }
                }
            } catch (IOException ignored) {
                // An unreadable preview falls back to the letter initial on the card.
            }
        }
        if (thumb != null) {
            card.setThumb(thumb.glId, thumb.width, thumb.height);
        } else {
            card.setThumb(-1, 0, 0);
        }
    }

    private static String typeLabel(CosmeticAsset asset) {
        if (asset.type() == CosmeticType.STATIC_CAPE && asset.animated()) {
            return CosmeticType.ANIMATED_CAPE.displayName();
        }
        return asset.type().displayName();
    }

    /** Chip index to filter; the enum declaration order is the chip order. */
    private static Filter filterAt(int index) {
        Filter[] values = Filter.values();
        return index >= 0 && index < values.length ? values[index] : Filter.ALL;
    }

    /** The slot an "All"/type filter's None card empties; null when the filter has no slot. */
    private static CosmeticType clearedSlotFor(Filter filter) {
        switch (filter) {
            case CAPES: return CosmeticType.STATIC_CAPE;
            case WINGS: return CosmeticType.WINGS;
            case HATS: return CosmeticType.HAT;
            case HALOS: return CosmeticType.HALO;
            case TRAILS: return CosmeticType.TRAIL;
            default: return null;
        }
    }

    private void equip(CosmeticAsset asset) {
        client.cosmetics().select(asset.id());
        saveQuietly();
        setStatus(asset.name() + " equipped.");
    }

    private void clearSlot() {
        CosmeticType slot = clearedSlotFor(filter);
        if (slot == null) return;
        client.cosmetics().clear(slot);
        if (slot == CosmeticType.STATIC_CAPE) {
            // The capes filter owns both cape slots, so emptying it empties both.
            client.cosmetics().clear(CosmeticType.ANIMATED_CAPE);
        }
        saveQuietly();
        setStatus("Slot cleared.");
    }

    private void toggleFavorite(CosmeticAsset asset) {
        client.cosmetics().toggleFavorite(asset.id());
        saveQuietly();
        if (filter == Filter.FAVORITES) {
            // Membership of this filter just changed; the others only re-tint the star.
            refresh();
        }
    }

    /* ── import / folder actions ────────────────────────────────────────── */

    private void rescan() {
        try {
            client.cosmetics().rescan();
            thumbs.clear(); // files may have been edited in place
            refresh();
            saveQuietly();
            setStatus("Library rescanned.");
        } catch (IOException failed) {
            setStatus("Rescan failed: " + failed.getMessage());
        }
    }

    private void importDropped() {
        try {
            CosmeticValidationResult result =
                client.cosmetics().importNewestDroppedCosmetic(importType());
            setStatus(result.message());
            if (result.valid()) {
                refresh();
                saveQuietly();
            }
        } catch (IOException failed) {
            setStatus("Import failed: " + failed.getMessage());
        }
    }

    /**
     * The native file dialog. {@code FileDialog} is the lightest weight picker the JVM
     * offers (no Swing), it blocks the game loop like any modal, and if the platform
     * refuses to build one the action degrades to opening the drop folder instead of
     * doing nothing.
     */
    private void chooseFile() {
        try {
            FileDialog dialog = new FileDialog((Frame) null, "Import cosmetic PNG", FileDialog.LOAD);
            dialog.setDirectory(client.cosmetics().storageDirectory().toAbsolutePath().toString());
            dialog.setFile("*.png");
            dialog.setVisible(true);
            String fileName = dialog.getFile();
            String directory = dialog.getDirectory();
            dialog.dispose();
            if (fileName == null || directory == null) {
                setStatus("Import cancelled.");
                return;
            }
            CosmeticValidationResult result =
                client.cosmetics().importPng(Paths.get(directory, fileName), importType());
            setStatus(result.message());
            if (result.valid()) {
                refresh();
                saveQuietly();
            }
        } catch (Throwable failed) {
            setStatus("File picker unavailable; opened the drop folder instead.");
            FolderOpener.open(client.cosmetics().importDirectory());
        }
    }

    private void openFolder(Path directory, String what) {
        boolean opened = FolderOpener.open(directory);
        setStatus(opened ? "Opened " + what + "." : "Could not open " + what + ".");
    }

    /** The slot imports from the current filter target; everything else imports capes. */
    private CosmeticType importType() {
        CosmeticType slot = clearedSlotFor(filter);
        if (slot != null) return slot;
        if (filter == Filter.ANIMATED) return CosmeticType.ANIMATED_CAPE;
        return CosmeticType.STATIC_CAPE;
    }

    /* ── import popover ─────────────────────────────────────────────────── */

    private List<PopItem> popoverItems() {
        List<PopItem> items = new ArrayList<PopItem>();
        items.add(new PopItem("Open Cosmetics Folder", new Runnable() {
            public void run() {
                openFolder(client.cosmetics().storageDirectory(), "cosmetics folder");
            }
        }));
        if (filter != Filter.ALL && filter != Filter.FAVORITES && filter != Filter.CUSTOM) {
            final CosmeticType type = importType();
            final Path dir = client.cosmetics().typeDirectory(type);
            items.add(new PopItem("Open " + dir.getFileName() + " folder", new Runnable() {
                public void run() {
                    openFolder(dir, dir.getFileName().toString());
                }
            }));
        }
        items.add(new PopItem("Import dropped PNG", new Runnable() {
            public void run() {
                importDropped();
            }
        }));
        items.add(new PopItem("Choose file...", new Runnable() {
            public void run() {
                chooseFile();
            }
        }));
        return items;
    }

    private int[] popoverRect() {
        int height = POPOVER_PAD * 2 + popoverItems().size() * POPOVER_ITEM_H;
        return new int[] {btnImportX, BTN_Y + BTN_H + POPOVER_GAP, POPOVER_W, height};
    }

    private boolean popoverRectContains(double mx, double my) {
        int[] rect = popoverRect();
        return mx >= rect[0] && mx <= rect[0] + rect[2] && my >= rect[1] && my <= rect[1] + rect[3];
    }

    private int popoverItemAt(double mx, double my) {
        int[] rect = popoverRect();
        if (mx < rect[0] || mx > rect[0] + rect[2] || my < rect[1] || my > rect[1] + rect[3]) {
            return -1;
        }
        int index = (int) ((my - rect[1] - POPOVER_PAD) / POPOVER_ITEM_H);
        return index >= 0 && index < popoverItems().size() ? index : -1;
    }

    private void drawPopover(double mx, double my) {
        float alpha = popoverAnim.value();
        int[] rect = popoverRect();
        // The panel slides the last few units into place as it fades in; rect is design,
        // GL wants pixels, so the whole thing is converted once here.
        int designTop = rect[1] + Math.round((1.0F - alpha) * -4.0F);
        int left = GuiScale.x(rect[0]);
        int top = GuiScale.y(designTop);
        int right = GuiScale.x(rect[0] + rect[2]);
        int bottom = GuiScale.y(designTop + rect[3]);
        AetherUi.drawRoundRect(left, top, right, bottom, 8,
            AnimationMath.scaleAlpha(AetherUi.PANEL, alpha));
        AetherUi.outline(left, top, right, bottom,
            AnimationMath.scaleAlpha(AetherUi.BORDER, alpha));
        List<PopItem> items = popoverItems();
        for (int i = 0; i < items.size(); i++) {
            int itemTopDesign = designTop + POPOVER_PAD + i * POPOVER_ITEM_H;
            int itemTop = GuiScale.y(itemTopDesign);
            int itemBottom = GuiScale.y(itemTopDesign + POPOVER_ITEM_H - 2);
            boolean hovered = popoverItemAt(mx, my) == i;
            if (hovered) {
                AetherUi.drawRoundRect(left + 4, itemTop, right - 4, itemBottom, 6,
                    AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, alpha * 0.07F));
            }
            int textY = itemTop + (itemBottom - itemTop - AetherFont.height(AetherFont.Size.CAPTION)) / 2;
            AetherFont.draw(AetherFont.Size.CAPTION, items.get(i).label, left + GuiScale.w(12), textY,
                AnimationMath.scaleAlpha(hovered ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY, alpha));
        }
    }

    /* ── toolbar buttons ────────────────────────────────────────────────── */

    /**
     * Right-aligns the three toolbar buttons against the list edge. Measured through the
     * font and the current GUI scale every frame, so a window resize re-fits them, and
     * called again before hit-testing so a click cannot land on last frame's geometry.
     */
    private void layoutButtons() {
        int right = LIST_X + LIST_W;
        btnRefreshW = buttonWidth("Refresh");
        btnFolderW = buttonWidth("Open Folder");
        btnImportW = buttonWidth("Import") + 14; // caret gutter
        btnRefreshX = right - btnRefreshW;
        btnFolderX = btnRefreshX - BUTTON_GAP - btnFolderW;
        btnImportX = btnFolderX - BUTTON_GAP - btnImportW;
    }

    private static int buttonWidth(String label) {
        double unit = Math.max(0.01D, GuiScale.unit());
        return (int) Math.ceil(AetherFont.width(AetherFont.Size.CAPTION, label) / unit) + 26;
    }

    private static boolean inside(int x, int width, double mx, double my) {
        return mx >= x && mx <= x + width && my >= BTN_Y && my <= BTN_Y + BTN_H;
    }

    private void drawButtons() {
        drawButton(btnImportX, btnImportW, "Import", importHover.value());
        drawCaretDown(GuiScale.x(btnImportX + btnImportW - 16), GuiScale.y(BTN_Y + BTN_H / 2 - 2));
        drawButton(btnFolderX, btnFolderW, "Open Folder", folderHover.value());
        drawButton(btnRefreshX, btnRefreshW, "Refresh", refreshHover.value());
    }

    /** Draws one toolbar button; the rectangle arrives in design units, the GL layer wants pixels. */
    private static void drawButton(int x, int width, String label, float hot) {
        int left = GuiScale.x(x);
        int right = GuiScale.x(x + width);
        int top = GuiScale.y(BTN_Y);
        int bottom = GuiScale.y(BTN_Y + BTN_H);
        int surface = AetherUi.blend(AetherUi.SEARCH, AetherUi.CARD_HOVER, hot);
        AetherUi.drawRoundRect(left, top, right, bottom, 8, surface);
        if (hot > 0.02F) {
            AetherUi.outline(left, top, right, bottom,
                AnimationMath.scaleAlpha(AetherUi.ACCENT, hot * 0.30F));
        }
        int textY = top + (bottom - top - AetherFont.height(AetherFont.Size.CAPTION)) / 2;
        AetherFont.draw(AetherFont.Size.CAPTION, label, left + GuiScale.w(13), textY,
            AetherUi.blend(AetherUi.TEXT_SECONDARY, AetherUi.TEXT_PRIMARY, hot));
    }

    private static void drawCaretDown(int x, int y) {
        int color = AetherUi.TEXT_SECONDARY;
        Mc189Compat.drawRect(x, y, x + 10, y + 1, color);
        Mc189Compat.drawRect(x + 2, y + 2, x + 8, y + 3, color);
        Mc189Compat.drawRect(x + 4, y + 4, x + 6, y + 5, color);
    }

    private void layoutFilters() {
        // Left-aligned on its own row under the toolbar; measured per frame like the
        // module browser's bar so a resize re-fits the chips.
        filters.place(LIST_X, CHIPS_Y, Math.max(filters.requiredWidth(), 1));
    }

    /* ── helpers ────────────────────────────────────────────────────────── */

    private double mouseYFromTop(double my) {
        // Leaf's preview measures the mouse offset from the screen centre, with the
        // vertical delta clamped to +-30 design units.
        double dy = 540.0D - my;
        return Math.max(-30.0D, Math.min(30.0D, dy));
    }

    private void drawEmptyState() {
        String folder = client.cosmetics().typeDirectory(importType()).getFileName().toString();
        AetherFont.drawCentered(AetherFont.Size.BODY, "Nothing here yet.",
            GuiScale.x(LIST_X), GuiScale.y(LIST_TOP + 140), GuiScale.w(LIST_W), AetherUi.TEXT_SECONDARY);
        AetherFont.drawCentered(AetherFont.Size.CAPTION,
            "Drop PNGs into " + folder + "/ and press Refresh.",
            GuiScale.x(LIST_X), GuiScale.y(LIST_TOP + 170), GuiScale.w(LIST_W), AetherUi.TEXT_DISABLED);
    }

    private void setStatus(String message) {
        this.status = message;
        this.statusAtMillis = System.currentTimeMillis();
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    /** Scrolls one viewport down, for the screenshot walker's scrolled gallery shot. */
    public void debugNextPage() {
        scroll.scrollBy(LIST_H);
    }

    /** Switches the chip filter to index {@code index}, as a click on the chip would. */
    public void debugSelectFilter(int index) {
        filter = filterAt(index);
        filters.select(index);
        refresh();
    }

    /** Selects card {@code index}, the same action a click on it performs (equip + sound). */
    public void debugSelectCard(int index) {
        if (index < 0 || index >= cards.size()) {
            return;
        }
        CosmeticCard card = cards.get(index);
        card.onMouseClick(card.getX() + card.getWidth() / 2.0,
            card.getY() + card.getHeight() / 2.0, 0);
    }

    /** Opens/closes the import popover, so a screenshot can show it. */
    public void debugToggleImportMenu() {
        popoverOpen = !popoverOpen;
    }

    /** Legacy hook kept for old walkers: imports the newest dropped PNG as a cape. */
    public void debugImportCape() {
        importDropped();
    }
}
