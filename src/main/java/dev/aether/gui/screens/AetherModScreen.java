package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dev.aether.gui.leaf.ModuleCard;
import dev.aether.gui.leaf.PageBar;
import dev.aether.gui.leaf.SelectButton;
import dev.aether.forge189.Mc189Compat;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.ui.GuiSection;
import dev.aether.ui.ModuleSearch;

/**
 * The Modules screen, ported from Leaf Client's {@code ModSettings} (GPLv3, see
 * docs/GUI_REBUILD.md): the four navigation tiles up top, a 4x2 grid of 170x182
 * module cards aligned under them (top half toggles the module, bottom half opens its
 * settings), a 32-wide page scrollbar in the middle gap exactly where Leaf puts it,
 * and a category selector in Leaf's SelectButton shape. The grid pages eight cards at
 * a time, which is Leaf's scrolling behavior.
 * <p>
 * The cards are a presentation layer: enabling/disabling goes through
 * {@code ModuleRegistry}, filtering through the tested {@link ModuleSearch}, and every
 * change persists through the client's save path.
 */
public final class AetherModScreen extends AetherGuiScreen {

    /** Leaf's grid geometry: cards sit under the nav tiles, 220 apart vertically. */
    private static final int GRID_TOP = 400;
    private static final int GRID_PITCH_Y = 220;
    private static final int CARDS_PER_PAGE = 8;

    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private ModuleCategory category;
    private final PageBar pageBar = new PageBar(945, GRID_TOP, 32, 400, CARDS_PER_PAGE, 0);
    private SelectButton categoryButton;

    public AetherModScreen(dev.aether.AetherClient client) {
        super(client);
        this.categoryButton = buildCategoryButton();
        refresh();
    }

    /** Opens the screen pre-filtered to one category (the theme selector's entry point). */
    public void focusCategory(ModuleCategory category) {
        this.category = category;
        this.categoryButton = buildCategoryButton();
        refresh();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.MODULES;
    }

    @Override
    protected void renderContent(double mx, double my) {
        for (ModuleCard card : pageCards()) {
            card.onMouseMove(mx, my);
            card.render();
        }
        pageBar.render();
        categoryButton.render();
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (categoryButton.onMouseClick(mx, my, button)) {
            return true;
        }
        for (ModuleCard card : pageCards()) {
            if (card.onMouseClick(mx, my, button)) {
                return true;
            }
        }
        return true; // the backdrop swallows everything else, like Leaf's fullscreen texture
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        for (ModuleCard card : pageCards()) {
            card.onMouseRelease(mx, my, button);
        }
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        // Leaf pages on any wheel movement over the screen.
        if (delta < 0) {
            pageBar.onScroll();
        } else {
            pageBar.onUnScroll();
        }
        return true;
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private SelectButton buildCategoryButton() {
        ModuleCategory[] options = orderedCategories();
        List<String> labels = new ArrayList<String>();
        for (ModuleCategory item : options) {
            labels.add(item == null ? "All" : displayLabel(item));
        }
        String current = category == null ? "All" : displayLabel(category);
        // Leaf's panel ends at y = 901 and the card grid at y = 802, so the filter sits in the strip
        // between them instead of hanging off the panel's bottom edge.
        return new SelectButton("Category", 430, 806, 300, 90, labels, current, new Runnable() {
            public void run() {
                String selected = categoryButton.current();
                category = "All".equals(selected) ? null : ModuleCategory.valueOf(categoryValue(selected));
                refresh();
            }
        });
    }

    private void refresh() {
        ModuleSearch search = new ModuleSearch();
        search.source(client.modules().all());
        search.category(category);
        List<ClientModule> results = new ArrayList<ClientModule>(search.results());
        Collections.sort(results, new Comparator<ClientModule>() {
            public int compare(ClientModule a, ClientModule b) {
                return a.metadata().name().compareToIgnoreCase(b.metadata().name());
            }
        });
        visible.clear();
        visible.addAll(results);
        pageBar.setListSize(visible.size());
    }

    private List<ModuleCard> pageCards() {
        List<ModuleCard> page = new ArrayList<ModuleCard>();
        int index = pageBar.getIndex();
        for (int slot = 0; slot < CARDS_PER_PAGE && index + slot < visible.size(); slot++) {
            ClientModule module = visible.get(index + slot);
            int column = slot % 4;
            int row = (slot / 4) % 2;
            page.add(new ModuleCard(module.metadata().name(),
                module.state() == ModuleState.ENABLED, !module.settings().isEmpty(),
                NAV_X[column], GRID_TOP + row * GRID_PITCH_Y,
                new ToggleAction(module), new SettingsAction(module)));
        }
        return page;
    }

    private final class ToggleAction implements Runnable {
        private final ClientModule module;

        ToggleAction(ClientModule module) {
            this.module = module;
        }

        public void run() {
            boolean enable = module.state() != ModuleState.ENABLED;
            client.modules().setEnabled(module.metadata().id(), enable);
            saveQuietly();
            refresh();
        }
    }

    private final class SettingsAction implements Runnable {
        private final ClientModule module;

        SettingsAction(ClientModule module) {
            this.module = module;
        }

        public void run() {
            Mc189Compat.displayGuiScreen(
                dev.aether.gui.AetherGui.moduleSettings(client, module.metadata().id()));
        }
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    public void debugNextPage() {
        pageBar.onScroll();
    }

    public void debugOpenFirstModuleSettings() {
        if (!visible.isEmpty()) {
            Mc189Compat.displayGuiScreen(
                dev.aether.gui.AetherGui.moduleSettings(client, visible.get(0).metadata().id()));
        }
    }

    /* ── category naming ────────────────────────────────────────────────── */

    private static ModuleCategory[] orderedCategories() {
        return new ModuleCategory[] {
            null,
            ModuleCategory.HUD, ModuleCategory.PVP, ModuleCategory.GRAPHICS, ModuleCategory.RENDER,
            ModuleCategory.INTERFACE, ModuleCategory.PERFORMANCE, ModuleCategory.COSMETICS,
            ModuleCategory.THEMES, ModuleCategory.MOVEMENT, ModuleCategory.AUDIO,
            ModuleCategory.ACCESSIBILITY, ModuleCategory.GENERAL
        };
    }

    private static String categoryValue(String label) {
        for (ModuleCategory item : orderedCategories()) {
            if (item != null && displayLabel(item).equals(label)) {
                return item.name();
            }
        }
        return "GENERAL";
    }

    static String displayLabel(ModuleCategory category) {
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
}
