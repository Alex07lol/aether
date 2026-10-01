package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.List;

import dev.aether.gui.leaf.CosmeticEntry;
import dev.aether.gui.leaf.PageBar;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.theme.ThemeModule;
import dev.aether.ui.GuiSection;

/**
 * The Themes screen: Aether's own destination, built in the idiom the rest of the port uses
 * (Leaf has no theme picker to port). It is the Cosmetics screen's list without the player
 * preview - vertical 300x90 pills at Leaf's cosmetics geometry (x = 480, first row at y = 400,
 * 100 pitch) paged three at a time by the scrollbar at (945, 400, 32, 400), the wheel paging the
 * list wherever the cursor is, exactly as Leaf pages its lists.
 * <p>
 * The list is built by scanning the registry for {@link ThemeModule}s, so it carries no theme
 * names of its own: a new palette module appears here with no screen change. A pill's brightness
 * is its state - the equipped theme is the bright pill - and clicking one enables that module,
 * which the registry's group rule makes exclusive, so the palette of every screen and the HUD
 * changes on the next frame and persists through the client's save path. Clicking the equipped
 * theme again switches themes off and the client returns to its default palette.
 * <p>
 * There is deliberately no category or family filter here. With one palette per family the
 * filter would be a dropdown of six themes' own names over a list of six, and any grouping
 * invented on top of that is a control that cannot change what the screen shows - the pager
 * already covers the list, and the registry scan means a seventh theme pages rather than
 * overflows.
 */
public final class AetherThemesScreen extends AetherGuiScreen {

    private static final int LIST_X = 480;
    private static final int LIST_TOP = 400;
    private static final int ENTRY_PITCH = 100;
    private static final int ENTRIES_PER_PAGE = 3;

    private final PageBar pageBar = new PageBar(945, LIST_TOP, 32, 400, ENTRIES_PER_PAGE, 0);
    /** The pills of the current page, rebuilt every frame from the registry (cheap, under ten). */
    private final List<CosmeticEntry> pageEntries = new ArrayList<CosmeticEntry>();

    public AetherThemesScreen(dev.aether.AetherClient client) {
        super(client);
        pageBar.setListSize(themeCount());
    }

    @Override
    protected GuiSection section() {
        return GuiSection.THEMES;
    }

    /** Every registered theme, in registry order - the order the Modules screen lists them in. */
    private List<ClientModule> themes() {
        List<ClientModule> themes = new ArrayList<ClientModule>();
        for (ClientModule module : client.modules().all()) {
            if (module instanceof ThemeModule) {
                themes.add(module);
            }
        }
        return themes;
    }

    private int themeCount() {
        return themes().size();
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        List<ClientModule> themes = themes();
        pageEntries.clear();
        int index = pageBar.getIndex();
        for (int slot = 0; slot < ENTRIES_PER_PAGE && index + slot < themes.size(); slot++) {
            ClientModule theme = themes.get(index + slot);
            boolean active = theme.state() == ModuleState.ENABLED;
            CosmeticEntry entry = new CosmeticEntry(theme.metadata().name(), LIST_X, LIST_TOP + slot * ENTRY_PITCH,
                300, 90, active, new SelectTheme(theme));
            entry.onMouseMove(mx, my);
            entry.render();
            pageEntries.add(entry);
        }
        pageBar.render();
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        for (CosmeticEntry entry : pageEntries) {
            if (entry.onMouseClick(mx, my, button)) {
                return true;
            }
        }
        return true; // the backdrop swallows everything else, like Leaf's fullscreen texture
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        // Pills have no drag behaviour.
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        // The base screen already normalised the direction and applied the user's invert choice.
        if (delta > 0) {
            pageBar.onScroll();
        } else {
            pageBar.onUnScroll();
        }
        return true;
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    public void debugNextPage() {
        pageBar.onScroll();
    }

    /**
     * Clicks the pill in one slot of the current page, through the entry's own click path, so the
     * visual debugger exercises the same selection the mouse does (including the pick sound).
     */
    public void debugSelect(int slot) {
        if (slot < 0 || slot >= pageEntries.size()) {
            return;
        }
        CosmeticEntry entry = pageEntries.get(slot);
        entry.onMouseClick(entry.getX() + entry.getWidth() / 2.0,
            entry.getY() + entry.getHeight() / 2.0, 0);
    }

    /** Enables the picked theme; the registry's group rule disables the previous one. */
    private final class SelectTheme implements Runnable {
        private final ClientModule theme;

        SelectTheme(ClientModule theme) {
            this.theme = theme;
        }

        public void run() {
            boolean enable = theme.state() != ModuleState.ENABLED;
            client.modules().setEnabled(theme.metadata().id(), enable);
            saveQuietly();
        }
    }
}
