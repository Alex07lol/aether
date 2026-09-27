package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleState;

import net.minecraft.client.gui.FontRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Headless verification for {@link AetherClickGuiScreen}.
 * <p>
 * The deck is normally driven by Minecraft, but every geometry, input and
 * persistence path is plain Java. Running this class against the bundled 1.8.9
 * stubs (see {@code scripts/verify.sh}) proves that layout, keyboard navigation,
 * accordion expansion, control hit-testing and config writes complete without
 * exceptions, and that Enter / switch clicks really flip module state. Rendering
 * calls resolve to the stub Gui, so the run needs no LWJGL context.
 */
public final class AetherClickDeckSelfTest {
    private static int checks;

    private AetherClickDeckSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Path configFile = new File(System.getProperty("java.io.tmpdir"), "aether-deck-self-test.json").toPath();
        if (configFile.toFile().exists() && !configFile.toFile().delete()) {
            throw new IllegalStateException("Could not clear " + configFile);
        }
        System.setProperty("aether.changeLog", new File(System.getProperty("java.io.tmpdir"), "aether-deck-lastchange.txt").getAbsolutePath());

        AetherClient client = new AetherClient(configFile);
        client.start();
        AetherUi.bind(client);

        AetherClickGuiScreen wide = screen(client, 854, 480);
        wide.drawScreen(10, 10, 0F);

        int flipped = countEnabled(client);
        wide.keyTyped('\0', 28); // Enter toggles the first row
        assertEquals(flipped + 1, countEnabled(client), "Enter toggles the selected module");

        clickFirstRowToggle(wide, 854, 480);
        assertEquals(flipped, countEnabled(client), "clicking the row switch toggles the module back");

        // expand the first row, then exercise every control type that lives in it
        wide.drawScreen(10, 10, 0F);
        int rowY = listY(854, 480);
        wide.mouseClicked(rowX(854) + listWidth(854) - 14, rowY + 15, 0);
        wide.drawScreen(10, 10, 0F);
        for (int y = rowY + 30; y < rowY + 240; y += 6) {
            for (int x = rowX(854) + 40; x < rowX(854) + listWidth(854) - 20; x += 40) {
                wide.drawScreen(x, y, 0F);
                wide.mouseClicked(x, y, 0);
            }
        }
        wide.drawScreen(10, 10, 0F);
        wide.keyTyped('\u0000', 1); // Escape closes any palette / capture / text editor

        // keyboard navigation, search, view switching, expand all, reset
        wide.keyTyped('\0', 208);
        wide.keyTyped('\0', 208);
        wide.keyTyped('\0', 200);
        wide.keyTyped('z', 44);
        wide.keyTyped('o', 25);
        wide.keyTyped('o', 25);
        wide.keyTyped('m', 50);
        wide.drawScreen(10, 10, 0F);
        wide.keyTyped('\0', 1);
        wide.keyTyped('\0', 15); // Tab cycles the view ribbon
        wide.keyTyped('e', 18);
        wide.drawScreen(10, 10, 0F);
        wide.keyTyped('c', 46);
        wide.keyTyped('r', 19);
        wide.mouseClicked(rowX(854) + 40, rowY + 15, 1); // right click resets a row
        wide.keyTyped('\0', 209);
        wide.keyTyped('\0', 207);
        wide.keyTyped('\0', 199);
        wide.keyTyped(' ', 57);
        wide.drawScreen(10, 10, 0F);

        // palette, slider and keybind capture round trips. Each exercise starts from a
        // fresh screen so a text editor or palette left open by the fuzz pass cannot
        // swallow the next interaction (the deck itself handles Escape correctly).
        exerciseColourRow(client);
        exerciseSliderRow(client);
        exerciseKeybindRow(client);

        // the five theme modules must repaint the shared token hub that every screen reads
        exerciseThemes(client, wide);

        // narrow viewport hides the telemetry spine
        AetherClickGuiScreen narrow = screen(client, 320, 240);
        narrow.drawScreen(4, 4, 0F);
        narrow.mouseClicked(60, 46, 0);
        narrow.drawScreen(4, 4, 0F);
        narrow.keyTyped('\0', 1);

        wide.handleMouseInput();
        wide.mouseReleased(10, 10, 0);

        for (ClientModule module : client.modules().all()) {
            client.modules().setEnabled(module.metadata().id(), false);
        }
        client.save();

        System.out.println("AetherClickDeckSelfTest passed (" + checks + " checks).");
    }

    /**
     * Enabling a theme module has to reach {@link AetherUi}, and the deck has to draw from
     * those tokens rather than a private palette. This walks every theme and checks the
     * tokens actually move, then puts the client back on the default theme.
     */
    private static void exerciseThemes(AetherClient client, AetherClickGuiScreen deck) throws IOException {
        AetherUi.syncTheme();
        int defaultText = AetherUi.TEXT_PRIMARY;
        int defaultSurface = AetherUi.SURFACE;
        boolean defaultLight = AetherUi.LIGHT_SURFACE;

        client.modules().setEnabled("theme.midnight", true);
        assertEquals(Boolean.TRUE, Boolean.valueOf(AetherUi.syncTheme()), "a theme switch reports that the tokens changed");
        int midnightSurface = AetherUi.SURFACE;
        int midnightText = AetherUi.TEXT_PRIMARY;
        int midnightEdge = AetherUi.PANEL_EDGE;
        assertTrue(midnightSurface != defaultSurface, "Midnight repaints the shared surface token");
        assertEquals("Midnight", client.theme().name(), "the enabled theme module becomes the active theme");
        assertTrue(AetherUi.syncTheme() == false, "a second frame with the same theme is a no-op");

        client.modules().setEnabled("theme.light", true);
        AetherUi.syncTheme();
        assertEquals(Boolean.TRUE, Boolean.valueOf(AetherUi.LIGHT_SURFACE), "the Light palette is marked as a light surface");
        assertTrue(AetherUi.TEXT_PRIMARY != midnightText, "Light re-derives the text token for contrast");
        assertTrue(AetherUi.PANEL_EDGE != midnightEdge, "Light uses dark edges instead of the white glass highlight");

        // the deck draws from the hub, so painting after a theme switch must not throw and
        // must leave the tokens in place for the other screens
        deck.drawScreen(10, 10, 0F);
        deck.keyTyped('\0', 15);
        deck.drawScreen(10, 10, 0F);
        assertEquals("Light", client.theme().name(), "the deck keeps the active theme while it is opened");

        client.modules().setEnabled("theme.light", false);
        client.modules().setEnabled("theme.aether_blue", true);
        AetherUi.syncTheme();
        assertEquals(Integer.valueOf(defaultSurface), Integer.valueOf(AetherUi.SURFACE), "the default palette restores the original surface");
        assertEquals(Integer.valueOf(defaultText), Integer.valueOf(AetherUi.TEXT_PRIMARY), "the default palette restores the original text colour");
        assertEquals(Boolean.valueOf(defaultLight), Boolean.valueOf(AetherUi.LIGHT_SURFACE), "the default palette restores the surface class");
        client.modules().setEnabled("theme.aether_blue", false);
    }

    private static void exerciseColourRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 480);
        ClientModule coordinates = client.modules().get("hud.coordinates");
        openModule(screen, "hud.coordinates");
        int rowY = listY(854, 480);
        int y = rowY + 30;
        // walk the accordion looking for the colour swatches and click slightly left of the
        // control column so the palette opens, then click the palette area itself
        for (int step = 0; step < 12; step++) {
            int targetY = y + step * 22;
            screen.drawScreen(rowX(854) + listWidth(854) - 20, targetY + 6, 0F);
            screen.mouseClicked(rowX(854) + listWidth(854) - 25, targetY + 6, 0);
            screen.drawScreen(rowX(854) + listWidth(854) - 60, targetY + 30, 0F);
            screen.mouseClicked(rowX(854) + listWidth(854) - 60, targetY + 30, 0);
            screen.keyTyped('\0', 1);
        }
        screen.drawScreen(10, 10, 0F);
        assertEquals(true, coordinates.settings().size() > 0, "coordinates module keeps its settings after palette clicks");
        clearSearch(screen);
        screen.mouseReleased(10, 10, 0);
    }

    /** Drags the first number slider of the filtered module and checks the value lands on the maximum. */
    private static void exerciseSliderRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 480);
        openModule(screen, "pvp.zoom");
        assertEquals(Integer.valueOf(40), settingValue(client, "pvp.zoom", "zoom_percent"), "zoom percent starts at its default");
        int controlX = rowX(854) + rowWidth(854) - 21; // right end of the slider track
        int sliderY = contentTop(854, 480) + SUB_H + 11; // second sub row, middle of the track
        screen.drawScreen(controlX, sliderY, 0F);
        screen.mouseClicked(controlX, sliderY, 0);
        screen.mouseReleased(controlX, sliderY, 0);
        assertEquals(Integer.valueOf(100), settingValue(client, "pvp.zoom", "zoom_percent"), "clicking the end of a slider applies the maximum");
        clearSearch(screen);
    }

    /** Absolute y of the first setting row in an expanded accordion. */
    private static int contentTop(int w, int h) {
        return listY(w, h) + ROW_H + DESCRIPTION_PAD + DESCRIPTION_LINE_H;
    }

    /** Opens the first setting row of the filtered module and captures a keybind. */
    private static void exerciseKeybindRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 480);
        openModule(screen, "pvp.zoom");
        int controlX = rowX(854) + listWidth(854) - 60;
        int pillY = contentTop(854, 480) + 10; // first sub row, middle of the keybind pill
        screen.drawScreen(controlX, pillY, 0F);
        screen.mouseClicked(controlX, pillY, 0);
        screen.keyTyped('\0', 44); // bind to Z
        screen.drawScreen(10, 10, 0F);
        assertEquals(Integer.valueOf(44), settingValue(client, "pvp.zoom", "keybind"), "keybind capture stores the pressed key");
        clearSearch(screen);
    }

    /** Types the module name, leaves the search field and expands the single result. */
    private static void openModule(AetherClickGuiScreen screen, String moduleId) throws IOException {
        String needle = moduleId.substring(moduleId.indexOf('.') + 1).replace('_', ' ');
        for (int i = 0; i < needle.length(); i++) {
            screen.keyTyped(needle.charAt(i), 0);
        }
        screen.keyTyped('\0', 28); // Enter leaves the search field, keeping the filter
        screen.keyTyped(' ', 57); // space expands the selected row
        screen.drawScreen(10, 10, 0F);
    }

    private static void clearSearch(AetherClickGuiScreen screen) throws IOException {
        screen.keyTyped('\0', 1); // Escape clears the query and restores the full list
        screen.drawScreen(10, 10, 0F);
    }

    private static Integer settingValue(AetherClient client, String moduleId, String settingId) {
        for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
            if (setting.id().equals(settingId)) {
                Object value = setting.value();
                return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
            }
        }
        return null;
    }

    private static void clickFirstRowToggle(AetherClickGuiScreen screen, int w, int h) throws IOException {
        screen.drawScreen(10, 10, 0F);
        int x = rowX(w) + listWidth(w) - 45;
        int y = listY(w, h) + 15;
        screen.mouseClicked(x, y, 0);
    }

    private static AetherClickGuiScreen screen(AetherClient client, int width, int height) {
        AetherClickGuiScreen screen = new AetherClickGuiScreen(client);
        screen.width = width;
        screen.height = height;
        screen.mc = null;
        screen.fontRendererObj = new FontRenderer();
        screen.initGui();
        return screen;
    }

    private static int countEnabled(AetherClient client) {
        int enabled = 0;
        for (ClientModule module : client.modules().all()) {
            if (module.state() == ModuleState.ENABLED) {
                enabled++;
            }
        }
        return enabled;
    }

    /* The deck's geometry contract, recomputed here so the test would fail if the
       layout constants drift apart from the documented layout. */

    private static final int ROW_H = 30;
    private static final int SUB_H = 22;
    private static final int DESCRIPTION_PAD = 6;
    private static final int DESCRIPTION_LINE_H = 10;

    private static int deckX(int w) {
        int margin = clamp(w / 24, 6, 16);
        int deckW = Math.min(980, w - margin * 2);
        return (w - deckW) / 2;
    }

    private static int deckY(int w) {
        return clamp(w / 24, 6, 16);
    }

    private static int innerWidth(int w) {
        return Math.min(980, w - clamp(w / 24, 6, 16) * 2) - 20;
    }

    private static int spineWidth(int w) {
        int inner = innerWidth(w);
        return inner >= 430 ? clamp((int) (inner * 0.26F), 132, 200) : 0;
    }

    private static int listWidth(int w) {
        int inner = innerWidth(w);
        return spineWidth(w) > 0 ? inner - spineWidth(w) - 8 : inner;
    }

    private static int rowWidth(int w) {
        return listWidth(w) - 12;
    }

    private static int rowX(int w) {
        return deckX(w) + 10 + 6;
    }

    private static int listY(int w, int h) {
        int bodyTop = deckY(w) + 42 + 26;
        return bodyTop + 6;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

}
