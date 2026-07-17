package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.module.setting.Setting.SettingType;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AetherModMenuScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final int KEY_BACKSPACE = 14;
    private static final int KEY_UP = 200;
    private static final int KEY_DOWN = 208;
    private static final int DRAG_NONE = 0, DRAG_SLIDER = 1;

    private final AetherClient client;
    private final GuiScreen parent;

    private SidebarPanel sidebarPanel;
    private ModuleListPanel moduleListPanel;
    private PropertyPanel propertyPanel;

    private Category selectedCategory = Category.HOME;
    private ClientModule selectedModule;
    private String searchQuery = "";
    private int dragState = DRAG_NONE;
    private Setting<?> draggedSetting = null;
    private SettingController draggedController = null;
    private static final int[] COLOR_PRESETS = {
        0xFFFFFFFF,
        0xFF52BEEB,
        0xFF387DFF,
        0xFFFF5555,
        0xFF55FF88,
        0xFFFFFF55,
        0xFFFF55FF,
        0x6F000000,
        0xAA10141B,
        0x00000000
    };

    private static class NumberProperty {
        final int min, max, step;
        NumberProperty(int min, int max, int step) {
            this.min = min;
            this.max = max;
            this.step = step;
        }
    }

    private static final Map<String, NumberProperty> NUMBER_PROPERTIES = new HashMap<>();
    private static final Map<String, String[]> CHOICE_OPTIONS = new HashMap<>();

    static {
        NUMBER_PROPERTIES.put("fps_limit", new NumberProperty(30, 1000, 10));
        NUMBER_PROPERTIES.put("zoom_percent", new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("min_zoom_percent", new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("max_zoom_percent", new NumberProperty(5, 100, 5));
        NUMBER_PROPERTIES.put("strength", new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("amount", new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("shake_amount", new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("box_size", new NumberProperty(14, 34, 1));
        NUMBER_PROPERTIES.put("click_size", new NumberProperty(14, 34, 1));
        NUMBER_PROPERTIES.put("spacebar_height", new NumberProperty(8, 24, 1));
        NUMBER_PROPERTIES.put("gap", new NumberProperty(0, 8, 1));
        NUMBER_PROPERTIES.put("scroll_step", new NumberProperty(1, 25, 1));
        NUMBER_PROPERTIES.put("thickness", new NumberProperty(1, 25, 1));
        NUMBER_PROPERTIES.put("fade_time", new NumberProperty(0, 500, 1));
        NUMBER_PROPERTIES.put("brightness", new NumberProperty(0, 100, 5));
        NUMBER_PROPERTIES.put("max_render_distance", new NumberProperty(2, 32, 1));
        NUMBER_PROPERTIES.put("memory_threshold", new NumberProperty(1, 95, 5));
        NUMBER_PROPERTIES.put("scale", new NumberProperty(50, 150, 1));
        NUMBER_PROPERTIES.put("opacity", new NumberProperty(0, 100, 1));
        NUMBER_PROPERTIES.put("size", new NumberProperty(1, 32, 1));
        NUMBER_PROPERTIES.put("sensitivity", new NumberProperty(10, 250, 10));

        CHOICE_OPTIONS.put("hud.clock.format", new String[] {"24h", "12h"});
        CHOICE_OPTIONS.put("hud.direction.style", new String[] {"Compass", "Simple"});
        CHOICE_OPTIONS.put("graphics.custom_crosshair.shape", new String[] {"Cross", "Dot", "Circle"});
        CHOICE_OPTIONS.put("graphics.particles.show_criticals", new String[] {"Vanilla", "Always", "Never"});
        CHOICE_OPTIONS.put("graphics.particles.show_sharpness", new String[] {"Vanilla", "Always", "Never"});
        CHOICE_OPTIONS.put("hud.coordinates.mode", new String[] {"Horizontal", "Vertical"});
        CHOICE_OPTIONS.put("hud.potions.mode", new String[] {"Compact", "Detailed"});
        // Special handling for module-specific "mode" settings
        CHOICE_OPTIONS.put("pvp.toggle_sprint.mode", new String[] {"Modern"});
        CHOICE_OPTIONS.put("hud.cps.mode", new String[] {"Modern"});
    }

    private interface SettingController {
        void draw(Object font, Setting<?> setting, int y, int mouseX);
        void click(Setting<?> setting, int mouseX, int mouseY);
        default void drag(Setting<?> setting, int mouseX) {}
        default void release() {}
    }

    public AetherModMenuScreen(AetherClient client) {
        this(client, null);
    }

    public AetherModMenuScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);

        int screenMargin = width < 420 ? 8 : 18;
        int columnGap = width < 520 ? 8 : 12;
        int totalWidth = Math.min(760, Math.max(1, width - screenMargin * 2));
        int panelHeight = Math.min(height - screenMargin * 2, Math.max(180, height * 8 / 10));
        if (panelHeight < 1) {
            panelHeight = Math.max(1, height);
        }

        int sidebarWidth = Math.min(120, Math.max(84, totalWidth / 6));
        int propertiesWidth = Math.min(230, Math.max(150, totalWidth / 3));
        int centerWidth = totalWidth - sidebarWidth - propertiesWidth - columnGap * 2;
        if (centerWidth < 180) {
            propertiesWidth = Math.max(120, propertiesWidth - (180 - centerWidth));
            centerWidth = totalWidth - sidebarWidth - propertiesWidth - columnGap * 2;
        }
        if (centerWidth < 140) {
            sidebarWidth = Math.max(72, sidebarWidth - (140 - centerWidth));
            centerWidth = totalWidth - sidebarWidth - propertiesWidth - columnGap * 2;
        }

        int panelX = (width - totalWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        this.sidebarPanel = new SidebarPanel(panelX, panelY, sidebarWidth, panelHeight);
        this.moduleListPanel = new ModuleListPanel(panelX + sidebarWidth + columnGap, panelY, centerWidth, panelHeight);
        this.propertyPanel = new PropertyPanel(panelX + sidebarWidth + columnGap + centerWidth + columnGap, panelY, propertiesWidth, panelHeight);

        List<ClientModule> modules = filteredModules();
        if (this.selectedModule == null && !modules.isEmpty()) {
            this.selectedModule = modules.get(0);
        } else if (this.selectedModule != null && !modules.contains(this.selectedModule)) {
            this.selectedModule = modules.isEmpty() ? null : modules.get(0);
        }
        this.moduleListPanel.setModules(modules);
        this.propertyPanel.setModule(this.selectedModule);
    }

    public void func_73866_w_() {
        initGui();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    protected void mouseClicked(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void func_73864_a(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(this.parent);
            return;
        }

        ensurePanels();

        if (this.moduleListPanel.isSearchFocused()) {
            if (keyCode == KEY_BACKSPACE) {
                if (this.searchQuery.length() > 0) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                    this.updateFilteredModules();
                }
            } else if (typedChar >= 32 && typedChar < 127) {
                this.searchQuery += typedChar;
                this.updateFilteredModules();
            }
        }

        if (keyCode == KEY_UP) {
            this.moduleListPanel.scrollBy(-30);
        } else if (keyCode == KEY_DOWN) {
            this.moduleListPanel.scrollBy(30);
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean func_73868_f() {
        return false;
    }

    public void handleWheelScroll(int delta) {
        ensurePanels();
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object minecraft = Mc189Compat.minecraft();
        int mouseX = Mc189Compat.mouseX() * width / Math.max(1, Mc189Compat.displayWidth(minecraft));
        int mouseY = height - Mc189Compat.mouseY() * height / Math.max(1, Mc189Compat.displayHeight(minecraft)) - 1;

        if (this.propertyPanel != null && this.propertyPanel.isHovered(mouseX, mouseY)) {
            this.propertyPanel.scrollBy(delta);
        } else if (this.moduleListPanel != null && this.moduleListPanel.isHovered(mouseX, mouseY)) {
            this.moduleListPanel.scrollBy(delta);
        }
    }


    private void render(int mouseX, int mouseY, float partialTicks) {
        ensurePanels();

        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);

        Mc189Compat.drawRect(0, 0, width, height, 0x660B0E14);

        this.sidebarPanel.draw(font, mouseX, mouseY, partialTicks);
        this.moduleListPanel.draw(font, mouseX, mouseY, partialTicks);
        this.propertyPanel.draw(font, mouseX, mouseY, partialTicks);
    }

    private void click(int mouseX, int mouseY, int clickedButton) {
        ensurePanels();

        this.sidebarPanel.mouseClicked(mouseX, mouseY, clickedButton);
        this.moduleListPanel.mouseClicked(mouseX, mouseY, clickedButton);
        this.propertyPanel.mouseClicked(mouseX, mouseY, clickedButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.dragState == DRAG_SLIDER && this.draggedSetting != null && this.draggedController != null) {
            this.draggedController.drag(this.draggedSetting, mouseX);
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.dragState != DRAG_NONE) {
            if (this.draggedController != null) {
                this.draggedController.release();
            }
            this.dragState = DRAG_NONE;
            this.draggedSetting = null;
            this.draggedController = null;
            saveClient();
        }
    }

    private void ensurePanels() {
        if (this.sidebarPanel == null || this.moduleListPanel == null || this.propertyPanel == null) {
            initGui();
        }
    }

    private void updateFilteredModules() {
        this.moduleListPanel.setModules(filteredModules());
        this.moduleListPanel.scrollOffset = 0;
    }

    private List<ClientModule> filteredModules() {
        List<ClientModule> result = new ArrayList<ClientModule>();
        String needle = this.searchQuery.toLowerCase(Locale.ENGLISH);
        for (ClientModule module : client.modules().all()) {
            if (!this.selectedCategory.matches(module) && needle.isEmpty()) {
                continue;
            }
            if (!needle.isEmpty() && !matchesSearch(module, needle)) {
                continue;
            }
            result.add(module);
        }
        result.sort((a, b) -> a.metadata().name().compareTo(b.metadata().name()));
        return result;
    }

    private boolean matchesSearch(ClientModule module, String needle) {
        String text = module.metadata().name() + " " + module.metadata().description() + " " + module.metadata().category().name();
        return text.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    private void toggleModule(ClientModule module) {
        boolean enabled = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enabled);
        saveClient();
    }

    private void saveClient() {
        try {
            client.save();
        } catch (IOException exception) {
            // In a real app, show a notification. For now, fail silently.
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setSettingValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }

    private static int nextColor(int current) {
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            if (COLOR_PRESETS[i] == current) {
                return COLOR_PRESETS[(i + 1) % COLOR_PRESETS.length];
            }
        }
        return COLOR_PRESETS[0];
    }

    private static String colorHex(int color) {
        return String.format("#%08X", Integer.valueOf(color));
    }

    private static int nextKeybind(int current) {
        int[] keys = {0, 46, 56, 29, 54, 57, 63};
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == current) {
                return keys[(i + 1) % keys.length];
            }
        }
        return keys[0];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private abstract class Panel {
        protected int x, y, width, height;

        Panel(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        abstract void draw(Object font, int mouseX, int mouseY, float deltaTime);

        abstract void mouseClicked(int mouseX, int mouseY, int button);

        protected boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
        }
    }

    private class SidebarPanel extends Panel {
        private final int hudEditorButtonX;
        private final int hudEditorButtonY;
        private final int hudEditorButtonSize;

        SidebarPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.hudEditorButtonSize = 28;
            this.hudEditorButtonX = this.x + (this.width - this.hudEditorButtonSize) / 2;
            this.hudEditorButtonY = this.y + this.height - this.hudEditorButtonSize - 10;
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float deltaTime) {
            Mc189Compat.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, AetherUi.COLOR_PANEL);
            Mc189Compat.drawRect(this.x, this.y, this.x + 3, this.y + this.height, AetherUi.MODERN_UI_ACCENT);
            Mc189Compat.drawRect(this.x + 3, this.y, this.x + this.width, this.y + 1, AetherUi.COLOR_BORDER);

            AetherUi.centered(font, "AETHER", this.x + 3, this.y + 18, this.width - 3, AetherUi.COLOR_TEXT_PRIMARY);
            AetherUi.centered(font, "MODS", this.x + 3, this.y + 32, this.width - 3, AetherUi.COLOR_TEXT_SECONDARY);

            int buttonHeight = 28;
            int buttonGap = 8;
            int buttonX = this.x + 11;
            int buttonWidth = this.width - 18;
            int buttonY = this.y + 62;

            for (Category category : Category.values()) {
                boolean isSelected = selectedCategory == category;

                if (isSelected) {
                    Mc189Compat.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, AetherUi.COLOR_CARD_HOVER);
                    Mc189Compat.drawRect(buttonX, buttonY, buttonX + 3, buttonY + buttonHeight, AetherUi.MODERN_UI_ACCENT);
                    Mc189Compat.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + 1, AetherUi.COLOR_BORDER);
                } else {
                    Mc189Compat.drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, AetherUi.COLOR_CARD);
                }

                int textColor = isSelected ? AetherUi.COLOR_TEXT_PRIMARY : AetherUi.COLOR_TEXT_SECONDARY;
                AetherUi.centered(font, category.label, buttonX + 4, buttonY + 10, buttonWidth - 4, textColor);

                buttonY += buttonHeight + buttonGap;
            }

            // HUD Editor button
            boolean isHovered = mouseX >= this.hudEditorButtonX && mouseX <= this.hudEditorButtonX + this.hudEditorButtonSize &&
                                mouseY >= this.hudEditorButtonY && mouseY <= this.hudEditorButtonY + this.hudEditorButtonSize;

            int buttonBg = isHovered ? AetherUi.COLOR_CARD_HOVER : AetherUi.COLOR_CARD;
            Mc189Compat.drawRect(this.hudEditorButtonX, this.hudEditorButtonY, this.hudEditorButtonX + this.hudEditorButtonSize, this.hudEditorButtonY + this.hudEditorButtonSize, buttonBg);

            int iconColor = isHovered ? AetherUi.COLOR_TEXT_PRIMARY : AetherUi.COLOR_TEXT_SECONDARY;
            AetherUi.drawEditIcon(this.hudEditorButtonX + (this.hudEditorButtonSize - 12) / 2, this.hudEditorButtonY + (this.hudEditorButtonSize - 12) / 2, iconColor);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0) {
                int buttonHeight = 28;
                int buttonGap = 8;
                int buttonY = this.y + 62;
                for (Category category : Category.values()) {
                    if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                        selectedCategory = category;
                        updateFilteredModules();
                        return;
                    }
                    buttonY += buttonHeight + buttonGap;
                }

                if (mouseX >= this.hudEditorButtonX && mouseX <= this.hudEditorButtonX + this.hudEditorButtonSize &&
                    mouseY >= this.hudEditorButtonY && mouseY <= this.hudEditorButtonY + this.hudEditorButtonSize) {
                    Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
                    return;
                }
            }
        }
    }

    private class ModuleListPanel extends Panel {
        private List<ClientModule> modules = new ArrayList<>();
        private float scrollOffset = 0;
        private float maxScroll = 0;
        private boolean searchFocused = false;

        ModuleListPanel(int x, int y, int width, int height) { 
            super(x, y, width, height);
        }

        void setModules(List<ClientModule> modules) {
            this.modules = modules;
            this.maxScroll = Math.max(0, modules.size() * 60 - (this.height - 60));
        }

        boolean isSearchFocused() {
            return this.searchFocused;
        }

        void scrollBy(float amount) {
            this.scrollOffset += amount;
            if (this.scrollOffset < 0) this.scrollOffset = 0;
            if (this.scrollOffset > this.maxScroll) this.scrollOffset = this.maxScroll;
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float deltaTime) {
            // Search Bar
            int searchX = this.x;
            int searchY = this.y;
            int searchHeight = 40;
            int searchBg = this.searchFocused ? AetherUi.COLOR_SEARCH_FOCUS : AetherUi.COLOR_SEARCH;
            Mc189Compat.drawRect(searchX, searchY, searchX + this.width, searchY + searchHeight, searchBg);
            Mc189Compat.drawRect(searchX, searchY, searchX + this.width, searchY + 1, AetherUi.COLOR_BORDER);
            if (searchQuery.isEmpty() && !this.searchFocused) {
                AetherUi.text(font, "Search modules...", searchX + 12, searchY + 16, AetherUi.COLOR_TEXT_DISABLED);
            } else {
                AetherUi.text(font, searchQuery + (this.searchFocused && System.currentTimeMillis() % 1000 > 500 ? "_" : ""), searchX + 12, searchY + 16, AetherUi.COLOR_TEXT_PRIMARY);
            }

            // Module List
            int listY = searchY + searchHeight + 10;
            int listHeight = this.height - (searchHeight + 10);

            int screenHeight = Mc189Compat.screenHeight(AetherModMenuScreen.this);
            int scale = Mc189Compat.scaleFactor(new net.minecraft.client.gui.ScaledResolution((net.minecraft.client.Minecraft) Mc189Compat.minecraft()));
            Mc189Compat.enableScissor();
            Mc189Compat.scissor(this.x * scale, (screenHeight - (listY + listHeight)) * scale, this.width * scale, listHeight * scale);

            int cardY = listY - (int) this.scrollOffset;
            for (ClientModule module : this.modules) {
                drawModuleCard(font, module, this.x, cardY, this.width, 50, mouseX, mouseY, deltaTime);
                cardY += 60;
            }

            Mc189Compat.disableScissor();

            // Scrollbar
            if (this.maxScroll > 0) {
                int scrollbarHeight = listHeight;
                int thumbHeight = (int) ((scrollbarHeight / (scrollbarHeight + this.maxScroll)) * scrollbarHeight);
                int thumbY = listY + (int) ((this.scrollOffset / this.maxScroll) * (scrollbarHeight - thumbHeight));
                Mc189Compat.drawRect(this.x + this.width + 4, thumbY, this.x + this.width + 8, thumbY + thumbHeight, AetherUi.MODERN_UI_ACCENT);
            }
        }

        private void drawModuleCard(Object font, ClientModule module, int x, int y, int width, int height, int mouseX, int mouseY, float deltaTime) {
            boolean isSelected = selectedModule == module;

            Mc189Compat.drawRect(x, y, x + width, y + height, AetherUi.COLOR_CARD);

            if (isSelected) {
                Mc189Compat.drawRect(x, y, x + width, y + height, AetherUi.COLOR_CARD_HOVER);
                Mc189Compat.drawRect(x, y, x + 2, y + height, AetherUi.MODERN_UI_ACCENT);
            }

            AetherUi.text(font, module.metadata().name(), x + 15, y + 12, AetherUi.COLOR_TEXT_PRIMARY);
            AetherUi.text(font, AetherUi.trim(font, module.metadata().description(), width - 70), x + 15, y + 26, AetherUi.COLOR_TEXT_SECONDARY);

            // Toggle
            boolean enabled = module.state() == ModuleState.ENABLED;
            int toggleX = x + width - 45;
            int toggleY = y + 18;
            Mc189Compat.drawRect(toggleX, toggleY, toggleX + 30, toggleY + 14, AetherUi.COLOR_TOGGLE_BG);
            Mc189Compat.drawRect(toggleX + 2, toggleY + 2, toggleX + 28, toggleY + 12, enabled ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0) {
                // Search bar focus
                this.searchFocused = mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 40;

                // Module cards
                int listY = this.y + 50;
                int listHeight = this.height - 50;
                if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= listY && mouseY <= listY + listHeight) {
                    int cardY = listY - (int) this.scrollOffset;
                    for (ClientModule module : this.modules) {
                        if (mouseY >= cardY && mouseY <= cardY + 50) {
                            // Check toggle click
                            int toggleX = this.x + this.width - 45;
                            if (mouseX >= toggleX && mouseX <= toggleX + 30) {
                                toggleModule(module);
                            } else {
                                selectedModule = module;
                                propertyPanel.setModule(module);
                            }
                            return;
                        }
                        cardY += 60;
                    }
                }
            }
        }
    }

    private class PropertyPanel extends Panel {
        private ClientModule module;
        private float scrollOffset = 0;
        private float maxScroll = 0;
        private final Map<SettingType, SettingController> settingControllers;

        PropertyPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.settingControllers = new EnumMap<>(SettingType.class);
            this.initControllers();
        }

        void scrollBy(float amount) {
            this.scrollOffset = clamp(this.scrollOffset + amount, 0.0f, this.maxScroll);
        }

        private void initControllers() {
            settingControllers.put(SettingType.BOOLEAN, new SettingController() {
                @Override
                public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    drawBooleanControl(((Boolean) setting.value()).booleanValue(), y);
                }
                @Override
                public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int toggleX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    if (mouseX >= toggleX && mouseX <= toggleX + 30 && mouseY >= rowY(setting) && mouseY <= rowY(setting) + 14) {
                        setSettingValue(setting, !((Boolean) setting.value()));
                        saveClient();
                    }
                }
            });

            settingControllers.put(SettingType.COLOR, new SettingController() {
                @Override
                public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    drawColorControl(((Number) setting.value()).intValue(), y);
                }
                @Override
                public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int swatchX = PropertyPanel.this.x + PropertyPanel.this.width - 45;
                    if (mouseX >= swatchX - 1 && mouseX <= swatchX + 31 && mouseY >= rowY(setting) -1 && mouseY <= rowY(setting) + 15) {
                        setSettingValue(setting, nextColor(((Number) setting.value()).intValue()));
                        saveClient();
                    }
                }
            });

            settingControllers.put(SettingType.NUMBER, new SettingController() {
                @Override
                public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    drawNumberSlider(font, setting, ((Number) setting.value()).intValue(), y, mouseX);
                }
                @Override
                public void click(Setting<?> setting, int mouseX, int mouseY) {
                    int sliderWidth = 80;
                    int sliderX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - sliderWidth;
                    if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= rowY(setting) + 2 && mouseY <= rowY(setting) + 12) {
                        draggedSetting = setting;
                        dragState = DRAG_SLIDER;
                        draggedController = this;
                        this.drag(setting, mouseX);
                    }
                }
                @Override
                public void drag(Setting<?> setting, int mouseX) {
                    if (setting == null) return;
                    int sliderWidth = 80;
                    int sliderX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - sliderWidth;
                    float percent = (float) (mouseX - sliderX) / (float) sliderWidth;
                    percent = clamp(percent, 0.0F, 1.0F);

                    int min = getNumberMin(setting);
                    int max = getNumberMax(setting);
                    int value = min + (int) (percent * (max - min));
                    int step = getNumberStep(setting);
                    if (step > 0) {
                        value = Math.round((float) value / step) * step;
                    }

                    setSettingValue(setting, Integer.valueOf(clamp(value, min, max)));
                }
                @Override
                public void release() {
                    saveClient();
                }
            });

            SettingController pillController = new SettingController() {
                @Override
                public void draw(Object font, Setting<?> setting, int y, int mouseX) {
                    String text = setting.type() == SettingType.KEYBIND
                        ? Mc189Compat.keyName(((Number) setting.value()).intValue())
                        : String.valueOf(setting.value());
                    drawValuePill(font, text, y);
                }
                @Override
                public void click(Setting<?> setting, int mouseX, int mouseY) {
                    Object font = Mc189Compat.screenFontRenderer(AetherModMenuScreen.this);
                    String text = setting.type() == SettingType.KEYBIND
                        ? Mc189Compat.keyName(((Number) setting.value()).intValue())
                        : String.valueOf(setting.value());
                    String trimmed = AetherUi.trim(font, text, 74);
                    int width = Math.max(34, Mc189Compat.stringWidth(font, trimmed) + 10);
                    int controlX = PropertyPanel.this.x + PropertyPanel.this.width - 15 - width;
                    int controlY = rowY(setting);

                    if (mouseX >= controlX && mouseX <= controlX + width && mouseY >= controlY && mouseY <= controlY + 14) {
                        if (setting.type() == SettingType.KEYBIND) {
                            setSettingValue(setting, nextKeybind(((Number) setting.value()).intValue()));
                        } else {
                            setSettingValue(setting, getNextChoice(PropertyPanel.this.module, setting));
                        }
                        saveClient();
                    }
                }
            };
            settingControllers.put(SettingType.CHOICE, pillController);
            settingControllers.put(SettingType.KEYBIND, pillController);
            settingControllers.put(SettingType.TEXT, pillController);
        }

        private int rowY(Setting<?> target) {
            int settingY = this.y - (int) this.scrollOffset + 60;
            settingY += 20; // General
            settingY += 25; // Enabled
            if (!this.module.settings().isEmpty()) {
                settingY += 10; // SETTINGS header
                settingY += 20;
                for (Setting<?> setting : this.module.settings()) {
                    if (setting == target) {
                        return settingY;
                    }
                    settingY += 22;
                }
            }
            return -1;
        }

        void setModule(ClientModule module) {
            if (this.module != module) {
                this.scrollOffset = 0;
                this.maxScroll = 0;
            }
            this.module = module;
        }

        @Override
        void draw(Object font, int mouseX, int mouseY, float deltaTime) {
            Mc189Compat.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, AetherUi.COLOR_PANEL);
            Mc189Compat.drawRect(this.x, this.y, this.x + this.width, this.y + 1, AetherUi.COLOR_BORDER);

            if (this.module == null) {
                AetherUi.centered(font, "No module selected", this.x, this.y + this.height / 2 - 4, this.width, AetherUi.COLOR_TEXT_DISABLED);
                return;
            }

            int screenHeight = Mc189Compat.screenHeight(AetherModMenuScreen.this);
            int scale = Mc189Compat.scaleFactor(new net.minecraft.client.gui.ScaledResolution((net.minecraft.client.Minecraft) Mc189Compat.minecraft()));
            Mc189Compat.enableScissor();
            Mc189Compat.scissor(this.x * scale, (screenHeight - (this.y + this.height)) * scale, this.width * scale, this.height * scale);

            int contentY = this.y - (int) this.scrollOffset;

            AetherUi.text(font, this.module.metadata().name(), this.x + 15, contentY + 20, AetherUi.COLOR_TEXT_PRIMARY);
            AetherUi.text(font, AetherUi.trim(font, this.module.metadata().description(), this.width - 30), this.x + 15, contentY + 34, AetherUi.COLOR_TEXT_SECONDARY);

            int settingY = contentY + 60;

            // General section
            AetherUi.text(font, "GENERAL", this.x + 15, settingY, AetherUi.COLOR_TEXT_DISABLED);
            settingY += 20;

            // State Toggle
            drawSettingRow(font, "Enabled", settingY, mouseX, mouseY);
            boolean enabled = this.module.state() == ModuleState.ENABLED;
            int toggleX = this.x + this.width - 45;
            Mc189Compat.drawRect(toggleX, settingY, toggleX + 30, settingY + 14, AetherUi.COLOR_TOGGLE_BG);
            Mc189Compat.drawRect(toggleX + 2, settingY + 2, toggleX + 28, settingY + 12, enabled ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED);
            settingY += 25;

            // Other settings
            if (!this.module.settings().isEmpty()) {
                settingY += 10;
                AetherUi.text(font, "SETTINGS", this.x + 15, settingY, AetherUi.COLOR_TEXT_DISABLED);
                settingY += 20;

                for (Setting<?> setting : this.module.settings()) {
                    drawSettingRow(font, setting.label(), settingY, mouseX, mouseY);
                    drawSettingControl(font, setting, settingY, mouseX);
                    settingY += 22;
                }
            }

            this.maxScroll = Math.max(0, (settingY + (int) this.scrollOffset) - (this.y + this.height) + 10);
            Mc189Compat.disableScissor();
        }

        @SuppressWarnings("rawtypes")
        private void drawSettingRow(Object font, String label, int y, int mouseX, int mouseY) {
            AetherUi.text(font, AetherUi.trim(font, label, this.width - 120), this.x + 15, y + 4, AetherUi.COLOR_TEXT_PRIMARY);
        }

        private void drawSettingControl(Object font, Setting<?> setting, int y, int mouseX) {
            SettingController controller = this.settingControllers.get(setting.type());
            if (controller != null) {
                controller.draw(font, setting, y, mouseX);
            } else {
                // Fallback for unhandled types
                drawValuePill(font, String.valueOf(setting.value()), y);
            }
        }

        private void drawBooleanControl(boolean enabled, int y) {
            int toggleX = this.x + this.width - 45;
            Mc189Compat.drawRect(toggleX, y, toggleX + 30, y + 14, AetherUi.COLOR_TOGGLE_BG);
            Mc189Compat.drawRect(toggleX + 2, y + 2, toggleX + 28, y + 12, enabled ? AetherUi.MODERN_UI_ACCENT : AetherUi.COLOR_TEXT_DISABLED);
        }

        private void drawColorControl(int color, int y) {
            int swatchX = this.x + this.width - 45;
            Mc189Compat.drawRect(swatchX - 1, y - 1, swatchX + 31, y + 15, AetherUi.COLOR_BORDER);
            Mc189Compat.drawRect(swatchX, y, swatchX + 30, y + 14, color);
        }

        private void drawNumberSlider(Object font, Setting<?> setting, int value, int y, int mouseX) {
            int sliderWidth = 80;
            int sliderX = this.x + this.width - 15 - sliderWidth;
            int min = getNumberMin(setting);
            int max = getNumberMax(setting);
            float percent = (float) (value - min) / (float) (max - min);
            int fillWidth = (int) (percent * sliderWidth);

            Mc189Compat.drawRect(sliderX, y + 2, sliderX + sliderWidth, y + 12, AetherUi.COLOR_TOGGLE_BG);
            Mc189Compat.drawRect(sliderX, y + 2, sliderX + fillWidth, y + 12, AetherUi.MODERN_UI_ACCENT);

            String valueStr = String.valueOf(value);
            int textX = sliderX - Mc189Compat.stringWidth(font, valueStr) - 6;
            AetherUi.text(font, valueStr, textX, y + 4, AetherUi.COLOR_TEXT_SECONDARY);
        }

        @SuppressWarnings("rawtypes")
        private void drawValuePill(Object font, String value, int y) {
            String text = AetherUi.trim(font, value, 74);
            int width = Math.max(34, Mc189Compat.stringWidth(font, text) + 10);
            int controlX = this.x + this.width - 15 - width;
            Mc189Compat.drawRect(controlX, y, controlX + width, y + 14, AetherUi.COLOR_CARD);
            AetherUi.centered(font, text, controlX, y + 4, width, AetherUi.COLOR_TEXT_SECONDARY);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0 && this.module != null && isHovered(mouseX, mouseY)) {
                int contentY = this.y - (int) this.scrollOffset;
                int settingY = contentY + 80;
                // State Toggle
                if (mouseY >= settingY && mouseY <= settingY + 14) {
                    int toggleX = this.x + this.width - 45;
                    if (mouseX >= toggleX && mouseX <= toggleX + 30) {
                        toggleModule(this.module);
                        return;
                    }
                }
                settingY += 25;

                int rowY = settingY;
                if (!this.module.settings().isEmpty()) {
                    rowY += 30;
                    for (Setting<?> setting : this.module.settings()) {
                        if (mouseY >= rowY && mouseY <= rowY + 22) {
                            SettingController controller = settingControllers.get(setting.type());
                            if (controller != null) {
                                controller.click(setting, mouseX, mouseY);
                            }
                            return;
                        }
                        rowY += 22;
                    }
                }
            }
        }

        private String[] getChoiceOptions(ClientModule module, Setting<?> setting) {
            String key = module.metadata().id() + "." + setting.id();
            if (CHOICE_OPTIONS.containsKey(key)) {
                return CHOICE_OPTIONS.get(key);
            }
            return new String[]{String.valueOf(setting.value())};
        }

        private String getNextChoice(ClientModule module, Setting<?> setting) {
            String current = String.valueOf(setting.value());
            String[] options = getChoiceOptions(module, setting);
            if (options.length <= 1) {
                return current;
            }
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(current)) {
                    return options[(i + 1) % options.length];
                }
            }
            return options[0];
        }

        private int getNumberMin(Setting<?> setting) {
            NumberProperty p = NUMBER_PROPERTIES.get(setting.id());
            return p != null ? p.min : 0;
        }

        private int getNumberMax(Setting<?> setting) {
            NumberProperty p = NUMBER_PROPERTIES.get(setting.id());
            return p != null ? p.max : 100;
        }

        private int getNumberStep(Setting<?> setting) {
            NumberProperty p = NUMBER_PROPERTIES.get(setting.id());
            return p != null ? p.step : 1;
        }
    }

    private enum Category {
        HOME("Home", ModuleCategory.INTERFACE), // Simplified matching
        HUD("HUD", ModuleCategory.HUD),
        GAMEPLAY("Gameplay", ModuleCategory.PVP, ModuleCategory.MOVEMENT),
        RENDER("Render", ModuleCategory.RENDER, ModuleCategory.GRAPHICS),
        PERFORMANCE("Performance", ModuleCategory.PERFORMANCE),
        COSMETICS("Cosmetics", ModuleCategory.COSMETICS),
        CLIENT("Client", ModuleCategory.GENERAL, ModuleCategory.THEMES);

        private final String label;
        private final ModuleCategory[] categories;

        Category(String label, ModuleCategory... categories) {
            this.label = label;
            this.categories = categories;
        }

        boolean matches(ClientModule module) {
            if (this == HOME) {
                // In a real implementation, this would check favorites.
                // For now, show all enabled.
                return module.state() == ModuleState.ENABLED;
            }
            for (ModuleCategory cat : categories) {
                if (cat == module.metadata().category()) {
                    return true;
                }
            }
            return false;
        }

    }
}
