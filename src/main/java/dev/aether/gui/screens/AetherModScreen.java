package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.Button;
import dev.aether.gui.components.ChoicePill;
import dev.aether.gui.components.Slider;
import dev.aether.gui.components.TextField;
import dev.aether.gui.components.Toggle;
import dev.aether.gui.core.ScrollContainer;
import dev.aether.gui.core.ScreenShell;
import dev.aether.gui.core.UiComponent;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.ui.GuiSection;
import dev.aether.ui.ModuleSearch;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * The Modules screen - the equivalent of Leaf Client's {@code ModSettings}:
 * branding and search on top, All/Live/Favorites chips and category pills under them,
 * then the scrollable card list. A gear on a card opens that module's configuration
 * panel in place of the list (Leaf's {@code ModDetailSettings} concept), and a star
 * toggles the user's favorite flag.
 * <p>
 * Filtering never rebuilds the screen: the card components are rebuilt inside the
 * existing scroll container, and the search/filter state lives in the tested
 * {@link ModuleSearch} core. Module state stays in {@code ModuleRegistry}; the panel
 * only reads and writes it.
 */
public final class AetherModScreen extends AetherGuiScreen {

    private static final int CARD_GAP = 10;
    private static final int CHIPS_HEIGHT = 26;
    private static final int PILL_HEIGHT = 22;
    private static final int ROW_GAP = 8;

    private final ModuleSearch search = new ModuleSearch();
    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private final TextField searchField = new TextField("Search modules..", new java.util.function.Consumer<String>() {
        public void accept(String value) {
            search.query(value);
            syncVisible();
        }
    });

    private final ScrollContainer cardList = new ScrollContainer();
    private final ScrollContainer settingsList = new ScrollContainer();
    private final Button backButton = new Button("< Back", Button.Style.QUIET, new Runnable() {
        public void run() {
            closeModuleSettings();
        }
    });

    private String openModuleId;
    private Slider activeSlider;
    private String captureKeybindSettingId;
    private boolean draggingNumber;

    /** Fixed chip positions, recomputed on layout. */
    private final int[][] chipRects = new int[3][4];
    private final List<int[]> pillRects = new ArrayList<int[]>();

    public AetherModScreen(dev.aether.AetherClient client) {
        super(client);
        syncVisible();
    }

    /** Opens the screen pre-filtered to one category (the theme selector's entry point). */
    public void focusCategory(ModuleCategory category) {
        searchField.reset();
        search.query("");
        search.category(category);
        syncVisible();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.MODULES;
    }

    @Override
    protected String title() {
        return "Modules";
    }

    @Override
    protected TextField searchField() {
        return searchField;
    }

    @Override
    protected String footerHint() {
        if (captureKeybindSettingId != null) {
            return "Press any key to bind it - ESC cancels";
        }
        return "Right Shift: close  |  The star marks a favorite";
    }

    @Override
    protected void layout(double contentX, double contentY, double contentW, double contentH) {
        searchField.at((int) shell.searchX(), shell.searchY()).size(ScreenShell.SEARCH_WIDTH, ScreenShell.SEARCH_HEIGHT);

        int chipW = (int) Math.min(150.0D, contentW / 5.0D);
        for (int i = 0; i < chipRects.length; i++) {
            chipRects[i][0] = (int) contentX + i * (chipW + 8);
            chipRects[i][1] = (int) contentY;
            chipRects[i][2] = chipW;
            chipRects[i][3] = CHIPS_HEIGHT;
        }
        layoutPills(contentX, contentY + CHIPS_HEIGHT + 10, contentW);

        if (openModuleId != null) {
            backButton.at((int) contentX, (int) contentY).size(chipW, CHIPS_HEIGHT);
            double panelTop = contentY + CHIPS_HEIGHT + 14;
            settingsList.at((int) contentX, (int) panelTop)
                .size((int) contentW, (int) (contentH - (panelTop - contentY)));
            rebuildSettingRows();
        } else {
            double listTop = contentY + CHIPS_HEIGHT + 10 + PILL_HEIGHT + 10 + (pillRows() - 1) * (PILL_HEIGHT / 2 + 4);
            cardList.at((int) contentX, (int) listTop)
                .size((int) contentW, (int) (contentH - (listTop - contentY)));
            rebuildCards();
        }
    }

    private int pillRows() {
        return Math.max(1, (pillRects.size() + 7) / 8);
    }

    private void layoutPills(double contentX, double y, double contentW) {
        pillRects.clear();
        double px = contentX;
        double py = y;
        for (ModuleCategory category : orderedCategories()) {
            String name = label(category);
            int measuredPx = AetherFont.width(AetherFont.Size.SMALL, name) + GuiScale.w(16);
            double width = GuiScale.mouseX(measuredPx); // px back into design units
            if (px + width > contentX + contentW) {
                px = contentX;
                py += PILL_HEIGHT + 6;
            }
            pillRects.add(new int[] {(int) px, (int) py, (int) width, PILL_HEIGHT});
            px += width + 6;
        }
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private void syncVisible() {
        search.source(client.modules().all());
        List<ClientModule> results = search.results();
        visible.clear();
        boolean favFilter = search.favoritesOnly();
        for (ClientModule module : results) {
            if (favFilter && !isFavorite(module)) {
                continue;
            }
            visible.add(module);
        }
        if (openModuleId == null) {
            rebuildCards();
        }
    }

    private boolean isFavorite(ClientModule module) {
        return module.metadata().favoriteByDefault() || client.favorites().isFavorite(module.metadata().id());
    }

    private void rebuildCards() {
        cardList.clear();
        int cardWidth = cardList.getWidth();
        double y = 0;
        for (ClientModule module : visible) {
            ModuleCard card = new ModuleCard(module);
            card.at(0, (int) y).size(Math.max(60, cardWidth - 12), card.cardHeight());
            cardList.add(card);
            y += card.cardHeight() + CARD_GAP;
        }
        cardList.clampOffset();
    }

    private void rebuildSettingRows() {
        settingsList.clear();
        activeSlider = null;
        ClientModule module = moduleById(openModuleId);
        if (module == null) {
            return;
        }
        double y = 0;
        int rowWidth = Math.max(60, settingsList.getWidth() - 12);
        for (Setting<?> setting : module.settings()) {
            SettingRow row = new SettingRow(module, setting);
            row.at(0, (int) y).size(rowWidth, rowHeight(setting));
            settingsList.add(row);
            y += rowHeight(setting) + ROW_GAP;
        }
        settingsList.clampOffset();
    }

    private ClientModule moduleById(String id) {
        if (id == null) {
            return null;
        }
        try {
            return client.modules().get(id);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private void toggleModule(ClientModule module) {
        boolean enable = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enable);
        saveQuietly();
        search.invalidate();
        syncVisible();
    }

    private void openModuleSettings(ClientModule module) {
        openModuleId = module.metadata().id();
        captureKeybindSettingId = null;
        draggingNumber = false;
        relayout();
    }

    private void closeModuleSettings() {
        openModuleId = null;
        captureKeybindSettingId = null;
        draggingNumber = false;
        relayout();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        if (openModuleId != null) {
            renderSettingsView(mx, my);
            return;
        }
        drawFilterChips(mx, my);
        drawCategoryPills(mx, my);
        cardList.render();
    }

    private void renderSettingsView(double mx, double my) {
        backButton.render();
        ClientModule module = moduleById(openModuleId);
        if (module != null) {
            int nameX = backButton.getX() + backButton.getWidth() + (int) GuiScale.w(16);
            AetherFont.draw(AetherFont.Size.SECTION, module.metadata().name(), GuiScale.x(nameX),
                GuiScale.y(settingsList.getY() - 36), AetherUi.TEXT_PRIMARY);
            String desc = module.metadata().description();
            if (!desc.isEmpty()) {
                AetherFont.draw(AetherFont.Size.SMALL, desc, GuiScale.x(nameX),
                    GuiScale.y(settingsList.getY() - 14), AetherUi.TEXT_SECONDARY);
            }
        }
        settingsList.render();
    }

    private void drawFilterChips(double mx, double my) {
        String[] labels = {"All", "Live", "Favorites"};
        for (int i = 0; i < labels.length; i++) {
            boolean active = i == 0 ? !search.liveOnly() && !search.favoritesOnly()
                : i == 1 ? search.liveOnly() : search.favoritesOnly();
            drawChip(labels[i], chipRects[i], active, isOver(chipRects[i], mx, my));
        }
    }

    private void drawChip(String label, int[] rect, boolean active, boolean hover) {
        int gx = GuiScale.x(rect[0]);
        int gy = GuiScale.y(rect[1]);
        int gw = GuiScale.w(rect[2]);
        int gh = GuiScale.h(rect[3]);
        int fill = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
        AetherUi.drawRoundRect(gx, gy, gx + gw, gy + gh, GuiScale.h(7), fill);
        AetherFont.drawCentered(AetherFont.Size.SMALL, label, gx, gy + (gh - AetherFont.height(AetherFont.Size.SMALL)) / 2, gw,
            active ? AetherUi.readableOn(AetherUi.ACCENT) : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
    }

    private void drawCategoryPills(double mx, double my) {
        List<ModuleCategory> categories = orderedCategories();
        for (int i = 0; i < categories.size() && i < pillRects.size(); i++) {
            int[] rect = pillRects.get(i);
            boolean active = search.category() == categories.get(i);
            boolean hover = mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3];
            int gx = GuiScale.x(rect[0]);
            int gy = GuiScale.y(rect[1]);
            int gw = GuiScale.w(rect[2]);
            int gh = GuiScale.h(rect[3]);
            int fill = active ? AetherUi.withAlpha(AetherUi.ACCENT, 0x3D)
                : hover ? AetherUi.ROW_HOVER : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22);
            AetherUi.drawRoundRect(gx, gy, gx + gw, gy + gh, GuiScale.h(6), fill);
            AetherFont.draw(AetherFont.Size.SMALL, label(categories.get(i)), gx + GuiScale.w(8),
                gy + (gh - AetherFont.height(AetherFont.Size.SMALL)) / 2,
                active ? AetherUi.TEXT_PRIMARY : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        }
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (openModuleId != null) {
            // Only one text field may own the keyboard: drop focus everywhere first,
            // then let the dispatch re-focus the field that was actually clicked.
            settingsList.forEachChild(new java.util.function.Consumer<UiComponent>() {
                public void accept(UiComponent child) {
                    if (child instanceof SettingRow) {
                        ((SettingRow) child).defocus();
                    }
                }
            });
            backButton.onMouseClick(mx, my, button);
            settingsList.onMouseClick(mx, my, button);
            return true;
        }
        for (int i = 0; i < chipRects.length; i++) {
            if (isOver(chipRects[i], mx, my)) {
                if (i == 0) {
                    search.liveOnly(false);
                    search.favoritesOnly(false);
                } else if (i == 1) {
                    search.liveOnly(true);
                    search.favoritesOnly(false);
                } else {
                    search.favoritesOnly(!search.favoritesOnly());
                    search.liveOnly(false);
                }
                syncVisible();
                return true;
            }
        }
        List<ModuleCategory> categories = orderedCategories();
        for (int i = 0; i < categories.size() && i < pillRects.size(); i++) {
            int[] rect = pillRects.get(i);
            if (mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3]) {
                search.category(search.category() == categories.get(i) ? null : categories.get(i));
                syncVisible();
                return true;
            }
        }
        return cardList.onMouseClick(mx, my, button);
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        cardList.onMouseRelease(mx, my, button);
        settingsList.onMouseRelease(mx, my, button);
        boolean changed = false;
        if (draggingNumber) {
            draggingNumber = false;
            changed = true;
        }
        if (activeSlider != null) {
            activeSlider.endDrag();
            activeSlider = null;
            changed = true;
        }
        if (changed) {
            saveQuietly();
        }
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        if (captureKeybindSettingId != null) {
            if (keyCode != 1) { // ESC cancels without binding
                ClientModule module = moduleById(openModuleId);
                Setting<?> setting = findSetting(module, captureKeybindSettingId);
                if (setting != null) {
                    setValue(setting, keyCode);
                    saveQuietly();
                }
            }
            captureKeybindSettingId = null;
            return true;
        }
        return settingsList.onKeyTyped(typedChar, keyCode);
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        return cardList.onWheel(mx, my, delta) || settingsList.onWheel(mx, my, delta);
    }

    @Override
    protected void disposeContent() {
        cardList.dispose();
        settingsList.dispose();
    }

    /* ── helpers ────────────────────────────────────────────────────────── */

    /** Headless test / visual-debug hook: scrolls the card list directly. */
    public void debugScrollTo(double offset) {
        cardList.scrollTo(offset);
    }

    /** Headless test / visual-debug hook: opens the first visible module that has settings. */
    public void debugOpenFirstModuleSettings() {
        for (ClientModule module : visible) {
            if (!module.settings().isEmpty()) {
                openModuleSettings(module);
                return;
            }
        }
        if (!visible.isEmpty()) {
            openModuleSettings(visible.get(0));
        }
    }

    private static boolean isOver(int[] rect, double mx, double my) {
        return mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3];
    }

    private static Setting<?> findSetting(ClientModule module, String settingId) {
        if (module == null) {
            return null;
        }
        for (Setting<?> setting : module.settings()) {
            if (setting.id().equals(settingId)) {
                return setting;
            }
        }
        return null;
    }

    private static int rowHeight(Setting<?> setting) {
        return setting.type() == Setting.SettingType.BOOLEAN ? 36 : 52;
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

    /* ── the module card ────────────────────────────────────────────────── */

    /**
     * One module row: name, category badge, description, favorite star, gear, toggle.
     * Hit regions live in content space (the scroll container converts mouse
     * coordinates before dispatch), the drawing converts through {@link GuiScale}.
     */
    private final class ModuleCard extends UiComponent {

        private final ClientModule module;
        private boolean hover;

        ModuleCard(ClientModule module) {
            this.module = module;
        }

        int cardHeight() {
            return module.metadata().description().isEmpty() ? 52 : 66;
        }

        /* Hit regions, design units relative to the card's own rectangle. */
        private boolean inToggle(double mx, double my) {
            return region(mx, my, 14, 46, 8, 20, 0);
        }

        private boolean inGear(double mx, double my) {
            return region(mx, my, 14 + 46 + 8, 22, 0, 0, 0);
        }

        private boolean inStar(double mx, double my) {
            return region(mx, my, 14 + 46 + 8 + 22 + 8, 22, 0, 0, 0);
        }

        private boolean region(double mx, double my, double fromRight, double w, double yOff, double h, double extraRight) {
            double x = this.x + this.width - fromRight - w - extraRight;
            double y = this.y + yOff;
            return mx >= x && mx < x + w && my >= y && my < y + (h > 0 ? h : this.height);
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            boolean on = module.state() == ModuleState.ENABLED;

            int base = hover ? AetherUi.CARD_HOVER : AetherUi.CARD;
            AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(8), base);
            AetherUi.outline(left, top, left + w, top + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));
            if (on) {
                Mc189Compat.drawRect(left, top + GuiScale.h(6), left + GuiScale.w(3), top + h - GuiScale.h(6), AetherUi.ACCENT_ON);
            }

            int pad = GuiScale.w(14);
            int textTop = top + GuiScale.h(9);
            int nameColor = on ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED;
            AetherFont.draw(AetherFont.Size.BODY, module.metadata().name(), left + pad, textTop, nameColor);

            String tag = label(module.metadata().category());
            int tagW = AetherFont.width(AetherFont.Size.CAPTION, tag) + GuiScale.w(12);
            int tagX = left + w - pad - GuiScale.w(46) - GuiScale.w(96) - tagW;
            int tagY = textTop + GuiScale.h(1);
            AetherUi.drawRoundRect(tagX, tagY, tagX + tagW, tagY + GuiScale.h(15), GuiScale.h(5),
                AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));
            AetherFont.draw(AetherFont.Size.CAPTION, tag, tagX + GuiScale.w(6),
                tagY + (GuiScale.h(15) - AetherFont.height(AetherFont.Size.CAPTION)) / 2, AetherUi.TEXT_SECONDARY);

            if (!module.metadata().description().isEmpty()) {
                double maxDesc = width - 14.0D * 2 - 46.0D - 96.0D;
                String desc = AetherFont.trimTo(AetherFont.Size.SMALL, module.metadata().description(),
                    GuiScale.w(Math.max(60.0D, maxDesc)));
                AetherFont.draw(AetherFont.Size.SMALL, desc, left + pad, textTop + GuiScale.h(26), AetherUi.TEXT_DISABLED);
            }

            drawToggle(left + w - pad - GuiScale.w(46), top + GuiScale.h(8), on);
            drawGear(left + w - pad - GuiScale.w(46) - GuiScale.w(8) - GuiScale.w(14), top + GuiScale.h(10));
            drawStar(left + w - pad - GuiScale.w(46) - GuiScale.w(8) - GuiScale.w(14) - GuiScale.w(8) - GuiScale.w(14), top + GuiScale.h(10));
        }

        private void drawToggle(int x, int y, boolean on) {
            int tw = GuiScale.w(46);
            int th = GuiScale.h(20);
            int track = on ? AetherUi.ACCENT_ON : hover ? AetherUi.TOGGLE_BG : AetherUi.withAlpha(AetherUi.TOGGLE_BG, 0xCC);
            AetherUi.drawRoundRect(x, y, x + tw, y + th, th / 2, track);
            int knob = th - GuiScale.h(4);
            int knobX = on ? x + tw - knob - GuiScale.h(2) : x + GuiScale.h(2);
            AetherUi.drawRoundRect(knobX, y + GuiScale.h(2), knobX + knob, y + th - GuiScale.h(2), knob / 2,
                on ? 0xFF0B1210 : 0xFFE8E9F0);
        }

        private void drawGear(int x, int y) {
            int color = AetherUi.TEXT_SECONDARY;
            int cx = x + GuiScale.w(7);
            int cy = y + GuiScale.h(7);
            AetherUi.drawCircle(cx, cy, GuiScale.h(6), color);
            AetherUi.drawCircle(cx, cy, GuiScale.h(2), AetherUi.CARD);
            for (int i = 0; i < 3; i++) {
                Mc189Compat.drawRect(cx - 1, cy - GuiScale.h(9) + i * GuiScale.h(8), cx + 1, cy - GuiScale.h(7) + i * GuiScale.h(8), color);
            }
            for (int i = 0; i < 3; i++) {
                Mc189Compat.drawRect(cx - GuiScale.h(9) + i * GuiScale.h(8), cy - 1, cx - GuiScale.h(7) + i * GuiScale.h(8), cy + 1, color);
            }
        }

        private void drawStar(int x, int y) {
            boolean fav = isFavorite(module);
            int color = fav ? AetherUi.STAR : AetherUi.TEXT_DISABLED;
            int s = GuiScale.h(14);
            Mc189Compat.drawRect(x + s / 2 - Math.max(1, GuiScale.w(1)), y, x + s / 2 + Math.max(1, GuiScale.w(1)), y + s, color);
            Mc189Compat.drawRect(x, y + s / 3, x + s, y + s / 3 + GuiScale.h(3), color);
            AetherUi.drawCircle(x + s / 2, y + s / 2, s / 4, color);
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
            if (inToggle(mx, my)) {
                toggleModule(module);
                return true;
            }
            if (inGear(mx, my)) {
                openModuleSettings(module);
                return true;
            }
            if (inStar(mx, my)) {
                client.favorites().toggle(module.metadata().id());
                saveQuietly();
                search.invalidate();
                syncVisible();
                return true;
            }
            return true; // the card surface swallows the click; nothing behind it may react
        }
    }

    /* ── the setting row ────────────────────────────────────────────────── */

    /**
     * One row of the configuration panel. Boolean rows draw their own pill; number,
     * choice and text rows delegate to the matching kit component, which the row
     * positions and forwards input to.
     */
    private final class SettingRow extends UiComponent {

        private final ClientModule module;
        private final Setting<?> setting;
        private final Slider slider;
        private final ChoicePill choicePill;
        private final Toggle boolToggle;
        private final TextField textEdit;
        private boolean hover;
        private boolean textDirty;

        SettingRow(ClientModule module, Setting<?> setting) {
            this.module = module;
            this.setting = setting;
            Slider createdSlider = null;
            ChoicePill createdPill = null;
            Toggle createdToggle = null;
            TextField createdText = null;
            switch (setting.type()) {
                case NUMBER: {
                    final Setting<?> bound = setting;
                    Setting.Range range = setting.range();
                    createdSlider = new Slider("", range != null ? range.min() : 0.0D,
                        range != null ? range.max() : 100.0D,
                        range != null ? range.step() : 1.0D, "",
                        new Slider.DoubleReader() {
                            public double read() {
                                return bound.value() instanceof Number ? ((Number) bound.value()).doubleValue() : 0.0D;
                            }
                        },
                        new Slider.DoubleWriter() {
                            public void write(double value) {
                                setValue(bound, bound.value() instanceof Float
                                    ? Float.valueOf((float) value) : Integer.valueOf((int) Math.round(value)));
                            }
                        },
                        new java.util.function.Consumer<Slider>() {
                            public void accept(Slider s) {
                                activeSlider = s;
                            }
                        });
                    createdSlider.setVisible(true);
                    break;
                }
                case CHOICE: {
                    final Setting<?> bound = setting;
                    List<String> choices = setting.choices() == null
                        ? Collections.<String>emptyList() : setting.choices();
                    createdPill = new ChoicePill("", choices,
                        new ChoicePill.IntReader() {
                            public int read() {
                                return choices.indexOf(String.valueOf(bound.value()));
                            }
                        },
                        new ChoicePill.IntWriter() {
                            public void write(int index) {
                                if (index >= 0 && index < choices.size()) {
                                    setValue(bound, choices.get(index));
                                    saveQuietly();
                                }
                            }
                        });
                    break;
                }
                case BOOLEAN: {
                    final Setting<?> bound = setting;
                    createdToggle = new Toggle(new Toggle.StateProvider() {
                        public boolean isOn() {
                            return Boolean.TRUE.equals(bound.value());
                        }
                    }, new Runnable() {
                        public void run() {
                            setValue(bound, !Boolean.TRUE.equals(bound.value()));
                            saveQuietly();
                        }
                    });
                    createdToggle.size(46, 20);
                    break;
                }
                case TEXT: {
                    final Setting<?> bound = setting;
                    createdText = new TextField("", new java.util.function.Consumer<String>() {
                        public void accept(String value) {
                            setValue(bound, value);
                            textDirty = true;
                        }
                    });
                    createdText.setText(String.valueOf(setting.value()));
                    break;
                }
                default:
                    break;
            }
            this.slider = createdSlider;
            this.choicePill = createdPill;
            this.boolToggle = createdToggle;
            this.textEdit = createdText;
        }

        @Override
        public UiComponent at(int x, int y) {
            super.at(x, y);
            layoutControl();
            return this;
        }

        @Override
        public UiComponent size(int width, int height) {
            super.size(width, height);
            layoutControl();
            return this;
        }

        private void layoutControl() {
            int pad = 12;
            if (slider != null) {
                slider.at(x + pad, y + 24).size(Math.max(40, width - pad * 2), 24);
            } else if (choicePill != null) {
                choicePill.at(x + pad, y + 24).size(Math.max(60, width - pad * 2), 24);
            } else if (boolToggle != null) {
                boolToggle.at(x + width - pad - 46, y + 8);
            } else if (textEdit != null) {
                textEdit.at(x + pad, y + 22).size(Math.max(60, width - pad * 2), 24);
            }
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(7),
                hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
            int pad = GuiScale.w(12);
            AetherFont.draw(AetherFont.Size.SMALL, setting.label(), left + pad, top + GuiScale.h(6), AetherUi.TEXT_SECONDARY);

            if (slider != null) {
                renderChild(slider);
            } else if (choicePill != null) {
                renderChild(choicePill);
            } else if (boolToggle != null) {
                renderChild(boolToggle);
            } else if (textEdit != null) {
                renderChild(textEdit);
            } else {
                switch (setting.type()) {
                    case KEYBIND:
                        drawKeybind(left + pad, top + GuiScale.h(24), w - pad * 2);
                        break;
                    case COLOR:
                        drawColor(left + pad, top + GuiScale.h(24), w - pad * 2);
                        break;
                    default:
                        break;
                }
            }
        }

        private void drawKeybind(int x, int y, int w) {
            boolean capturing = setting.id().equals(captureKeybindSettingId);
            int key = setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0;
            String name = capturing ? "> press a key <" : Keyboard.getKeyName(key);
            AetherUi.drawRoundRect(x, y, x + w, y + GuiScale.h(22), GuiScale.h(6),
                capturing ? AetherUi.withAlpha(AetherUi.ACCENT, 0x3D) : AetherUi.ROW_HOVER);
            AetherUi.outline(x, y, x + w, y + GuiScale.h(22),
                capturing ? AetherUi.withAlpha(AetherUi.ACCENT, 0x88) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));
            AetherFont.draw(AetherFont.Size.SMALL, name, x + GuiScale.w(10),
                y + (GuiScale.h(22) - AetherFont.height(AetherFont.Size.SMALL)) / 2,
                capturing ? AetherUi.ACCENT : AetherUi.TEXT_PRIMARY);
        }

        private void drawColor(int x, int y, int w) {
            int current = setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0xFF9B8CFF;
            AetherUi.drawRoundRect(x, y, x + GuiScale.w(22), y + GuiScale.h(18), GuiScale.h(4), current | 0xFF000000);
            AetherUi.outline(x, y, x + GuiScale.w(22), y + GuiScale.h(18), AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x66));
            int[] presets = {0xFF9B8CFF, 0xFF38E0A8, 0xFF52BEEB, 0xFFFFD166, 0xFFFF7A6B, 0xFFF2F2F5, 0xFF737585, 0xFF0B0B10};
            int swatchX = x + GuiScale.w(30);
            for (int preset : presets) {
                AetherUi.drawRoundRect(swatchX, y + GuiScale.h(3), swatchX + GuiScale.h(12), y + GuiScale.h(15), GuiScale.h(3), preset);
                swatchX += GuiScale.w(16);
            }
        }

        @Override
        public void onMouseMove(double mx, double my) {
            hover = contains(mx, my);
            if (slider != null) {
                slider.onMouseMove(mx, my);
            } else if (choicePill != null) {
                choicePill.onMouseMove(mx, my);
            } else if (boolToggle != null) {
                boolToggle.onMouseMove(mx, my);
            } else if (textEdit != null) {
                textEdit.onMouseMove(mx, my);
            }
        }

        @Override
        public boolean onMouseClick(double mx, double my, int button) {
            if (button != 0 || !contains(mx, my)) {
                return false;
            }
            if (slider != null && slider.onMouseClick(mx, my, button)) {
                draggingNumber = true;
                return true;
            }
            if (choicePill != null && choicePill.onMouseClick(mx, my, button)) {
                return true;
            }
            if (boolToggle != null && boolToggle.onMouseClick(mx, my, button)) {
                return true;
            }
            if (textEdit != null && textEdit.onMouseClick(mx, my, button)) {
                return true;
            }
            switch (setting.type()) {
                case KEYBIND:
                    captureKeybindSettingId = setting.id();
                    return true;
                case COLOR: {
                    int[] presets = {0xFF9B8CFF, 0xFF38E0A8, 0xFF52BEEB, 0xFFFFD166, 0xFFFF7A6B, 0xFFF2F2F5, 0xFF737585, 0xFF0B0B10};
                    int swatchX = getX() + 12 + 30;
                    for (int preset : presets) {
                        if (mx >= swatchX && mx < swatchX + 12) {
                            setValue(setting, preset);
                            saveQuietly();
                            return true;
                        }
                        swatchX += 16;
                    }
                    return true;
                }
                default:
                    return true; // the row surface swallows the click
            }
        }

        @Override
        public void onMouseRelease(double mx, double my, int button) {
            if (slider != null) {
                slider.onMouseRelease(mx, my, button);
            } else if (choicePill != null) {
                choicePill.onMouseRelease(mx, my, button);
            } else if (boolToggle != null) {
                boolToggle.onMouseRelease(mx, my, button);
            } else if (textEdit != null) {
                textEdit.onMouseRelease(mx, my, button);
            }
        }

        @Override
        public boolean onKeyTyped(char typedChar, int keyCode) {
            if (textEdit != null) {
                return textEdit.onKeyTyped(typedChar, keyCode);
            }
            return false;
        }

        /** Drops keyboard focus from this row's text field, persisting an edit first. */
        void defocus() {
            if (textEdit != null) {
                textEdit.clickOutside();
                if (textDirty) {
                    textDirty = false;
                    saveQuietly();
                }
            }
        }
    }
}
