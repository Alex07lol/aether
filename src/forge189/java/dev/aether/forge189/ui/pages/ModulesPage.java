package dev.aether.forge189.ui.pages;

import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherToggle;
import dev.aether.forge189.ui.AetherMetrics;
import dev.aether.ui.ControlCenterState;
import java.util.List;

/**
 * Modules page – displays a modern card list of modules with expandable accordions.
 */
public final class ModulesPage extends Page {

    private static final int CARD_PADDING = 12;
    private static final int CARD_GAP = 10;
    private static final int ROW_H = 34;

    // Filter chip state
    private String activeFilter = "!all";
    private ModuleCategory activeCategory = null;

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        // Draw header area
        int headerY = layout.listY + 8;
        int searchX = layout.listX + layout.listW - 220;

        // Sync filter state from nav
        String filterKey = screen.getNav().filterKey();
        if (filterKey.startsWith("cat:")) {
            try {
                activeCategory = ModuleCategory.valueOf(filterKey.substring(4));
                activeFilter = filterKey;
            } catch (IllegalArgumentException ignored) {
                activeCategory = null;
                activeFilter = "!all";
            }
        } else {
            activeCategory = null;
            activeFilter = filterKey;
        }

        // Filter chips (All / Live / Favorites)
        drawFilterChips(font, layout.listX, headerY + 24, mouseX, mouseY, screen);

        // Category pills
        drawCategoryPills(font, layout.listX, headerY + 50, mouseX, mouseY, screen);

        // Module cards
        List<ClientModule> visible = screen.getVisibleModules();
        int y = layout.listY + 90;
        for (ClientModule module : visible) {
            drawModuleCard(screen, font, module, layout, mouseX, mouseY, y);
            y += ROW_H + CARD_GAP;
        }
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        // Handle filter chip clicks
        int chipY = layout.listY + 8 + 24;
        String[] chips = {"!all", "!live", "!fav"};
        String[] chipLabels = {"All", "Live", "Favorites"};
        for (int i = 0; i < chips.length; i++) {
            int chipX = layout.listX + i * 80;
            if (mouseX >= chipX && mouseX <= chipX + 70 &&
                mouseY >= chipY && mouseY <= chipY + 20) {
                screen.getNav().filterKey(chips[i]);
                screen.syncVisible();
                return;
            }
        }

        // Handle category pill clicks
        int catY = layout.listY + 8 + 50;
        for (ModuleCategory category : ModuleCategory.values()) {
            if (mouseX >= layout.listX && mouseX <= layout.listX + 100 &&
                mouseY >= catY && mouseY <= catY + 20) {
                screen.getNav().filterKey("cat:" + category.name());
                screen.getSearch().category(category);
                screen.syncVisible();
                return;
            }
            catY += 24;
        }

        // Handle clicks on module toggle buttons
        List<ClientModule> visible = screen.getVisibleModules();
        int y = layout.listY + 90;
        for (ClientModule module : visible) {
            int cardX = layout.listX + 8;
            int cardW = layout.listW - 16;
            int toggleX = cardX + cardW - 44;
            int toggleY = y + 6;
            if (mouseX >= toggleX && mouseX <= toggleX + 26 &&
                mouseY >= toggleY && mouseY <= toggleY + 13) {
                screen.toggleModule(module);
                return;
            }

            // Chevron click to expand/collapse
            int chevronX = cardX + cardW - 20;
            if (mouseX >= chevronX - 5 && mouseX <= chevronX + 5 &&
                mouseY >= y + 5 && mouseY <= y + 15) {
                screen.getNav().toggleExpanded(module.metadata().id());
                return;
            }

            // If expanded, hit-test the accordion settings
            if (screen.getNav().isExpanded(module.metadata().id())) {
                int accordionTop = y + ROW_H;
                int rowY = accordionTop + 8;
                for (dev.aether.module.setting.Setting<?> setting : module.settings()) {
                    int settingY = rowY;
                    int settingX = cardX + 8;
                    int settingW = cardW - 16;
                    if (mouseX >= settingX && mouseX <= settingX + settingW &&
                        mouseY >= settingY && mouseY <= settingY + 22) {
                        handleSettingClick(screen, module, setting, settingX, settingY, settingW, mouseX, mouseY, button);
                        return;
                    }
                    rowY += 28;
                }
            }

            y += ROW_H + CARD_GAP;
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
            AetherUi.drawRoundRect(chipX, y, chipX + 70, y + 20, 4, bg);
            AetherUi.text(font, chips[i], chipX + 10, y + 6,
                         active ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
        }
    }

    private void drawCategoryPills(Object font, int x, int y, int mouseX, int mouseY, AetherClickGuiScreen screen) {
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean active = category == activeCategory;
            boolean hover = mouseX >= x && mouseX <= x + 100 &&
                            mouseY >= y && mouseY <= y + 20;
            int pillW = 100;
            int bg = active ? AetherUi.ACCENT : hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
            AetherUi.drawRoundRect(x, y, x + pillW, y + 20, 4, bg);
            AetherUi.text(font, category.name(), x + 10, y + 6,
                         active ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);
            y += 24;
        }
    }

    private void drawModuleCard(AetherClickGuiScreen screen, Object font, ClientModule module,
                                Layout layout, int mouseX, int mouseY, int y) {
        int cardX = layout.listX + 8;
        int cardW = layout.listW - 16;
        int cardH = ROW_H + 10;
        boolean expanded = screen.getNav().isExpanded(module.metadata().id());

        // Hover effect with subtle transparency animation
        boolean hover = mouseX >= cardX && mouseX <= cardX + cardW &&
                        mouseY >= y && mouseY <= y + cardH;
        int baseBg = expanded ? AetherUi.CARD_HOVER : AetherUi.CARD;
        int bg = hover ? AetherUi.lerpColor(baseBg, AetherUi.ACCENT, 0.04f) : baseBg;
        Mc189Compat.drawRect(cardX, y, cardX + cardW, y + cardH, bg);

        // Module name
        AetherUi.text(font, module.metadata().name(),
                       cardX + 16, y + 8,
                       module.state() == ClientModule.ModuleState.ENABLED
                           ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);

        // Category badge
        String tag = label(module.metadata().category());
        int tagW = Mc189Compat.stringWidth(font, tag) + 10;
        int tagX = cardX + cardW - tagW - 8;
        if (tagX > cardX + 16) {
            Mc189Compat.drawRect(tagX, y + 8, tagX + tagW, y + 20,
                                 AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
            AetherUi.text(font, tag, tagX + 6, y + 12, AetherUi.TEXT_DISABLED);
        }

        // Toggle button (right side)
        int toggleX = cardX + cardW - 44;
        int toggleY = y + 6;
        boolean on = module.state() == ClientModule.ModuleState.ENABLED;
        boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + 26 &&
                              mouseY >= toggleY && mouseY <= toggleY + 13;
        new AetherToggle(toggleX, toggleY, 26, 13, on).render(toggleHover);

        // Chevron for expand/collapse
        if (expanded) {
            drawChevron(cardX + cardW - 20, y + 10, 1, AetherUi.TEXT_DISABLED);
        } else {
            drawChevron(cardX + cardW - 20, y + 10, -1, AetherUi.TEXT_DISABLED);
        }

        // If expanded, draw settings
        if (expanded) {
            drawAccordion(font, module, cardX, y, cardW, layout, mouseX, mouseY);
        }
    }

    private void drawAccordion(Object font, ClientModule module,
                               int cardX, int y, int cardW, Layout layout,
                               int mouseX, int mouseY) {
        int accordionTop = y + ROW_H;
        // Subtle separator line
        Mc189Compat.drawRect(cardX + 2, accordionTop - 1,
                             cardX + cardW - 2, accordionTop, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x18));
        int rowY = accordionTop + 8;
        for (dev.aether.module.setting.Setting<?> setting : module.settings()) {
            drawSettingRow(font, setting, cardX + 8, rowY, cardW - 16, mouseX, mouseY);
            rowY += 28;
        }
    }

    private void drawSettingRow(Object font, dev.aether.module.setting.Setting<?> setting,
                                int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w &&
                        mouseY >= y && mouseY <= y + 22;
        if (hover) {
            Mc189Compat.drawRect(x, y, x + w, y + 22, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x10));
        }
        AetherUi.text(font, setting.label(), x + 8, y + 6,
                     hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        switch (setting.type()) {
            case BOOLEAN:
                boolean bool = Boolean.TRUE.equals(setting.value());
                new AetherToggle(x + w - 38, y + 3, 26, 13, bool).render(false);
                break;
            case NUMBER:
                int val = (Integer) setting.value();
                int min = 0;
                int max = 100;
                if (setting.hasRange()) {
                    dev.aether.module.setting.Setting.Range range = setting.range();
                    min = range.min();
                    max = range.max();
                }
                Mc189Compat.drawRect(x + w - 80, y + 4, x + w - 20, y + 18, AetherUi.TRACK);
                int fill = (val - min) * 60 / Math.max(1, max - min);
                if (fill > 0) {
                    Mc189Compat.drawRect(x + w - 80, y + 4, x + w - 80 + fill, y + 18, AetherUi.ACCENT);
                }
                AetherUi.text(font, String.valueOf(val), x + w - 60, y + 6, AetherUi.TEXT_PRIMARY);
                break;
            case COLOR:
                int color = (Integer) setting.value();
                Mc189Compat.drawRect(x + w - 38, y + 3, x + w - 6, y + 17, color);
                Mc189Compat.drawRect(x + w - 38, y + 3, x + w - 6, y + 17, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x66));
                break;
            case KEYBIND:
                int keyCode = (Integer) setting.value();
                String keyName = keyCodeToName(keyCode);
                Mc189Compat.drawRect(x + w - 90, y + 3, x + w - 8, y + 17, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
                AetherUi.text(font, keyName, x + w - 85, y + 6, AetherUi.TEXT_PRIMARY);
                break;
            case TEXT:
                String text = setting.value().toString();
                AetherUi.text(font, text, x + w - 80, y + 6, AetherUi.TEXT_PRIMARY);
                break;
            case CHOICE:
                String choice = String.valueOf(setting.value());
                Mc189Compat.drawRect(x + w - 80, y + 3, x + w - 8, y + 17, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x1E));
                AetherUi.text(font, choice, x + w - 70, y + 8, AetherUi.TEXT_PRIMARY);
                break;
        }
    }

    private void handleSettingClick(AetherClickGuiScreen screen, ClientModule module,
                                    dev.aether.module.setting.Setting<?> setting,
                                    int settingX, int settingY, int settingW,
                                    int mouseX, int mouseY, int button) {
        switch (setting.type()) {
            case BOOLEAN: {
                boolean newVal = !Boolean.TRUE.equals(setting.value());
                setValue(setting, newVal);
                screen.writeLastChange("Setting '" + setting.label() + "' -> " + newVal);
                break;
            }
            case NUMBER: {
                int min = 0;
                int max = 100;
                if (setting.hasRange()) {
                    dev.aether.module.setting.Setting.Range range = setting.range();
                    min = range.min();
                    max = range.max();
                }
                int trackLeft = settingX + settingW - 80;
                int trackRight = trackLeft + 60;
                if (mouseX >= trackLeft && mouseX <= trackRight) {
                    int ratio = (mouseX - trackLeft) * 100 / 60;
                    int val = min + (max - min) * ratio / 100;
                    val = Math.max(min, Math.min(max, val));
                    setValue(setting, val);
                    screen.writeLastChange("Setting '" + setting.label() + "' -> " + val);
                }
                break;
            }
            case COLOR:
                screen.getNav().openPalette(setting, settingX + settingW / 2, settingY + 11);
                break;
            case KEYBIND:
                screen.getNav().focusKeybind(setting);
                break;
            case CHOICE: {
                List<String> choices = setting.choices();
                Object current = setting.value();
                int idx = choices.indexOf(String.valueOf(current));
                Object next = choices.get((idx + 1) % choices.size());
                setValue(setting, next);
                screen.writeLastChange("Setting '" + setting.label() + "' -> " + next);
                break;
            }
            case TEXT:
                screen.getNav().focusText(setting);
                break;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(dev.aether.module.setting.Setting<?> setting, Object value) {
        ((dev.aether.module.setting.Setting) setting).setValue(value);
    }

    private void drawChevron(int x, int y, int direction, int colour) {
        for (int i = 0; i < 5; i++) {
            int dx = direction > 0 ? i : -i;
            Mc189Compat.drawRect(x + dx, y + i, x + dx + 1, y + i + 1, colour);
            Mc189Compat.drawRect(x + dx, y - i, x + dx + 1, y - i + 1, colour);
        }
    }

    private String keyCodeToName(int keyCode) {
        switch (keyCode) {
            case 0: return "None";
            case 1: return "ESC";
            case 14: return "BACKSPACE";
            case 15: return "TAB";
            case 28: return "ENTER";
            case 29: return "LCTRL";
            case 42: return "LSHIFT";
            case 54: return "RSHIFT";
            case 56: return "LALT";
            case 57: return "SPACE";
            case 200: return "UP";
            case 203: return "LEFT";
            case 205: return "RIGHT";
            case 208: return "DOWN";
            default:
                if (keyCode >= 2 && keyCode <= 11) {
                    // 1-9, 0
                    return String.valueOf((keyCode - 1) % 10);
                }
                if (keyCode >= 16 && keyCode <= 25) {
                    // Q-P
                    return String.valueOf((char)('Q' + keyCode - 16));
                }
                if (keyCode >= 30 && keyCode <= 38) {
                    // A-L
                    return String.valueOf((char)('A' + keyCode - 30));
                }
                if (keyCode >= 44 && keyCode <= 50) {
                    // Z-M
                    return String.valueOf((char)('Z' + keyCode - 44));
                }
                if (keyCode >= 59 && keyCode <= 68) {
                    // F1-F10
                    return "F" + (keyCode - 58);
                }
                return "Key:" + keyCode;
        }
    }

    private static String label(ModuleCategory category) {
        switch (category) {
            case GENERAL:   return "General";
            case PERFORMANCE: return "Performance";
            case GRAPHICS:  return "Graphics";
            case RENDER:    return "Render";
            case INTERFACE: return "Interface";
            case MOVEMENT:  return "Movement";
            case AUDIO:     return "Audio";
            case HUD:       return "HUD";
            case PVP:       return "PvP";
            case COSMETICS: return "Cosmetics";
            case ACCESSIBILITY: return "Accessibility";
            case THEMES:    return "Themes";
            default:        return category.name();
        }
    }
}