package dev.aether.gui.screens;

import dev.aether.forge189.AetherUi;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.leaf.PageBar;
import dev.aether.theme.ThemeDefinition;
import dev.aether.theme.ThemeManager;
import dev.aether.ui.GuiSection;

import java.util.ArrayList;
import java.util.List;

/**
 * The Appearance screen: where a theme is worn.
 * <p>
 * The screen reads the palettes straight from {@code ThemeManager} - the theme system's own
 * state, which never passes through the module registry - so it carries no theme names of its
 * own and no screen change is needed when a palette is added. A pill's brightness is its state:
 * the worn theme is the bright pill, and clicking one calls {@code ThemeManager.select}, which
 * changes the palette of every screen and the HUD on the next frame and persists through the
 * client's save path.
 * <p>
 * The layout is the idiom the rest of the port uses (Leaf's pills and pager); Leaf has no theme
 * picker to port. There is deliberately no category filter: with one palette per family a
 * filter would be a dropdown of six themes' own names over a list of six.
 */
public final class AetherAppearanceScreen extends AetherGuiScreen {

    private static final int LIST_X = 480;
    private static final int LIST_TOP = 400;
    private static final int ENTRY_PITCH = 100;
    private static final int ENTRIES_PER_PAGE = 5;

    private final PageBar pageBar = new PageBar(945, LIST_TOP, 32, 400, ENTRIES_PER_PAGE, 0);
    /** The pills of the current page, rebuilt every frame from the manager (cheap, under ten). */
    private final List<ThemePill> pageEntries = new ArrayList<ThemePill>();

    public AetherAppearanceScreen(dev.aether.AetherClient client) {
        super(client);
        pageBar.setListSize(client.themes().count());
    }

    @Override
    protected GuiSection section() {
        return GuiSection.APPEARANCE;
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        ThemeDefinition[] themes = client.themes().themes();
        pageBar.setListSize(themes.length);
        pageEntries.clear();
        int index = pageBar.getIndex();
        for (int slot = 0; slot < ENTRIES_PER_PAGE && index + slot < themes.length; slot++) {
            ThemeDefinition theme = themes[index + slot];
            boolean active = theme.id().equals(client.themes().activeId());
            ThemePill entry = new ThemePill(theme, LIST_X, LIST_TOP + slot * ENTRY_PITCH, 300, 90,
                active, new Runnable() {
                    public void run() {
                        boolean alreadyWorn = client.themes().activeId().equals(theme.id());
                        client.themes().select(alreadyWorn
                            ? dev.aether.theme.ThemeManager.DEFAULT_THEME_ID : theme.id());
                        saveQuietly();
                    }
                });
            entry.onMouseMove(mx, my);
            entry.render();
            pageEntries.add(entry);
        }
        pageBar.render();

        // What the player is looking at, in one line: the worn palette's own name.
        AetherFont.draw(AetherFont.Size.CAPTION, "Theme: " + client.theme().name(),
            GuiScale.x(LIST_X + 340), GuiScale.y(LIST_TOP + 8), AetherUi.TEXT_SECONDARY);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        for (ThemePill entry : pageEntries) {
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
        ThemePill entry = pageEntries.get(slot);
        entry.onMouseClick(entry.getX() + entry.getWidth() / 2.0,
            entry.getY() + entry.getHeight() / 2.0, 0);
    }
}
