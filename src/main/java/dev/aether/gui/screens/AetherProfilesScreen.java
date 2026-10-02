package dev.aether.gui.screens;

import java.util.ArrayList;
import java.util.List;

import dev.aether.forge189.AetherUi;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.leaf.LeafArt;
import dev.aether.gui.leaf.LeafTextBox;
import dev.aether.gui.leaf.NavButton;
import dev.aether.gui.leaf.PageBar;
import dev.aether.gui.core.UiComponent;
import dev.aether.ui.GuiSection;

/**
 * The profile manager, Aether's own destination (Leaf has no profile system): a named snapshot
 * of the module configuration, saved into the same config file and applied in one click.
 * <p>
 * The rows moved here from the client settings screen, where they shared a page with the
 * preference toggles and the data actions; the backdrops and row geometry are the same
 * {@code main_mod.png} composition those screens use, so nothing about the visual language
 * changes - only where a player looks for it.
 */
public final class AetherProfilesScreen extends AetherGuiScreen {

    private static final int FIELD_X = 920;
    private static final int ROW_TOP = 410;
    private static final int ROW_PITCH = 100;
    private static final int ROWS_PER_PAGE = 5;

    private final List<UiComponent> rows = new ArrayList<UiComponent>();
    private PageBar pageBar;
    private String status;
    private long statusAtMillis;

    public AetherProfilesScreen(dev.aether.AetherClient client) {
        super(client);
        buildRows();
    }

    @Override
    protected GuiSection section() {
        return GuiSection.PROFILES;
    }

    @Override
    protected String backdropArt() {
        return LeafArt.BACKDROP_SETTINGS;
    }

    /* ── rows ───────────────────────────────────────────────────────────── */

    private void buildRows() {
        rows.clear();
        int y = ROW_TOP;

        // The field carries its own caption as a placeholder, so it can sit next to the button
        // instead of pushing a label outside the panel.
        final LeafTextBox[] nameHolder = new LeafTextBox[1];
        nameHolder[0] = new LeafTextBox(null, FIELD_X, y, 400, 67, "", new Runnable() {
            public void run() {
                // Leaf commits text on deselect; saving the profile is the button's job.
            }
        }).setPlaceholder("New profile");
        rows.add(nameHolder[0]);
        final int saveY = y;
        rows.add(new NavButton(LeafArt.SELECT, "Save", 720, saveY, 170, 67, new Runnable() {
            public void run() {
                saveProfile(nameHolder[0]);
            }
        }));
        y += ROW_PITCH;

        List<String> names = client.profiles().names();
        for (final String name : names) {
            rows.add(profileRowLabel(name, y));
            rows.add(new NavButton(LeafArt.SELECT, "Apply", 930, y, 170, 60, new Runnable() {
                public void run() {
                    boolean applied = client.profiles().apply(name, client.modules());
                    flash(applied ? "Profile '" + name + "' applied." : "Could not apply the profile.");
                    saveQuietly();
                }
            }));
            rows.add(new NavButton(LeafArt.SELECT, "Delete", 1110, y, 170, 60, new Runnable() {
                public void run() {
                    client.profiles().delete(name);
                    flash("Profile '" + name + "' deleted.");
                    saveQuietly();
                    buildRows();
                }
            }));
            y += ROW_PITCH;
        }

        if (rows.size() > ROWS_PER_PAGE) {
            pageBar = new PageBar(1230, ROW_TOP, 32, 400, ROWS_PER_PAGE, rows.size());
        } else {
            pageBar = null;
        }
    }

    /**
     * The toggle rows draw their label 410 units left of the tile; rows that are pure buttons
     * (apply/delete/save) get their caption inside the tile instead, so profile names are drawn
     * as a separate small label component at that offset.
     */
    private UiComponent profileRowLabel(final String name, final int y) {
        return new UiComponent() {
            {
                // The same 410-left caption offset the other rows use, clipped to stop short of
                // the Apply pill so a long profile name cannot run underneath it.
                at(680, y).size(240, 60);
            }

            @Override
            public void render() {
                AetherFont.draw(AetherFont.Size.BODY,
                    AetherFont.trimTo(AetherFont.Size.BODY, name, gw()),
                    gx(), gy() + (gh() - AetherFont.height(AetherFont.Size.BODY)) / 2,
                    AetherUi.TEXT_SECONDARY);
            }
        };
    }

    private void saveProfile(LeafTextBox field) {
        String name = dev.aether.config.ProfileStore.sanitize(field.text());
        if (name.isEmpty()) {
            flash("Type a name for the profile first.");
            return;
        }
        boolean saved = client.profiles().save(name, client.modules());
        flash(saved ? "Profile '" + name + "' saved." : "Could not save the profile.");
        field.setText("");
        saveQuietly();
        buildRows();
    }

    private void flash(String message) {
        status = message;
        statusAtMillis = System.currentTimeMillis();
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        for (UiComponent row : pageRows()) {
            row.onMouseMove(mx, my);
            row.render();
        }
        if (pageBar != null) {
            pageBar.render();
        }
        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(AetherFont.Size.SMALL, status,
                GuiScale.x(FIELD_X), GuiScale.y(250), AetherUi.TEXT_SECONDARY);
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
}
