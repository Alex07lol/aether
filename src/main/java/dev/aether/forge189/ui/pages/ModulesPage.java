package dev.aether.forge189.ui.pages;

import java.util.List;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.module.ClientModule;
import dev.aether.module.setting.Setting;
import dev.aether.forge189.font.GlyphPageFontRenderer;

/**
 * Modules page - displays a modern card list of modules with expandable accordions.
 * <p>
 * The list scrolls: every y coordinate is the unscrolled content position plus the
 * layout's scroll offset, drawing is clipped to the list rectangle, and hit-testing
 * converts mouse coordinates back into content space with the same offset. One row
 * height function is shared by drawing, hit-testing and the layout's maxScroll
 * computation, so the three can never disagree.
 */
public final class ModulesPage extends Page {

    public static final int CARD_GAP = 10;
    public static final int ROW_H = 34;
    public static final int SETTING_ROW_H = 28;
    public static final int LIST_CONTENT_TOP = 90;

    // Filter chip state
    private String activeFilter = "!all";
    private ClientModule.ModuleCategory activeCategory = null;

    /** x, y, w, h per category pill, cached by the last render() for hit-testing. */
    private int[] pillRects = new int[0];

    /** The most recent layout, so pill wrapping can respect the list width. */
    private Layout layoutCache;

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        layoutCache = layout;
        int scroll = Math.round(layout.scroll);

        // Header area (filter chips + category pills) stays fixed; only the cards scroll.
        int headerY = layout.listY + 8;
        drawFilterChips(font, layout.listX, headerY + 24, mouseX, mouseY, screen);
        drawCategoryPills(font, layout.listX, headerY + 50, mouseX, mouseY, screen);

        // Clipped, scrolled card list.
        Mc189Compat.pushScissor(layout.listX, layout.listY, layout.listW, layout.listH);
        int y = layout.listY + LIST_CONTENT_TOP - scroll;
        for (ClientModule module : screen.getVisibleModules()) {
            int rowH = rowHeight(screen, module);
            if (y + rowH >= layout.listY && y <= layout.listY + layout.listH) {
                drawModuleCard(screen, font, module, layout, mouseX, mouseY, y);
            }
            y += rowH + CARD_GAP;
        }
        Mc189Compat.popScissor();
    }

    /** Total drawn height of one module's card, accordion rows included. */
    public static int rowHeight(AetherClickGuiScreen screen, ClientModule module) {
        int height = ROW_H + 10;
        if (screen.getNav().isExpanded(module.metadata().id())) {
            height += 8;
            for (@SuppressWarnings("unused") Setting<?> setting : module.settings()) {
                height += SETTING_ROW_H;
            }
        }
        return height;
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        // Filter chips (fixed header area). They drive the search state directly:
        // syncVisible() rebuilds nav.filterKey from the search, so writing the key
        // alone would be discarded before the next sync.
        int chipY = layout.listY + 8 + 24;
        String[] chips = {"!all", "!live", "!fav"};
        for (int i = 0; i < chips.length; i++) {
            int chipX = layout.listX + i * 80;
            if (mouseX >= chipX && mouseX <= chipX + 70 &&
                mouseY >= chipY && mouseY <= chipY + 20) {
                if ("!live".equals(chips[i])) {
                    boolean wasOn = screen.getSearch().liveOnly();
                    screen.getSearch().liveOnly(!wasOn);
                    if (!wasOn) {
                        screen.getSearch().favoritesOnly(false);
                    }
                } else if ("!fav".equals(chips[i])) {
                    boolean wasOn = screen.getSearch().favoritesOnly();
                    screen.getSearch().favoritesOnly(!wasOn);
                    if (!wasOn) {
                        screen.getSearch().liveOnly(false);
                    }
                } else {
                    screen.getSearch().liveOnly(false);
                    screen.getSearch().favoritesOnly(false);
                }
                screen.syncVisible();
                return;
            }
        }

        // Category pills (fixed header area) - rects cached by render().
        ClientModule.ModuleCategory[] categories = ClientModule.ModuleCategory.values();
        for (int i = 0; i < categories.length && i * 4 + 3 < pillRects.length; i++) {
            if (pillRects[i * 4 + 2] <= 0) continue; // wrapped out of the row
            int px = pillRects[i * 4];
            int py = pillRects[i * 4 + 1];
            int pw = pillRects[i * 4 + 2];
            if (mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + 18) {
                screen.getNav().filterKey("cat:" + categories[i].name());
                screen.getSearch().category(categories[i]);
                screen.syncVisible();
                return;
            }
        }

        // Module cards: convert mouse y into content space, then hit-test unscrolled rows.
        int contentMouseY = mouseY + Math.round(layout.scroll);
        List<ClientModule> visible = screen.getVisibleModules();
        int y = layout.listY + LIST_CONTENT_TOP;
        for (ClientModule module : visible) {
            int rowH = rowHeight(screen, module);
            if (contentMouseY >= y && contentMouseY < y + rowH) {
                if (mouseY >= layout.listY && mouseY <= layout.listY + layout.listH) {
                    int cardX = layout.listX + 8;
                    int cardW = layout.listW - 16;
                    int toggleX = cardX + cardW - 44;
                    int toggleY = y + 6;
                    if (mouseX >= toggleX && mouseX <= toggleX + 26 &&
                        contentMouseY >= toggleY && contentMouseY <= toggleY + 13) {
                        screen.toggleModule(module);
                        return;
                    }

                    int chevronX = cardX + cardW - 20;
                    if (mouseX >= chevronX - 5 && mouseX <= chevronX + 5 &&
                        contentMouseY >= y + 5 && contentMouseY <= y + 15) {
                        screen.getNav().toggleExpanded(module.metadata().id());
                        return;
                    }

                    if (screen.getNav().isExpanded(module.metadata().id())) {
                        int accordionTop = y + ROW_H;
                        int rowY = accordionTop + 8;
                        for (Setting<?> setting : module.settings()) {
                            int settingY = rowY;
                            int settingX = cardX + 8;
                            int settingW = cardW - 16;
                            if (contentMouseY >= settingY && contentMouseY < settingY + SETTING_ROW_H) {
                                handleSettingClick(screen, module, setting, settingX, settingY, settingW, mouseX, button);
                                return;
                            }
                            rowY += SETTING_ROW_H;
                        }
                    }
                }
                return;
            }
            y += rowH + CARD_GAP;
        }
    }

    private void drawModuleCard(AetherClickGuiScreen screen, Object font, ClientModule module,
                                Layout layout, int mouseX, int mouseY, int y) {
        int cardX = layout.listX + 8;
        int cardW = layout.listW - 16;
        int cardH = ROW_H + 10;
        boolean expanded = screen.getNav().isExpanded(module.metadata().id());

        // Hover effect with a subtle accent blend
        boolean hover = mouseX >= cardX && mouseX <= cardX + cardW &&
                        mouseY >= y && mouseY <= y + cardH;
        int baseBg = expanded ? AetherUi.CARD_HOVER : AetherUi.CARD;
        int bg = hover ? AetherUi.lerpColor(baseBg, AetherUi.ACCENT, 0.04f) : baseBg;
        AetherUi.drawRoundRect(cardX, y, cardX + cardW, y + cardH, 8, bg);
        AetherUi.outline(cardX, y, cardX + cardW, y + cardH,
            AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));

        // Module name
        AetherUi.textSmooth(font, module.metadata().name(), cardX + 16, y + 8,
            module.state() == ClientModule.ModuleState.ENABLED
                ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);

        // Category badge
        String tag = label(module.metadata().category());
        int tagW = Mc189Compat.stringWidth(font, tag) + 10;
        int tagX = cardX + cardW - tagW - 56;
        AetherUi.drawRoundRect(tagX, y + 7, tagX + tagW, y + 20, 4,
            AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));
        AetherUi.textSmooth(font, tag, tagX + 5, y + 9, AetherUi.TEXT_SECONDARY);

        // Toggle pill
        boolean on = module.state() == ClientModule.ModuleState.ENABLED;
        int toggleX = cardX + cardW - 44;
        int toggleY = y + 8;
        AetherUi.drawRoundRect(toggleX, toggleY, toggleX + 26, toggleY + 13, 6,
            on ? AetherUi.ACCENT_ON : AetherUi.TOGGLE_BG);
        int knob = on ? toggleX + 26 - 11 : toggleX + 2;
        AetherUi.drawRoundRect(knob, toggleY + 2, knob + 9, toggleY + 11, 4,
            on ? 0xFF0B1210 : 0xFFE8E9F0);

        // Chevron
        int chevronX = cardX + cardW - 20;
        int chevronY = y + ROW_H / 2 + 5;
        int chevronColor = AetherUi.TEXT_SECONDARY;
        if (expanded) {
            Mc189Compat.drawRect(chevronX - 4, chevronY - 1, chevronX + 4, chevronY, chevronColor);
            Mc189Compat.drawRect(chevronX - 3, chevronY - 2, chevronX - 2, chevronY - 1, chevronColor);
            Mc189Compat.drawRect(chevronX + 2, chevronY - 2, chevronX + 3, chevronY - 1, chevronColor);
        } else {
            Mc189Compat.drawRect(chevronX - 3, chevronY - 2, chevronX + 3, chevronY - 1, chevronColor);
            Mc189Compat.drawRect(chevronX - 2, chevronY - 1, chevronX - 1, chevronY + 1, chevronColor);
            Mc189Compat.drawRect(chevronX + 1, chevronY - 1, chevronX + 2, chevronY + 1, chevronColor);
        }

        // Accordion settings
        if (expanded) {
            int accordionTop = y + ROW_H;
            int bodyH = rowHeight(screen, module) - ROW_H - 10;
            AetherUi.drawRoundRect(cardX + 2, accordionTop, cardX + cardW - 2,
                accordionTop + bodyH, 6, AetherUi.withAlpha(AetherUi.PANEL, 0x77));
            int rowY = accordionTop + 8;
            for (Setting<?> setting : module.settings()) {
                drawSettingRow(font, setting, cardX, rowY, cardW);
                rowY += SETTING_ROW_H;
            }
        }
    }

    private void drawSettingRow(Object font, Setting<?> setting, int cardX, int rowY, int cardW) {
        AetherUi.textSmooth(font, setting.label(), cardX + 16, rowY + 2, AetherUi.TEXT_SECONDARY);

        int trackX = cardX + 16;
        int trackY = rowY + 15;
        int trackW = cardW - 32 - 40;

        switch (setting.type()) {
            case BOOLEAN: {
                boolean on = Boolean.TRUE.equals(setting.value());
                AetherUi.drawRoundRect(cardX + cardW - 40, rowY + 2, cardX + cardW - 16, rowY + 15, 6,
                    on ? AetherUi.ACCENT_ON : AetherUi.TOGGLE_BG);
                int knob = on ? cardX + cardW - 40 + 18 : cardX + cardW - 40 + 2;
                AetherUi.drawRoundRect(knob, rowY + 4, knob + 9, rowY + 13, 4,
                    on ? 0xFF0B1210 : 0xFFE8E9F0);
                break;
            }
            case NUMBER: {
                float fraction = 0f;
                dev.aether.module.setting.Setting.Range range = setting.range();
                if (range != null && setting.value() instanceof Number) {
                    float min = range.min();
                    float max = range.max();
                    float v = ((Number) setting.value()).floatValue();
                    if (max > min) {
                        fraction = Math.max(0f, Math.min(1f, (v - min) / (max - min)));
                    }
                }
                AetherUi.drawRoundRect(trackX, trackY, trackX + trackW, trackY + 4, 2, AetherUi.TRACK);
                AetherUi.drawRoundRect(trackX, trackY, trackX + (int) (trackW * fraction), trackY + 4, 2,
                    AetherUi.ACCENT);
                String text = setting.value() + (range != null
                    ? "  [" + range.min() + "-" + range.max() + "]" : "");
                AetherUi.textSmooth(font, text, trackX + trackW + 8, rowY + 11, AetherUi.TEXT_PRIMARY);
                break;
            }
            case CHOICE: {
                AetherUi.textSmooth(font, String.valueOf(setting.value()),
                    cardX + cardW - 16 - Mc189Compat.stringWidth(font, String.valueOf(setting.value())),
                    rowY + 2, AetherUi.TEXT_PRIMARY);
                break;
            }
            default:
                break;
        }
    }

    private void drawFilterChips(Object font, int x, int y, int mouseX, int mouseY, AetherClickGuiScreen screen) {
        String[] chips = {"All", "Live", "Favorites"};
        String[] chipKeys = {"!all", "!live", "!fav"};
        for (int i = 0; i < chips.length; i++) {
            boolean active = chipKeys[i].equals(activeFilter);
            boolean hover = mouseX >= x + i * 80 && mouseX <= x + i * 80 + 70 &&
                            mouseY >= y && mouseY <= y + 20;
            int chipX = x + i * 80;
            int bg = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            AetherUi.drawRoundRect(chipX, y, chipX + 70, y + 20, 6, bg);
            AetherUi.textSmooth(font, chips[i], chipX + 8, y + 5,
                active ? 0xFF0B0B10 : AetherUi.TEXT_SECONDARY);
        }
    }

    private void drawCategoryPills(Object font, int x, int y, int mouseX, int mouseY, AetherClickGuiScreen screen) {
        ClientModule.ModuleCategory[] categories = ClientModule.ModuleCategory.values();
        pillRects = new int[categories.length * 4];
        int px = x;
        int py = y;
        int maxX = layoutCache != null ? layoutCache.listX + layoutCache.listW : x + 600;
        for (int i = 0; i < categories.length; i++) {
            ClientModule.ModuleCategory category = categories[i];
            String name = label(category);
            int w = Mc189Compat.stringWidth(font, name) + 14;
            if (px + w > maxX) { // wrap to the next pill row
                px = x;
                py += 22;
            }
            boolean active = ("cat:" + category.name()).equals(activeFilter);
            boolean hover = mouseX >= px && mouseX <= px + w &&
                            mouseY >= py && mouseY <= py + 18;
            int bg = active ? AetherUi.ACCENT : hover
                ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x22);
            AetherUi.drawRoundRect(px, py, px + w, py + 18, 6, bg);
            AetherUi.textSmooth(font, name, px + 7, py + 4,
                active ? 0xFF0B0B10 : AetherUi.TEXT_SECONDARY);
            pillRects[i * 4] = px;
            pillRects[i * 4 + 1] = py;
            pillRects[i * 4 + 2] = w;
            pillRects[i * 4 + 3] = 18;
            px += w + 6;
        }
    }

    private void handleSettingClick(AetherClickGuiScreen screen, ClientModule module,
                                    Setting<?> setting,
                                    int settingX, int settingY, int settingW,
                                    int mouseX, int button) {
        switch (setting.type()) {
            case BOOLEAN: {
                @SuppressWarnings("unchecked")
                Setting<Boolean> boolSetting = (Setting<Boolean>) setting;
                boolSetting.setValue(!Boolean.TRUE.equals(setting.value()));
                break;
            }
            case NUMBER: {
                dev.aether.module.setting.Setting.Range range = setting.range();
                if (range == null) {
                    break;
                }
                float min = range.min();
                float max = range.max();
                int trackX = settingX + 16;
                int trackW = settingW - 32 - 40;
                float fraction = (mouseX - trackX) / (float) Math.max(1, trackW);
                fraction = Math.max(0f, Math.min(1f, fraction));
                float value = min + fraction * (max - min);
                @SuppressWarnings("unchecked")
                Setting<Number> numberSetting = (Setting<Number>) setting;
                if (setting.value() instanceof Integer
                        || (setting.value() instanceof Float && value == Math.round(value))) {
                    numberSetting.setValue(Integer.valueOf(Math.round(value)));
                } else {
                    numberSetting.setValue(Float.valueOf(value));
                }
                break;
            }
            default:
                break;
        }
    }

    private static String label(ClientModule.ModuleCategory category) {
        switch (category) {
            case GENERAL: return "General";
            case PERFORMANCE: return "Perf";
            case GRAPHICS: return "Graphics";
            case RENDER: return "Render";
            case INTERFACE: return "Interface";
            case MOVEMENT: return "Movement";
            case AUDIO: return "Audio";
            case HUD: return "HUD";
            case PVP: return "PvP";
            case COSMETICS: return "Cosmetics";
            case ACCESSIBILITY: return "Access";
            case THEMES: return "Themes";
            default: return category.name();
        }
    }
}
