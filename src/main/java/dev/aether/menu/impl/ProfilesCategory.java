package dev.aether.menu.impl;

import java.util.List;

import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.menu.comp.UiTextBox;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;

/**
 * The Profiles category: one card per saved profile - 123x46, three per row - with
 * apply and delete icon buttons on the card, pagination over profiles (never over
 * individual widgets), and an add-card that slides in a creation scene with a name
 * field. Profiles are snapshots of the module configuration; the backend is untouched.
 */
public final class ProfilesCategory extends MenuCategory {

    private static final float CARD_W = 123.0F;
    private static final float CARD_H = 46.0F;
    private static final float STEP_X = 133.0F;
    private static final float STEP_Y = 56.0F;
    private static final float TOP = 36.0F;

    private final UiTextBox nameField = new UiTextBox(140.0F, 16.0F, "Profile name", new Runnable() {
        public void run() {
            createProfile();
        }
    });

    /** The creation scene slides over the grid, like the reference's detail views. */
    private boolean creating;
    private float createSlide;

    public ProfilesCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
    }

    @Override
    public String title() {
        return creating ? "New Profile" : "Profiles";
    }

    @Override
    public void onShow() {
        super.onShow();
        creating = false;
        createSlide = 0.0F;
        nameField.reset();
    }

    @Override
    public void update() {
        super.update();
        float target = creating ? 1.0F : 0.0F;
        createSlide += (target - createSlide) * Math.min(1.0F, 0.22F);
    }

    @Override
    public void draw(double mx, double my) {
        if (creating && createSlide > 0.001F) {
            drawCreateScene(mx, my);
        }
        if (!creating || createSlide < 0.999F) {
            drawGrid(mx, my);
        }
    }

    private void drawGrid(double mx, double my) {
        List<String> names = client.profiles().names();
        int columns = Math.max(1, (int) ((innerW() + (STEP_X - CARD_W)) / STEP_X));
        int rows = (names.size() + 1 + columns - 1) / columns; // +1: the add card
        setContentHeight(TOP + rows * STEP_Y + 10.0F);

        clipContent();
        try {
            // Add card first.
            float addX = innerX() + 15.0F;
            float addY = contentY() + TOP - scroll;
            boolean addHot = mx >= addX && mx < addX + CARD_W && my >= addY && my < addY + CARD_H;
            UiCanvas.roundRect(addX, addY, CARD_W, CARD_H, 6.0F,
                addHot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 230));
            UiCanvas.outline(addX, addY, CARD_W, CARD_H, 6.0F, UiTheme.withAlpha(UiTheme.accent(), 140), 1.0F);
            AetherFont.drawIcon(UiIcon.ADD, 16.0F,
                addX + (CARD_W - AetherFont.iconWidth(UiIcon.ADD, 16.0F)) / 2.0F,
                addY + (CARD_H - AetherFont.height(16.0F)) / 2.0F - 6.0F, UiTheme.accent());
            AetherFont.drawCentered(7.5F, "New profile", addX, addY + CARD_H - 18.0F, CARD_W, UiTheme.textSoft());

            for (int i = 0; i < names.size(); i++) {
                int slot = i + 1;
                int col = slot % columns;
                int row = slot / columns;
                float cardX = innerX() + 15.0F + col * STEP_X;
                float cardY = contentY() + TOP - scroll + row * STEP_Y;
                if (cardY > contentY() + contentH() || cardY + CARD_H < contentY()) {
                    continue;
                }
                drawProfileCard(names.get(i), cardX, cardY, mx, my);
            }
        } finally {
            UiCanvas.clearScissor();
        }

        int sheet = UiTheme.sheet();
        for (int i = 0; i < 6; i++) {
            int alpha = Math.round(255 * (1.0F - (i + 0.5F) / 6.0F));
            int band = UiTheme.withAlpha(sheet, alpha);
            UiCanvas.roundRect(contentX(), contentY() + i * 2.0F, contentW(), 2.0F, 0.0F, band);
            UiCanvas.roundRect(contentX(), contentY() + contentH() - (i + 1) * 2.0F, contentW(), 2.0F, 0.0F, band);
        }
    }

    private void drawProfileCard(String name, float x, float y, double mx, double my) {
        boolean hot = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
        UiCanvas.roundRect(x, y, CARD_W, CARD_H, 6.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 240));

        AetherFont.draw(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
            AetherFont.trim(10.0F, name, (int) (CARD_W - 46.0F)), x + 10.0F, y + 8.0F, UiTheme.text());
        AetherFont.draw(7.5F, client.profiles().enabledCount(name) + " modules",
            x + 10.0F, y + 24.0F, UiTheme.textFaint());

        // Apply + delete icon buttons on the card's right edge.
        boolean applyHot = mx >= x + CARD_W - 20.0F && mx < x + CARD_W - 4.0F && my >= y + 6.0F && my < y + 22.0F;
        boolean deleteHot = mx >= x + CARD_W - 20.0F && mx < x + CARD_W - 4.0F && my >= y + 25.0F && my < y + 41.0F;
        AetherFont.drawIcon(UiIcon.CHECK, 11.0F, x + CARD_W - 17.0F, y + 9.0F,
            applyHot ? UiTheme.accent() : UiTheme.textSoft());
        AetherFont.drawIcon(UiIcon.TRASH, 11.0F, x + CARD_W - 17.0F, y + 28.0F,
            deleteHot ? 0xFFFF7A6B : UiTheme.textSoft());
    }

    private void drawCreateScene(double mx, double my) {
        float slide = createSlide * createSlide * (3.0F - 2.0F * createSlide);
        float panelX = innerX() + (1.0F - slide) * 80.0F;
        float panelY = contentY() + 15.0F;
        float panelW = innerW();
        float panelH = contentH() - 30.0F;
        UiCanvas.roundRect(panelX, panelY, panelW, panelH, 10.0F, UiTheme.withAlpha(UiTheme.card(), 250));

        float backX = panelX + 10.0F;
        float backY = panelY + 8.0F;
        boolean backHot = mx >= backX - 3.0F && mx < backX + 16.0F && my >= backY - 3.0F && my < backY + 16.0F;
        AetherFont.drawIcon(UiIcon.CHEVRON_LEFT, 13.0F, backX, backY, backHot ? UiTheme.text() : UiTheme.textSoft());
        AetherFont.draw(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, "New profile",
            panelX + 30.0F, panelY + 10.0F, UiTheme.text());

        AetherFont.draw(10.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM, "Name",
            panelX + 26.0F, panelY + 60.0F, UiTheme.textSoft());
        nameField.draw(panelX + 26.0F, panelY + 76.0F, mx, my);

        // Create button: an accent pill at the field's right.
        float buttonX = panelX + 180.0F;
        float buttonY = panelY + 76.0F;
        boolean hot = mx >= buttonX && mx < buttonX + 75.0F && my >= buttonY && my < buttonY + 16.0F;
        UiCanvas.gradientRoundRect(buttonX, buttonY, 75.0F, 16.0F, 4.0F,
            UiTheme.withAlpha(UiTheme.accent(), hot ? 255 : 230),
            UiTheme.withAlpha(UiTheme.accentDeep(), hot ? 255 : 230));
        AetherFont.drawCentered(8.0F, "Create", buttonX, buttonY + (16.0F - AetherFont.height(8.0F)) / 2.0F,
            75.0F, UiTheme.readableOn(UiTheme.accent()));

        AetherFont.draw(7.5F, "A profile snapshots every module's enabled state and settings.",
            panelX + 26.0F, panelY + 104.0F, UiTheme.textFaint());
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return true;
        }
        if (creating && createSlide > 0.5F) {
            float panelX = innerX();
            float panelY = contentY() + 15.0F;
            if (mx >= panelX + 10.0F - 3.0F && mx < panelX + 26.0F && my >= panelY + 5.0F && my < panelY + 24.0F) {
                creating = false;
                return true;
            }
            if (nameField.click(panelX + 26.0F, panelY + 76.0F, mx, my, button)) {
                return true;
            }
            if (mx >= panelX + 180.0F && mx < panelX + 255.0F && my >= panelY + 76.0F && my < panelY + 92.0F) {
                createProfile();
                return true;
            }
            nameField.clickOutside();
            return true;
        }

        List<String> names = client.profiles().names();
        int columns = Math.max(1, (int) ((innerW() + (STEP_X - CARD_W)) / STEP_X));
        // Add card
        float addX = innerX() + 15.0F;
        float addY = contentY() + TOP - scroll;
        if (mx >= addX && mx < addX + CARD_W && my >= addY && my < addY + CARD_H) {
            creating = true;
            nameField.reset();
            return true;
        }
        for (int i = 0; i < names.size(); i++) {
            int slot = i + 1;
            int col = slot % columns;
            int row = slot / columns;
            float cardX = innerX() + 15.0F + col * STEP_X;
            float cardY = contentY() + TOP - scroll + row * STEP_Y;
            if (mx >= cardX && mx < cardX + CARD_W && my >= cardY && my < cardY + CARD_H) {
                String name = names.get(i);
                boolean applyZone = mx >= cardX + CARD_W - 20.0F && mx < cardX + CARD_W - 4.0F
                    && my >= cardY + 6.0F && my < cardY + 22.0F;
                boolean deleteZone = mx >= cardX + CARD_W - 20.0F && mx < cardX + CARD_W - 4.0F
                    && my >= cardY + 25.0F && my < cardY + 41.0F;
                if (applyZone) {
                    client.profiles().apply(name, client.modules());
                    saveQuietly();
                } else if (deleteZone) {
                    client.profiles().delete(name);
                    saveQuietly();
                } else {
                    client.profiles().apply(name, client.modules());
                    saveQuietly();
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (creating) {
            return nameField.key(typedChar, keyCode);
        }
        return false;
    }

    @Override
    public boolean capturesKeyboard() {
        return creating && nameField.focused();
    }

    @Override
    public boolean stepBack() {
        if (creating) {
            creating = false;
            return true;
        }
        if (nameField.focused()) {
            nameField.setFocused(false);
            return true;
        }
        return false;
    }

    private void createProfile() {
        String name = dev.aether.config.ProfileStore.sanitize(nameField.text());
        if (name.isEmpty()) {
            return;
        }
        client.profiles().save(name, client.modules());
        nameField.reset();
        creating = false;
        saveQuietly();
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    public void debugOpenCreate() {
        creating = true;
        nameField.reset();
    }

    public void debugTypeProfile(String name) {
        nameField.setText(name);
    }

    public void debugCreate() {
        createProfile();
    }

    public void debugBack() {
        stepBack();
    }
}
