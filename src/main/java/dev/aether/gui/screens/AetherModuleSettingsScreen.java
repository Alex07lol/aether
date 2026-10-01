package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.gui.leaf.ColorChart;
import dev.aether.gui.leaf.LeafArt;
import dev.aether.gui.leaf.LeafBar;
import dev.aether.gui.leaf.LeafTextBox;
import dev.aether.gui.leaf.LeafToggle;
import dev.aether.gui.leaf.NavButton;
import dev.aether.gui.leaf.PageBar;
import dev.aether.gui.leaf.SelectButton;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.module.ClientModule;
import dev.aether.module.setting.Setting;
import dev.aether.ui.GuiSection;

/**
 * One module's configuration screen, ported from Leaf Client's
 * {@code ModDetailSettings} (GPLv3, see docs/GUI_REBUILD.md): a "home" tile at Leaf's
 * position (640, 220, 80x80) that returns to Modules, and the module's settings laid
 * out with Leaf's control geometry - toggles (100x60, label 410 to the left) at
 * x = 1120, sliders (255 wide, label 250 to the left) at x = 960, choice pills
 * (300x90, label 210 to the left) at x = 920, text fields (400x67) at x = 770 and
 * colour swatches (255x40) at x = 960, all starting at y = 310 with a 100 pitch and
 * paged five rows at a time by the scrollbar at (1230, 310, 32, 460) when needed.
 * While a colour palette is open it blocks the other controls, like Leaf's
 * {@code isBlock} rule.
 * <p>
 * Aether's setting model drives the rows generically (Leaf special-cased every
 * module); values are written to the {@code Setting} objects and persisted through
 * the client's save path.
 */
public final class AetherModuleSettingsScreen extends AetherGuiScreen {

    private static final int ROW_TOP = 310;
    private static final int ROW_PITCH = 100;
    private static final int ROWS_PER_PAGE = 5;

    private final String moduleId;
    private final NavButton home = new NavButton(LeafArt.HOME, 640, 220, 80, 80, new Runnable() {
        public void run() {
            dev.aether.gui.AetherGui.open(client, GuiSection.MODULES);
        }
    });
    private final List<UiComponent> rows = new ArrayList<UiComponent>();
    private final java.util.Map<String, SelectButton> choiceRows =
        new java.util.HashMap<String, SelectButton>();
    private PageBar pageBar;
    private String capturingKeybind;

    private String status;
    private long statusAtMillis;

    private AetherModuleSettingsScreen(dev.aether.AetherClient client, String moduleId) {
        super(client);
        this.moduleId = moduleId;
        buildRows();
    }

    public static AetherModuleSettingsScreen forModule(dev.aether.AetherClient client, String moduleId) {
        return new AetherModuleSettingsScreen(client, moduleId);
    }

    private ClientModule module() {
        try {
            return client.modules().get(moduleId);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    @Override
    protected GuiSection section() {
        return GuiSection.MODULES;
    }

    @Override
    protected boolean showsNav() {
        return false; // Leaf's detail screen has only the home tile
    }

    @Override
    protected String backdropArt() {
        return LeafArt.BACKDROP_SETTINGS; // Leaf's ModDetailSettings backdrop
    }

    @Override
    protected GuiScreen escapeScreen() {
        // ESC mirrors the home tile: back to the module list, like Leaf's flow.
        return dev.aether.gui.AetherGui.modules(client);
    }

    @Override
    protected void disposeContent() {
        // Leaving the screen ends a pending capture, so the router hands the keyboard back to the
        // modules instead of keeping them suppressed for the rest of the session.
        capturingKeybind = null;
        client.input().capturingKey(false);
    }

    /* ── rows ───────────────────────────────────────────────────────────── */

    private void buildRows() {
        rows.clear();
        choiceRows.clear();
        pageBar = null;
        capturingKeybind = null;
        client.input().capturingKey(false);
        ClientModule module = module();
        if (module == null) {
            return;
        }
        int y = ROW_TOP;
        for (Setting<?> setting : module.settings()) {
            switch (setting.type()) {
                case BOOLEAN:
                    rows.add(new LeafToggle(setting.label(), 1120, y, 100, 60,
                        booleanReader(setting), new Runnable() {
                            public void run() {
                                setValue(setting, !Boolean.TRUE.equals(setting.value()));
                                saveQuietly();
                            }
                        }));
                    break;
                case NUMBER: {
                    Setting.Range range = setting.range();
                    double min = range != null ? range.min() : 0.0D;
                    double max = range != null ? range.max() : 100.0D;
                    double step = range != null ? range.step() : 1.0D;
                    rows.add(new LeafBar(setting.label(), 960, y, 255, 90, min, max, step, "",
                        numberReader(setting), numberWriter(setting), new Runnable() {
                            public void run() {
                                saveQuietly();
                            }
                        }));
                    break;
                }
                case CHOICE: {
                    List<String> choices = setting.choices() == null
                        ? new ArrayList<String>() : new ArrayList<String>(setting.choices());
                    SelectButton pill = new SelectButton(setting.label(), 920, y, 300, 90, choices,
                        String.valueOf(setting.value()), new Runnable() {
                            public void run() {
                                SelectButton row = choiceRows.get(setting.id());
                                if (row != null) {
                                    setValue(setting, row.current());
                                }
                                saveQuietly();
                            }
                        });
                    choiceRows.put(setting.id(), pill);
                    rows.add(pill);
                    break;
                }
                case TEXT: {
                    // Leaf puts its text fields at x = 770 with no caption; Aether labels every row,
                    // so the field sits in the same column as the choice rows (x = 920) where its
                    // 210-left caption lines up with theirs and the field still fits Leaf's panel.
                    final LeafTextBox[] holder = new LeafTextBox[1];
                    holder[0] = new LeafTextBox(setting.label(), 920, y, 400, 67,
                        String.valueOf(setting.value()), new Runnable() {
                            public void run() {
                                setValue(setting, holder[0].text());
                                saveQuietly();
                            }
                        });
                    rows.add(holder[0]);
                    break;
                }
                case COLOR: {
                    int current = setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0xFFFFFFFF;
                    final ColorChart[] holder = new ColorChart[1];
                    holder[0] = new ColorChart(setting.label(), 960, y, 255, 40, current, new Runnable() {
                        public void run() {
                            setValue(setting, Integer.valueOf(holder[0].colorCode()));
                            saveQuietly();
                        }
                    });
                    rows.add(holder[0]);
                    break;
                }
                case KEYBIND:
                    rows.add(new KeybindButton(setting.label(), 920, y, 300, 90, setting));
                    break;
                default:
                    break;
            }
            y += ROW_PITCH;
        }
        if (rows.size() > ROWS_PER_PAGE) {
            pageBar = new PageBar(1230, ROW_TOP, 32, 460, ROWS_PER_PAGE, rows.size());
        }
    }

    private LeafToggle.StateReader booleanReader(final Setting<?> setting) {
        return new LeafToggle.StateReader() {
            public boolean isOn() {
                return Boolean.TRUE.equals(setting.value());
            }
        };
    }

    private LeafBar.ValueReader numberReader(final Setting<?> setting) {
        return new LeafBar.ValueReader() {
            public double read() {
                return setting.value() instanceof Number ? ((Number) setting.value()).doubleValue() : 0.0D;
            }
        };
    }

    private LeafBar.ValueWriter numberWriter(final Setting<?> setting) {
        return new LeafBar.ValueWriter() {
            public void write(double value) {
                setValue(setting, setting.value() instanceof Float
                    ? Float.valueOf((float) value) : Integer.valueOf((int) Math.round(value)));
            }
        };
    }

    private UiComponent findRow(Setting<?> setting) {
        return choiceRows.get(setting.id());
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        home.render();

        ClientModule module = module();
        if (module != null) {
            AetherFont.drawShadowed(AetherFont.Size.SECTION, module.metadata().name(),
                GuiScale.x(640 + 80 + 24), GuiScale.y(220 + 22), AetherUi.TEXT_PRIMARY);
            String desc = module.metadata().description();
            if (!desc.isEmpty()) {
                AetherFont.draw(AetherFont.Size.SMALL, desc, GuiScale.x(640 + 80 + 24),
                    GuiScale.y(220 + 52), AetherUi.TEXT_SECONDARY);
            }
        }

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(AetherFont.Size.SMALL, status, GuiScale.x(1100), GuiScale.y(250), AetherUi.TEXT_SECONDARY);
        }

        boolean blocked = isColorPanelOpen();
        for (UiComponent row : pageRows()) {
            // While a colour palette is open the other rows freeze, like Leaf's isBlock.
            if (row instanceof ColorChart || !blocked) {
                row.render();
            }
        }
        if (pageBar != null) {
            pageBar.render();
        }
        if (capturingKeybind != null) {
            AetherFont.drawShadowed(AetherFont.Size.BODY, "Press any key to bind it - ESC cancels",
                GuiScale.x(1100), GuiScale.y(880), AetherUi.ACCENT);
        }
    }

    private boolean isColorPanelOpen() {
        for (UiComponent row : rows) {
            if (row instanceof ColorChart && ((ColorChart) row).isPanelOpen()) {
                return true;
            }
        }
        return false;
    }

    private List<UiComponent> pageRows() {
        if (pageBar == null) {
            return rows;
        }
        List<UiComponent> page = new ArrayList<UiComponent>();
        int index = pageBar.getIndex();
        for (int slot = 0; slot < ROWS_PER_PAGE && index + slot < rows.size(); slot++) {
            page.add(rows.get(index + slot));
        }
        return page;
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (home.onMouseClick(mx, my, button)) {
            return true;
        }
        boolean blocked = isColorPanelOpen();
        for (UiComponent row : pageRows()) {
            if (row instanceof ColorChart || !blocked) {
                row.onMouseClick(mx, my, button);
            }
        }
        return true;
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        for (UiComponent row : rows) {
            row.onMouseRelease(mx, my, button);
        }
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        if (capturingKeybind != null) {
            if (keyCode != 1) {
                ClientModule module = module();
                if (module != null) {
                    for (Setting<?> setting : module.settings()) {
                        if (setting.id().equals(capturingKeybind)) {
                            setValue(setting, keyCode);
                            saveQuietly();
                            status = "Bound to " + Keyboard.getKeyName(keyCode);
                            statusAtMillis = System.currentTimeMillis();
                            break;
                        }
                    }
                }
            }
            capturingKeybind = null;
            client.input().capturingKey(false);
            buildRows(); // rebuild so the pill shows the new key
            return true;
        }
        boolean consumed = false;
        for (UiComponent row : pageRows()) {
            if (row.onKeyTyped(typedChar, keyCode)) {
                consumed = true;
            }
        }
        return consumed;
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        if (pageBar == null || isColorPanelOpen()) {
            return true;
        }
        if (delta > 0) {
            pageBar.onScroll();
        } else {
            pageBar.onUnScroll();
        }
        return true;
    }

    /* ── the keybind pill (Leaf has no keybind control; styled like SelectButton) ── */

    private final class KeybindButton extends UiComponent {

        private final String rowName;
        private final Setting<?> setting;
        private boolean hover;

        KeybindButton(String rowName, int x, int y, int width, int height, Setting<?> setting) {
            this.rowName = rowName;
            this.setting = setting;
            at(x, y).size(width, height);
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            boolean capturing = setting.id().equals(capturingKeybind);

            if (rowName != null && !rowName.isEmpty()) {
                int labelSize = AetherFont.height(AetherFont.Size.BODY);
                AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(210),
                    top + (h - labelSize) / 2, AetherUi.TEXT_SECONDARY);
            }

            int key = setting.value() instanceof Number ? ((Number) setting.value()).intValue() : 0;
            String name = capturing ? "< press a key >" : Keyboard.getKeyName(key);
            float brightness = capturing || hover ? LeafArt.BRIGHT : LeafArt.NORMAL;
            if (hover) {
                LeafArt.drawHovered(LeafArt.SELECT, left, top, w, h, brightness);
            } else {
                LeafArt.draw(LeafArt.SELECT, left, top, w, h, brightness);
            }
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, name, left, top + (h - labelSize) / 2, w,
                capturing ? AetherUi.ACCENT : AetherUi.TEXT_PRIMARY);
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
            capturingKeybind = setting.id();
            // The router blocks every module key while a capture is live, so the press that is about
            // to be bound cannot also fire the module that already owns that key.
            client.input().capturingKey(true);
            return true;
        }

        @Override
        public void onMouseRelease(double mx, double my, int button) {
            hover = contains(mx, my);
        }
    }
}
