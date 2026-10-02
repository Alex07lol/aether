package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.List;

import dev.aether.gui.leaf.LeafArt;
import dev.aether.gui.leaf.LeafToggle;
import dev.aether.gui.leaf.NavButton;
import dev.aether.gui.leaf.PageBar;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.Mc189Compat;
import dev.aether.ui.GuiSection;

/**
 * The client settings screen, ported from Leaf Client's {@code ClientSettings}
 * (GPLv3, see docs/GUI_REBUILD.md): a column of state tiles at Leaf's position
 * (x = 1090, starting y = 310, 100 pitch, 100x60 each, row labels 410 to the left),
 * paged five rows at a time by the scrollbar at (1230, 310, 32, 460) when the list is
 * longer. Aether's rows are its own real settings - the persisted preferences and the
 * data actions - presented with Leaf's geometry. The profile manager lives on the
 * Profiles destination and the palettes on Appearance; neither belongs here.
 */
public final class AetherClientSettingsScreen extends AetherGuiScreen {

    private static final int ROW_X = 1090;
    /**
     * Leaf's ClientSettings starts its rows at y = 310, but Leaf's screen has no
     * navigation row - Aether's does (y = 250..356), so the rows start one pitch
     * lower to keep the composition clear of it.
     */
    private static final int ROW_TOP = 410;
    private static final int ROW_PITCH = 100;
    private static final int ROWS_PER_PAGE = 5;

    private final List<UiComponent> rows = new ArrayList<UiComponent>();
    private PageBar pageBar;
    private String status;
    private long statusAtMillis;
    private boolean screenshotRequested;

    public AetherClientSettingsScreen(dev.aether.AetherClient client) {
        super(client);
        buildRows();
    }

    @Override
    protected GuiSection section() {
        return GuiSection.SETTINGS;
    }

    @Override
    protected String backdropArt() {
        return LeafArt.BACKDROP_SETTINGS; // Leaf's ClientSettings backdrop
    }

    /* ── rows ───────────────────────────────────────────────────────────── */

    private void buildRows() {
        rows.clear();
        int y = ROW_TOP;

        rows.add(new LeafToggle("Save on close", ROW_X, y, 100, 60,
            new LeafToggle.StateReader() {
                public boolean isOn() {
                    return client.preferences().saveOnClose();
                }
            }, new Runnable() {
                public void run() {
                    client.preferences().setSaveOnClose(!client.preferences().saveOnClose());
                    saveQuietly();
                }
            }));
        y += ROW_PITCH;

        rows.add(new LeafToggle("Show tooltips", ROW_X, y, 100, 60,
            new LeafToggle.StateReader() {
                public boolean isOn() {
                    return client.preferences().showTooltips();
                }
            }, new Runnable() {
                public void run() {
                    client.preferences().setShowTooltips(!client.preferences().showTooltips());
                    saveQuietly();
                }
            }));
        y += ROW_PITCH;

        // The wheel direction every paged screen follows; flipping it here flips it everywhere,
        // because the screens read one normalised delta.
        rows.add(new LeafToggle("Invert Scroll", ROW_X, y, 100, 60,
            new LeafToggle.StateReader() {
                public boolean isOn() {
                    return client.preferences().invertScroll();
                }
            }, new Runnable() {
                public void run() {
                    client.preferences().setInvertScroll(!client.preferences().invertScroll());
                    saveQuietly();
                }
            }));
        y += ROW_PITCH;

        rows.add(new NavButton(LeafArt.SELECT, "Save Config", ROW_X, y, 170, 60, new Runnable() {
            public void run() {
                saveQuietly();
                flash("Configuration saved.");
            }
        }));
        y += ROW_PITCH;
        rows.add(new NavButton(LeafArt.SELECT, "Screenshot", ROW_X, y, 170, 60, new Runnable() {
            public void run() {
                screenshotRequested = true;
            }
        }));

        if (rows.size() > ROWS_PER_PAGE) {
            pageBar = new PageBar(1230, ROW_TOP, 32, 400, ROWS_PER_PAGE, rows.size());
        } else {
            pageBar = null;
        }
    }

    private void flash(String message) {
        status = message;
        statusAtMillis = System.currentTimeMillis();
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        pollScreenshotRequest();
        for (UiComponent row : pageRows()) {
            row.onMouseMove(mx, my);
            row.render();
        }
        if (pageBar != null) {
            pageBar.render();
        }
        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            dev.aether.gui.AetherFont.draw(dev.aether.gui.AetherFont.Size.SMALL, status,
                dev.aether.gui.GuiScale.x(ROW_X), dev.aether.gui.GuiScale.y(250),
                dev.aether.forge189.AetherUi.TEXT_SECONDARY);
        }
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
        for (UiComponent row : pageRows()) {
            if (row.onMouseClick(mx, my, button)) {
                return true;
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
        if (pageBar == null) {
            return true;
        }
        if (delta > 0) {
            pageBar.onScroll();
        } else {
            pageBar.onUnScroll();
        }
        return true;
    }

    /* ── screenshot action ──────────────────────────────────────────────── */

    private void pollScreenshotRequest() {
        if (!screenshotRequested) {
            return;
        }
        screenshotRequested = false;
        if (client.screenshots().state().isInFlight()) {
            return;
        }
        int width = Mc189Compat.displayWidth(Mc189Compat.minecraft());
        int height = Mc189Compat.displayHeight(Mc189Compat.minecraft());
        if (width <= 0 || height <= 0) {
            return;
        }
        int[] pixels = Mc189Compat.readFramePixels(width, height);
        if (pixels == null) {
            flash("Screenshot failed.");
            return;
        }
        if (client.screenshots().encode(pixels, width, height)) {
            flash("Saving screenshot..");
        }
    }
}
