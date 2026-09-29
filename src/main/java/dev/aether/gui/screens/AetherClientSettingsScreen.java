package dev.aether.gui.screens;

import java.util.List;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.Button;
import dev.aether.gui.components.TextField;
import dev.aether.gui.components.Toggle;
import dev.aether.gui.core.ScrollContainer;
import dev.aether.gui.core.ScreenShell;
import dev.aether.gui.core.UiComponent;
import dev.aether.ui.GuiSection;

/**
 * The client settings screen - the equivalent of Leaf Client's {@code ClientSettings}
 * toggle list (see docs/GUI_REBUILD.md), backed by the client's own managers:
 * Aether preferences (persisted switches), module profiles (save/apply/delete) and
 * the data actions (save now, take screenshot).
 */
public final class AetherClientSettingsScreen extends AetherGuiScreen {

    private static final int ROW_GAP = 8;

    private final ScrollContainer rows = new ScrollContainer();
    private final TextField profileName = new TextField("Profile name..", null);
    private final Button saveProfileButton = new Button("Save", Button.Style.PRIMARY, new Runnable() {
        public void run() {
            saveProfile();
        }
    });
    private final Button saveNowButton = new Button("Save configuration", Button.Style.QUIET, new Runnable() {
        public void run() {
            saveQuietly();
            status = "Configuration saved.";
            statusAtMillis = System.currentTimeMillis();
            rebuildRows();
        }
    });
    private final Button screenshotButton = new Button("Take screenshot", Button.Style.QUIET, new Runnable() {
        public void run() {
            screenshotRequested = true;
        }
    });

    private String status;
    private long statusAtMillis;
    private boolean screenshotRequested;

    public AetherClientSettingsScreen(dev.aether.AetherClient client) {
        super(client);
        rebuildRows();
    }

    @Override
    protected GuiSection section() {
        return GuiSection.SETTINGS;
    }

    @Override
    protected String title() {
        return "Settings";
    }

    @Override
    protected TextField searchField() {
        return null;
    }

    @Override
    protected String footerHint() {
        return "Right Shift: close";
    }

    @Override
    protected void layout(double contentX, double contentY, double contentW, double contentH) {
        int top = (int) contentY;
        int headerW = (int) Math.min(260.0D, contentW * 0.4D);
        profileName.at((int) contentX, top).size(headerW, ScreenShell.SEARCH_HEIGHT - 4);
        saveProfileButton.at((int) (contentX + headerW + 8), top).size(90, ScreenShell.SEARCH_HEIGHT - 4);

        double listTop = top + ScreenShell.SEARCH_HEIGHT - 4 + 12;
        rows.at((int) contentX, (int) listTop).size((int) contentW, (int) (contentH - (listTop - contentY)));
        rebuildRows();
    }

    /* ── rows ───────────────────────────────────────────────────────────── */

    private void rebuildRows() {
        rows.clear();
        double y = 0;
        double w = Math.max(80, rows.getWidth() - 12);

        y = addHeading(rows, y, w, "Preferences");
        y = addPrefToggle(rows, y, w, "Save on close",
            "Write the configuration whenever the GUI closes.", true);
        y = addPrefToggle(rows, y, w, "Show tooltips",
            "Explain controls on hover where available.", false);
        y += 10;

        y = addHeading(rows, y, w, "Profiles");
        List<String> names = client.profiles().names();
        if (names.isEmpty()) {
            y = addNote(rows, y, w, "No saved profiles yet - name one above and press Save.");
        }
        for (final String name : names) {
            ProfileRow row = new ProfileRow(name);
            row.at(0, (int) y).size((int) w, 40);
            rows.add(row);
            y += 40 + ROW_GAP;
        }
        y += 10;

        y = addHeading(rows, y, w, "Data");
        Button saveNow = new Button("Save configuration", Button.Style.QUIET, saveNowAction());
        saveNow.at(0, (int) y).size((int) w, 34);
        rows.add(saveNow);
        y += 34 + ROW_GAP;
        Button shot = new Button("Take screenshot", Button.Style.QUIET, screenshotAction());
        shot.at(0, (int) y).size((int) w, 34);
        rows.add(shot);
        y += 34 + ROW_GAP;

        rows.clampOffset();
    }

    private Runnable saveNowAction() {
        return new Runnable() {
            public void run() {
                saveQuietly();
                status = "Configuration saved.";
                statusAtMillis = System.currentTimeMillis();
            }
        };
    }

    private Runnable screenshotAction() {
        return new Runnable() {
            public void run() {
                screenshotRequested = true;
            }
        };
    }

    private UiComponent prefToggle(String title, String description, boolean saveOnClose) {
        final Toggle toggle = new Toggle(new Toggle.StateProvider() {
            public boolean isOn() {
                return saveOnClose ? client.preferences().saveOnClose() : client.preferences().showTooltips();
            }
        }, new Runnable() {
            public void run() {
                if (saveOnClose) {
                    client.preferences().setSaveOnClose(!client.preferences().saveOnClose());
                } else {
                    client.preferences().setShowTooltips(!client.preferences().showTooltips());
                }
                saveQuietly();
            }
        });
        toggle.size(46, 20);
        PrefRow row = new PrefRow(title, description, toggle);
        return row;
    }

    private double addPrefToggle(ScrollContainer container, double y, double w, String title, String description, boolean saveOnClose) {
        UiComponent row = prefToggle(title, description, saveOnClose);
        row.at(0, (int) y).size((int) w, 46);
        container.add(row);
        return y + 46 + ROW_GAP;
    }

    private double addHeading(ScrollContainer container, double y, double w, String title) {
        Heading heading = new Heading(title);
        heading.at(0, (int) y).size((int) w, 24);
        container.add(heading);
        return y + 30;
    }

    private double addNote(ScrollContainer container, double y, double w, String note) {
        Note noteRow = new Note(note);
        noteRow.at(0, (int) y).size((int) w, 18);
        container.add(noteRow);
        return y + 22;
    }

    private void saveProfile() {
        String name = dev.aether.config.ProfileStore.sanitize(profileName.text());
        if (name.isEmpty()) {
            status = "Type a name for the profile first.";
            statusAtMillis = System.currentTimeMillis();
            return;
        }
        boolean saved = client.profiles().save(name, client.modules());
        status = saved ? "Profile '" + name + "' saved." : "Could not save the profile.";
        statusAtMillis = System.currentTimeMillis();
        profileName.reset();
        saveQuietly();
        rebuildRows();
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    protected void renderContent(double mx, double my) {
        pollScreenshotRequest();
        profileName.render();
        saveProfileButton.render();

        if (status != null && System.currentTimeMillis() - statusAtMillis < 6000L) {
            AetherFont.draw(AetherFont.Size.SMALL, status,
                GuiScale.x(saveProfileButton.getX() + saveProfileButton.getWidth() + 12),
                GuiScale.y(profileName.getY() + 9), AetherUi.TEXT_SECONDARY);
        }

        rows.render();
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        if (profileName.onMouseClick(mx, my, button) || saveProfileButton.onMouseClick(mx, my, button)) {
            return true;
        }
        profileName.clickOutside();
        return rows.onMouseClick(mx, my, button);
    }

    @Override
    protected void releaseContent(double mx, double my, int button) {
        rows.onMouseRelease(mx, my, button);
    }

    @Override
    protected boolean keyContent(char typedChar, int keyCode) {
        return profileName.onKeyTyped(typedChar, keyCode);
    }

    @Override
    protected boolean wheelContent(double mx, double my, int delta) {
        return rows.onWheel(mx, my, delta);
    }

    @Override
    protected void disposeContent() {
        rows.dispose();
    }

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
            status = "Screenshot failed.";
            statusAtMillis = System.currentTimeMillis();
            return;
        }
        if (client.screenshots().encode(pixels, width, height)) {
            status = "Saving screenshot..";
            statusAtMillis = System.currentTimeMillis();
        }
    }

    /* ── simple row components ──────────────────────────────────────────── */

    /** A section heading inside the scrolled list. */
    private static final class Heading extends UiComponent {
        private final String text;

        Heading(String text) {
            this.text = text;
        }

        @Override
        public void render() {
            AetherFont.draw(AetherFont.Size.SECTION, text, gx(), gy(), AetherUi.TEXT_PRIMARY);
            Mc189Compat.drawRect(gx(), gy() + GuiScale.h(26), gx() + gw(), gy() + GuiScale.h(27),
                AetherUi.withAlpha(AetherUi.ACCENT, 0x44));
        }
    }

    /** A muted explanatory line. */
    private static final class Note extends UiComponent {
        private final String text;

        Note(String text) {
            this.text = text;
        }

        @Override
        public void render() {
            AetherFont.draw(AetherFont.Size.SMALL, text, gx(), gy(), AetherUi.TEXT_DISABLED);
        }
    }

    /** A preference title + description with a toggle on the right. */
    private final class PrefRow extends UiComponent {
        private final String title;
        private final String description;
        private final Toggle toggle;
        private boolean hover;

        PrefRow(String title, String description, Toggle toggle) {
            this.title = title;
            this.description = description;
            this.toggle = toggle;
        }

        @Override
        public UiComponent at(int x, int y) {
            super.at(x, y);
            toggle.at(x + width - 12 - 46, y + (height - 20) / 2);
            return this;
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(7),
                hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG);
            AetherFont.draw(AetherFont.Size.BODY, title, left + GuiScale.w(12), top + GuiScale.h(6), AetherUi.TEXT_PRIMARY);
            AetherFont.draw(AetherFont.Size.SMALL, description, left + GuiScale.w(12), top + GuiScale.h(26), AetherUi.TEXT_SECONDARY);
            renderChild(toggle);
        }

        @Override
        public void onMouseMove(double mx, double my) {
            hover = contains(mx, my);
            toggle.onMouseMove(mx, my);
        }

        @Override
        public boolean onMouseClick(double mx, double my, int button) {
            if (!contains(mx, my)) {
                return false;
            }
            return toggle.onMouseClick(mx, my, button);
        }

        @Override
        public void onMouseRelease(double mx, double my, int button) {
            toggle.onMouseRelease(mx, my, button);
        }
    }

    /** One saved profile: name, enabled-module count, apply and delete actions. */
    private final class ProfileRow extends UiComponent {
        private final String name;
        private boolean hover;

        ProfileRow(String name) {
            this.name = name;
        }

        private boolean inApply(double mx, double my) {
            return regionFromRight(mx, my, 12, 64);
        }

        private boolean inDelete(double mx, double my) {
            return regionFromRight(mx, my, 12 + 64 + 8, 64);
        }

        private boolean regionFromRight(double mx, double my, double fromRight, double w) {
            double x = this.x + this.width - fromRight - w;
            return mx >= x && mx < x + w && my >= this.y && my < this.y + this.height;
        }

        @Override
        public void render() {
            int left = gx();
            int top = gy();
            int w = gw();
            int h = gh();
            AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(7),
                hover ? AetherUi.CARD_HOVER : AetherUi.CARD);
            AetherUi.outline(left, top, left + w, top + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));
            AetherFont.draw(AetherFont.Size.BODY, name, left + GuiScale.w(12), top + GuiScale.h(5), AetherUi.TEXT_PRIMARY);
            AetherFont.draw(AetherFont.Size.CAPTION, client.profiles().enabledCount(name) + " modules enabled",
                left + GuiScale.w(12), top + GuiScale.h(24), AetherUi.TEXT_DISABLED);

            drawAction("Apply", left + w - GuiScale.w(12) - GuiScale.w(64), top + (h - GuiScale.h(22)) / 2,
                AetherUi.withAlpha(AetherUi.ACCENT, 0x55));
            drawAction("Delete", left + w - GuiScale.w(12) - GuiScale.w(64) - GuiScale.w(8) - GuiScale.w(64),
                top + (h - GuiScale.h(22)) / 2, AetherUi.withAlpha(AetherUi.WARN, 0x44));
        }

        private void drawAction(String label, int x, int y, int fill) {
            AetherUi.drawRoundRect(x, y, x + GuiScale.w(64), y + GuiScale.h(22), GuiScale.h(5), fill);
            AetherFont.drawCentered(AetherFont.Size.CAPTION, label, x, y + (GuiScale.h(22) - AetherFont.height(AetherFont.Size.CAPTION)) / 2,
                GuiScale.w(64), AetherUi.TEXT_PRIMARY);
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
            if (inApply(mx, my)) {
                boolean applied = client.profiles().apply(name, client.modules());
                status = applied ? "Profile '" + name + "' applied." : "Could not apply the profile.";
                statusAtMillis = System.currentTimeMillis();
                saveQuietly();
                return true;
            }
            if (inDelete(mx, my)) {
                client.profiles().delete(name);
                status = "Profile '" + name + "' deleted.";
                statusAtMillis = System.currentTimeMillis();
                saveQuietly();
                rebuildRows();
                return true;
            }
            return true;
        }
    }
}
