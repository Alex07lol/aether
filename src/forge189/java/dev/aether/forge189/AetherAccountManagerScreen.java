package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AetherAccountManagerScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final AetherClient client;
    private final GuiScreen parent;
    private final List<AetherButton> buttons = new ArrayList<AetherButton>();
    private String notice = "Launcher session detected.";
    private long noticeUntil = System.currentTimeMillis() + 2000L;

    public AetherAccountManagerScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    protected void mouseClicked(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void func_73864_a(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean func_73868_f() {
        return false;
    }

    private void render(int mouseX, int mouseY) {
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);
        buttons.clear();

        Mc189Compat.drawRect(0, 0, width, height, 0x660B0E14);
        int panelWidth = clamp(width - 28, 300, 430);
        int panelHeight = 184;
        int left = width / 2 - panelWidth / 2;
        int top = height / 2 - panelHeight / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        Mc189Compat.drawRect(left, top, right, bottom, AetherUi.COLOR_PANEL);
        AetherUi.centered(font, "Account Manager", left, top + 14, panelWidth, AetherUi.COLOR_TEXT_PRIMARY);
        AetherUi.avatar(font, left + 20, top + 42, Mc189Compat.username());
        AetherUi.text(font, "Current account", left + 56, top + 43, AetherUi.COLOR_TEXT_SECONDARY);
        AetherUi.text(font, AetherUi.trim(font, Mc189Compat.username(), panelWidth - 90), left + 56, top + 57, AetherUi.COLOR_TEXT_PRIMARY);

        int y = top + 84;
        drawLine(font, left + 20, y, "Session source", "Minecraft launcher / active profile", panelWidth);
        y += 15;
        drawLine(font, left + 20, y, "Client version", client.version().name(), panelWidth);
        y += 15;
        drawLine(font, left + 20, y, "Config", client.configFile().toString(), panelWidth);
        y += 15;
        drawLine(font, left + 20, y, "Checked", new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date()), panelWidth);

        int buttonTop = bottom - 34;
        addButton("Back", left + 18, buttonTop, 70, 22, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(parent);
            }
        });
        addButton("Mods", left + 96, buttonTop, 70, 22, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(new AetherModMenuScreen(client, AetherAccountManagerScreen.this));
            }
        });
        addButton("Save", left + 174, buttonTop, 70, 22, new ScreenAction() {
            public void run() {
                save();
            }
        });

        for (AetherButton button : buttons) {
            drawModernButton(font, button, mouseX, mouseY);
        }
        drawNotice(font, right, top);
    }

    private void drawModernButton(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int fill = hover ? AetherUi.COLOR_CARD_HOVER : AetherUi.COLOR_CARD;
        int text = hover ? AetherUi.COLOR_TEXT_PRIMARY : AetherUi.COLOR_TEXT_SECONDARY;
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
        AetherUi.centered(font, button.label(), button.x(), button.y() + (button.height() - 8) / 2, button.width(), text);
    }

    private void drawLine(Object font, int x, int y, String label, String value, int panelWidth) {
        AetherUi.text(font, label, x, y, AetherUi.COLOR_TEXT_SECONDARY);
        AetherUi.text(font, AetherUi.trim(font, value, panelWidth - 150), x + 108, y, AetherUi.COLOR_TEXT_PRIMARY);
    }

    private void addButton(String label, int x, int y, int width, int height, ScreenAction action) {
        buttons.add(new AetherButton(label, x, y, width, height, action));
    }

    private void click(int mouseX, int mouseY, int clickedButton) {
        if (clickedButton != 0) {
            return;
        }
        for (AetherButton button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                button.click();
                return;
            }
        }
    }

    private void save() {
        try {
            client.save();
            notify("Account profile saved.");
        } catch (IOException exception) {
            notify("Save failed.");
        }
    }

    private void notify(String message) {
        notice = message;
        noticeUntil = System.currentTimeMillis() + 2200L;
    }

    private void drawNotice(Object font, int panelRight, int panelTop) {
        if (notice == null || System.currentTimeMillis() > noticeUntil) {
            return;
        }
        int width = Math.min(210, Math.max(116, Mc189Compat.stringWidth(font, notice) + 18));
        int left = panelRight - width - 14;
        int top = panelTop + 42;
        Mc189Compat.drawRect(left, top, left + width, top + 20, AetherUi.COLOR_PANEL);
        Mc189Compat.drawRect(left, top, left + 2, top + 20, AetherUi.MODERN_UI_ACCENT);
        AetherUi.text(font, AetherUi.trim(font, notice, width - 12), left + 7, top + 7, AetherUi.COLOR_TEXT_PRIMARY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
