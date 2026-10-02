package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.ChipBar;
import dev.aether.gui.components.ModuleRow;
import dev.aether.gui.components.ScrollView;
import dev.aether.gui.components.SearchBox;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.forge189.AetherUi;
import dev.aether.ui.GuiSection;
import dev.aether.ui.ModuleSearch;

/**
 * The Modules screen: every feature the client ships, as a scrolling list of rounded rows.
 * <p>
 * The composition is still Leaf's - its backdrop, its navigation row at the top, everything inside
 * the measured panel - but the 4x2 grid of 170x182 cards is gone. A card that big fits eight modules
 * and hides the rest behind pages, and it has room for a name and nothing else; the brief's Glide
 * reference instead wants a compact row per module with an icon, a name, a one-line description and
 * the controls on the right, which is also what makes a search field and a category filter useful
 * rather than decorative.
 * <p>
 * The list is built from {@code ModuleRegistry.userVisible()}, so themes, cosmetic slots and screen
 * launchers never reach it, and the categories come from the same source - a filter for a category
 * with no features in it is not offered. Sorting, filtering and ranking stay in the tested
 * {@link ModuleSearch}; this class only lays the results out and animates them.
 */
public final class AetherModScreen extends AetherGuiScreen {

    private static final int LIST_X = 430;
    private static final int LIST_WIDTH = 1020;
    private static final int SEARCH_Y = 386;
    private static final int SEARCH_WIDTH = 360;
    private static final int FILTER_Y = 388;
    private static final int LIST_TOP = 442;
    private static final int LIST_HEIGHT = 424;

    private final List<ClientModule> visible = new ArrayList<ClientModule>();
    private final List<ModuleRow> rows = new ArrayList<ModuleRow>();
    private final ScrollView scroll = new ScrollView().bounds(LIST_X, LIST_TOP, LIST_WIDTH, LIST_HEIGHT);
    private final SearchBox search = new SearchBox("Search modules", new Runnable() {
        public void run() {
            refresh();
        }
    }).place(LIST_X, SEARCH_Y, SEARCH_WIDTH, 40);
    private final ChipBar filters = new ChipBar(new Runnable() {
        public void run() {
            category = categoryAt(filters.selected());
            refresh();
        }
    });
    private ModuleCategory category;
    private String lastSignature = "";

    public AetherModScreen(dev.aether.AetherClient client) {
        super(client);
        refresh();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    @Override
    protected GuiSection section() {
        return GuiSection.MODULES;
    }

    @Override
    protected void renderContent(double mx, double my) {
        search.update();
        filters.update();
        scroll.update();
        for (ModuleRow row : rows) {
            ClientModule module = moduleAt(row.index());
            if (module != null) {
                row.setEnabled(module.state() == ModuleState.ENABLED);
            }
            row.update();
        }

        search.render();
        layoutFilters();
        filters.render();

        double contentMouseY = my + scroll.offset();
        // Scissor coordinates are GUI-scale pixels, so the design rectangle is converted first.
        Mc189Compat.pushScissor(GuiScale.x(LIST_X), GuiScale.y(LIST_TOP), GuiScale.w(LIST_WIDTH + 20),
            GuiScale.h(LIST_HEIGHT));
        try {
            for (ModuleRow row : rows) {
                row.renderOffset(0.0D, -scroll.offset());
                row.onMouseMove(mx, contentMouseY);
                row.render();
            }
        } finally {
            Mc189Compat.popScissor();
        }
        scroll.render();

        String count = visible.size() + (visible.size() == 1 ? " module" : " modules");
        AetherFont.draw(AetherFont.Size.CAPTION, count, GuiScale.x(LIST_X + SEARCH_WIDTH + 16),
            GuiScale.y(SEARCH_Y + 30), AetherUi.TEXT_DISABLED);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (search.onMouseClick(mx, my, button)) {
            return true;
        }
        if (filters.onMouseClick(mx, my, button)) {
            return true;
        }
        double contentMouseY = my + scroll.offset();
        if (scroll.onMouseClick(mx, my, button)) {
            return true;
        }
        for (ModuleRow row : rows) {
            if (row.onMouseClick(mx, contentMouseY, button)) {
                return true;
            }
        }
        search.blur();
        return true; // the backdrop swallows everything else, like Leaf's fullscreen texture
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        double contentMouseY = my + scroll.offset();
        for (ModuleRow row : rows) {
            row.onMouseRelease(mx, contentMouseY, button);
        }
        scroll.onMouseRelease();
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        return search.onKeyTyped(typedChar, keyCode);
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        // The base screen normalised the direction and applied the user's invert preference.
        scroll.wheel(delta);
        return true;
    }

    @Override
    protected void disposeContent() {
        for (ModuleRow row : rows) {
            row.dispose();
        }
    }

    /* ── data ───────────────────────────────────────────────────────────── */

    private void refresh() {
        ModuleSearch query = new ModuleSearch();
        // Only features: the registry owns that decision, so themes, cosmetic slots and the internal
        // services stay out of the browser however they are registered.
        query.source(client.modules().userVisible());
        query.query(search.text());
        query.category(category);
        List<ClientModule> results = new ArrayList<ClientModule>(query.results());
        Collections.sort(results, new Comparator<ClientModule>() {
            public int compare(ClientModule a, ClientModule b) {
                return a.metadata().name().compareToIgnoreCase(b.metadata().name());
            }
        });
        visible.clear();
        visible.addAll(results);

        String signature = category + "|" + search.text() + "|" + visible.size();
        boolean animated = !signature.equals(lastSignature);
        lastSignature = signature;

        syncFilters();
        rebuildRows(animated);
        scroll.content(rows.size() * ModuleRow.ROW_PITCH);
    }

    private void rebuildRows(boolean animated) {
        rows.clear();
        for (int i = 0; i < visible.size(); i++) {
            final ClientModule module = visible.get(i);
            ModuleRow row = new ModuleRow(module.metadata().name(), module.metadata().description(),
                module.metadata().category(), module.state() == ModuleState.ENABLED,
                !module.settings().isEmpty(), new Runnable() {
                    public void run() {
                        toggle(module);
                    }
                }, new Runnable() {
                    public void run() {
                        Mc189Compat.displayGuiScreen(
                            dev.aether.gui.AetherGui.moduleSettings(client, module.metadata().id()));
                    }
                });
            row.place(LIST_X, LIST_TOP + i * ModuleRow.ROW_PITCH, LIST_WIDTH, i, animated);
            rows.add(row);
        }
    }

    private void toggle(ClientModule module) {
        boolean enable = module.state() != ModuleState.ENABLED;
        client.modules().setEnabled(module.metadata().id(), enable);
        saveQuietly();
    }

    private ClientModule moduleAt(int index) {
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    /* ── filters ────────────────────────────────────────────────────────── */

    private void syncFilters() {
        List<String> labels = new ArrayList<String>();
        for (ModuleCategory item : orderedCategories()) {
            labels.add(displayLabel(item));
        }
        filters.labels(labels);
        filters.select(categoryIndex());
    }

    /**
     * Right-aligns the chip bar next to the search field. Done per frame rather than on a filter
     * change because the chip widths are measured through {@code GuiScale}, which is only valid once
     * the screen has been laid out - and because a window resize re-scales every label.
     */
    private void layoutFilters() {
        int width = filters.requiredWidth();
        filters.place(Math.max(LIST_X + SEARCH_WIDTH + 24, LIST_X + LIST_WIDTH - width), FILTER_Y, Math.max(width, 1));
    }

    private int categoryIndex() {
        List<ModuleCategory> options = orderedCategories();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i) == category) {
                return i;
            }
        }
        return 0;
    }

    private ModuleCategory categoryAt(int index) {
        List<ModuleCategory> options = orderedCategories();
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }

    /**
     * "All" plus every category that has at least one user-facing module. Derived from the registry,
     * so a category whose only members are themes or cosmetic slots is not offered at all.
     */
    private List<ModuleCategory> orderedCategories() {
        List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
        categories.add(null);
        categories.addAll(client.modules().userVisibleCategories());
        return categories;
    }

    static String displayLabel(ModuleCategory category) {
        if (category == null) {
            return "All";
        }
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

    /** Scrolls one viewport down, for the screenshot walker's paged shot. */
    public void debugNextPage() {
        scroll.scrollBy(LIST_HEIGHT);
    }

    /** Types a query into the search field, as a user would, then re-filters. */
    public void debugSearch(String query) {
        search.setText(query);
    }

    /**
     * Switches the category chip to the label named {@code label}, through the same
     * callback path a click on the chip takes.
     */
    public void debugSelectCategory(String label) {
        List<ModuleCategory> options = orderedCategories();
        for (int i = 0; i < options.size(); i++) {
            if (displayLabel(options.get(i)).equalsIgnoreCase(label)) {
                category = options.get(i);
                refresh();
                return;
            }
        }
    }

    /** Toggles the first listed module, the way clicking its row would. */
    public void debugToggleFirst() {
        if (!visible.isEmpty()) {
            toggle(visible.get(0));
        }
    }

    /**
     * Toggles the first module inside the current viewport. The walker scrolls the list before it
     * proves a toggle, and the first module in the *list* is off-screen by then, so its change
     * would not appear in the shot.
     */
    public void debugToggleVisible() {
        int first = (int) Math.floor(scroll.offset() / (double) ModuleRow.ROW_PITCH);
        if (first >= 0 && first < visible.size()) {
            toggle(visible.get(first));
        }
    }

    public void debugOpenFirstModuleSettings() {
        if (!visible.isEmpty()) {
            Mc189Compat.displayGuiScreen(
                dev.aether.gui.AetherGui.moduleSettings(client, visible.get(0).metadata().id()));
        }
    }
}
