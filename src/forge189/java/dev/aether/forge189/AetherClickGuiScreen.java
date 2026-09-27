package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.config.ClientPreferences;
import dev.aether.config.ProfileStore;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.screenshot.ScreenshotInfo;
import dev.aether.ui.ControlCenterSection;
import dev.aether.ui.ControlCenterState;
import dev.aether.ui.ControlFocus;
import dev.aether.ui.ModuleSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aether Control Center — the primary click GUI.
 * <p>
 * The deck keeps its proven body: one full-width row per module, an inline accordion with real
 * controls, a telemetry spine, keyboard-first input, one {@code computeLayout} feeding both
 * painting and hit-testing, and viewport culling. What changes is the navigation model:
 * <ul>
 *   <li>a sidebar of pages (Modules / Profiles / Themes / Cosmetics / Screenshots / Settings)
 *       driven by {@link ControlCenterSection}, replacing the old view-chip ribbon,</li>
 *   <li>an explicit {@link ControlCenterState.MenuState} machine instead of loose booleans,</li>
 *   <li>search and filtering go through the cached {@link ModuleSearch} instead of re-scoring
 *       every module every frame,</li>
 *   <li>Modules / Themes / Cosmetics are the same module-list machinery against different
 *       sources, so no section is a second implementation of a module list.</li>
 *   <li>Profiles, Screenshots and Settings are real pages with real content: profile
 *       save/apply/delete, the async screenshot list, and the client-wide preferences.</li>
 * </ul>
 * Layout stays single-source: the sidebar geometry, the module boxes and the page rows are all
 * computed in {@link #computeLayout} and the click path reads the same boxes the renderer drew.
 */
public final class AetherClickGuiScreen extends GuiScreen {
    /* ── keys ───────────────────────────────────────────────────────────── */
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_BACKSPACE = 14;
    private static final int KEY_TAB = 15;
    private static final int KEY_RETURN = 28;
    private static final int KEY_LEFT_CONTROL = 29;
    private static final int KEY_HOME = 199;
    private static final int KEY_UP = 200;
    private static final int KEY_PAGE_UP = 201;
    private static final int KEY_LEFT = 203;
    private static final int KEY_RIGHT = 205;
    private static final int KEY_END = 207;
    private static final int KEY_DOWN = 208;
    private static final int KEY_PAGE_DOWN = 209;
    private static final int KEY_DELETE = 211;

    /* ── metrics ────────────────────────────────────────────────────────── */
    private static final int HEADER_H = 42;
    private static final int FOOTER_H = 18;
    private static final int SIDEBAR_W = 128;
    private static final int SIDEBAR_ITEM_H = 26;
    private static final int ROW_H = 30;
    private static final int SUB_H = 22;
    private static final int SWITCH_W = 26;
    private static final int SWITCH_H = 13;
    private static final int SWATCH_W = 34;
    private static final int SWATCH_H = 13;
    private static final int SLIDER_W = 118;
    private static final int PILL_W = 96;
    private static final int KEYPILL_W = 104;
    private static final int TEXTFIELD_W = 150;
    private static final int PALETTE_COLS = 10;
    private static final int PALETTE_CELL = 14;
    private static final int DESCRIPTION_PAD = 6;
    private static final int DESCRIPTION_LINE_H = 10;
    private static final int ACCORDION_PAD = 8;
    private static final int SHOT_THUMB = 92;

    /** Filter chips shown under the section list; keys match the filter-key contract. */
    private static final String[] FILTER_CHIPS = {"!all", "!live", "!fav"};

    private static final int[] PALETTE_ROW_1 = {
        0xFFFFFFFF, 0xFFFF6B6B, 0xFFFF9F43, 0xFFFFE066, 0xFF7BED9F,
        0xFF38E0A8, 0xFF4C8DFF, 0xFF9B6BFF, 0xFFFF6BD5, 0xFF8FA0BF
    };
    private static final int[] PALETTE_ALPHA = {
        0x00FFFFFF, 0x40FFFFFF, 0x66FFFFFF, 0x99FFFFFF, 0xCCFFFFFF, 0xFFFFFFFF
    };

    /* ── state ──────────────────────────────────────────────────────────── */
    private final AetherClient client;
    private final GuiScreen parent;
    private boolean blurLoaded;

    private final ControlCenterState nav = new ControlCenterState();
    private final ModuleSearch search = new ModuleSearch();
    /** Rows currently visible on the module-driven pages; rebuilt only when the search is dirty. */
    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private final List<Box> boxes = new ArrayList<Box>();
    private final Map<String, Float> hoverBlend = new HashMap<String, Float>();
    private final int[] fpsSamples = new int[48];
    private final Rect[] spineButtons = new Rect[3];
    private final Map<String, Rect> sidebarHits = new HashMap<String, Rect>();
    private final Map<String, Rect> pageRowHits = new HashMap<String, Rect>();
    private final Map<String, Rect> pageActionHits = new HashMap<String, Rect>();

    private String query = "";
    private boolean searchFocused;
    private int selected;
    private float scroll;
    private float maxScroll;
    private long lastSaveFlash;
    private long lastSampleMillis;
    private long lastFrameMillis;
    private int fpsCursor;
    private String activeProfile;
    private String profileDraftName = "";
    private boolean screenshotRequested;
    private List<ScreenshotInfo> screenshots = Collections.emptyList();

    /* ── geometry (single source: computeLayout; renderer + hit-testing share it) ── */
    private int deckX;
    private int deckY;
    private int deckW;
    private int deckH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int spineX;
    private int spineY;
    private int spineW;
    private int spineH;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private int footerY;
    private int contentHeight;
    private long screenshotsReadAtMillis;
    private String screenshotStatus;
    private long screenshotStatusAtMillis;

    /* ── construction ───────────────────────────────────────────────────── */

    public AetherClickGuiScreen(AetherClient client) {
        this(client, null);
    }

    public AetherClickGuiScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    /**
     * Opens the deck directly on one category. The theme selector module uses this so it lands on
     * the Modules page filtered to the category, where the theme modules are listed.
     */
    void focusCategory(ModuleCategory category) {
        nav.showSection(ControlCenterSection.MODULES);
        nav.filterKey("cat:" + category.name());
        search.category(category);
    }

    ControlCenterState nav() {
        return nav;
    }

    @Override
    public void initGui() {
        search.source(client.modules().all());
        search.query(query);
        syncVisible();
        selected = clamp(selected, 0, Math.max(0, visible.size() - 1));
        lastFrameMillis = System.currentTimeMillis();
        for (ClientModule module : client.modules().all()) {
            hoverBlend.put(module.metadata().id(), Float.valueOf(0F));
        }
    }

    // MCP alias for initGui, matching the other Aether screens
    public void func_73866_w_() {
        initGui();
    }

    @Override
    public void onGuiClosed() {
        // Transient input state belongs to this screen instance: close it out so a slider drag that
        // was interrupted by the close still records its last value, and so nothing is left focused,
        // capturing a key or holding a palette when the deck is re-opened.
        if (nav.focus().kind() == ControlFocus.Kind.SLIDER && nav.focus().setting() != null) {
            writeLastChange("Setting '" + nav.focus().setting().label() + "' -> " + nav.focus().setting().value());
        }
        nav.clearFocus();
        searchFocused = false;
        if (this.blurLoaded) {
            this.blurLoaded = false;
            try {
                Mc189Compat.stopShader();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean func_73868_f() {
        return false;
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    /** The source list for a module-driven page: Themes and Cosmetics show only their categories. */
    private List<ClientModule> sectionSource() {
        ControlCenterSection section = nav.section();
        if (section == ControlCenterSection.THEMES) {
            return client.modules().byCategory(ModuleCategory.THEMES);
        }
        if (section == ControlCenterSection.COSMETICS) {
            return client.modules().byCategory(ModuleCategory.COSMETICS);
        }
        return client.modules().all();
    }

    private void syncVisible() {
        search.query(query);
        List<ClientModule> source = sectionSource();
        search.source(source);
        nav.filterKey(currentFilterKey());
        applyFilterKeyToSearch();
        List<ClientModule> results = search.results();
        if (visible.size() != results.size() || !visible.containsAll(results)) {
            visible.clear();
            visible.addAll(results);
            selected = clamp(selected, 0, Math.max(0, visible.size() - 1));
        }
    }

    private String currentFilterKey() {
        ModuleCategory category = search.category();
        boolean live = search.liveOnly();
        boolean favs = search.favoritesOnly();
        if (category != null) {
            return "cat:" + category.name();
        }
        if (live) {
            return "!live";
        }
        if (favs) {
            return "!fav";
        }
        return "!all";
    }

    private void applyFilterKeyToSearch() {
        String key = nav.filterKey();
        search.category(null);
        search.liveOnly(false);
        search.favoritesOnly(false);
        if ("!live".equals(key)) {
            search.liveOnly(true);
        } else if ("!fav".equals(key)) {
            search.favoritesOnly(true);
        } else if (key.startsWith("cat:")) {
            try {
                search.category(ModuleCategory.valueOf(key.substring(4)));
            } catch (IllegalArgumentException unknownCategory) {
                nav.filterKey("!all");
            }
        }
    }

    /* ── layout ─────────────────────────────────────────────────────────── */

    private void computeLayout(int w, int h) {
        int mouseX = scaledMouseX(w);
        int mouseY = scaledMouseY(h);

        int margin = clamp(w / 24, 6, 16);
        deckW = Math.min(1040, w - margin * 2);
        deckH = h - margin * 2;
        deckX = (w - deckW) / 2;
        deckY = margin;

        int bodyTop = deckY + HEADER_H;
        footerY = deckY + deckH - FOOTER_H;

        sidebarX = deckX + 8;
        sidebarY = bodyTop + 6;
        sidebarW = deckW >= 560 ? SIDEBAR_W : 0;
        sidebarH = footerY - sidebarY - 6;

        listX = sidebarW > 0 ? sidebarX + sidebarW + 8 : sidebarX;
        int innerW = deckX + deckW - listX - 8;
        boolean showSpine = innerW >= 430 && sidebarW > 0;
        spineW = showSpine ? clamp((int) (innerW * 0.26F), 132, 200) : 0;
        spineX = listX + innerW - spineW;
        spineY = sidebarY;
        spineH = sidebarH;

        listW = innerW - spineW;
        listY = sidebarY;
        listH = footerY - listY - 6;

        boxes.clear();
        sidebarHits.clear();
        pageRowHits.clear();
        pageActionHits.clear();
        int rowX = listX + 6;
        int rowW = listW - 12;
        // Page rows start below the page's title line; the module list does not have one.
        int y = (nav.showsModuleList() ? listY : listY + 24) - (int) scroll;
        if (nav.showsModuleList()) {
            for (ClientModule module : visible) {
                boolean isOpen = nav.isExpanded(module.metadata().id());
                int height = ROW_H + (isOpen ? accordionHeight(module) : 0);
                Box box = new Box(module, rowX, y, rowW, height, isOpen);
                box.header = rect(rowX, y, rowW, ROW_H);
                box.toggle = rect(rowX + rowW - 58, y + 9, SWITCH_W, SWITCH_H);
                box.chevron = rect(rowX + rowW - 20, y + 10, 12, 10);
                if (isOpen) {
                    int accordionTop = y + ROW_H;
                    if (module.metadata().category() == ModuleCategory.HUD) {
                        box.reset = rect(rowX + rowW - 96, accordionTop + 3, 42, 12);
                        box.hud = rect(rowX + rowW - 48, accordionTop + 3, 42, 12);
                    } else {
                        box.reset = rect(rowX + rowW - 48, accordionTop + 3, 42, 12);
                    }
                    int controlTop = accordionContentTop(box);
                    for (Setting<?> setting : module.settings()) {
                        box.controls.put(setting.id(), controlRect(setting, rowX, rowW, controlTop));
                        controlTop += SUB_H;
                    }
                }
                boxes.add(box);
                y += height + 3;
            }
        } else {
            y = layoutPageRows(rowX, rowW, y);
        }
        contentHeight = (y + (int) scroll) - listY;
        maxScroll = Math.max(0, contentHeight - listH);
        scroll = clamp(scroll, 0, maxScroll);
        layoutSidebar();
        advanceHover(mouseX, mouseY);
    }

    /**
     * Registers the sidebar's hit rectangles during layout, not during painting: the click path
     * runs {@link #computeLayout} first, and a map cleared by layout and refilled by the paint
     * pass would hand every click an empty map.
     */
    private void layoutSidebar() {
        sidebarHits.clear();
        if (sidebarW <= 0) {
            return;
        }
        int x = sidebarX + 8;
        int w = sidebarW - 16;
        int y = sidebarY + 8;
        for (ControlCenterSection section : ControlCenterSection.ordered()) {
            sidebarHits.put(section.key(), rect(x, y, w, SIDEBAR_ITEM_H - 6));
            y += SIDEBAR_ITEM_H;
        }
        y += 8 + 12; // FILTER caption
        for (String chip : FILTER_CHIPS) {
            sidebarHits.put(chip, rect(x, y, w, 15));
            y += 18;
        }
        for (ModuleCategory category : orderedCategories()) {
            if (client.modules().byCategory(category).isEmpty()) {
                continue;
            }
            if (y + 15 > sidebarY + sidebarH) {
                break;
            }
            sidebarHits.put("cat:" + category.name(), rect(x, y, w, 15));
            y += 18;
        }
    }

    /** Lays out the non-module pages' rows; returns the y after the last row. */
    private int layoutPageRows(int rowX, int rowW, int y) {
        switch (nav.section()) {
            case PROFILES: {
                for (String name : client.profiles().names()) {
                    pageRowHits.put(name, rect(rowX, y, rowW, ROW_H));
                    pageActionHits.put("apply:" + name, rect(rowX + rowW - 52, y + 9, 46, 12));
                    pageActionHits.put("delete:" + name, rect(rowX + rowW - 100, y + 9, 44, 12));
                    y += ROW_H + 3;
                }
                pageActionHits.put("save", rect(rowX, y, 100, 14));
                y += 14 + 6;
                pageActionHits.put("newname", rect(rowX + 106, y - 20, 150, 14));
                break;
            }
            case SCREENSHOTS: {
                refreshScreenshotList();
                for (ScreenshotInfo shot : screenshots) {
                    pageRowHits.put(shot.name(), rect(rowX, y, rowW, SHOT_THUMB));
                    pageActionHits.put("open:" + shot.name(), rect(rowX + rowW - 104, y + SHOT_THUMB / 2 - 6, 48, 12));
                    pageActionHits.put("delete:" + shot.name(), rect(rowX + rowW - 52, y + SHOT_THUMB / 2 - 6, 46, 12));
                    y += SHOT_THUMB + 4;
                }
                pageActionHits.put("capture", rect(rowX, y, 130, 14));
                break;
            }
            case SETTINGS: {
                pageActionHits.put("pref:save_on_close", rect(rowX, y, rowW, ROW_H));
                y += ROW_H + 3;
                pageActionHits.put("pref:show_tooltips", rect(rowX, y, rowW, ROW_H));
                y += ROW_H + 3;
                break;
            }
            default:
                break;
        }
        return y;
    }

    private void refreshScreenshotList() {
        long now = System.currentTimeMillis();
        // Metadata read is throttled: a directory listing per frame would hammer the disk.
        if (screenshotsReadAtMillis == 0L || now - screenshotsReadAtMillis >= 2000L) {
            screenshotsReadAtMillis = now;
            client.screenshots().store().ensureDirectory();
            screenshots = client.screenshots().store().list();
        }
    }

    private void advanceHover(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        float delta = lastFrameMillis == 0L ? 0.05F : Math.min(0.1F, (now - lastFrameMillis) / 1000F);
        lastFrameMillis = now;
        for (Box box : boxes) {
            String id = box.module.metadata().id();
            boolean overRow = mouseX >= box.x && mouseX <= box.x + box.w && mouseY >= box.y && mouseY <= box.y + box.height
                && mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH;
            boolean hovered = !searchFocused && overRow;
            Float current = hoverBlend.get(id);
            float value = current == null ? 0F : current.floatValue();
            float target = hovered ? 1F : 0F;
            value += (target - value) * delta * 14F;
            if (target == 0F && value < 0.02F) {
                value = 0F;
            } else if (target == 1F && value > 0.98F) {
                value = 1F;
            }
            hoverBlend.put(id, Float.valueOf(value));
        }
    }

    private static Rect controlRect(Setting<?> setting, int rowX, int rowW, int top) {
        int right = rowX + rowW - 20;
        switch (setting.type()) {
            case BOOLEAN:
                return rect(right - SWITCH_W, top + 4, SWITCH_W, SWITCH_H);
            case NUMBER:
                return rect(right - SLIDER_W, top + 7, SLIDER_W, 8);
            case COLOR:
                return rect(right - SWATCH_W, top + 4, SWATCH_W, SWATCH_H);
            case CHOICE:
                return rect(right - PILL_W, top + 3, PILL_W, 15);
            case KEYBIND:
                return rect(right - KEYPILL_W, top + 3, KEYPILL_W, 15);
            case TEXT:
            default:
                return rect(right - TEXTFIELD_W, top + 2, TEXTFIELD_W, 16);
        }
    }

    /**
     * Absolute y of the first setting row inside an expanded accordion. Layout and painting both
     * anchor on this, so a control can never be drawn offset from the rectangle that receives its
     * clicks.
     */
    private int accordionContentTop(Box box) {
        return box.y + ROW_H + DESCRIPTION_PAD + descriptionLineCount(box.module) * DESCRIPTION_LINE_H;
    }

    private int descriptionLineCount(ClientModule module) {
        List<String> lines = wrap(module.metadata().description(), Math.max(80, deckW / 3));
        return clamp(lines.size(), 1, 2);
    }

    private int accordionHeight(ClientModule module) {
        return DESCRIPTION_PAD + descriptionLineCount(module) * DESCRIPTION_LINE_H
            + module.settings().size() * SUB_H + ACCORDION_PAD;
    }

    private int scaledMouseX(int w) {
        return Mc189Compat.mouseX() * w / Math.max(1, Mc189Compat.displayWidth(Mc189Compat.minecraft()));
    }

    private int scaledMouseY(int h) {
        return h - Mc189Compat.mouseY() * h / Math.max(1, Mc189Compat.displayHeight(Mc189Compat.minecraft())) - 1;
    }

    /* ── drawing ────────────────────────────────────────────────────────── */

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    private void render(int mouseX, int mouseY, float partialTicks) {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) {
            return;
        }
        syncTheme();
        syncBlur();
        pollScreenshotRequest();
        Object font = Mc189Compat.screenFontRenderer(this);
        computeLayout(w, h);
        sampleFps();

        drawBackdrop(w, h);
        drawDeck();
        drawHeader(font, mouseX, mouseY);
        drawSidebar(font, mouseX, mouseY);
        pushClip(listX, listY, listW, listH);
        if (nav.showsModuleList()) {
            drawRows(font, mouseX, mouseY);
        } else {
            drawPageRows(font, mouseX, mouseY);
        }
        popClip();
        if (spineW > 0) {
            drawSpine(font, mouseX, mouseY);
        }
        drawFooter(font);
        drawPalette(font, mouseX, mouseY);
    }

    private void drawBackdrop(int w, int h) {
        int dim = backdropDim();
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            int top = i * h / steps;
            int bottom = (i + 1) * h / steps + 1;
            int tint = lerpColor(AetherUi.SCRIM_TOP, AetherUi.SCRIM_BOTTOM, (float) i / steps);
            Mc189Compat.drawRect(0, top, w, bottom, AetherUi.withAlpha(tint, dim));
        }
        long time = System.currentTimeMillis() / 40L;
        for (int i = 0; i < 14; i++) {
            int x = (int) ((i * 137L + time) % Math.max(1L, (long) w + 80L)) - 40;
            int y = (i * 53) % Math.max(1, h);
            int size = 1 + (i % 3);
            Mc189Compat.drawRect(x, y, x + size, y + size, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        }
    }

    private void drawDeck() {
        roundRect(deckX - 4, deckY - 4, deckX + deckW + 4, deckY + deckH + 4, 8, AetherUi.withAlpha(AetherUi.SHADOW, 0x33));
        roundRect(deckX, deckY, deckX + deckW, deckY + deckH, 6, AetherUi.DECK_BG);
        outline(deckX, deckY, deckX + deckW, deckY + deckH, AetherUi.DECK_EDGE);
        Mc189Compat.drawRect(deckX + 1, deckY + 1, deckX + deckW - 1, deckY + 2, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));
        Mc189Compat.drawRect(deckX + 1, deckY + HEADER_H, deckX + deckW - 1, deckY + HEADER_H + 1, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        Mc189Compat.drawRect(deckX + 1, footerY - 1, deckX + deckW - 1, footerY, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
    }

    private void drawHeader(Object font, int mouseX, int mouseY) {
        int x = deckX + 12;
        int y = deckY + 12;
        drawMark(x, y + 2, AetherUi.ACCENT);
        text(font, "AETHER", x + 16, y + 1, AetherUi.TEXT_PRIMARY);
        String sectionLabel = nav.section().label();
        text(font, sectionLabel.toUpperCase(Locale.ENGLISH), x + 16 + width(font, "AETHER") + 8, y + 1, AetherUi.TEXT_DISABLED);
        text(font, "v" + client.version().name(), x + 16, y + 12, AetherUi.TEXT_DISABLED);

        int searchW = clamp(deckW / 3, 120, 260);
        int searchX = deckX + deckW - searchW - 96;
        int searchY = deckY + 10;
        boolean overSearch = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + 20;
        if (overSearch && !searchFocused) {
            searchFocused = true;
        }
        roundRect(searchX, searchY, searchX + searchW, searchY + 20, 4, searchFocused ? AetherUi.withAlpha(AetherUi.SURFACE, 0xF0) : AetherUi.withAlpha(AetherUi.SURFACE, 0xB0));
        outline(searchX, searchY, searchX + searchW, searchY + 20, searchFocused ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        drawSearchGlyph(searchX + 8, searchY + 6, searchFocused ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);
        String shown = query.isEmpty() && !searchFocused ? "type to search" : query;
        int textColor = query.isEmpty() && !searchFocused ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY;
        String caret = searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "";
        text(font, trim(font, shown + caret, searchW - 30), searchX + 22, searchY + 6, textColor);
        if (!query.isEmpty()) {
            String matches = String.valueOf(visible.size());
            text(font, matches, searchX + searchW - width(font, matches) - 6, searchY + 6, AetherUi.ACCENT);
        }

        int chipW = 62;
        int chipX = deckX + deckW - chipW - 12;
        roundRect(chipX, searchY, chipX + chipW, searchY + 20, 4, AetherUi.withAlpha(AetherUi.SURFACE, 0xB0));
        text(font, "FPS", chipX + 8, searchY + 6, AetherUi.TEXT_DISABLED);
        text(font, String.valueOf(Mc189Compat.debugFps()), chipX + 32, searchY + 6, AetherUi.TEXT_PRIMARY);
    }

    private void drawSidebar(Object font, int mouseX, int mouseY) {
        if (sidebarW <= 0) {
            return;
        }
        roundRect(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, 4, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1A));
        outline(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1F));
        int x = sidebarX + 8;
        int w = sidebarW - 16;
        int y = sidebarY + 8;
        for (ControlCenterSection section : ControlCenterSection.ordered()) {
            Rect hit = sidebarHits.get(section.key());
            boolean active = section == nav.section();
            boolean hover = hit != null && hit.contains(mouseX, mouseY);
            roundRect(x, y, x + w, y + SIDEBAR_ITEM_H - 6, 3, active ? AetherUi.ROW_SELECTED : (hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG));
            if (active) {
                Mc189Compat.drawRect(x, y + 2, x + 2, y + SIDEBAR_ITEM_H - 8, AetherUi.ACCENT);
            }
            text(font, section.label(), x + 10, y + 5, active ? AetherUi.TEXT_PRIMARY : (hover ? AetherUi.TEXT_SECONDARY : AetherUi.TEXT_DISABLED));
            y += SIDEBAR_ITEM_H;
        }

        y += 8;
        text(font, "FILTER", x, y, AetherUi.TEXT_DISABLED);
        y += 12;
        for (String chip : FILTER_CHIPS) {
            String label = "!all".equals(chip) ? "All" : ("!live".equals(chip) ? "Live" : "Favorites");
            boolean activeChip = nav.filterKey().equals(chip);
            Rect hit = sidebarHits.get(chip);
            boolean chipHover = hit != null && hit.contains(mouseX, mouseY);
            roundRect(x, y, x + w, y + 15, 3, activeChip ? AetherUi.ROW_SELECTED : (chipHover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG));
            text(font, label, x + 10, y + 4, activeChip ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
            y += 18;
        }
        for (ModuleCategory category : orderedCategories()) {
            if (client.modules().byCategory(category).isEmpty()) {
                continue;
            }
            if (y + 15 > sidebarY + sidebarH) {
                break;
            }
            String key = "cat:" + category.name();
            String label = label(category);
            boolean activeChip = nav.filterKey().equals(key);
            Rect hit = sidebarHits.get(key);
            boolean chipHover = hit != null && hit.contains(mouseX, mouseY);
            roundRect(x, y, x + w, y + 15, 3, activeChip ? AetherUi.ROW_SELECTED : (chipHover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG));
            text(font, trim(font, label, w - 16), x + 10, y + 4, activeChip ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
            y += 18;
        }
    }

    private void drawRows(Object font, int mouseX, int mouseY) {
        if (visible.isEmpty()) {
            text(font, query.isEmpty() ? "Nothing here yet." : "No module matches \"" + query + "\".", listX + 14, listY + 18, AetherUi.TEXT_DISABLED);
            return;
        }
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            if (box.y + box.height < listY - 4 || box.y > listY + listH + 4) {
                continue;
            }
            drawRow(font, box, i == selected, mouseX, mouseY);
        }
    }

    private void drawRow(Object font, Box box, boolean isSelected, int mouseX, int mouseY) {
        ClientModule module = box.module;
        boolean on = module.state() == ModuleState.ENABLED;
        Float blendValue = hoverBlend.get(module.metadata().id());
        float blend = blendValue == null ? 0F : blendValue.floatValue();
        boolean headerHover = box.header.contains(mouseX, mouseY);

        int fill = on ? AetherUi.ROW_ON_TINT : AetherUi.ROW_BG;
        if (blend > 0F) {
            fill = blendColor(fill, AetherUi.ROW_HOVER, blend * 0.8F);
        }
        if (isSelected) {
            fill = blendColor(fill, AetherUi.ROW_SELECTED, 0.85F);
        }
        roundRect(box.x, box.y, box.x + box.w, box.y + ROW_H, 4, fill);
        if (on) {
            Mc189Compat.drawRect(box.x, box.y + 5, box.x + 2, box.y + ROW_H - 5, AetherUi.ACCENT_ON);
        }
        if (isSelected) {
            Mc189Compat.drawRect(box.x, box.y + 4, box.x + 2, box.y + ROW_H - 4, AetherUi.ACCENT);
        }

        drawDot(box.x + 10, box.y + ROW_H / 2 - 3, on ? AetherUi.ACCENT_ON : AetherUi.TRACK);

        int nameX = box.x + 22;
        int nameW = box.w - 22 - 130;
        if (module.metadata().favoriteByDefault()) {
            drawStar(box.x + 8, box.y + 4, AetherUi.STAR);
            nameX += 10;
            nameW -= 10;
        }
        text(font, trim(font, module.metadata().name(), nameW), nameX, box.y + 6, on ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        String subtitle = headerHover && !isSelected
            ? module.metadata().description()
            : (isSelected ? label(module.metadata().category()) + " / " + module.metadata().id() : module.metadata().id());
        text(font, trim(font, subtitle, nameW), nameX, box.y + 17, headerHover && !isSelected ? AetherUi.TEXT_SECONDARY : AetherUi.TEXT_DISABLED);

        String tag = label(module.metadata().category());
        int tagW = width(font, tag) + 10;
        int tagX = box.x + box.w - 130 - tagW;
        if (tagX > nameX + 130) {
            roundRect(tagX, box.y + 8, tagX + tagW, box.y + 21, 3, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
            text(font, tag, tagX + 5, box.y + 11, AetherUi.TEXT_DISABLED);
        }
        if (!module.settings().isEmpty()) {
            text(font, module.settings().size() + " set", box.x + box.w - 84, box.y + 11, AetherUi.TEXT_DISABLED);
        }
        drawSwitch(box.toggle, on);
        drawChevron(box.chevron.x + 3, box.y + 12, box.expanded ? 1 : -1, isSelected ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);

        if (box.expanded) {
            drawAccordion(font, box, mouseX, mouseY);
        }
    }

    private void drawAccordion(Object font, Box box, int mouseX, int mouseY) {
        int top = box.y + ROW_H;
        Mc189Compat.drawRect(box.x + 1, top - 1, box.x + box.w - 1, top + box.height - ROW_H, AetherUi.withAlpha(AetherUi.SHADOW, 0x18));
        Mc189Compat.drawRect(box.x + 10, top, box.x + 11, top + box.height - ROW_H - 4, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));

        List<String> lines = wrap(box.module.metadata().description(), Math.max(80, box.w - 140));
        int lineY = top + 2;
        for (int i = 0; i < lines.size() && i < 2; i++) {
            text(font, trim(font, lines.get(i), box.w - 140), box.x + 20, lineY, AetherUi.TEXT_DISABLED);
            lineY += DESCRIPTION_LINE_H;
        }

        if (box.reset != null) {
            boolean resetHover = box.reset.contains(mouseX, mouseY);
            roundRect(box.reset.x, box.reset.y, box.reset.x + box.reset.w, box.reset.y + box.reset.h, 3,
                resetHover ? AetherUi.withAlpha(AetherUi.WARN, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF));
            text(font, "RESET", box.reset.x + 8, box.reset.y + 3, resetHover ? AetherUi.WARN : AetherUi.TEXT_DISABLED);
        }
        if (box.hud != null) {
            boolean hudHover = box.hud.contains(mouseX, mouseY);
            roundRect(box.hud.x, box.hud.y, box.hud.x + box.hud.w, box.hud.y + box.hud.h, 3,
                hudHover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF));
            text(font, "HUD", box.hud.x + 13, box.hud.y + 3, hudHover ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);
        }

        if (box.module.settings().isEmpty()) {
            text(font, "This module has no settings.", box.x + 20, top + 20, AetherUi.TEXT_DISABLED);
            return;
        }

        int rowTop = accordionContentTop(box);
        for (Setting<?> setting : box.module.settings()) {
            boolean rowHover = mouseY >= rowTop && mouseY <= rowTop + SUB_H && mouseX >= box.x + 18 && mouseX <= box.x + box.w - 12;
            if (rowHover) {
                Mc189Compat.drawRect(box.x + 18, rowTop, box.x + box.w - 12, rowTop + SUB_H - 2, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x10));
            }
            text(font, trim(font, setting.label(), box.w - 220), box.x + 24, rowTop + 6, rowHover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
            Rect control = box.controls.get(setting.id());
            if (control != null) {
                drawControl(font, setting, control, mouseX, mouseY);
            }
            rowTop += SUB_H;
        }
    }

    private void drawControl(Object font, Setting<?> setting, Rect rect, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        ControlFocus focus = nav.focus();
        boolean editing = focus.is(ControlFocus.Kind.TEXT) && focus.targets(setting);
        boolean capturing = focus.is(ControlFocus.Kind.KEYBIND) && focus.targets(setting);
        switch (setting.type()) {
            case BOOLEAN: {
                drawSwitch(rect, Boolean.TRUE.equals(setting.value()));
                break;
            }
            case NUMBER: {
                int value = number(setting.value());
                int[] range = range(setting);
                float pct = range[1] > range[0] ? clamp((float) (value - range[0]) / (float) (range[1] - range[0]), 0F, 1F) : 0F;
                roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 3, AetherUi.TRACK);
                int fillW = (int) (pct * rect.w);
                if (fillW > 0) {
                    int active = hover || (focus.is(ControlFocus.Kind.SLIDER) && focus.targets(setting)) ? AetherUi.ACCENT_ON : AetherUi.ACCENT;
                    roundRect(rect.x, rect.y, rect.x + Math.max(3, fillW), rect.y + rect.h, 3, active);
                }
                Mc189Compat.drawRect(rect.x + fillW - 1, rect.y - 3, rect.x + fillW + 1, rect.y + rect.h + 3, AetherUi.TEXT_PRIMARY);
                String valueText = String.valueOf(value);
                text(font, valueText, rect.x - width(font, valueText) - 8, rect.y - 3, hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
                break;
            }
            case COLOR: {
                int color = number(setting.value());
                Mc189Compat.drawRect(rect.x - 1, rect.y - 1, rect.x + rect.w + 1, rect.y + rect.h + 1, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
                Mc189Compat.drawRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h - 4, color);
                if (((color >> 24) & 0xFF) < 0xFF) {
                    for (int i = 0; i < rect.w; i += 4) {
                        Mc189Compat.drawRect(rect.x + i, rect.y + rect.h - 4, rect.x + Math.min(rect.w, i + 2), rect.y + rect.h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
                    }
                }
                String hex = "hex " + String.format("%08X", Integer.valueOf(color));
                text(font, hex, rect.x - width(font, hex) - 8, rect.y, hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
                break;
            }
            case KEYBIND: {
                int key = number(setting.value());
                String shown = capturing ? "press a key" : Mc189Compat.keyName(key);
                roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 3, capturing ? AetherUi.withAlpha(AetherUi.ACCENT, 0x66) : (hover ? AetherUi.ROW_HOVER : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E)));
                outline(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, capturing ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
                center(font, trim(font, shown, rect.w - 8), rect.x, rect.y + 4, rect.w, capturing ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
                break;
            }
            case TEXT: {
                String value = editing ? focus.text() : String.valueOf(setting.value());
                roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 3, editing ? AetherUi.withAlpha(AetherUi.SURFACE, 0xF0) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
                outline(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, editing ? AetherUi.ACCENT : (hover ? AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22)));
                String shown = value == null || value.isEmpty() ? (editing ? "" : "empty") : value;
                if (editing && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                    shown = shown + "_";
                }
                text(font, trim(font, shown, rect.w - 10), rect.x + 5, rect.y + 4, editing ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
                break;
            }
            case CHOICE:
            default: {
                String value = String.valueOf(setting.value());
                roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 3, hover ? AetherUi.ROW_HOVER : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
                center(font, trim(font, value, rect.w - 20), rect.x, rect.y + 4, rect.w, hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
                drawChevron(rect.x + 5, rect.y + 6, 1, AetherUi.TEXT_DISABLED);
                drawChevron(rect.x + rect.w - 7, rect.y + 6, -1, AetherUi.TEXT_DISABLED);
                break;
            }
        }
    }

    private void drawSpine(Object font, int mouseX, int mouseY) {
        roundRect(spineX, spineY, spineX + spineW, spineY + spineH, 4, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1A));
        outline(spineX, spineY, spineX + spineW, spineY + spineH, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1F));

        int x = spineX + 10;
        int y = spineY + 9;
        int w = spineW - 20;
        text(font, "TELEMETRY", x, y, AetherUi.TEXT_DISABLED);
        Mc189Compat.drawRect(x, y + 12, x + w, y + 13, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
        y += 20;

        int fps = Mc189Compat.debugFps();
        text(font, String.valueOf(fps), x, y, fps >= 120 ? AetherUi.ACCENT_ON : (fps >= 60 ? AetherUi.TEXT_PRIMARY : AetherUi.WARN));
        text(font, "fps", x + width(font, String.valueOf(fps)) + 6, y + 2, AetherUi.TEXT_DISABLED);
        y += 12;
        drawSparkline(x, y, w, 22);
        y += 28;

        int total = client.modules().all().size();
        int enabled = 0;
        for (ClientModule module : client.modules().all()) {
            if (module.state() == ModuleState.ENABLED) {
                enabled++;
            }
        }
        text(font, "ENABLED", x, y, AetherUi.TEXT_DISABLED);
        String ratio = enabled + "/" + total;
        text(font, ratio, x + w - width(font, ratio), y, AetherUi.TEXT_PRIMARY);
        y += 12;
        float pct = total == 0 ? 0F : (float) enabled / total;
        roundRect(x, y, x + w, y + 6, 2, AetherUi.TRACK);
        if (pct > 0F) {
            roundRect(x, y, x + Math.max(3, (int) (pct * w)), y + 6, 2, AetherUi.ACCENT);
        }
        y += 14;

        y += 4;
        text(font, "CLIENT", x, y, AetherUi.TEXT_DISABLED);
        y += 12;
        text(font, trim(font, "build " + client.version().name(), w), x, y, AetherUi.TEXT_SECONDARY);
        y += 10;
        text(font, trim(font, "java " + client.platform().javaVersion(), w), x, y, AetherUi.TEXT_SECONDARY);
        y += 10;
        text(font, trim(font, "theme " + client.theme().name(), w), x, y, AetherUi.TEXT_SECONDARY);
        y += 10;
        String server = Mc189Compat.serverAddress(Mc189Compat.minecraft());
        text(font, trim(font, server == null ? "on the title screen" : "server " + server, w), x, y, AetherUi.TEXT_SECONDARY);
        y += 10;
        File configFile = client.configFile() == null ? null : client.configFile().toFile();
        text(font, trim(font, configFile == null ? "config n/a" : "config " + configFile.getName(), w), x, y, AetherUi.TEXT_DISABLED);

        if (y + 50 < spineY + spineH) {
            y = spineY + spineH - 47;
            drawSpineButton(font, x, y, w, "HUD EDITOR", mouseX, mouseY, 0);
            drawSpineButton(font, x, y + 15, w, "COSMETICS", mouseX, mouseY, 1);
            drawSpineButton(font, x, y + 30, w, "SAVE NOW", mouseX, mouseY, 2);
        } else {
            for (int i = 0; i < spineButtons.length; i++) {
                spineButtons[i] = null;
            }
        }
    }

    private void drawSpineButton(Object font, int x, int y, int w, String label, int mouseX, int mouseY, int index) {
        Rect rect = rect(x, y, w, 13);
        spineButtons[index] = rect;
        boolean hover = rect.contains(mouseX, mouseY);
        roundRect(x, y, x + w, y + 13, 3, hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.ROW_BG);
        text(font, label, x + (w - width(font, label)) / 2, y + 3, hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
    }

    /** Draws the non-module pages' rows, reading the same geometry the click path uses. */
    private void drawPageRows(Object font, int mouseX, int mouseY) {
        switch (nav.section()) {
            case PROFILES: {
                text(font, "PROFILES", listX + 14, listY + 8, AetherUi.TEXT_DISABLED);
                int y = listY + 24;
                for (String name : client.profiles().names()) {
                    Rect row = pageRowHits.get(name);
                    if (row == null || row.y + row.h < listY - 4 || row.y > listY + listH + 4) {
                        continue;
                    }
                    boolean hover = row.contains(mouseX, mouseY);
                    int enabledCount = client.profiles().enabledCount(name);
                    roundRect(row.x, row.y, row.x + row.w, row.y + row.h, 4, hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
                    text(font, trim(font, name, row.w - 160), row.x + 12, row.y + 6, AetherUi.TEXT_PRIMARY);
                    text(font, enabledCount + " modules on", row.x + 12, row.y + 17, AetherUi.TEXT_DISABLED);
                    drawPageAction(font, pageActionHits.get("apply:" + name), "APPLY", mouseX, mouseY, AetherUi.ACCENT);
                    drawPageAction(font, pageActionHits.get("delete:" + name), "DELETE", mouseX, mouseY, AetherUi.WARN);
                    y = row.y + ROW_H + 3;
                }
                Rect save = pageActionHits.get("save");
                if (save != null) {
                    drawPageAction(font, save, "SAVE CURRENT", mouseX, mouseY, AetherUi.ACCENT);
                }
                Rect nameField = pageActionHits.get("newname");
                if (nameField != null) {
                    boolean editing = nav.focus().is(ControlFocus.Kind.TEXT);
                    roundRect(nameField.x, nameField.y, nameField.x + nameField.w, nameField.y + nameField.h, 3,
                        editing ? AetherUi.withAlpha(AetherUi.SURFACE, 0xF0) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
                    outline(nameField.x, nameField.y, nameField.x + nameField.w, nameField.y + nameField.h,
                        editing ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22));
                    String buffer = nav.focus().is(ControlFocus.Kind.TEXT) ? nav.focus().text() : profileDraftName;
                    if (nav.focus().is(ControlFocus.Kind.TEXT) && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                        buffer = buffer + "_";
                    }
                    text(font, trim(font, buffer.isEmpty() ? "new profile name" : buffer, nameField.w - 10),
                        nameField.x + 5, nameField.y + 3, AetherUi.TEXT_SECONDARY);
                }
                break;
            }
            case SCREENSHOTS: {
                text(font, "SCREENSHOTS", listX + 14, listY + 8, AetherUi.TEXT_DISABLED);
                int y = listY + 24;
                if (screenshots.isEmpty()) {
                    text(font, "No screenshots yet.", listX + 14, y + 4, AetherUi.TEXT_DISABLED);
                }
                for (ScreenshotInfo shot : screenshots) {
                    Rect row = pageRowHits.get(shot.name());
                    if (row == null || row.y + row.h < listY - 4 || row.y > listY + listH + 4) {
                        continue;
                    }
                    boolean hover = row.contains(mouseX, mouseY);
                    roundRect(row.x, row.y, row.x + row.w, row.y + row.h, 4, hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
                    text(font, trim(font, shot.name(), row.w - 180), row.x + 12, row.y + 12, AetherUi.TEXT_PRIMARY);
                    text(font, shot.timestamp() + " · " + (shot.sizeBytes() / 1024) + " KB", row.x + 12, row.y + 24, AetherUi.TEXT_DISABLED);
                    text(font, "pixels are never decoded for this list", row.x + 12, row.y + SHOT_THUMB - 16, AetherUi.TEXT_DISABLED);
                    drawPageAction(font, pageActionHits.get("open:" + shot.name()), "OPEN FOLDER", mouseX, mouseY, AetherUi.ACCENT);
                    drawPageAction(font, pageActionHits.get("delete:" + shot.name()), "DELETE", mouseX, mouseY, AetherUi.WARN);
                }
                Rect capture = pageActionHits.get("capture");
                if (capture != null) {
                    drawPageAction(font, capture, client.screenshots().state().isInFlight() ? "ENCODING…" : "TAKE SCREENSHOT", mouseX, mouseY, AetherUi.ACCENT);
                }
                break;
            }
            case SETTINGS: {
                text(font, "SETTINGS", listX + 14, listY + 8, AetherUi.TEXT_DISABLED);
                ClientPreferences prefs = client.preferences();
                drawPreferenceRow(font, pageActionHits.get("pref:save_on_close"), "Save config when the menu closes",
                    prefs.saveOnClose(), mouseX, mouseY);
                drawPreferenceRow(font, pageActionHits.get("pref:show_tooltips"), "Show tooltips",
                    prefs.showTooltips(), mouseX, mouseY);
                text(font, "The menu opens on " + prefs.openSection() + " and remembers it.", listX + 20, listY + 24 + ROW_H * 2 + 12, AetherUi.TEXT_DISABLED);
                break;
            }
            default:
                // Themes and Cosmetics are module pages; SETTINGS etc. handled above.
                break;
        }
    }

    private void drawPageAction(Object font, Rect rect, String label, int mouseX, int mouseY, int accent) {
        if (rect == null) {
            return;
        }
        boolean hover = rect.contains(mouseX, mouseY);
        roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 3, hover ? AetherUi.withAlpha(accent, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF));
        text(font, trim(font, label, rect.w - 8), rect.x + 6, rect.y + 2, hover ? accent : AetherUi.TEXT_DISABLED);
    }

    private void drawPreferenceRow(Object font, Rect rect, String label, boolean on, int mouseX, int mouseY) {
        if (rect == null) {
            return;
        }
        boolean hover = rect.contains(mouseX, mouseY);
        roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 4, hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
        text(font, label, rect.x + 12, rect.y + 6, AetherUi.TEXT_PRIMARY);
        drawSwitch(rect(rect.x + rect.w - SWITCH_W - 12, rect.y + 9, SWITCH_W, SWITCH_H), on);
    }

    private void drawFooter(Object font) {
        int y = footerY + 5;
        text(font, trim(font, "up/down move   enter toggle   space expand   tab page   / search   esc close", deckW / 2), deckX + 12, y, AetherUi.TEXT_DISABLED);
        String status;
        int color;
        if (nav.focus().is(ControlFocus.Kind.TEXT)) {
            status = "editing text";
            color = AetherUi.ACCENT;
        } else if (nav.focus().is(ControlFocus.Kind.KEYBIND)) {
            status = "binding " + (nav.focus().setting() == null ? "" : nav.focus().setting().label());
            color = AetherUi.ACCENT;
        } else if (nav.focus().is(ControlFocus.Kind.PALETTE)) {
            status = "pick a colour";
            color = AetherUi.ACCENT;
        } else if (System.currentTimeMillis() - lastSaveFlash < 900L) {
            status = "saved";
            color = AetherUi.ACCENT_ON;
        } else if (screenshotStatus != null && System.currentTimeMillis() - screenshotStatusAtMillis < 2500L) {
            status = screenshotStatus;
            color = AetherUi.ACCENT;
        } else if (!query.isEmpty()) {
            status = visible.size() + " match" + (visible.size() == 1 ? "" : "es");
            color = AetherUi.TEXT_SECONDARY;
        } else {
            status = nav.section().label() + " · " + visible.size() + " modules";
            color = AetherUi.TEXT_DISABLED;
        }
        String trimmed = trim(font, status, deckW / 2);
        text(font, trimmed, deckX + deckW - 12 - width(font, trimmed), y, color);
    }

    private void drawPalette(Object font, int mouseX, int mouseY) {
        ControlFocus focus = nav.focus();
        if (!focus.is(ControlFocus.Kind.PALETTE)) {
            return;
        }
        int w = PALETTE_COLS * PALETTE_CELL + 10;
        int h = 20 + 3 * PALETTE_CELL + 10;
        int x = clamp(focus.anchorX(), deckX + 4, Math.max(deckX + 4, deckX + deckW - w - 4));
        int y = clamp(focus.anchorY(), deckY + 4, Math.max(deckY + 4, deckY + deckH - h - 4));
        roundRect(x - 2, y - 2, x + w + 2, y + h + 2, 5, AetherUi.withAlpha(AetherUi.SHADOW, 0x66));
        roundRect(x, y, x + w, y + h, 4, AetherUi.withAlpha(AetherUi.PANEL, 0xF5));
        outline(x, y, x + w, y + h, AetherUi.ACCENT_SOFT);
        text(font, "PALETTE " + trim(font, focus.setting() == null ? "" : focus.setting().label(), w - 60), x + 5, y + 5, AetherUi.TEXT_SECONDARY);
        int current = number(focus.setting() == null ? null : focus.setting().value());
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < PALETTE_COLS; col++) {
                int color = PALETTE_ROW_1[col];
                if (row == 1) {
                    color = (color & 0x00FFFFFF) | 0x6F000000;
                }
                int cx = x + 5 + col * PALETTE_CELL;
                int cy = y + 18 + row * PALETTE_CELL;
                Mc189Compat.drawRect(cx, cy, cx + PALETTE_CELL - 2, cy + PALETTE_CELL - 2, color);
                boolean hover = mouseX >= cx && mouseX <= cx + PALETTE_CELL - 2 && mouseY >= cy && mouseY <= cy + PALETTE_CELL - 2;
                if (hover || sameRgb(color, current)) {
                    outline(cx - 1, cy - 1, cx + PALETTE_CELL - 1, cy + PALETTE_CELL - 1, hover ? AetherUi.TEXT_PRIMARY : AetherUi.ACCENT);
                }
            }
        }
        for (int col = 0; col < PALETTE_ALPHA.length; col++) {
            int color = (PALETTE_ALPHA[col] & 0xFF000000) | (current & 0x00FFFFFF);
            int cx = x + 5 + col * PALETTE_CELL;
            int cy = y + 18 + 2 * PALETTE_CELL;
            Mc189Compat.drawRect(cx, cy, cx + PALETTE_CELL - 2, cy + PALETTE_CELL - 2, color);
            boolean hover = mouseX >= cx && mouseX <= cx + PALETTE_CELL - 2 && mouseY >= cy && mouseY <= cy + PALETTE_CELL - 2;
            if (hover) {
                outline(cx - 1, cy - 1, cx + PALETTE_CELL - 1, cy + PALETTE_CELL - 1, AetherUi.TEXT_PRIMARY);
            }
        }
    }

    /* ── interactions ───────────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        click(mouseX, mouseY, button);
    }

    protected void func_73864_a(int mouseX, int mouseY, int button) throws IOException {
        click(mouseX, mouseY, button);
    }

    private void click(int mouseX, int mouseY, int button) {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) {
            return;
        }
        computeLayout(w, h);

        ControlFocus focus = nav.focus();
        if (focus.is(ControlFocus.Kind.PALETTE)) {
            if (paletteClick(mouseX, mouseY)) {
                return;
            }
            nav.clearFocus();
        }

        if (mouseY >= deckY && mouseY <= deckY + HEADER_H) {
            int searchW = clamp(deckW / 3, 120, 260);
            int searchX = deckX + deckW - searchW - 96;
            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= deckY + 10 && mouseY <= deckY + 30) {
                searchFocused = true;
                nav.beginSearch();
                return;
            }
        }

        if (sidebarW > 0 && mouseX >= sidebarX && mouseX <= sidebarX + sidebarW) {
            for (Map.Entry<String, Rect> entry : sidebarHits.entrySet()) {
                if (!entry.getValue().contains(mouseX, mouseY)) {
                    continue;
                }
                String key = entry.getKey();
                if ("!all".equals(key) || "!live".equals(key) || "!fav".equals(key) || key.startsWith("cat:")) {
                    nav.filterKey(key);
                    applyFilterKeyToSearch();
                    scroll = 0;
                    selected = 0;
                    syncVisible();
                } else {
                    nav.showSection(ControlCenterSection.fromLabel(key, ControlCenterSection.MODULES));
                    scroll = 0;
                    selected = 0;
                    syncVisible();
                }
                return;
            }
        }

        if (spineW > 0) {
            for (int i = 0; i < spineButtons.length; i++) {
                Rect rect = spineButtons[i];
                if (rect == null || !rect.contains(mouseX, mouseY)) {
                    continue;
                }
                if (i == 0) {
                    Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
                } else if (i == 1) {
                    Mc189Compat.displayGuiScreen(new AetherCosmeticsScreen(client, this));
                } else {
                    save();
                }
                return;
            }
        }

        // Rows own the list column and only the list column: a click in the margins must not fall
        // through to a row that happens to share its y range, and a row scrolled out of the
        // viewport must not be reachable at all even though its geometry still exists.
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            searchFocused = false;
            nav.endSearch();
            return;
        }

        if (!nav.showsModuleList()) {
            clickPageRow(mouseX, mouseY, button);
            return;
        }

        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            if (mouseY < box.y || mouseY > box.y + box.height) {
                continue;
            }
            if (box.expanded) {
                if (box.reset != null && box.reset.contains(mouseX, mouseY)) {
                    resetModule(box.module);
                    return;
                }
                if (box.hud != null && box.hud.contains(mouseX, mouseY)) {
                    Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
                    return;
                }
                for (Setting<?> setting : box.module.settings()) {
                    Rect control = box.controls.get(setting.id());
                    if (control == null || !control.contains(mouseX, mouseY)) {
                        continue;
                    }
                    selected = i;
                    if (button == 1) {
                        setValue(setting, setting.defaultValue());
                        writeLastChange("Reset setting '" + setting.label() + "' on " + box.module.metadata().name());
                        save();
                        return;
                    }
                    handleControlClick(box.module, setting, control, mouseX);
                    return;
                }
            }
            if (box.chevron.contains(mouseX, mouseY)
                    || mouseY >= box.y + ROW_H - 8 && mouseX >= box.x && mouseX <= box.x + box.w) {
                nav.toggleExpanded(box.module.metadata().id());
                return;
            }
            if (box.toggle.contains(mouseX, mouseY)) {
                selected = i;
                toggleModule(box.module);
                return;
            }
            selected = i;
            if (button == 1) {
                resetModule(box.module);
                return;
            }
            if (box.header.contains(mouseX, mouseY) && box.module.settings().isEmpty()) {
                toggleModule(box.module);
            }
            return;
        }

        searchFocused = false;
        nav.endSearch();
    }

    /** Handles a click on the non-module pages. */
    private void clickPageRow(int mouseX, int mouseY, int button) {
        switch (nav.section()) {
            case PROFILES: {
                ProfileStore profiles = client.profiles();
                for (Map.Entry<String, Rect> entry : pageActionHits.entrySet()) {
                    String key = entry.getKey();
                    Rect rect = entry.getValue();
                    if (!rect.contains(mouseX, mouseY)) {
                        continue;
                    }
                    if ("save".equals(key)) {
                        String draft = profileDraftName;
                        String name = draft == null || draft.trim().isEmpty() ? "profile " + (profiles.size() + 1) : draft;
                        if (profiles.save(name, client.modules())) {
                            profileDraftName = "";
                            nav.clearFocus();
                            writeLastChange("Saved profile '" + name + "'");
                            save();
                        }
                        return;
                    }
                    if (key.startsWith("apply:")) {
                        String name = key.substring(6);
                        if (profiles.apply(name, client.modules())) {
                            activeProfile = name;
                            writeLastChange("Applied profile '" + name + "'");
                            syncVisible();
                            save();
                        }
                        return;
                    }
                    if (key.startsWith("delete:")) {
                        String name = key.substring(7);
                        if (profiles.delete(name)) {
                            if (name.equals(activeProfile)) {
                                activeProfile = null;
                            }
                            writeLastChange("Deleted profile '" + name + "'");
                            save();
                        }
                        return;
                    }
                }
                for (Map.Entry<String, Rect> entry : pageRowHits.entrySet()) {
                    if (entry.getValue().contains(mouseX, mouseY)) {
                        activeProfile = entry.getKey();
                        return;
                    }
                }
                Rect nameField = pageActionHits.get("newname");
                if (nameField != null && nameField.contains(mouseX, mouseY)) {
                    nav.setFocus(ControlFocus.text(null, profileDraftName == null ? "" : profileDraftName));
                } else if (nav.focus().is(ControlFocus.Kind.TEXT)) {
                    profileDraftName = nav.focus().text();
                    nav.clearFocus();
                }
                break;
            }
            case SCREENSHOTS: {
                for (Map.Entry<String, Rect> entry : pageActionHits.entrySet()) {
                    String key = entry.getKey();
                    Rect rect = entry.getValue();
                    if (!rect.contains(mouseX, mouseY)) {
                        continue;
                    }
                    if ("capture".equals(key)) {
                        takeScreenshot();
                        return;
                    }
                    if (key.startsWith("delete:")) {
                        String name = key.substring(7);
                        if (client.screenshots().store().delete(name)) {
                            screenshotsReadAtMillis = 0L;
                            refreshScreenshotList();
                        }
                        return;
                    }
                    if (key.startsWith("open:")) {
                        openScreenshotsFolder();
                        return;
                    }
                }
                break;
            }
            case SETTINGS: {
                ClientPreferences prefs = client.preferences();
                if (hit(pageActionHits.get("pref:save_on_close"), mouseX, mouseY)) {
                    prefs.setSaveOnClose(!prefs.saveOnClose());
                    save();
                } else if (hit(pageActionHits.get("pref:show_tooltips"), mouseX, mouseY)) {
                    prefs.setShowTooltips(!prefs.showTooltips());
                    save();
                }
                break;
            }
            default:
                break;
        }
    }

    private static boolean hit(Rect rect, int mouseX, int mouseY) {
        return rect != null && rect.contains(mouseX, mouseY);
    }

    /** Captures through the async pipeline; the button never blocks on the encode. */
    private void takeScreenshot() {
        if (client.screenshots().state().isInFlight()) {
            return;
        }
        screenshotRequested = true;
    }

    /**
     * Runs the screenshot capture at the end of the current frame's drawing: the pixels handed to
     * the pipeline are the ones the framebuffer actually holds, and the encode happens off-thread.
     * The request flag keeps the click handler free of any framebuffer work.
     */
    private void pollScreenshotRequest() {
        if (!screenshotRequested) {
            return;
        }
        screenshotRequested = false;
        if (client.screenshots().state().isInFlight()) {
            return;
        }
        int width = Mc189Compat.displayWidth(Mc189Compat.minecraft());
        int height = Mc189Compat.displayHeight(Mc189Compat.minecraft());
        if (width <= 0 || height <= 0) {
            return;
        }
        int[] pixels = Mc189Compat.readFramePixels(width, height);
        if (pixels == null) {
            screenshotStatus = "screenshot failed";
            screenshotStatusAtMillis = System.currentTimeMillis();
            return;
        }
        if (client.screenshots().encode(pixels, width, height)) {
            screenshotStatus = "saving screenshot…";
            screenshotStatusAtMillis = System.currentTimeMillis();
        }
    }

    private void openScreenshotsFolder() {
        try {
            File dir = client.screenshots().store().directory().toFile();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] {"explorer.exe", dir.getAbsolutePath()});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] {"open", dir.getAbsolutePath()});
            } else {
                Runtime.getRuntime().exec(new String[] {"xdg-open", dir.getAbsolutePath()});
            }
        } catch (IOException ignored) {
            // Opening a file manager is best-effort; the folder path stays in the footer status.
        }
    }

    private void handleControlClick(ClientModule module, Setting<?> setting, Rect control, int mouseX) {
        switch (setting.type()) {
            case BOOLEAN:
                setValue(setting, Boolean.valueOf(!Boolean.TRUE.equals(setting.value())));
                writeLastChange("Setting '" + setting.label() + "' -> " + setting.value());
                save();
                break;
            case NUMBER:
                nav.setFocus(ControlFocus.slider(setting, control.x, control.w));
                applySlider(setting, mouseX);
                break;
            case COLOR:
                nav.setFocus(ControlFocus.palette(setting, control.x - (PALETTE_COLS * PALETTE_CELL + 10) / 2, control.y + control.h + 4));
                break;
            case KEYBIND:
                nav.setFocus(ControlFocus.keybind(setting));
                break;
            case TEXT:
                nav.setFocus(ControlFocus.text(setting, String.valueOf(setting.value())));
                break;
            case CHOICE:
            default:
                setValue(setting, nextChoice(setting));
                writeLastChange("Setting '" + setting.label() + "' -> " + setting.value());
                save();
                break;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (nav.focus().is(ControlFocus.Kind.SLIDER)) {
            applySlider(nav.focus().setting(), mouseX);
        }
    }

    protected void func_73862_b(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (nav.focus().is(ControlFocus.Kind.SLIDER) && nav.focus().setting() != null) {
            writeLastChange("Setting '" + nav.focus().setting().label() + "' -> " + nav.focus().setting().value());
            nav.clearFocus();
            save();
        }
    }

    protected void func_73861_b(int mouseX, int mouseY, int state) {
        mouseReleased(mouseX, mouseY, state);
    }

    private void applySlider(Setting<?> setting, int mouseX) {
        int[] range = range(setting);
        ControlFocus focus = nav.focus();
        float pct = focus.trackW() <= 0 ? 0F : clamp((float) (mouseX - focus.trackX()) / focus.trackW(), 0F, 1F);
        int value = range[0] + Math.round(pct * (range[1] - range[0]));
        if (range[2] > 0) {
            value = Math.round((float) value / range[2]) * range[2];
        }
        setValue(setting, Integer.valueOf(clamp(value, range[0], range[1])));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        handleKey(typedChar, keyCode);
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        handleKey(typedChar, keyCode);
    }

    private void handleKey(char typedChar, int keyCode) {
        ControlFocus focus = nav.focus();
        if (focus.is(ControlFocus.Kind.PALETTE) && keyCode == KEY_ESCAPE) {
            nav.clearFocus();
            return;
        }
        if (focus.is(ControlFocus.Kind.KEYBIND)) {
            if (keyCode == KEY_ESCAPE) {
                nav.clearFocus();
                return;
            }
            int bound = keyCode == KEY_DELETE || keyCode == KEY_BACKSPACE ? 0 : keyCode;
            Setting<?> target = focus.setting();
            setValue(target, Integer.valueOf(bound));
            writeLastChange("Bound '" + target.label() + "' to " + Mc189Compat.keyName(bound));
            nav.clearFocus();
            save();
            return;
        }
        if (focus.is(ControlFocus.Kind.TEXT)) {
            if (keyCode == KEY_ESCAPE) {
                if (focus.setting() == null) {
                    profileDraftName = focus.text();
                }
                nav.clearFocus();
                return;
            }
            if (keyCode == KEY_RETURN) {
                if (focus.setting() == null) {
                    profileDraftName = focus.text();
                } else {
                    setValue(focus.setting(), focus.text());
                    writeLastChange("Setting '" + focus.setting().label() + "' -> " + focus.text());
                    save();
                }
                nav.clearFocus();
                return;
            }
            if (keyCode == KEY_BACKSPACE) {
                focus.backspace();
                return;
            }
            if (typedChar >= 32 && typedChar < 127) {
                focus.append(typedChar);
            }
            return;
        }

        if (keyCode == KEY_ESCAPE) {
            if (searchFocused || !query.isEmpty()) {
                searchFocused = false;
                query = "";
                syncVisible();
                nav.endSearch();
                return;
            }
            Mc189Compat.displayGuiScreen(parent);
            return;
        }

        if (searchFocused) {
            if (keyCode == KEY_BACKSPACE) {
                if (query.length() > 0) {
                    query = query.substring(0, query.length() - 1);
                    syncVisible();
                }
                return;
            }
            if (keyCode == KEY_RETURN) {
                searchFocused = false;
                nav.endSearch();
                return;
            }
            if (typedChar >= 32 && typedChar < 127) {
                query = query + typedChar;
                selected = 0;
                scroll = 0;
                syncVisible();
                return;
            }
        }

        if (keyCode == KEY_LEFT_CONTROL) {
            return;
        }

        switch (keyCode) {
            case KEY_UP:
                move(-1);
                return;
            case KEY_DOWN:
                move(1);
                return;
            case KEY_PAGE_UP:
                move(-5);
                return;
            case KEY_PAGE_DOWN:
                move(5);
                return;
            case KEY_HOME:
                selected = 0;
                scroll = 0;
                return;
            case KEY_END:
                selected = Math.max(0, visible.size() - 1);
                scrollToSelected();
                return;
            case KEY_RETURN:
                if (nav.showsModuleList() && selected >= 0 && selected < visible.size()) {
                    toggleModule(visible.get(selected));
                }
                return;
            case KEY_RIGHT:
                if (nav.showsModuleList() && selected >= 0 && selected < visible.size()
                        && !nav.isExpanded(visible.get(selected).metadata().id())) {
                    nav.toggleExpanded(visible.get(selected).metadata().id());
                }
                return;
            case KEY_LEFT:
                if (nav.showsModuleList() && selected >= 0 && selected < visible.size()
                        && nav.isExpanded(visible.get(selected).metadata().id())) {
                    nav.toggleExpanded(visible.get(selected).metadata().id());
                }
                return;
            case KEY_TAB:
                nav.cycleSection(GuiScreen.isShiftKeyDown() ? -1 : 1);
                scroll = 0;
                selected = 0;
                syncVisible();
                return;
            default:
                break;
        }

        if (typedChar == ' ') {
            if (nav.showsModuleList() && selected >= 0 && selected < visible.size()) {
                nav.toggleExpanded(visible.get(selected).metadata().id());
            }
            return;
        }
        if (typedChar == '/') {
            searchFocused = true;
            nav.beginSearch();
            return;
        }
        if (typedChar == 'r' || typedChar == 'R') {
            if (nav.showsModuleList() && selected >= 0 && selected < visible.size()) {
                resetModule(visible.get(selected));
            }
            return;
        }
        if (typedChar == 'e' || typedChar == 'E') {
            List<String> ids = new ArrayList<String>();
            for (ClientModule module : visible) {
                ids.add(module.metadata().id());
            }
            nav.expand(ids);
            return;
        }
        if (typedChar == 'c' || typedChar == 'C') {
            nav.collapseAll();
            return;
        }
        if (typedChar >= 32 && typedChar < 127) {
            searchFocused = true;
            nav.beginSearch();
            query = query + typedChar;
            selected = 0;
            scroll = 0;
            syncVisible();
        }
    }

    private void move(int delta) {
        if (visible.isEmpty()) {
            return;
        }
        selected = clamp(selected + delta, 0, visible.size() - 1);
        scrollToSelected();
    }

    private void scrollToSelected() {
        if (visible.isEmpty()) {
            return;
        }
        ClientModule target = visible.get(clamp(selected, 0, visible.size() - 1));
        for (Box box : boxes) {
            if (box.module != target) {
                continue;
            }
            if (box.y - (int) scroll < listY) {
                scroll = box.y - listY;
            } else if (box.y + box.height - (int) scroll > listY + listH) {
                scroll = box.y + box.height - listY - listH;
            }
            scroll = clamp(scroll, 0, maxScroll);
            return;
        }
    }

    /** @return true when the click was consumed by the palette popover. */
    private boolean paletteClick(int mouseX, int mouseY) {
        ControlFocus focus = nav.focus();
        int w = PALETTE_COLS * PALETTE_CELL + 10;
        int h = 20 + 3 * PALETTE_CELL + 10;
        int x = clamp(focus.anchorX(), deckX + 4, Math.max(deckX + 4, deckX + deckW - w - 4));
        int y = clamp(focus.anchorY(), deckY + 4, Math.max(deckY + 4, deckY + deckH - h - 4));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < PALETTE_COLS; col++) {
                int cx = x + 5 + col * PALETTE_CELL;
                int cy = y + 18 + row * PALETTE_CELL;
                if (mouseX < cx || mouseX > cx + PALETTE_CELL - 2 || mouseY < cy || mouseY > cy + PALETTE_CELL - 2) {
                    continue;
                }
                int color = PALETTE_ROW_1[col];
                if (row == 1) {
                    color = (color & 0x00FFFFFF) | 0x6F000000;
                }
                applyPaletteColor(color);
                return true;
            }
        }
        for (int col = 0; col < PALETTE_ALPHA.length; col++) {
            int cx = x + 5 + col * PALETTE_CELL;
            int cy = y + 18 + 2 * PALETTE_CELL;
            if (mouseX < cx || mouseX > cx + PALETTE_CELL - 2 || mouseY < cy || mouseY > cy + PALETTE_CELL - 2) {
                continue;
            }
            applyPaletteColor((PALETTE_ALPHA[col] & 0xFF000000) | (number(focus.setting() == null ? null : focus.setting().value()) & 0x00FFFFFF));
            return true;
        }
        return mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 2 && mouseY <= y + h + 2;
    }

    private void applyPaletteColor(int color) {
        ControlFocus focus = nav.focus();
        if (!focus.is(ControlFocus.Kind.PALETTE) || focus.setting() == null) {
            nav.clearFocus();
            return;
        }
        Setting<?> target = focus.setting();
        setValue(target, Integer.valueOf(color));
        writeLastChange("Setting '" + target.label() + "' -> #" + String.format("%08X", Integer.valueOf(color)));
        nav.clearFocus();
        save();
    }

    @Override
    public void handleMouseInput() throws IOException {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) {
            return;
        }
        int button = Mc189Compat.getEventButton();
        int mouseX = scaledMouseX(w);
        int mouseY = scaledMouseY(h);
        int wheel = Mc189Compat.mouseWheelDelta();
        if (button != -1) {
            if (Mc189Compat.getEventButtonState()) {
                click(mouseX, mouseY, button);
            } else {
                mouseReleased(mouseX, mouseY, button);
            }
        } else if (wheel != 0) {
            scrollBy(-wheel / 24F * 22F);
        } else if (nav.focus().is(ControlFocus.Kind.SLIDER)) {
            applySlider(nav.focus().setting(), mouseX);
        }
    }

    public void func_146274_d() throws IOException {
        handleMouseInput();
    }

    private void scrollBy(float amount) {
        scroll = clamp(scroll + amount, 0, maxScroll);
    }

    /* ── actions ────────────────────────────────────────────────────────── */

    private void toggleModule(ClientModule module) {
        boolean enable = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enable);
        writeLastChange("Toggled module '" + module.metadata().name() + "' -> " + (enable ? "ENABLED" : "DISABLED"));
        save();
        search.invalidate();
        if (nav.filterKey().equals("!live")) {
            syncVisible();
        }
    }

    private void resetModule(ClientModule module) {
        for (Setting<?> setting : module.settings()) {
            setValue(setting, setting.defaultValue());
        }
        writeLastChange("Reset all settings for '" + module.metadata().name() + "'");
        save();
    }

    private void save() {
        try {
            client.save();
            lastSaveFlash = System.currentTimeMillis();
        } catch (IOException ignored) {
            // The deck stays usable when the config file is locked; the shutdown
            // hook and the next change both retry the write.
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }

    private static String nextChoice(Setting<?> setting) {
        String current = String.valueOf(setting.value());
        List<String> options = setting.choices();
        if (options == null || options.isEmpty()) {
            return current;
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(current)) {
                return options.get((i + 1) % options.size());
            }
        }
        return options.get(0);
    }

    /**
     * Slider geometry for a numeric setting: {@code {min, max, step}}. Settings declare their
     * own {@link Setting.Range}; the fallback only exists for a numeric setting that forgot to,
     * and {@code AetherSettingsMetadataTest} fails the build in that case.
     */
    private static int[] range(Setting<?> setting) {
        Setting.Range declared = setting.range();
        if (declared != null) {
            return new int[] {declared.min(), declared.max(), declared.step()};
        }
        int value = number(setting.value());
        return new int[] {0, value > 100 ? value + 100 : 100, 1};
    }

    private void sampleFps() {
        long now = System.currentTimeMillis();
        if (now - lastSampleMillis < 250L) {
            return;
        }
        lastSampleMillis = now;
        fpsSamples[fpsCursor % fpsSamples.length] = Mc189Compat.debugFps();
        fpsCursor++;
    }

    /* ── types ──────────────────────────────────────────────────────────── */

    private static final class Box {
        final ClientModule module;
        final int x;
        final int y;
        final int w;
        final int height;
        final boolean expanded;
        final Map<String, Rect> controls = new HashMap<String, Rect>();
        Rect header;
        Rect toggle;
        Rect chevron;
        Rect reset;
        Rect hud;

        Box(ClientModule module, int x, int y, int w, int height, boolean expanded) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.w = w;
            this.height = height;
            this.expanded = expanded;
        }
    }

    private static final class Rect {
        final int x;
        final int y;
        final int w;
        final int h;

        Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    /* ── helpers ────────────────────────────────────────────────────────── */

    private static Rect rect(int x, int y, int w, int h) {
        return new Rect(x, y, w, h);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static boolean sameRgb(int a, int b) {
        return (a & 0x00FFFFFF) == (b & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        int a = (int) (((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendColor(int from, int to, float t) {
        return lerpColor(from, to, clamp(t, 0F, 1F));
    }

    private static void text(Object font, String value, int x, int y, int color) {
        Mc189Compat.drawStringWithShadow(font, value, x, y, color);
    }

    private static void center(Object font, String value, int x, int y, int w, int color) {
        Mc189Compat.drawStringWithShadow(font, value, x + (w - width(font, value)) / 2, y, color);
    }

    private static int width(Object font, String value) {
        return Mc189Compat.stringWidth(font, value);
    }

    private static String trim(Object font, String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (maxWidth <= 0 || width(font, value) <= maxWidth) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (width(font, builder.toString() + value.charAt(i) + "..") > maxWidth) {
                break;
            }
            builder.append(value.charAt(i));
        }
        return builder + "..";
    }

    /**
     * Character-budget word wrap used for the accordion description. The budget
     * keeps layout font-free; painting trims each line again for safety.
     */
    private static List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        if (value == null || value.isEmpty()) {
            return lines;
        }
        int budget = Math.max(16, maxWidth / 6);
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= budget) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static void roundRect(int left, int top, int right, int bottom, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2)));
        if (r == 0) {
            Mc189Compat.drawRect(left, top, right, bottom, color);
            return;
        }
        Mc189Compat.drawRect(left + r, top, right - r, bottom, color);
        Mc189Compat.drawRect(left, top + r, right, bottom - r, color);
        for (int i = 0; i <= r; i++) {
            int dy = (int) Math.round(Math.sqrt((double) r * r - (double) i * i));
            Mc189Compat.drawRect(left + r - i, top + r - dy, left + r - i + 1, top + r + dy, color);
            Mc189Compat.drawRect(right - r + i, top + r - dy, right - r + i + 1, top + r + dy, color);
        }
    }

    private static void outline(int left, int top, int right, int bottom, int color) {
        Mc189Compat.drawRect(left, top, right, top + 1, color);
        Mc189Compat.drawRect(left, bottom - 1, right, bottom, color);
        Mc189Compat.drawRect(left, top, left + 1, bottom, color);
        Mc189Compat.drawRect(right - 1, top, right, bottom, color);
    }

    private static void drawSwitch(Rect rect, boolean on) {
        if (rect == null) {
            return;
        }
        roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, rect.h / 2, on ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x55) : AetherUi.TRACK);
        outline(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, on ? AetherUi.ACCENT_ON : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        int knob = rect.h - 4;
        int knobX = on ? rect.x + rect.w - knob - 2 : rect.x + 2;
        roundRect(knobX, rect.y + 2, knobX + knob, rect.y + 2 + knob, knob / 2, on ? AetherUi.PANEL_EDGE : AetherUi.TEXT_DISABLED);
    }

    /* ── theme ──────────────────────────────────────────────────────────── */

    /**
     * graphics.ui_blur loads the vanilla blur post-shader behind the deck, and its
     * {@code amount} is the dim of the backdrop scrim, so the two controls in that module
     * finally change what the click GUI looks like. The shader is only touched when the
     * module flips, never per frame.
     */
    private void syncBlur() {
        boolean wanted = moduleEnabled("graphics.ui_blur");
        if (wanted == this.blurLoaded) {
            return;
        }
        this.blurLoaded = wanted;
        try {
            if (wanted) {
                Mc189Compat.loadBlurShader();
            } else {
                Mc189Compat.stopShader();
            }
        } catch (Throwable ignored) {
            // A missing or unsupported shader must never take the GUI down with it.
        }
    }

    /** @return the backdrop alpha; without ui_blur the deck keeps its original heavy scrim. */
    private int backdropDim() {
        if (!moduleEnabled("graphics.ui_blur")) {
            return 0xF2;
        }
        return clamp(Math.round(settingNumber("graphics.ui_blur", "amount", 52) * 2.55F), 0, 255);
    }

    private boolean moduleEnabled(String id) {
        try {
            return client.modules().get(id).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int settingNumber(String moduleId, String settingId, int fallback) {
        try {
            for (Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    return ((Number) setting.value()).intValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    /**
     * Repaints the shared theme tokens when the enabled theme module changed. The deck
     * draws straight from {@link AetherUi}, so it has no colours of its own to refresh.
     */
    private void syncTheme() {
        AetherUi.syncTheme();
    }

    private static void drawDot(int x, int y, int color) {
        Mc189Compat.drawRect(x, y, x + 6, y + 6, color);
        Mc189Compat.drawRect(x + 1, y - 1, x + 5, y + 7, color);
        Mc189Compat.drawRect(x - 1, y + 1, x + 7, y + 5, color);
    }

    private static void drawStar(int x, int y, int color) {
        Mc189Compat.drawRect(x + 4, y, x + 5, y + 8, color);
        Mc189Compat.drawRect(x, y + 2, x + 9, y + 4, color);
        Mc189Compat.drawRect(x + 1, y + 4, x + 8, y + 6, color);
        Mc189Compat.drawRect(x + 2, y + 6, x + 7, y + 8, color);
    }

    private static void drawChevron(int x, int y, int direction, int color) {
        for (int i = 0; i < 5; i++) {
            int dx = direction > 0 ? i : -i;
            Mc189Compat.drawRect(x + dx, y + i, x + dx + 1, y + i + 1, color);
            Mc189Compat.drawRect(x + dx, y - i, x + dx + 1, y - i + 1, color);
        }
    }

    private static void drawSearchGlyph(int x, int y, int color) {
        roundRect(x, y, x + 8, y + 8, 4, color);
        roundRect(x + 2, y + 2, x + 6, y + 6, 2, AetherUi.withAlpha(AetherUi.PANEL, 0xF0));
        Mc189Compat.drawRect(x + 7, y + 7, x + 10, y + 9, color);
    }

    private static void drawMark(int x, int y, int color) {
        Mc189Compat.drawRect(x, y + 3, x + 10, y + 7, color);
        Mc189Compat.drawRect(x + 2, y + 1, x + 8, y + 9, color);
        Mc189Compat.drawRect(x + 4, y, x + 6, y + 10, color);
    }

    private void drawSparkline(int x, int y, int w, int h) {
        Mc189Compat.drawRect(x, y + h - 1, x + w, y + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        int count = Math.min(fpsCursor, fpsSamples.length);
        if (count <= 1) {
            return;
        }
        int previousX = -1;
        int previousY = -1;
        for (int i = 0; i < count; i++) {
            int index = ((fpsCursor - count + i) % fpsSamples.length + fpsSamples.length) % fpsSamples.length;
            int value = clamp(fpsSamples[index], 0, 240);
            int px = x + (int) ((float) i / Math.max(1, count - 1) * (w - 1));
            int py = y + h - 2 - (int) ((float) value / 240F * (h - 3));
            if (previousX >= 0) {
                int steps = Math.max(1, px - previousX);
                for (int s = 0; s <= steps; s++) {
                    int lx = previousX + s;
                    int ly = previousY + (int) ((float) (py - previousY) * s / steps);
                    Mc189Compat.drawRect(lx, ly, lx + 1, ly + 1, AetherUi.ACCENT);
                }
            }
            previousX = px;
            previousY = py;
        }
    }

    private void pushClip(int x, int y, int w, int h) {
        Object minecraft = Mc189Compat.minecraft();
        if (minecraft == null) {
            return;
        }
        int scale = Mc189Compat.scaleFactor(new ScaledResolution((Minecraft) minecraft));
        int screenHeight = Mc189Compat.screenHeight(this);
        Mc189Compat.enableScissor();
        Mc189Compat.scissor(x * scale, (screenHeight - (y + h)) * scale, Math.max(0, w) * scale, Math.max(0, h) * scale);
    }

    private void popClip() {
        Mc189Compat.disableScissor();
    }

    private static String label(ModuleCategory category) {
        switch (category) {
            case GENERAL:
                return "General";
            case PERFORMANCE:
                return "Performance";
            case GRAPHICS:
                return "Graphics";
            case RENDER:
                return "Render";
            case INTERFACE:
                return "Interface";
            case MOVEMENT:
                return "Movement";
            case AUDIO:
                return "Audio";
            case HUD:
                return "HUD";
            case PVP:
                return "PvP";
            case COSMETICS:
                return "Cosmetics";
            case ACCESSIBILITY:
                return "Accessibility";
            case THEMES:
                return "Themes";
            default:
                return category.name();
        }
    }

    private static List<ModuleCategory> orderedCategories() {
        List<ModuleCategory> order = new ArrayList<ModuleCategory>();
        Collections.addAll(order,
            ModuleCategory.HUD, ModuleCategory.PVP, ModuleCategory.GRAPHICS, ModuleCategory.RENDER,
            ModuleCategory.INTERFACE, ModuleCategory.PERFORMANCE, ModuleCategory.COSMETICS,
            ModuleCategory.THEMES, ModuleCategory.MOVEMENT, ModuleCategory.AUDIO,
            ModuleCategory.ACCESSIBILITY, ModuleCategory.GENERAL);
        return order;
    }

    /**
     * Mirrors the legacy manager: every deck change rewrites lastchange.txt so the
     * repository keeps a readable trail of the most recent GUI action.
     */
    private void writeLastChange(String description) {
        try {
            // The property lets automated runs (and portable installs) redirect the
            // change log away from the working directory.
            File changeFile = new File(System.getProperty("aether.changeLog", "lastchange.txt"));
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            PrintWriter writer = new PrintWriter(new FileWriter(changeFile, false));
            try {
                writer.println("=== Aether Control Center - Last Change ===");
                writer.println("Timestamp: " + timestamp);
                writer.println("Action:    " + description);
                writer.println("===========================================");
            } finally {
                writer.close();
            }
        } catch (IOException exception) {
            System.err.println("[Aether] Failed to write lastchange.txt: " + exception.getMessage());
        }
    }
}
