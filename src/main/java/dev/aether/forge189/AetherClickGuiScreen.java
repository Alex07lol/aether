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

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.ui.AetherMetrics;
import dev.aether.forge189.ui.AetherToastRenderer;
import dev.aether.forge189.ui.ControlCenterLayout;
import dev.aether.forge189.ui.ControlCenterRenderer;
import dev.aether.forge189.ui.ControlCenterInput;
import dev.aether.forge189.ui.ToastSystem;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.pages.ModulesPage;
import dev.aether.forge189.ui.pages.ProfilesPage;
import dev.aether.forge189.ui.pages.ThemesPage;
import dev.aether.forge189.ui.pages.CosmeticsPage;
import dev.aether.forge189.ui.pages.ScreenshotsPage;
import dev.aether.forge189.ui.pages.SettingsPage;
import dev.aether.forge189.ui.pages.Page;
import dev.aether.forge189.ui.components.AetherToggle;
import dev.aether.forge189.ui.components.AetherSearchBox;
import dev.aether.forge189.ui.components.AetherDropdown;
import dev.aether.forge189.ui.components.AetherSlider;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Aether Control Center — the primary click GUI.
 * <p>
 * Now a thin coordinator: layout, input and rendering are delegated to dedicated helper classes.
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

    /* ── core objects ───────────────────────────────────────────────────── */
    private final AetherClient client;
    private final GuiScreen parent;
    private boolean blurLoaded;

    private final ControlCenterState nav = new ControlCenterState();
    private final ModuleSearch search = new ModuleSearch();

    private final ControlCenterLayout layoutEngine = new ControlCenterLayout();
    private final ControlCenterRenderer renderer = new ControlCenterRenderer();
    private final ControlCenterInput inputHandler = new ControlCenterInput(this);
    private final ToastSystem toastSystem = new ToastSystem();

    // Pages - all implemented in the new UI package
    private final ModulesPage modulesPage = new ModulesPage();
    private final ProfilesPage profilesPage = new ProfilesPage();
    private final ThemesPage themesPage = new ThemesPage();
    private final CosmeticsPage cosmeticsPage = new CosmeticsPage();
    private final ScreenshotsPage screenshotsPage = new ScreenshotsPage();
    private final SettingsPage settingsPage = new SettingsPage();

    // UI components
    private final AetherToggle aetherToggle = new AetherToggle();
    private final AetherSearchBox aetherSearchBox = new AetherSearchBox();
    private final AetherDropdown aetherDropdown = new AetherDropdown();
    private final AetherSlider aetherSlider = new AetherSlider();

    // Custom smooth font renderer (anti-aliased TrueType text)
    private static final dev.aether.forge189.font.AetherFontManager fontManager =
        dev.aether.forge189.font.AetherFontManager.instance();

    // Cached layout for the current frame (used by hit-testing)
    private Layout currentLayout;

    // Legacy state kept for compatibility
    private String query = "";

    public void setQuery(String q) { this.query = q; }
    public String getQuery() { return query; }
    private boolean searchFocused;
    private int selected;
    private float scroll;
    private float maxScroll;
    private long lastSaveFlash;
    private long lastSampleMillis;
    private long lastFrameMillis;
    private int fpsCursor;
    private final int[] fpsSamples = new int[60];
    private String activeProfile;
    private String profileDraftName = "";
    private boolean screenshotRequested;
    private List<ScreenshotInfo> screenshots = Collections.emptyList();
    private final Map<String, Float> hoverBlend = new HashMap<>();
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

    public ControlCenterState nav() {
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

    public void syncVisible() {
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

    private void computeLayout() {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w > 0 && h > 0) {
            currentLayout = layoutEngine.compute(this, w, h);
        }
    }

    /* ── rendering – delegated ─────────────────────────────────────────── */

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) {
            return;
        }
        int scaledX = scaledMouseX(w);
        int scaledY = scaledMouseY(h);
        render(scaledX, scaledY, partialTicks);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) {
            return;
        }
        int scaledX = scaledMouseX(w);
        int scaledY = scaledMouseY(h);
        render(scaledX, scaledY, partialTicks);
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

        // Layout computation (single-source)
        computeLayout();
        if (currentLayout == null) return;

        Object font = Mc189Compat.screenFontRenderer(this);
        // Use the custom smooth font only when it owns a real OpenGL texture; otherwise Minecraft's
        // font draws the UI instead of nothing at all.
        Object smoothFont = fontManager.uiFont();
        Object uiFont = smoothFont != null ? smoothFont : font;
        renderer.render(this, uiFont, mouseX, mouseY, currentLayout);

        // Toasts
        toastSystem.tick();
        AetherToastRenderer.render(uiFont,
                                   toastSystem.snapshot(),
                                   w, h);
    }

    /* ── input – delegated ─────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        inputHandler.click(mx, my, button);
    }

    protected void func_73864_a(int mx, int my, int button) throws IOException {
        inputHandler.click(mx, my, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        inputHandler.handleKey(typedChar, keyCode);
    }

    /**
     * Public wrapper for headless tests to inject key events without
     * needing to call the protected {@link #keyTyped(char, int)} directly.
     */
    public void typeKey(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    public void releaseMouse(int mx, int my, int button) throws IOException {
        mouseReleased(mx, my, button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        if (w <= 0 || h <= 0) return;
        int button = Mc189Compat.getEventButton();
        int mx = scaledMouseX(w);
        int my = scaledMouseY(h);
        int wheel = Mc189Compat.mouseWheelDelta();

        if (button != -1) {
            if (Mc189Compat.getEventButtonState()) {
                inputHandler.click(mx, my, button);
            } else {
                mouseReleased(mx, my, button);
            }
        } else if (wheel != 0) {
            inputHandler.scroll(-wheel / 24f * 22f);
        } else {
            // pass wheel to scroll when a slider is focused
            if (nav.focus().is(ControlFocus.Kind.SLIDER)) {
                inputHandler.scroll(-wheel / 24f * 22f);
            }
        }
    }

    /* ── helpers used by pages / input ─────────────────────────────────── */

    public Layout getLayout() { return currentLayout; }
    public ToastSystem getToasts() { return toastSystem; }
    public AetherClient getClient() { return client; }
    public ControlCenterState getNav() { return nav; }
    public ModuleSearch getSearch() { return search; }
    public ControlCenterState getState() { return nav; }
    public ControlCenterState getCurrentState() { return nav; }
    public String getActiveProfile() { return activeProfile; }
    public String getProfileDraftName() { return profileDraftName; }
    public void setProfileDraftName(String name) { this.profileDraftName = name; }
    public boolean saveProfile() {
        String name = profileDraftName.trim();
        if (name.isEmpty()) {
            return false;
        }
        boolean ok = client.profiles().save(name, client.modules());
        if (ok) {
            activeProfile = name;
            profileDraftName = "";
        }
        return ok;
    }
    public boolean newProfile() {
        profileDraftName = "";
        return true;
    }
    public boolean applyProfile(String name) {
        return client.profiles().apply(name, client.modules());
    }
    public boolean deleteProfile(String name) {
        return client.profiles().delete(name);
    }

    public void handleKey(char typedChar, int keyCode) throws IOException {
        inputHandler.handleKey(typedChar, keyCode);
    }

    /**
     * Opens the glyph-render diagnostic. Reachable with F9 while no field has focus, so a broken
     * custom font can be told apart from a broken layout without leaving the game.
     */
    public void openFontDiagnostic() {
        Mc189Compat.displayGuiScreen(new AetherFontDiagScreen(client, this));
    }

    public Page getCurrentPage() {
        switch (nav.section()) {
            case MODULES: return modulesPage;
            case PROFILES: return profilesPage;
            case THEMES: return themesPage;
            case COSMETICS: return cosmeticsPage;
            case SCREENSHOTS: return screenshotsPage;
            case SETTINGS: return settingsPage;
            default: return modulesPage;
        }
    }

    public void handleSidebarClick(int mx, int my, int button) {
        if (currentLayout == null) return;
        // Map vertical position to section
        int relY = my - currentLayout.sidebarY;
        int index = relY / AetherMetrics.SIDEBAR_ITEM_HEIGHT;
        switch (index) {
            case 0: nav.showSection(ControlCenterSection.MODULES); break;
            case 1: nav.showSection(ControlCenterSection.PROFILES); break;
            case 2: nav.showSection(ControlCenterSection.THEMES); break;
            case 3: nav.showSection(ControlCenterSection.COSMETICS); break;
            case 4: nav.showSection(ControlCenterSection.SCREENSHOTS); break;
            case 5: nav.showSection(ControlCenterSection.SETTINGS); break;
        }
    }

    public void handleSpineClick(int mx, int my, int button) {
        if (currentLayout == null || currentLayout.spineW <= 0) return;
        // The spine has 3 buttons at fixed positions (HUD EDITOR, COSMETICS, SAVE NOW)
        int x = currentLayout.spineX + 10;
        int y = currentLayout.spineY + currentLayout.spineH - 47;
        int w = currentLayout.spineW - 20;
        // HUD EDITOR
        if (mx >= x && mx <= x + w && my >= y && my <= y + 13) {
            Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
        }
        // COSMETICS
        else if (mx >= x && mx <= x + w && my >= y + 15 && my <= y + 28) {
            Mc189Compat.displayGuiScreen(new AetherCosmeticsScreen(client, this));
        }
        // SAVE NOW
        else if (mx >= x && mx <= x + w && my >= y + 30 && my <= y + 43) {
            save();
        }
    }

    /* ── existing helper methods (kept for compatibility) ──────────────── */

    private void syncTheme() {
        AetherUi.syncTheme();
    }

    private void syncBlur() {
        boolean wanted = moduleEnabled("graphics.ui_blur");
        if (wanted == this.blurLoaded) return;
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

    public int backdropDim() {
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

    private int scaledMouseX(int w) {
        return Mc189Compat.mouseX() * w / Math.max(1, Mc189Compat.displayWidth(Mc189Compat.minecraft()));
    }

    private int scaledMouseY(int h) {
        return h - Mc189Compat.mouseY() * h / Math.max(1, Mc189Compat.displayHeight(Mc189Compat.minecraft())) - 1;
    }

    /* ── actions ───────────────────────────────────────────────────────── */

    public void toggleModule(ClientModule module) {
        boolean enable = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enable);
        writeLastChange("Toggled module '" + module.metadata().name() + "' -> " + (enable ? "ENABLED" : "DISABLED"));
        save();
        search.invalidate();
        if (nav.filterKey().equals("!live")) {
            syncVisible();
        }
    }

    void resetModule(ClientModule module) {
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

    public void writeLastChange(String description) {
        try {
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

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
    }

    public float getScroll() {
        return scroll;
    }

    public void clampScroll() {
        scroll = clamp(scroll, 0, maxScroll);
    }

    public void takeScreenshot() {
        if (client.screenshots().state().isInFlight()) return;
        screenshotRequested = true;
    }

    private void pollScreenshotRequest() {
        if (!screenshotRequested) return;
        screenshotRequested = false;
        if (client.screenshots().state().isInFlight()) return;
        int width = Mc189Compat.displayWidth(Mc189Compat.minecraft());
        int height = Mc189Compat.displayHeight(Mc189Compat.minecraft());
        if (width <= 0 || height <= 0) return;
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

    /* ── getter methods for pages ─────────────────────────────────────── */

    public List<ClientModule> getVisibleModules() {
        // Returns the filtered module list for the current section
        syncVisible();
        return visible;
    }

    /* ── legacy fields and types (kept for compatibility) ──────────────── */

    /** Rows currently visible on the module-driven pages; rebuilt only when the search is dirty. */
    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private final List<Box> boxes = new ArrayList<Box>();

    /* ── geometry types (kept for compatibility) ─────────────────────── */

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

    private static Rect rect(int x, int y, int w, int h) {
        return new Rect(x, y, w, h);
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
        if (value == null) return "";
        if (maxWidth <= 0 || width(font, value) <= maxWidth) return value;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (width(font, builder.toString() + value.charAt(i) + "..") > maxWidth) break;
            builder.append(value.charAt(i));
        }
        return builder + "..";
    }

    private static List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        if (value == null || value.isEmpty()) return lines;
        int budget = Math.max(16, maxWidth / 6);
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (line.length() == 0) line.append(word);
            else if (line.length() + 1 + word.length() <= budget) line.append(' ').append(word);
            else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
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
        if (rect == null) return;
        roundRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, rect.h / 2, on ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x55) : AetherUi.TRACK);
        outline(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, on ? AetherUi.ACCENT_ON : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        int knob = rect.h - 4;
        int knobX = on ? rect.x + rect.w - knob - 2 : rect.x + 2;
        roundRect(knobX, rect.y + 2, knobX + knob, rect.y + 2 + knob, knob / 2, on ? AetherUi.PANEL_EDGE : AetherUi.TEXT_DISABLED);
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
        if (count <= 1) return;
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
        if (minecraft == null) return;
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
            case GENERAL: return "General";
            case PERFORMANCE: return "Performance";
            case GRAPHICS: return "Graphics";
            case RENDER: return "Render";
            case INTERFACE: return "Interface";
            case MOVEMENT: return "Movement";
            case AUDIO: return "Audio";
            case HUD: return "HUD";
            case PVP: return "PvP";
            case COSMETICS: return "Cosmetics";
            case ACCESSIBILITY: return "Accessibility";
            case THEMES: return "Themes";
            default: return category.name();
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
}