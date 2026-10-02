package dev.aether.menu.impl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.menu.comp.UiCombo;
import dev.aether.menu.comp.UiKeybindBox;
import dev.aether.menu.comp.UiSearchBox;
import dev.aether.menu.comp.UiSlider;
import dev.aether.menu.comp.UiTextBox;
import dev.aether.menu.comp.UiToggle;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;
import dev.aether.ui.UiMotion;

/**
 * The Mods category: a chip row of the client's real module categories, a search
 * field, and a compact 40px row per module - the toggle is the 28x28 chip at the
 * row's left, which fills with the accent when the module is enabled; the gear at the
 * right opens the module's settings as a detail scene that slides in over the list.
 * <p>
 * Filtering, ranking and caching stay in the tested {@code ModuleSearch}; this class
 * lays the results out and animates them. Values are written straight to the module
 * registry and its {@code Setting}s and persisted through the client's save path.
 */
public final class ModsCategory extends MenuCategory {

    private static final float ROW_STEP = 50.0F;
    private static final float ROW_TOP = 36.0F;
    private static final float COL_PITCH = 194.0F;
    private static final float SETTING_STEP = 29.0F;

    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private final dev.aether.ui.ModuleSearch search = new dev.aether.ui.ModuleSearch();
    private final UiSearchBox searchBox = new UiSearchBox(new Runnable() {
        public void run() {
            refresh();
        }
    });

    private ModuleCategory category;
    /** The module whose settings scene is open, or null while the list shows. */
    private ClientModule openModule;
    private float sceneSlide;
    private boolean slideBack;

    /** Per-row enable animation state, keyed by module id. */
    private final java.util.Map<String, Float> enableAnim = new java.util.HashMap<String, Float>();

    // Settings-scene control state
    private String captureSettingId;
    private String openPickerId;
    private final UiToggle toggleControl = new UiToggle();
    private float sliderDragX;
    private float sliderDragW;
    private UiSlider draggingSlider;

    public ModsCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
        refresh();
    }

    @Override
    public String title() {
        return openModule != null ? openModule.metadata().name() : "Modules";
    }

    @Override
    public boolean hasSearch() {
        return openModule == null;
    }

    @Override
    public void onShow() {
        super.onShow();
        openModule = null;
        sceneSlide = 0.0F;
        slideBack = false;
        endCapture();
        refresh();
    }

    @Override
    public void update() {
        super.update();
        searchBox.update();
        // Row enable fades
        for (ClientModule module : visible) {
            String id = module.metadata().id();
            Float current = enableAnim.get(id);
            float target = module.state() == ModuleState.ENABLED ? 1.0F : 0.0F;
            float next = current == null ? target : current + (target - current) * 0.25F;
            enableAnim.put(id, Float.valueOf(next));
        }
        if (openModule != null) {
            float target = slideBack ? 0.0F : 1.0F;
            sceneSlide += (target - sceneSlide) * Math.min(1.0F, 0.22F);
            if (slideBack && sceneSlide < 0.01F) {
                openModule = null;
                slideBack = false;
            }
        }
        if (draggingSlider != null) {
            draggingSlider.drag(sliderDragX, sliderDragW, screen.mouseX(), screen.mouseDown());
        }
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private void refresh() {
        search.source(client.modules().userVisible());
        search.query(searchBox.text());
        search.category(category);
        visible.clear();
        visible.addAll(search.results());
        clampScroll();
    }

    private void setContentHeight() {
        float height = ROW_TOP + visible.size() * ROW_STEP + 10.0F;
        setContentHeight(height);
    }

    /* ── drawing ────────────────────────────────────────────────────────── */

    @Override
    public void draw(double mx, double my) {
        if (openModule != null && sceneSlide > 0.001F) {
            drawSettingsScene(mx, my);
        }
        if (openModule == null || sceneSlide < 0.999F) {
            drawList(mx, my);
        }
    }

    private void drawList(double mx, double my) {
        setContentHeight();
        float x = innerX();

        // Category chips: 16 tall, radius 6, advance = text width + 28.
        List<String> chipLabels = chipLabels();
        float chipX = x;
        float chipY = contentY() + 13.0F - scroll;
        for (int i = 0; i < chipLabels.size(); i++) {
            String label = chipLabels.get(i);
            float chipW = AetherFont.width(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, label) + 28.0F;
            boolean activeChip = isChipActive(i);
            boolean hot = !activeChip && mx >= chipX && mx < chipX + chipW && my >= chipY && my < chipY + 16.0F;
            if (activeChip) {
                UiCanvas.gradientRoundRect(chipX, chipY, chipW, 16.0F, 6.0F,
                    UiTheme.withAlpha(UiTheme.accent(), 235), UiTheme.withAlpha(UiTheme.accentDeep(), 235));
            } else {
                UiCanvas.roundRect(chipX, chipY, chipW, 16.0F, 6.0F,
                    UiTheme.withAlpha(UiTheme.card(), hot ? 255 : 220));
            }
            AetherFont.drawCentered(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, label,
                chipX, chipY + (16.0F - AetherFont.height(9.0F)) / 2.0F, chipW,
                activeChip ? UiTheme.readableOn(UiTheme.accent()) : hot ? UiTheme.text() : UiTheme.textSoft());
            chipX += chipW + 6.0F;
        }

        // Rows, clipped and scrolled, with edge fades.
        clipContent();
        try {
            float rowY = contentY() + ROW_TOP - scroll;
            float rowW = innerW();
            for (int i = 0; i < visible.size(); i++) {
                ClientModule module = visible.get(i);
                if (rowY > contentY() + contentH() || rowY + 40.0F < contentY()) {
                    rowY += ROW_STEP;
                    continue;
                }
                drawModuleRow(module, x + 15.0F, rowY, rowW, mx, my);
                rowY += ROW_STEP;
            }
        } finally {
            UiCanvas.clearScissor();
        }
        drawEdgeFades();
    }

    private void drawModuleRow(ClientModule module, float x, float y, float w, double mx, double my) {
        boolean enabled = module.state() == ModuleState.ENABLED;
        Float animValue = enableAnim.get(module.metadata().id());
        float on = animValue == null ? (enabled ? 1.0F : 0.0F) : animValue.floatValue();
        boolean hot = mx >= x && mx < x + (w - 60.0F) && my >= y && my < y + 40.0F;

        UiCanvas.roundRect(x, y, w, 40.0F, 8.0F,
            hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 235));

        // The toggle is the icon chip at the row's left; the accent fill scales in
        // from its centre when the module is enabled.
        float chipX = x + 6.0F;
        float chipY = y + 6.0F;
        float chip = 28.0F;
        UiCanvas.roundRect(chipX, chipY, chip, chip, 6.0F, UiTheme.withAlpha(UiTheme.cardHover(), 255));
        if (on > 0.01F) {
            float fillSize = chip * (0.35F + 0.65F * on) * (hot ? 1.0F : 0.96F);
            UiCanvas.gradientRoundRect(chipX + (chip - fillSize) / 2.0F, chipY + (chip - fillSize) / 2.0F,
                fillSize, fillSize, 6.0F * (0.35F + 0.65F * on),
                UiTheme.withAlpha(UiTheme.accent(), Math.round(235 * on)),
                UiTheme.withAlpha(UiTheme.accentDeep(), Math.round(235 * on)));
        }
        if (enabled) {
            float checkSize = 12.0F;
            AetherFont.drawIcon(UiIcon.CHECK, checkSize,
                chipX + (chip - AetherFont.iconWidth(UiIcon.CHECK, checkSize)) / 2.0F,
                chipY + (chip - AetherFont.height(checkSize)) / 2.0F,
                UiTheme.withAlpha(UiTheme.readableOn(UiTheme.accent()), Math.round(255 * on)));
        } else {
            String initial = module.metadata().name().substring(0, 1).toUpperCase(java.util.Locale.ENGLISH);
            AetherFont.drawCentered(10.0F, initial, chipX, chipY + (chip - AetherFont.height(10.0F)) / 2.0F,
                chip, UiTheme.textFaint());
        }

        // Name + inline description.
        String name = AetherFont.trim(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
            module.metadata().name(), 150);
        AetherFont.draw(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, name,
            x + 41.0F, y + (13.0F - AetherFont.height(13.0F)) / 2.0F + 1.0F,
            enabled ? UiTheme.text() : UiTheme.textSoft());
        String description = module.metadata().description();
        if (!description.isEmpty()) {
            float descX = x + 41.0F + AetherFont.width(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, name) + 6.0F;
            AetherFont.draw(9.0F, AetherFont.trim(9.0F, description, Math.max(40.0F, x + w - 60.0F - descX)),
                descX, y + 17.0F - 2.0F, UiTheme.textFaint());
        }

        // Gear on the right (only when settings exist).
        if (!module.settings().isEmpty()) {
            boolean gearHot = mx >= x + w - 44.0F && mx < x + w - 22.0F && my >= y + 9.0F && my < y + 31.0F;
            AetherFont.drawIcon(UiIcon.GEAR, 13.0F,
                x + w - 39.0F, y + 13.5F - 1.0F, gearHot ? UiTheme.text() : UiTheme.textSoft());
        }
    }

    private void drawEdgeFades() {
        float fx = contentX();
        float fw = contentW();
        int sheet = UiTheme.sheet();
        // 12px fades top and bottom as six stepped bands - the reveal cue of the
        // reference, drawn without any gradient geometry that could mis-blend.
        for (int i = 0; i < 6; i++) {
            int alpha = Math.round(255 * (1.0F - (i + 0.5F) / 6.0F));
            int band = UiTheme.withAlpha(sheet, alpha);
            UiCanvas.roundRect(fx, contentY() + i * 2.0F, fw, 2.0F, 0.0F, band);
            UiCanvas.roundRect(fx, contentY() + contentH() - (i + 1) * 2.0F, fw, 2.0F, 0.0F, band);
        }
    }

    /* ── the settings detail scene ──────────────────────────────────────── */

    private void drawSettingsScene(double mx, double my) {
        float slide = sceneSlide * sceneSlide * (3.0F - 2.0F * sceneSlide); // smoothstep
        float panelX = innerX() + (1.0F - slide) * 80.0F;
        float panelY = contentY() + 15.0F;
        float panelW = innerW();
        float panelH = contentH() - 30.0F;

        UiCanvas.roundRect(panelX, panelY, panelW, panelH, 10.0F, UiTheme.withAlpha(UiTheme.card(), 250));

        // Header: back chevron, module name, reset.
        float backX = panelX + 10.0F;
        float backY = panelY + 8.0F;
        boolean backHot = mx >= backX - 3.0F && mx < backX + 16.0F && my >= backY - 3.0F && my < backY + 16.0F;
        AetherFont.drawIcon(UiIcon.CHEVRON_LEFT, 13.0F, backX, backY, backHot ? UiTheme.text() : UiTheme.textSoft());

        String description = openModule.metadata().description();
        if (!description.isEmpty()) {
            AetherFont.draw(7.5F, AetherFont.trim(7.5F, description, panelW - 90.0F),
                panelX + 26.0F + AetherFont.width(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
                    AetherFont.trim(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, openModule.metadata().name(), 150)) + 8.0F,
                panelY + 13.0F, UiTheme.textFaint());
        }
        boolean resetHot = mx >= panelX + panelW - 39.0F && mx < panelX + panelW - 20.0F
            && my >= panelY + 8.0F && my < panelY + 26.0F;
        AetherFont.drawIcon(UiIcon.REFRESH, 13.0F, panelX + panelW - 39.0F, panelY + 10.0F,
            resetHot ? UiTheme.text() : UiTheme.textSoft());

        // Settings grid: two columns of 194, rows of 29, from y+44, scrolled.
        List<Setting<?>> settings = openModule.settings();
        float gridTop = panelY + 44.0F;
        int rowsPerCol = Math.max(1, (int) ((panelH - 50.0F) / SETTING_STEP));
        int perCol = Math.max(rowsPerCol, (settings.size() + 1) / 2);
        float contentH = ((settings.size() + 1) / 2) * SETTING_STEP + 20.0F;
        setContentHeight(contentH);

        UiCanvas.scissor(panelX + 1.0F, gridTop, panelW - 2.0F, panelH - (gridTop - panelY) - 6.0F);
        try {
            for (int i = 0; i < settings.size(); i++) {
                Setting<?> setting = settings.get(i);
                int col = i / perCol;
                int row = i % perCol;
                float sx = panelX + 15.0F + col * COL_PITCH;
                float sy = gridTop + row * SETTING_STEP - scroll;
                if (sy > panelY + panelH || sy + 24.0F < gridTop) {
                    continue;
                }
                drawSetting(sx, sy, panelW / 2.0F - 20.0F, setting, mx, my);
            }
        } finally {
            UiCanvas.clearScissor();
        }
    }

    private void drawSetting(float x, float y, float colW, Setting<?> setting, double mx, double my) {
        AetherFont.draw(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
            AetherFont.trim(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, setting.label(), 95.0F),
            x + 11.0F, y + 2.0F, UiTheme.textSoft());

        switch (setting.type()) {
            case BOOLEAN: {
                boolean on = Boolean.TRUE.equals(setting.value());
                toggleControl.update(on);
                toggleControl.draw(x + 168.0F, y - 2.0F, 0.85F, on, false);
                break;
            }
            case NUMBER: {
                Setting.Range range = setting.range();
                double min = range != null ? range.min() : 0.0D;
                double max = range != null ? range.max() : 100.0D;
                double step = range != null ? range.step() : 1.0D;
                UiSlider slider = sliderFor(setting, min, max, step);
                slider.draw(x + 122.0F, y + 6.0F, 75.0F, mx, my);
                if (slider.dragging()) {
                    draggingSlider = slider;
                    sliderDragX = x + 122.0F;
                    sliderDragW = 75.0F;
                }
                break;
            }
            case CHOICE: {
                UiCombo combo = comboFor(setting);
                combo.draw(x + 122.0F, y - 2.0F, mx, my);
                break;
            }
            case TEXT: {
                UiTextBox box = textFor(setting);
                box.draw(x + 122.0F, y - 4.0F, mx, my);
                break;
            }
            case KEYBIND: {
                boolean capturing = setting.id().equals(captureSettingId);
                keybindFor(setting).draw(x + 122.0F, y - 2.0F, capturing, mx, my);
                break;
            }
            case COLOR: {
                drawColorSetting(x, y, setting, mx, my);
                break;
            }
            default:
                break;
        }
    }

    /* ── control caches (one live control instance per setting) ─────────── */

    private final java.util.Map<String, UiSlider> sliders = new java.util.HashMap<String, UiSlider>();
    private final java.util.Map<String, UiCombo> combos = new java.util.HashMap<String, UiCombo>();
    private final java.util.Map<String, UiTextBox> textBoxes = new java.util.HashMap<String, UiTextBox>();
    private final java.util.Map<String, UiKeybindBox> keybinds = new java.util.HashMap<String, UiKeybindBox>();

    private UiSlider sliderFor(final Setting<?> setting, double min, double max, double step) {
        String key = setting.id() + "@" + min + ".." + max + "/" + step;
        UiSlider slider = sliders.get(key);
        if (slider == null) {
            slider = new UiSlider(min, max, step, "", new UiSlider.Read() {
                public double read() {
                    return setting.value() instanceof Number ? ((Number) setting.value()).doubleValue() : 0.0D;
                }
            }, new UiSlider.Write() {
                public void write(double value) {
                    setValue(setting, setting.value() instanceof Float
                        ? Float.valueOf((float) value) : Integer.valueOf((int) Math.round(value)));
                    saveQuietly();
                }
            });
            sliders.put(key, slider);
        }
        return slider;
    }

    private UiCombo comboFor(final Setting<?> setting) {
        UiCombo combo = combos.get(setting.id());
        if (combo == null) {
            final List<String> choices = setting.choices() == null
                ? new ArrayList<String>() : setting.choices();
            combo = new UiCombo(choices, new UiCombo.IntReader() {
                public int read() {
                    return choices.indexOf(String.valueOf(setting.value()));
                }
            }, new UiCombo.IntWriter() {
                public void write(int index) {
                    if (index >= 0 && index < choices.size()) {
                        setValue(setting, choices.get(index));
                        saveQuietly();
                    }
                }
            });
            combos.put(setting.id(), combo);
        }
        return combo;
    }

    private UiTextBox textFor(final Setting<?> setting) {
        final UiTextBox[] holder = new UiTextBox[1];
        UiTextBox box = textBoxes.get(setting.id());
        if (box == null) {
            box = new UiTextBox(75.0F, 16.0F, "", new Runnable() {
                public void run() {
                    setValue(setting, holder[0].text());
                    saveQuietly();
                }
            });
            holder[0] = box;
            box.setText(String.valueOf(setting.value()));
            textBoxes.put(setting.id(), box);
        }
        return box;
    }

    private UiKeybindBox keybindFor(final Setting<?> setting) {
        UiKeybindBox box = keybinds.get(setting.id());
        if (box == null) {
            box = new UiKeybindBox(new UiKeybindBox.KeyReader() {
                public int keyCode() {
                    return setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0;
                }
            }, new Runnable() {
                public void run() {
                    captureSettingId = setting.id();
                    client.input().capturingKey(true);
                }
            });
            keybinds.put(setting.id(), box);
        }
        return box;
    }

    private void endCapture() {
        if (captureSettingId != null) {
            captureSettingId = null;
            client.input().capturingKey(false);
        }
    }

    /* ── the colour picker ──────────────────────────────────────────────── */

    private void drawColorSetting(float x, float y, Setting<?> setting, double mx, double my) {
        int color = setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0xFFFFFFFF;
        boolean open = setting.id().equals(openPickerId);
        float swatchX = x + 98.0F;
        float swatchY = y - 2.0F;
        boolean hot = mx >= swatchX && mx < swatchX + 16.0F && my >= swatchY && my < swatchY + 16.0F;
        UiCanvas.roundRect(swatchX, swatchY, 16.0F, 16.0F, 4.0F, color);
        UiCanvas.outline(swatchX, swatchY, 16.0F, 16.0F, 4.0F,
            open ? UiTheme.withAlpha(UiTheme.accent(), 220) : hot ? UiTheme.textSoft() : UiTheme.edge(), 1.0F);

        if (!open) {
            return;
        }

        // The popover opens below the swatch, translated like the reference.
        float boxX = swatchX - 42.0F;
        float boxY = swatchY + 26.0F;
        float hue = hueOf(color);

        float[] panelBounds = {boxY, boxY + 100.0F};
        UiCanvas.roundRect(boxX - 8.0F, boxY - 8.0F, 100.0F + 28.0F, 116.0F, 8.0F, UiTheme.withAlpha(0x101116, 250));

        // Saturation/brightness square: white→hue horizontally, black overlay vertically.
        int hueColor = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F) | 0xFF000000;
        UiCanvas.scissor(boxX, boxY, 100.0F, 100.0F);
        UiCanvas.roundRect(boxX, boxY, 100.0F, 100.0F, 6.0F, 0xFFFFFFFF);
        UiCanvas.gradientRoundRect(boxX, boxY, 100.0F, 100.0F, 0.0F, 0xFFFFFFFF, hueColor);
        // The horizontal white→hue ramp needs the hue on the RIGHT: draw hue→white flipped.
        UiCanvas.gradientRoundRect(boxX, boxY, 100.0F, 100.0F, 0.0F, hueColor, 0xFFFFFFFF);
        UiCanvas.gradientRoundRect(boxX, boxY, 100.0F, 100.0F, 0.0F, 0x00000000, 0xFF000000);
        UiCanvas.clearScissor();

        // Hue strip: six stacked two-colour ramps.
        float stripX = boxX + 106.0F;
        int[] hueStops = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        for (int i = 0; i < 6; i++) {
            UiCanvas.gradientRoundRect(stripX, boxY + i * (100.0F / 6.0F), 12.0F, 100.0F / 6.0F + 0.5F, 0.0F,
                hueStops[i], hueStops[i + 1]);
        }

        // Markers.
        float[] sv = svOf(color, hue);
        float markerX = boxX + (float) (sv[0] * 100.0D);
        float markerY = boxY + (float) ((1.0D - sv[1]) * 100.0D);
        UiCanvas.circle(markerX, markerY, 3.0F, 0xFFFFFFFF);
        UiCanvas.circle(markerX, markerY, 2.0F, color);
        float hueY = boxY + hue * 100.0F;
        UiCanvas.roundRect(stripX - 2.0F, hueY - 1.5F, 16.0F, 3.0F, 1.5F, 0xFFFFFFFF);

        pickerState.update(boxX, boxY, setting, hue, mx, my);
    }

    private static class PickerState {
        boolean pickingSv;
        boolean pickingHue;
        float boxX;
        float boxY;
        float hue = 1.0F;
        Setting<?> bound;

        void update(float bx, float by, Setting<?> setting, float currentHue, double mx, double my) {
            this.boxX = bx;
            this.boxY = by;
            this.hue = currentHue;
            this.bound = setting;
            if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
                if (pickingSv || pickingHue) {
                    // commit on release like the reference
                }
                pickingSv = false;
                pickingHue = false;
                return;
            }
            if (pickingSv && mx >= bx && mx < bx + 100.0F && my >= by && my < by + 100.0F) {
                float s = (float) ((mx - bx) / 100.0D);
                float v = (float) (1.0D - (my - by) / 100.0D);
                int picked = java.awt.Color.HSBtoRGB(hue, Math.max(0.015F, s), Math.max(0.015F, v));
                setValue(bound, picked);
            } else if (pickingHue && mx >= bx + 106.0F && mx < bx + 118.0F && my >= by && my < by + 100.0F) {
                this.hue = (float) ((my - by) / 100.0D);
                float[] hsv = new float[3];
                int current = bound.value() instanceof Number ? ((Number) bound.value()).intValue() : 0xFFFFFFFF;
                java.awt.Color.RGBtoHSB((current >> 16) & 0xFF, (current >> 8) & 0xFF, current & 0xFF, hsv);
                int picked = java.awt.Color.HSBtoRGB(hue, Math.max(0.15F, hsv[1]), Math.max(0.15F, hsv[2]));
                setValue(bound, picked);
            }
        }

        boolean click(float bx, float by, double mx, double my) {
            this.boxX = bx;
            this.boxY = by;
            if (mx >= bx && mx < bx + 100.0F && my >= by && my < by + 100.0F) {
                pickingSv = true;
                return true;
            }
            if (mx >= bx + 106.0F && mx < bx + 118.0F && my >= by && my < by + 100.0F) {
                pickingHue = true;
                return true;
            }
            return false;
        }
    }

    private final PickerState pickerState = new PickerState();

    private static float hueOf(int argb) {
        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsv);
        return hsv[0];
    }

    private static float[] svOf(int argb, float hue) {
        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsv);
        return new float[] {hsv[1], hsv[2]};
    }

    /* ── header extras ──────────────────────────────────────────────────── */

    @Override
    public void drawHeaderExtras(double mx, double my) {
        if (hasSearch()) {
            searchBox.draw(contentX() + contentW() - 175.0F, windowY() + 6.5F, mx, my);
        }
    }

    @Override
    public boolean clickHeaderExtras(double mx, double my, int button) {
        if (!hasSearch()) {
            return false;
        }
        return searchBox.click(contentX() + contentW() - 175.0F, windowY() + 6.5F, mx, my, button);
    }

    @Override
    public boolean keyHeaderExtras(char typedChar, int keyCode) {
        if (!hasSearch()) {
            return false;
        }
        return searchBox.key(typedChar, keyCode);
    }

    @Override
    public boolean capturesKeyboard() {
        return searchBox.focused() || captureSettingId != null || anyTextBoxFocused();
    }

    private boolean anyTextBoxFocused() {
        for (UiTextBox box : textBoxes.values()) {
            if (box.focused()) {
                return true;
            }
        }
        return false;
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    public boolean click(double mx, double my, int button) {
        if (openModule != null && sceneSlide > 0.5F) {
            return clickSettings(mx, my, button);
        }
        return clickList(mx, my, button);
    }

    private boolean clickList(double mx, double my, int button) {
        if (button == 0) {
            // chips
            List<String> chipLabels = chipLabels();
            float chipX = innerX();
            float chipY = contentY() + 13.0F - scroll;
            for (int i = 0; i < chipLabels.size(); i++) {
                float chipW = AetherFont.width(9.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, chipLabels.get(i)) + 28.0F;
                if (mx >= chipX && mx < chipX + chipW && my >= chipY && my < chipY + 16.0F) {
                    applyChip(i);
                    return true;
                }
                chipX += chipW + 6.0F;
            }
            // rows
            float x = innerX() + 15.0F;
            float rowY = contentY() + ROW_TOP - scroll;
            float rowW = innerW();
            for (ClientModule module : visible) {
                if (mx >= x && mx < x + rowW && my >= rowY && my < rowY + 40.0F) {
                    boolean gearZone = mx >= x + rowW - 44.0F && mx < x + rowW - 22.0F;
                    if (gearZone && !module.settings().isEmpty()) {
                        openModule = module;
                        slideBack = false;
                        sliders.clear();
                        combos.clear();
                        textBoxes.clear();
                        keybinds.clear();
                        searchBox.setFocused(false);
                        return true;
                    }
                    if (mx < x + rowW - 60.0F) {
                        boolean enable = module.state() != ModuleState.ENABLED;
                        client.modules().setEnabled(module.metadata().id(), enable);
                        saveQuietly();
                        return true;
                    }
                    return true;
                }
                rowY += ROW_STEP;
            }
        }
        return true;
    }

    private boolean clickSettings(double mx, double my, int button) {
        float panelX = innerX();
        float panelW = innerW();
        float panelY = contentY() + 15.0F;

        // back chevron
        float backX = panelX + 10.0F;
        float backY = panelY + 8.0F;
        if (mx >= backX - 3.0F && mx < backX + 16.0F && my >= backY - 3.0F && my < backY + 16.0F) {
            stepBack();
            return true;
        }
        // reset all
        if (mx >= panelX + panelW - 39.0F && mx < panelX + panelW - 20.0F
            && my >= panelY + 8.0F && my < panelY + 26.0F) {
            for (Setting<?> setting : openModule.settings()) {
                setValue(setting, setting.defaultValue());
            }
            sliders.clear();
            textBoxes.clear();
            saveQuietly();
            return true;
        }

        // colour picker popover first (it overlays the grid)
        if (openPickerId != null) {
            float swatchX = pickerSwatchX(openPickerId);
            if (swatchX >= 0) {
                float boxX = swatchX - 42.0F;
                float boxY = contentY() + 15.0F + pickerSwatchY(openPickerId) + 26.0F;
                if (pickerState.click(boxX, boxY, mx, my)) {
                    saveQuietly();
                    return true;
                }
                if (button == 0) {
                    openPickerId = null; // click outside the popover closes it
                    return true;
                }
            }
        }

        List<Setting<?>> settings = openModule.settings();
        int perCol = Math.max(1, (settings.size() + 1) / 2);
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> setting = settings.get(i);
            int col = i / perCol;
            int row = i % perCol;
            float sx = panelX + 15.0F + col * COL_PITCH;
            float sy = panelY + 44.0F + row * SETTING_STEP - scroll;

            switch (setting.type()) {
                case BOOLEAN:
                    if (button == 0 && toggleControl.hits(sx + 168.0F, sy - 2.0F, 0.85F, mx, my)) {
                        setValue(setting, !Boolean.TRUE.equals(setting.value()));
                        saveQuietly();
                        return true;
                    }
                    break;
                case NUMBER: {
                    UiSlider slider = sliderFor(setting,
                        setting.range() != null ? setting.range().min() : 0.0D,
                        setting.range() != null ? setting.range().max() : 100.0D,
                        setting.range() != null ? setting.range().step() : 1.0D);
                    if (slider.click(sx + 122.0F, sy + 6.0F, 75.0F, mx, my, button)) {
                        draggingSlider = slider;
                        sliderDragX = sx + 122.0F;
                        sliderDragW = 75.0F;
                        return true;
                    }
                    break;
                }
                case CHOICE:
                    if (comboFor(setting).click(sx + 122.0F, sy - 2.0F, mx, my, button)) {
                        return true;
                    }
                    break;
                case TEXT:
                    if (textFor(setting).click(sx + 122.0F, sy - 4.0F, mx, my, button)) {
                        return true;
                    }
                    break;
                case KEYBIND:
                    if (keybindFor(setting).click(sx + 122.0F, sy - 2.0F, mx, my, button)) {
                        return true;
                    }
                    break;
                case COLOR: {
                    float swatchX = sx + 98.0F;
                    float swatchY = sy - 2.0F;
                    if (button == 0 && mx >= swatchX && mx < swatchX + 16.0F && my >= swatchY && my < swatchY + 16.0F) {
                        openPickerId = openPickerId == null || !openPickerId.equals(setting.id()) ? setting.id() : null;
                        return true;
                    }
                    break;
                }
                default:
                    break;
            }
        }
        // defocus text fields when clicking elsewhere in the scene
        for (UiTextBox box : textBoxes.values()) {
            box.clickOutside();
        }
        return true;
    }

    private float pickerSwatchX(String settingId) {
        if (openModule == null) {
            return -1.0F;
        }
        List<Setting<?>> settings = openModule.settings();
        int perCol = Math.max(1, (settings.size() + 1) / 2);
        for (int i = 0; i < settings.size(); i++) {
            if (settings.get(i).id().equals(settingId)) {
                int col = i / perCol;
                int row = i % perCol;
                return innerX() + 15.0F + col * COL_PITCH + 98.0F;
            }
        }
        return -1.0F;
    }

    private float pickerSwatchY(String settingId) {
        if (openModule == null) {
            return 0.0F;
        }
        List<Setting<?>> settings = openModule.settings();
        int perCol = Math.max(1, (settings.size() + 1) / 2);
        for (int i = 0; i < settings.size(); i++) {
            if (settings.get(i).id().equals(settingId)) {
                int row = i % perCol;
                return 44.0F + row * SETTING_STEP - 2.0F;
            }
        }
        return 0.0F;
    }

    @Override
    public void release(double mx, double my, int button) {
        if (draggingSlider != null) {
            draggingSlider.drag(sliderDragX, sliderDragW, mx, false);
            draggingSlider = null;
            saveQuietly();
        }
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (captureSettingId != null) {
            Setting<?> setting = findSetting(captureSettingId);
            if (keyCode != 1) {
                if (setting != null) {
                    setValue(setting, keyCode);
                    saveQuietly();
                }
            }
            endCapture();
            return true;
        }
        // Typing a printable character while the list shows refocuses search, like the reference.
        if (openModule == null && !searchBox.focused() && typedChar >= 32 && typedChar < 127) {
            searchBox.setFocused(true);
        }
        if (openModule != null) {
            for (UiTextBox box : textBoxes.values()) {
                if (box.key(typedChar, keyCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean stepBack() {
        if (captureSettingId != null) {
            endCapture();
            return true;
        }
        if (openPickerId != null) {
            openPickerId = null;
            return true;
        }
        if (openModule != null) {
            slideBack = true;
            saveQuietly();
            return true;
        }
        if (searchBox.focused()) {
            searchBox.setFocused(false);
            return true;
        }
        return false;
    }

    /* ── chips ──────────────────────────────────────────────────────────── */

    private List<String> chipLabels() {
        List<String> labels = new ArrayList<String>();
        labels.add("All");
        for (ModuleCategory item : client.modules().userVisibleCategories()) {
            labels.add(categoryLabel(item));
        }
        return labels;
    }

    private boolean isChipActive(int index) {
        if (index == 0) {
            return category == null;
        }
        List<ModuleCategory> categories = client.modules().userVisibleCategories();
        if (index - 1 >= categories.size()) {
            return false;
        }
        return category == categories.get(index - 1);
    }

    private void applyChip(int index) {
        category = index == 0 ? null : client.modules().userVisibleCategories().get(index - 1);
        refresh();
    }

    private static String categoryLabel(ModuleCategory category) {
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

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    /** Preselects a module category chip (the theme selector's entry point). */
    public void focusCategory(ModuleCategory category) {
        this.category = category;
        refresh();
    }

    public void debugSearch(String query) {
        searchBox.setText(query);
        refresh();
    }

    public void debugSelectCategory(String label) {
        List<String> labels = chipLabels();
        int index = labels.indexOf(label);
        if (index >= 0) {
            applyChip(index);
        }
    }

    public void debugToggleFirst() {
        if (!visible.isEmpty()) {
            ClientModule module = visible.get(0);
            client.modules().setEnabled(module.metadata().id(),
                module.state() != ModuleState.ENABLED);
            saveQuietly();
        }
    }

    public void debugOpenFirstSettings() {
        for (ClientModule module : visible) {
            if (!module.settings().isEmpty()) {
                openModule = module;
                slideBack = false;
                sliders.clear();
                combos.clear();
                textBoxes.clear();
                keybinds.clear();
                return;
            }
        }
    }

    public void debugBack() {
        stepBack();
    }

    /* ── helpers ────────────────────────────────────────────────────────── */

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }

    private Setting<?> findSetting(String id) {
        if (openModule == null) {
            return null;
        }
        for (Setting<?> setting : openModule.settings()) {
            if (setting.id().equals(id)) {
                return setting;
            }
        }
        return null;
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void dispose() {
        endCapture();
    }
}
