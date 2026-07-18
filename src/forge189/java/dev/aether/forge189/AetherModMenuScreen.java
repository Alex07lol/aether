package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.module.setting.Setting.SettingType;
import net.minecraft.client.gui.GuiScreen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Remade mod manager screen with image placeholders for each module.
 * <p>
 * Inspired by the CloudClient mod menu: each module is shown as a card tile
 * with a coloured icon placeholder (drawn procedurally based on category),
 * the module name, and an enabled/disabled indicator.
 * <p>
 * A {@code lastchange.txt} file is written every time any change occurs
 * (toggle, setting adjustment, category switch, etc.).
 * <p>
 * Layout: three-panel design – sidebar categories, centre module grid with
 * image placeholders, and right-side property/settings panel.
 */
public final class AetherModMenuScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_BACKSPACE = 14;
    private static final int KEY_UP = 200;
    private static final int KEY_DOWN = 208;
    private static final int DRAG_NONE = 0, DRAG_SLIDER = 1;

    private final AetherClient client;
    private final GuiScreen parent;

    private SidebarPanel sidebarPanel;
    private ModuleGridPanel moduleGridPanel;
    private PropertyPanel propertyPanel;

    private Category selectedCategory = Category.HOME;
    private ClientModule selectedModule;
    private String searchQuery = "";
    private int dragState = DRAG_NONE;
    private Setting<?> draggedSetting = null;
    private SettingController draggedController = null;

    /* ------------------------------------------------------------------ */
    /*  Icon placeholder colours per category                              */
    /* ------------------------------------------------------------------ */

    private static final Map<ModuleCategory, Integer> CATEGORY_ICON_COLORS = new EnumMap<>(ModuleCategory.class);
    static {
        CATEGORY_ICON_COLORS.put(ModuleCategory.GENERAL,       0xFF4A5568);
        CATEGORY_ICON_COLORS.put(ModuleCategory.PERFORMANCE,   0xFFD69E2E);
        CATEGORY_ICON_COLORS.put(ModuleCategory.GRAPHICS,      0xFF2D5AA0);
        CATEGORY_ICON_COLORS.put(ModuleCategory.RENDER,        0xFF3182CE);
        CATEGORY_ICON_COLORS.put(ModuleCategory.INTERFACE,     0xFF6B46C1);
        CATEGORY_ICON_COLORS.put(ModuleCategory.MOVEMENT,      0xFF38A169);
        CATEGORY_ICON_COLORS.put(ModuleCategory.AUDIO,         0xFFE53E3E);
        CATEGORY_ICON_COLORS.put(ModuleCategory.HUD,           0xFF289C50);
        CATEGORY_ICON_COLORS.put(ModuleCategory.PVP,           0xFFA03232);
        CATEGORY_ICON_COLORS.put(ModuleCategory.COSMETICS,     0xFFA03282);
        CATEGORY_ICON_COLORS.put(ModuleCategory.ACCESSIBILITY, 0xFF4299E1);
        CATEGORY_ICON_COLORS.put(ModuleCategory.THEMES,        0xFF505080);
    }

    /* ------------------------------------------------------------------ */
    /*  Number slider properties (reused from reference)                   */
    /* ------------------------------------------------------------------ */

    private static class NumberProperty {
        final int min, max, step;
        NumberProperty(int min, int max, int step) { this.min = min; this.max = max; this.step = step; }
    }

    private static final Map<String, NumberProperty> NUMBER_PROPERTIES = new HashMap<>();
    static {
        NUMBER_PROPERTIES.put("fps_limit",          new NumberProperty(30, 1000, 10));
        NUMBER_PROPERTIES.put("zoom_percent",       new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("min_zoom_percent",   new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("max_zoom_percent",   new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("strength",           new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("amount",             new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("shake_amount",       new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("box_size",           new NumberProperty(14, 34, 1));
        NUMBER_PROPERTIES.put("click_size",         new NumberProperty(14, 34, 1));
        NUMBER_PROPERTIES.put("spacebar_height",    new NumberProperty(8, 24, 1));
        NUMBER_PROPERTIES.put("gap",                new NumberProperty(0, 8, 1));
        NUMBER_PROPERTIES.put("scroll_step",        new NumberProperty(1, 25, 1));
        NUMBER_PROPERTIES.put("thickness",          new NumberProperty(1, 25, 1));
        NUMBER_PROPERTIES.put("fade_time",          new NumberProperty(0, 500, 1));
        NUMBER_PROPERTIES.put("brightness",         new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("max_render_distance", new NumberProperty(2, 32, 1));
        NUMBER_PROPERTIES.put("memory_threshold",   new NumberProperty(1, 95, 5));
        NUMBER_PROPERTIES.put("scale",              new NumberProperty(50, 150, 1));
        NUMBER_PROPERTIES.put("opacity",            new NumberProperty(0, 100, 1));
        NUMBER_PROPERTIES.put("size",               new NumberProperty(1, 32, 1));
        NUMBER_PROPERTIES.put("sensitivity",        new NumberProperty(10, 250, 10));
        NUMBER_PROPERTIES.put("offset",             new NumberProperty(0, 24000, 500));
        NUMBER_PROPERTIES.put("speed",              new NumberProperty(1, 100, 1));
    }

    private static final int[] COLOR_PRESETS = {
        0xFFFFFFFF, 0xFF52BEEB, 0xFF387DFF, 0xFFFF5555,
        0xFF55FF88, 0xFFFFFF55, 0xFFFF55FF, 0x6F000000,
        0xAA10141B, 0x00000000
    };

    private static final Map<String, String[]> CHOICE_OPTIONS = new HashMap<>();
    static {
        CHOICE_OPTIONS.put("hud.clock.format",                    new String[]{"24h", "12h"});
        CHOICE_OPTIONS.put("hud.direction.style",                 new String[]{"Compass", "Simple"});
        CHOICE_OPTIONS.put("graphics.custom_crosshair.shape",     new String[]{"Cross", "Dot", "Circle"});
        CHOICE_OPTIONS.put("graphics.particles.show_criticals",   new String[]{"Vanilla", "Always", "Never"});
        CHOICE_OPTIONS.put("graphics.particles.show_sharpness",   new String[]{"Vanilla", "Always", "Never"});
        CHOICE_OPTIONS.put("hud.coordinates.mode",                new String[]{"Horizontal", "Vertical"});
        CHOICE_OPTIONS.put("hud.potions.mode",                    new String[]{"Compact", "Detailed"});
        CHOICE_OPTIONS.put("pvp.toggle_sprint.mode",              new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("pvp.toggle_sneak.mode",               new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.cps.mode",                        new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.day_counter.mode",                new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.ping.mode",                       new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.reach_display.mode",              new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.speed_indicator.mode",            new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.server_address.mode",             new String[]{"Modern", "Legacy"});
        CHOICE_OPTIONS.put("hud.fps_graph.graph_mode",            new String[]{"Sparkline", "Bar Chart"});
        CHOICE_OPTIONS.put("pvp.hit_sound.sound_type",            new String[]{"Ding", "Anvil", "Pop", "Orb", "Subtle Click"});
    }

    /* ------------------------------------------------------------------ */
    /*  Setting controller interface                                       */
    /* ------------------------------------------------------------------ */

    private interface SettingController {
        void draw(Object font, Setting<?> setting, int y, int mouseX);
        void click(Setting<?> setting, int mouseX, int mouseY);
        default void drag(Setting<?> setting, int mouseX) {}
        default void release() {}
    }

    /* ------------------------------------------------------------------ */
    /*  Constructors                                                       */
    /* ------------------------------------------------------------------ */

    public AetherModMenuScreen(AetherClient client) {
        this(client, null);
    }

    public AetherModMenuScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public void initGui() {
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);

        int screenMargin = w < 420 ? 8 : 18;
        int columnGap = w < 520 ? 8 : 12;
        int totalWidth = Math.min(820, Math.max(1, w - screenMargin * 2));
        int panelHeight = Math.min(h - screenMargin * 2, Math.max(180, h * 8 / 10));
        if (panelHeight < 1) panelHeight = Math.max(1, h);

        int sidebarWidth = Math.min(110, Math.max(84, totalWidth / 7));
        int propertiesWidth = Math.min(220, Math.max(140, totalWidth / 3));
        int centerWidth = totalWidth - sidebarWidth - propertiesWidth - columnGap * 2;
        if (centerWidth < 200) {
            propertiesWidth = Math.max(120, propertiesWidth - (200 - centerWidth));
            centerWidth = totalWidth - sidebarWidth - propertiesWidth - columnGap * 2;
        }

        int panelX = (w - totalWidth) / 2;
        int panelY = (h - panelHeight) / 2;

        sidebarPanel = new SidebarPanel(panelX, panelY, sidebarWidth, panelHeight);
        moduleGridPanel = new ModuleGridPanel(panelX + sidebarWidth + columnGap, panelY, centerWidth, panelHeight);
        propertyPanel = new PropertyPanel(panelX + sidebarWidth + columnGap + centerWidth + columnGap, panelY, propertiesWidth, panelHeight);

        List<ClientModule> modules = filteredModules();
        if (selectedModule == null && !modules.isEmpty()) {
            selectedModule = modules.get(0);
        } else if (selectedModule != null && !modules.contains(selectedModule)) {
            selectedModule = modules.isEmpty() ? null : modules.get(0);
        }
        moduleGridPanel.setModules(modules);
        propertyPanel.setModule(selectedModule);
    }

    public void func_73866_w_() { initGui(); }

    /* ------------------------------------------------------------------ */
    /*  Rendering                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    private void render(int mouseX, int mouseY, float partialTicks) {
        ensurePanels();
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);

        Mc189Compat.drawRect(0, 0, w, h, 0x880B0E14);

        sidebarPanel.draw(font, mouseX, mouseY, partialTicks);
        moduleGridPanel.draw(font, mouseX, mouseY, partialTicks);
        propertyPanel.draw(font, mouseX, mouseY, partialTicks);
    }

    /* ------------------------------------------------------------------ */
    /*  Input                                                              */
    /* ------------------------------------------------------------------ */

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        click(mouseX, mouseY, btn);
    }

    protected void func_73864_a(int mouseX, int mouseY, int btn) throws IOException {
        click(mouseX, mouseY, btn);
    }

    private void click(int mouseX, int mouseY, int btn) {
        ensurePanels();
        sidebarPanel.mouseClicked(mouseX, mouseY, btn);
        moduleGridPanel.mouseClicked(mouseX, mouseY, btn);
        propertyPanel.mouseClicked(mouseX, mouseY, btn);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragState == DRAG_SLIDER && draggedSetting != null && draggedController != null) {
            draggedController.drag(draggedSetting, mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragState != DRAG_NONE) {
            if (draggedController != null) draggedController.release();
            dragState = DRAG_NONE;
            draggedSetting = null;
            draggedController = null;
            saveClient();
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
            return;
        }
        ensurePanels();
        if (moduleGridPanel.isSearchFocused()) {
            if (keyCode == KEY_BACKSPACE) {
                if (searchQuery.length() > 0) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    updateFilteredModules();
                }
            } else if (c >= 32 && c < 127) {
                searchQuery += c;
                updateFilteredModules();
            }
        }
        if (keyCode == KEY_UP) moduleGridPanel.scrollBy(-30);
        else if (keyCode == KEY_DOWN) moduleGridPanel.scrollBy(30);
    }

    protected void func_73869_a(char c, int keyCode) throws IOException { keyTyped(c, keyCode); }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    public boolean func_73868_f() { return false; }

    public void handleWheelScroll(int delta) {
        ensurePanels();
        int w = Mc189Compat.screenWidth(this);
        int h = Mc189Compat.screenHeight(this);
        Object mc = Mc189Compat.minecraft();
        int mouseX = Mc189Compat.mouseX() * w / Math.max(1, Mc189Compat.displayWidth(mc));
        int mouseY = h - Mc189Compat.mouseY() * h / Math.max(1, Mc189Compat.displayHeight(mc)) - 1;

        if (propertyPanel != null && propertyPanel.isHovered(mouseX, mouseY)) {
            propertyPanel.scrollBy(delta);
        } else if (moduleGridPanel != null && moduleGridPanel.isHovered(mouseX, mouseY)) {
            moduleGridPanel.scrollBy(delta);
        } else if (sidebarPanel != null && sidebarPanel.isHovered(mouseX, mouseY)) {
            sidebarPanel.scrollBy(delta);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private void ensurePanels() {
        if (sidebarPanel == null || moduleGridPanel == null || propertyPanel == null) initGui();
    }

    private void updateFilteredModules() {
        moduleGridPanel.setModules(filteredModules());
        moduleGridPanel.scrollOffset = 0;
    }

    private List<ClientModule> filteredModules() {
        List<ClientModule> result = new ArrayList<ClientModule>();
        String needle = searchQuery.toLowerCase(Locale.ENGLISH);
        for (ClientModule module : client.modules().all()) {
            if ("cosmetics.manager".equals(module.metadata().id()) || "interface.hud_editor".equals(module.metadata().id())) {
                continue;
            }
            if (!selectedCategory.matches(module) && needle.isEmpty()) continue;
            if (!needle.isEmpty() && !matchesSearch(module, needle)) continue;
            result.add(module);
        }
        result.sort((a, b) -> a.metadata().name().compareTo(b.metadata().name()));
        return result;
    }

    private boolean matchesSearch(ClientModule module, String needle) {
        String text = module.metadata().name() + " " + module.metadata().description()
                + " " + module.metadata().category().name();
        return text.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    private void toggleModule(ClientModule module) {
        boolean enabled = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enabled);
        saveClient();
        writeLastChange("Toggled module '" + module.metadata().name() + "' -> "
                + (enabled ? "ENABLED" : "DISABLED"));
    }

    private void saveClient() {
        try { client.save(); } catch (IOException ignored) { }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setSettingValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }

    private static int nextColor(int current) {
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            if (COLOR_PRESETS[i] == current) return COLOR_PRESETS[(i + 1) % COLOR_PRESETS.length];
        }
        return COLOR_PRESETS[0];
    }

    private static int nextKeybind(int current) {
        int[] keys = {0, 46, 56, 29, 54, 57, 63};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == current) return keys[(i + 1) % keys.length];
        }
        return keys[0];
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    /* ------------------------------------------------------------------ */
    /*  lastchange.txt logging                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Writes a timestamped entry to {@code lastchange.txt} in the working
     * directory every time any change occurs in the mod manager.
     */
    private void writeLastChange(String description) {
        try {
            File changeFile = new File("lastchange.txt");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            try (PrintWriter pw = new PrintWriter(new FileWriter(changeFile, false))) {
                pw.println("=== Aether Module Manager - Last Change ===");
                pw.println("Timestamp: " + timestamp);
                pw.println("Action:    " + description);
                pw.println("============================================");
            }
        } catch (IOException e) {
            System.err.println("[Aether] Failed to write lastchange.txt: " + e.getMessage());
        }
    }

    /* ================================================================== */
    /*  Inner panel: abstract base                                         */
    /* ================================================================== */

    private abstract class BasePanel {
        protected int x, y, width, height;
        BasePanel(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; }
        abstract void draw(Object font, int mouseX, int mouseY, float dt);
        abstract void mouseClicked(int mouseX, int mouseY, int button);
        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    /* ================================================================== */
    /*  Sidebar Panel                                                      */
    /* ================================================================== */

    private class SidebarPanel extends BasePanel {
        float scrollOffset = 0;
        private float maxScroll = 0;

        SidebarPanel(int x, int y, int w, int h) { super(x, y, w, h); }

        void scrollBy(float amount) {
            scrollOffset = clamp(scrollOffset + amount, 0f, maxScroll);
        }

        private void recalcMaxScroll() {
            int headerH = 44;
            int btnH = 24;
            int gap = 6;
            int totalH = Category.values().length * (btnH + gap);
            maxScroll = Math.max(0, totalH - (height - headerH - 8));
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float dt) {
            recalcMaxScroll();
            Mc189Compat.drawRect(x, y, x + width, y + height, AetherUi.COLOR_PANEL);
            Mc189Compat.drawRect(x, y, x + 3, y + height, AetherUi.MODERN_UI_ACCENT);
            Mc189Compat.drawRect(x + 3, y, x + width, y + 1, AetherUi.COLOR_BORDER);

            AetherUi.centered(font, "AETHER", x + 3, y + 14, width - 3, AetherUi.COLOR_TEXT_PRIMARY);
            AetherUi.centered(font, "MODS", x + 3, y + 26, width - 3, AetherUi.COLOR_TEXT_SECONDARY);

            int headerH = 44;
            int listY = y + headerH;
            int listH = height - headerH - 4;

            int screenHeight = Mc189Compat.screenHeight(AetherModMenuScreen.this);
            int scale = Mc189Compat.scaleFactor(new net.minecraft.client.gui.ScaledResolution(
                    (net.minecraft.client.Minecraft) Mc189Compat.minecraft()));
            Mc189Compat.enableScissor();
            Mc189Compat.scissor(x * scale, (screenHeight - (listY + listH)) * scale, width * scale, listH * scale);

            int btnH = 24;
            int gap = 6;
            int btnX = x + 6;
            int btnW = width - 12;
            int btnY = listY + 4 - (int) scrollOffset;

            for (Category cat : Category.values()) {
                if (btnY + btnH >= listY && btnY <= listY + listH) {
                    boolean sel = selectedCategory == cat;
                    boolean hover = mouseX >= btnX && mouseX <= btnX + btnW
                            && mouseY >= btnY && mouseY <= btnY + btnH
                            && mouseY >= listY && mouseY <= listY + listH;

                    if (sel) {
                        Mc189Compat.drawRect(btnX, btnY, btnX + btnW, btnY + btnH, AetherUi.COLOR_CARD_HOVER);
                        Mc189Compat.drawRect(btnX, btnY, btnX + 3, btnY + btnH, AetherUi.MODERN_UI_ACCENT);
                    } else if (hover) {
                        Mc189Compat.drawRect(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF1A1F2A);
                    } else {
                        Mc189Compat.drawRect(btnX, btnY, btnX + btnW, btnY + btnH, AetherUi.COLOR_CARD);
                    }

                    // Draw category icon placeholder (small coloured square)
                    int iconSize = 8;
                    int iconX = btnX + 6;
                    int iconY = btnY + (btnH - iconSize) / 2;
                    int iconColor = sel ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED;
                    Mc189Compat.drawRect(iconX, iconY, iconX + iconSize, iconY + iconSize, iconColor);

                    int textColor = sel ? AetherUi.COLOR_TEXT_PRIMARY : AetherUi.COLOR_TEXT_SECONDARY;
                    String trimmedLabel = AetherUi.trim(font, cat.label, btnW - 22);
                    AetherUi.text(font, trimmedLabel, btnX + 18, btnY + (btnH - 8) / 2, textColor);
                }

                btnY += btnH + gap;
            }

            Mc189Compat.disableScissor();

            // Scrollbar
            if (maxScroll > 0) {
                int barH = listH;
                int thumbH = Math.max(16, (int) ((barH / (barH + maxScroll)) * barH));
                int thumbY = listY + (int) ((scrollOffset / maxScroll) * (barH - thumbH));
                Mc189Compat.drawRect(x + width - 3, listY, x + width, listY + barH, 0x22FFFFFF);
                Mc189Compat.drawRect(x + width - 3, thumbY, x + width, thumbY + thumbH, AetherUi.MODERN_UI_ACCENT);
            }
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0) return;
            int headerH = 44;
            int listY = y + headerH;
            int listH = height - headerH - 4;
            if (mouseY < listY || mouseY > listY + listH) return;

            int btnH = 24;
            int gap = 6;
            int btnY = listY + 4 - (int) scrollOffset;
            for (Category cat : Category.values()) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= btnY && mouseY <= btnY + btnH) {
                    selectedCategory = cat;
                    updateFilteredModules();
                    writeLastChange("Switched category to '" + cat.label + "'");
                    return;
                }
                btnY += btnH + gap;
            }
        }
    }

    /* ================================================================== */
    /*  Module Grid Panel – with image placeholders                        */
    /* ================================================================== */

    private class ModuleGridPanel extends BasePanel {
        private List<ClientModule> modules = new ArrayList<>();
        float scrollOffset = 0;
        private float maxScroll = 0;
        private boolean searchFocused = false;

        ModuleGridPanel(int x, int y, int w, int h) { super(x, y, w, h); }

        void setModules(List<ClientModule> modules) {
            this.modules = modules;
            recalcMaxScroll();
        }

        boolean isSearchFocused() { return searchFocused; }

        void scrollBy(float amount) {
            scrollOffset = clamp(scrollOffset + amount, 0f, maxScroll);
        }

        private void recalcMaxScroll() {
            int gap = 8;
            int minCardW = 90;
            int cols = Math.max(1, (width - gap) / (minCardW + gap));
            int cardH = 96;
            int rows = (modules.size() + cols - 1) / cols;
            int totalH = rows * (cardH + gap);
            maxScroll = Math.max(0, totalH - (height - 52));
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float dt) {
            recalcMaxScroll();
            // Search bar
            int searchH = 36;
            int searchBg = searchFocused ? AetherUi.COLOR_SEARCH_FOCUS : AetherUi.COLOR_SEARCH;
            Mc189Compat.drawRect(x, y, x + width, y + searchH, searchBg);
            Mc189Compat.drawRect(x, y, x + width, y + 1, AetherUi.COLOR_BORDER);

            if (searchQuery.isEmpty() && !searchFocused) {
                AetherUi.text(font, AetherUi.trim(font, "Search modules...", width - 80), x + 12, y + 14, AetherUi.COLOR_TEXT_DISABLED);
            } else {
                String cursor = searchFocused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
                AetherUi.text(font, AetherUi.trim(font, searchQuery + cursor, width - 80), x + 12, y + 14, AetherUi.COLOR_TEXT_PRIMARY);
            }

            // Module count
            String countStr = modules.size() + " module" + (modules.size() != 1 ? "s" : "");
            int countW = Mc189Compat.stringWidth(font, countStr);
            if (width - countW - 12 > 100) {
                AetherUi.text(font, countStr, x + width - countW - 8, y + 14, AetherUi.COLOR_TEXT_DISABLED);
            }

            // Grid area with scissor
            int gridY = y + searchH + 6;
            int gridH = height - searchH - 6;

            int screenHeight = Mc189Compat.screenHeight(AetherModMenuScreen.this);
            int scale = Mc189Compat.scaleFactor(new net.minecraft.client.gui.ScaledResolution(
                    (net.minecraft.client.Minecraft) Mc189Compat.minecraft()));
            Mc189Compat.enableScissor();
            Mc189Compat.scissor(x * scale, (screenHeight - (gridY + gridH)) * scale, width * scale, gridH * scale);

            int gap = 8;
            int minCardW = 90;
            int cols = Math.max(1, (width - gap) / (minCardW + gap));
            int cardW = Math.max(76, (width - gap - (cols * gap)) / cols);
            int cardH = 96;
            int gridStartX = x + (width - (cols * cardW + (cols - 1) * gap)) / 2;

            for (int i = 0; i < modules.size(); i++) {
                ClientModule mod = modules.get(i);
                int col = i % cols;
                int row = i / cols;
                int cx = gridStartX + col * (cardW + gap);
                int cy = gridY + row * (cardH + gap) - (int) scrollOffset;

                // Skip off-screen cards
                if (cy + cardH < gridY - 20 || cy > gridY + gridH + 20) continue;

                boolean isSelected = mod == selectedModule;
                boolean isEnabled = mod.state() == ModuleState.ENABLED;
                boolean hover = mouseX >= cx && mouseX <= cx + cardW
                        && mouseY >= cy && mouseY <= cy + cardH
                        && mouseY >= gridY && mouseY <= gridY + gridH;

                // Card background
                int cardBg = isSelected ? AetherUi.COLOR_CARD_HOVER : AetherUi.COLOR_CARD;
                if (hover && !isSelected) cardBg = 0xFF1A1F2A;
                Mc189Compat.drawRect(cx, cy, cx + cardW, cy + cardH, cardBg);

                // Accent bar at top when enabled
                if (isEnabled) {
                    Mc189Compat.drawRect(cx, cy, cx + cardW, cy + 3, AetherUi.MODERN_UI_ACCENT);
                }

                // Selection indicator
                if (isSelected) {
                    Mc189Compat.drawRect(cx, cy, cx + 2, cy + cardH, AetherUi.MODERN_UI_ACCENT);
                }

                // ---- IMAGE PLACEHOLDER ----
                int iconSize = Math.min(30, cardW - 20);
                int iconX = cx + (cardW - iconSize) / 2;
                int iconY = cy + 12;
                drawModuleIconPlaceholder(font, mod, iconX, iconY, iconSize);

                // Module name (centred below icon)
                String name = mod.metadata().name();
                String trimmedName = AetherUi.trim(font, name, cardW - 8);
                AetherUi.centered(font, trimmedName, cx, iconY + iconSize + 6, cardW, AetherUi.COLOR_TEXT_PRIMARY);

                // Enabled/Disabled label
                String status = isEnabled ? "Enabled" : "Disabled";
                int statusColor = isEnabled ? 0xFF55FF88 : AetherUi.COLOR_TEXT_DISABLED;
                AetherUi.centered(font, status, cx, iconY + iconSize + 18, cardW, statusColor);

                // Settings gear indicator (top-right)
                if (!mod.settings().isEmpty()) {
                    AetherUi.text(font, "\u2699", cx + cardW - 12, cy + 5, AetherUi.COLOR_TEXT_DISABLED);
                }

                // Subtle hover border
                if (hover) {
                    int bc = 0x44FFFFFF;
                    Mc189Compat.drawRect(cx, cy, cx + cardW, cy + 1, bc);
                    Mc189Compat.drawRect(cx, cy + cardH - 1, cx + cardW, cy + cardH, bc);
                    Mc189Compat.drawRect(cx, cy, cx + 1, cy + cardH, bc);
                    Mc189Compat.drawRect(cx + cardW - 1, cy, cx + cardW, cy + cardH, bc);
                }
            }

            Mc189Compat.disableScissor();

            // Scrollbar
            if (maxScroll > 0) {
                int barH = gridH;
                int thumbH = (int) ((barH / (barH + maxScroll)) * barH);
                thumbH = Math.max(20, thumbH);
                int thumbY = gridY + (int) ((scrollOffset / maxScroll) * (barH - thumbH));
                Mc189Compat.drawRect(x + width - 4, gridY, x + width, gridY + barH, 0x22FFFFFF);
                Mc189Compat.drawRect(x + width - 4, thumbY, x + width, thumbY + thumbH, AetherUi.MODERN_UI_ACCENT);
            }
        }

        /**
         * Draws a coloured rectangle with the first letter of the module name
         * as an image placeholder for the module icon.
         */
        private void drawModuleIconPlaceholder(Object font, ClientModule mod, int ix, int iy, int size) {
            // Try to load actual texture first
            String texturePath = "textures/mod/" + mod.metadata().id() + ".png";
            try {
                Mc189Compat.drawTexture(texturePath, ix, iy, size, size);
                return; // If texture exists, we're done
            } catch (Exception ignored) {
                // Fall through to placeholder
            }

            // Coloured placeholder based on category
            Integer catColor = CATEGORY_ICON_COLORS.get(mod.metadata().category());
            int bgColor = catColor != null ? catColor : 0xFF4A5568;
            Mc189Compat.drawRect(ix, iy, ix + size, iy + size, bgColor);

            // Subtle inner border
            Mc189Compat.drawRect(ix, iy, ix + size, iy + 1, 0x33FFFFFF);
            Mc189Compat.drawRect(ix, iy + size - 1, ix + size, iy + size, 0x22000000);
            Mc189Compat.drawRect(ix, iy, ix + 1, iy + size, 0x22FFFFFF);
            Mc189Compat.drawRect(ix + size - 1, iy, ix + size, iy + size, 0x22000000);

            // First letter centred
            String letter = mod.metadata().name().substring(0, 1).toUpperCase();
            AetherUi.centered(font, letter, ix, iy + (size - 8) / 2, size, 0xFFFFFFFF);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0) {
                // Search bar focus
                searchFocused = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 36;

                // Module cards
                int gridY = y + 42;
                int gridH = height - 42;
                if (mouseX >= x && mouseX <= x + width && mouseY >= gridY && mouseY <= gridY + gridH) {
                    int gap = 8;
                    int minCardW = 90;
                    int cols = Math.max(1, (width - gap) / (minCardW + gap));
                    int cardW = Math.max(76, (width - gap - (cols * gap)) / cols);
                    int cardH = 96;
                    int gridStartX = x + (width - (cols * cardW + (cols - 1) * gap)) / 2;

                    for (int i = 0; i < modules.size(); i++) {
                        ClientModule mod = modules.get(i);
                        int col = i % cols;
                        int row = i / cols;
                        int cx = gridStartX + col * (cardW + gap);
                        int cy = gridY + row * (cardH + gap) - (int) scrollOffset;

                        if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                            selectedModule = mod;
                            propertyPanel.setModule(mod);
                            writeLastChange("Selected module '" + mod.metadata().name() + "'");
                            return;
                        }
                    }
                }
            }
        }
    }

    /* ================================================================== */
    /*  Property/Settings Panel                                            */
    /* ================================================================== */

    private class PropertyPanel extends BasePanel {
        private ClientModule module;
        private float scrollOffset = 0;
        private float maxScroll = 0;
        private final Map<SettingType, SettingController> settingControllers;

        PropertyPanel(int x, int y, int w, int h) {
            super(x, y, w, h);
            settingControllers = new EnumMap<>(SettingType.class);
            initControllers();
        }

        void scrollBy(float amount) {
            scrollOffset = clamp(scrollOffset + amount, 0f, maxScroll);
        }

        void setModule(ClientModule module) {
            if (this.module != module) {
                scrollOffset = 0;
                maxScroll = 0;
            }
            this.module = module;
        }

        private void initControllers() {
            settingControllers.put(SettingType.BOOLEAN, new SettingController() {
                @Override public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    boolean on = ((Boolean) setting.value()).booleanValue();
                    int toggleX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    Mc189Compat.drawRect(toggleX, y, toggleX + 30, y + 14, AetherUi.COLOR_TOGGLE_BG);
                    Mc189Compat.drawRect(toggleX + 2, y + 2, toggleX + 28, y + 12,
                            on ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED);
                }
                @Override public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int toggleX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    int rowY = findRowY(setting);
                    if (mouseX >= toggleX && mouseX <= toggleX + 30 && mouseY >= rowY && mouseY <= rowY + 14) {
                        setSettingValue(setting, !((Boolean) setting.value()));
                        saveClient();
                        writeLastChange("Setting '" + setting.label() + "' -> " + setting.value());
                    }
                }
            });

            settingControllers.put(SettingType.COLOR, new SettingController() {
                @Override public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    int swatchX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    Mc189Compat.drawRect(swatchX - 1, y - 1, swatchX + 31, y + 15, AetherUi.COLOR_BORDER);
                    Mc189Compat.drawRect(swatchX, y, swatchX + 30, y + 14, ((Number) setting.value()).intValue());
                }
                @Override public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int swatchX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    int rowY = findRowY(setting);
                    if (mouseX >= swatchX - 1 && mouseX <= swatchX + 31 && mouseY >= rowY - 1 && mouseY <= rowY + 15) {
                        setSettingValue(setting, nextColor(((Number) setting.value()).intValue()));
                        saveClient();
                        writeLastChange("Setting '" + setting.label() + "' color changed");
                    }
                }
            });

            settingControllers.put(SettingType.NUMBER, new SettingController() {
                @Override public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    int value = ((Number) setting.value()).intValue();
                    int sliderW = Math.min(80, PropertyPanel.this.width - 120);
                    if (sliderW < 30) sliderW = 30;
                    int sliderX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - sliderW;
                    int min = getNumberMin(setting);
                    int max = getNumberMax(setting);
                    float pct = max > min ? (float) (value - min) / (float) (max - min) : 0;
                    int fillW = (int) (pct * sliderW);

                    Mc189Compat.drawRect(sliderX, y + 2, sliderX + sliderW, y + 12, AetherUi.COLOR_TOGGLE_BG);
                    Mc189Compat.drawRect(sliderX, y + 2, sliderX + fillW, y + 12, AetherUi.MODERN_UI_ACCENT);

                    // Knob
                    int knobX = sliderX + fillW - 2;
                    Mc189Compat.drawRect(knobX, y, knobX + 4, y + 14, 0xDDFFFFFF);

                    String valStr = String.valueOf(value);
                    int textX = sliderX - Mc189Compat.stringWidth(font, valStr) - 6;
                    AetherUi.text(font, valStr, textX, y + 4, AetherUi.COLOR_TEXT_SECONDARY);
                }
                @Override public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int sliderW = Math.min(80, PropertyPanel.this.width - 120);
                    if (sliderW < 30) sliderW = 30;
                    int sliderX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - sliderW;
                    int rowY = findRowY(setting);
                    if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= rowY + 2 && mouseY <= rowY + 12) {
                        draggedSetting = setting;
                        dragState = DRAG_SLIDER;
                        draggedController = this;
                        drag(setting, mouseX);
                    }
                }
                @Override public void drag(Setting<?> setting, int mouseX) {
                    int sliderW = Math.min(80, PropertyPanel.this.width - 120);
                    if (sliderW < 30) sliderW = 30;
                    int sliderX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - sliderW;
                    float pct = clamp((float) (mouseX - sliderX) / sliderW, 0f, 1f);
                    int min = getNumberMin(setting);
                    int max = getNumberMax(setting);
                    int step = getNumberStep(setting);
                    int value = min + (int) (pct * (max - min));
                    if (step > 0) value = Math.round((float) value / step) * step;
                    setSettingValue(setting, Integer.valueOf(clamp(value, min, max)));
                }
                @Override public void release() {
                    saveClient();
                    if (draggedSetting != null) {
                        writeLastChange("Setting '" + draggedSetting.label() + "' -> " + draggedSetting.value());
                    }
                }
            });

            SettingController pillController = new SettingController() {
                @Override public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    String text = setting.type() == SettingType.KEYBIND
                            ? Mc189Compat.keyName(((Number) setting.value()).intValue())
                            : String.valueOf(setting.value());
                    int maxPillW = Math.max(30, PropertyPanel.this.width - 100);
                    String trimmed = AetherUi.trim(font, text, maxPillW - 10);
                    int w = Math.min(maxPillW, Math.max(34, Mc189Compat.stringWidth(font, trimmed) + 10));
                    int ctrlX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - w;
                    Mc189Compat.drawRect(ctrlX, y, ctrlX + w, y + 14, AetherUi.COLOR_CARD);
                    AetherUi.centered(font, trimmed, ctrlX, y + 4, w, AetherUi.COLOR_TEXT_SECONDARY);
                }
                @Override public void click(Setting<?> setting, int mouseX, int mouseY) {
                    Object font = Mc189Compat.screenFontRenderer(AetherModMenuScreen.this);
                    String text = setting.type() == SettingType.KEYBIND
                            ? Mc189Compat.keyName(((Number) setting.value()).intValue())
                            : String.valueOf(setting.value());
                    int maxPillW = Math.max(30, PropertyPanel.this.width - 100);
                    String trimmed = AetherUi.trim(font, text, maxPillW - 10);
                    int w = Math.min(maxPillW, Math.max(34, Mc189Compat.stringWidth(font, trimmed) + 10));
                    int ctrlX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - w;
                    int rowY = findRowY(setting);
                    if (mouseX >= ctrlX && mouseX <= ctrlX + w && mouseY >= rowY && mouseY <= rowY + 14) {
                        if (setting.type() == SettingType.KEYBIND) {
                            setSettingValue(setting, nextKeybind(((Number) setting.value()).intValue()));
                        } else {
                            setSettingValue(setting, getNextChoice(module, setting));
                        }
                        saveClient();
                        writeLastChange("Setting '" + setting.label() + "' -> " + setting.value());
                    }
                }
            };
            settingControllers.put(SettingType.CHOICE, pillController);
            settingControllers.put(SettingType.KEYBIND, pillController);
            settingControllers.put(SettingType.TEXT, pillController);
        }

        private int getSettingStartY() {
            int contentY = y - (int) scrollOffset;
            int descY = contentY + 34;
            String desc = module != null ? module.metadata().description() : null;
            int maxDescW = width - 30;
            Object font = Mc189Compat.screenFontRenderer(AetherModMenuScreen.this);
            if (desc != null && !desc.isEmpty()) {
                String[] words = desc.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() == 0) {
                        line.append(word);
                    } else if (Mc189Compat.stringWidth(font, line + " " + word) <= maxDescW) {
                        line.append(" ").append(word);
                    } else {
                        descY += 11;
                        line = new StringBuilder(word);
                    }
                }
                if (line.length() > 0) descY += 11;
            } else {
                descY += 11;
            }
            return descY + 30;
        }

        private int findRowY(Setting<?> target) {
            if (module == null || module.settings().isEmpty()) return -1;
            int sY = getSettingStartY() + 20;
            for (Setting<?> s : module.settings()) {
                if (s == target) return sY;
                sY += 22;
            }
            return -1;
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float dt) {
            Mc189Compat.drawRect(x, y, x + width, y + height, AetherUi.COLOR_PANEL);
            Mc189Compat.drawRect(x, y, x + width, y + 1, AetherUi.COLOR_BORDER);

            if (module == null) {
                AetherUi.centered(font, "Select a module", x, y + height / 2 - 4, width, AetherUi.COLOR_TEXT_DISABLED);
                return;
            }

            int screenH = Mc189Compat.screenHeight(AetherModMenuScreen.this);
            int scale = Mc189Compat.scaleFactor(new net.minecraft.client.gui.ScaledResolution(
                    (net.minecraft.client.Minecraft) Mc189Compat.minecraft()));
            Mc189Compat.enableScissor();
            Mc189Compat.scissor(x * scale, (screenH - (y + height)) * scale, width * scale, height * scale);

            int contentY = y - (int) scrollOffset;

            // Module icon placeholder in header
            int headerIconSize = 24;
            int headerIconX = x + 12;
            int headerIconY = contentY + 14;
            Integer catColor = CATEGORY_ICON_COLORS.get(module.metadata().category());
            int iconBg = catColor != null ? catColor : 0xFF4A5568;
            Mc189Compat.drawRect(headerIconX, headerIconY, headerIconX + headerIconSize, headerIconY + headerIconSize, iconBg);
            String letter = module.metadata().name().substring(0, 1).toUpperCase();
            AetherUi.centered(font, letter, headerIconX, headerIconY + (headerIconSize - 8) / 2, headerIconSize, 0xFFFFFFFF);

            String titleStr = AetherUi.trim(font, module.metadata().name(), width - (headerIconSize + 28));
            AetherUi.text(font, titleStr, headerIconX + headerIconSize + 8, contentY + 18, AetherUi.COLOR_TEXT_PRIMARY);

            // Description (multiline word-wrap to fit inside box)
            String desc = module.metadata().description();
            int maxDescW = width - 30;
            int descY = contentY + 34;
            if (desc != null && !desc.isEmpty()) {
                String[] words = desc.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() == 0) {
                        line.append(word);
                    } else if (Mc189Compat.stringWidth(font, line + " " + word) <= maxDescW) {
                        line.append(" ").append(word);
                    } else {
                        AetherUi.text(font, line.toString(), x + 15, descY, AetherUi.COLOR_TEXT_SECONDARY);
                        descY += 11;
                        line = new StringBuilder(word);
                    }
                }
                if (line.length() > 0) {
                    AetherUi.text(font, AetherUi.trim(font, line.toString(), maxDescW), x + 15, descY, AetherUi.COLOR_TEXT_SECONDARY);
                    descY += 11;
                }
            } else {
                descY += 11;
            }

            int settingY = descY + 10;

            // General section
            AetherUi.text(font, "GENERAL", x + 15, settingY, AetherUi.COLOR_TEXT_DISABLED);
            settingY += 20;

            // Enabled toggle
            AetherUi.text(font, AetherUi.trim(font, "Enabled", width - 70), x + 15, settingY + 4, AetherUi.COLOR_TEXT_PRIMARY);
            boolean enabled = module.state() == ModuleState.ENABLED;
            int toggleX = x + width - 45;
            Mc189Compat.drawRect(toggleX, settingY, toggleX + 30, settingY + 14, AetherUi.COLOR_TOGGLE_BG);
            Mc189Compat.drawRect(toggleX + 2, settingY + 2, toggleX + 28, settingY + 12,
                    enabled ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED);
            settingY += 25;

            // Module settings
            if (!module.settings().isEmpty()) {
                settingY += 10;
                AetherUi.text(font, "SETTINGS", x + 15, settingY, AetherUi.COLOR_TEXT_DISABLED);
                settingY += 20;

                for (Setting<?> s : module.settings()) {
                    AetherUi.text(font, AetherUi.trim(font, s.label(), width - 110), x + 15, settingY + 4, AetherUi.COLOR_TEXT_PRIMARY);
                    SettingController ctrl = settingControllers.get(s.type());
                    if (ctrl != null) ctrl.draw(font, s, settingY, mouseX);
                    settingY += 22;
                }
            }

            maxScroll = Math.max(0, (settingY + (int) scrollOffset) - (y + height) + 10);
            Mc189Compat.disableScissor();

            // Scrollbar for property panel
            if (maxScroll > 0) {
                int barH = height;
                int thumbH = Math.max(20, (int) ((barH / (barH + maxScroll)) * barH));
                int thumbY = y + (int) ((scrollOffset / maxScroll) * (barH - thumbH));
                Mc189Compat.drawRect(x + width - 3, y, x + width, y + barH, 0x22FFFFFF);
                Mc189Compat.drawRect(x + width - 3, thumbY, x + width, thumbY + thumbH, AetherUi.MODERN_UI_ACCENT);
            }
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0 || module == null || !isHovered(mouseX, mouseY)) return;

            int contentY = y - (int) scrollOffset;
            int descY = contentY + 34;
            String desc = module.metadata().description();
            int maxDescW = width - 30;
            Object font = Mc189Compat.screenFontRenderer(AetherModMenuScreen.this);
            if (desc != null && !desc.isEmpty()) {
                String[] words = desc.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() == 0) {
                        line.append(word);
                    } else if (Mc189Compat.stringWidth(font, line + " " + word) <= maxDescW) {
                        line.append(" ").append(word);
                    } else {
                        descY += 11;
                        line = new StringBuilder(word);
                    }
                }
                if (line.length() > 0) descY += 11;
            } else {
                descY += 11;
            }

            int settingY = descY + 30;

            // Enabled toggle
            if (mouseY >= settingY && mouseY <= settingY + 14) {
                int toggleX = x + width - 45;
                if (mouseX >= toggleX && mouseX <= toggleX + 30) {
                    toggleModule(module);
                    return;
                }
            }
            settingY += 25;

            if (!module.settings().isEmpty()) {
                settingY += 20;
                for (Setting<?> s : module.settings()) {
                    if (mouseY >= settingY && mouseY <= settingY + 22) {
                        SettingController ctrl = settingControllers.get(s.type());
                        if (ctrl != null) ctrl.click(s, mouseX, mouseY);
                        return;
                    }
                    settingY += 22;
                }
            }
        }

        private String getNextChoice(ClientModule module, Setting<?> setting) {
            String current = String.valueOf(setting.value());
            String key = module.metadata().id() + "." + setting.id();
            String[] options = CHOICE_OPTIONS.getOrDefault(key, new String[]{current});
            if (options.length <= 1) return current;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(current)) return options[(i + 1) % options.length];
            }
            return options[0];
        }

        private int getNumberMin(Setting<?> s) {
            NumberProperty p = NUMBER_PROPERTIES.get(s.id());
            return p != null ? p.min : 0;
        }
        private int getNumberMax(Setting<?> s) {
            NumberProperty p = NUMBER_PROPERTIES.get(s.id());
            return p != null ? p.max : 100;
        }
        private int getNumberStep(Setting<?> s) {
            NumberProperty p = NUMBER_PROPERTIES.get(s.id());
            return p != null ? p.step : 1;
        }
    }

    /* ================================================================== */
    /*  Categories                                                         */
    /* ================================================================== */

    private enum Category {
        HOME("Home"),
        HUD("HUD"),
        GAMEPLAY("Gameplay"),
        RENDER("Render"),
        PERFORMANCE("Performance"),
        COSMETICS("Cosmetics"),
        CLIENT("Client");

        final String label;
        Category(String label) { this.label = label; }

        boolean matches(ClientModule module) {
            ModuleCategory cat = module.metadata().category();
            switch (this) {
                case HOME:        return module.state() == ModuleState.ENABLED;
                case HUD:         return cat == ModuleCategory.HUD;
                case GAMEPLAY:    return cat == ModuleCategory.PVP || cat == ModuleCategory.MOVEMENT;
                case RENDER:      return cat == ModuleCategory.RENDER || cat == ModuleCategory.GRAPHICS;
                case PERFORMANCE: return cat == ModuleCategory.PERFORMANCE;
                case COSMETICS:   return cat == ModuleCategory.COSMETICS;
                case CLIENT:      return cat == ModuleCategory.GENERAL || cat == ModuleCategory.THEMES
                                         || cat == ModuleCategory.INTERFACE;
                default: return false;
            }
        }
    }
}
