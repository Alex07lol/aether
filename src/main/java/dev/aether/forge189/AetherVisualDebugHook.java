package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.gui.AetherGui;
import dev.aether.gui.screens.AetherCosmeticScreen;
import dev.aether.gui.screens.AetherHudEditorScreen;
import dev.aether.gui.screens.AetherModScreen;
import dev.aether.gui.screens.AetherThemesScreen;
import dev.aether.gui.screens.AetherClientSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.Display;

import java.io.File;

/**
 * TEMPORARY dev-only visual debugger, active only when the JVM property
 * {@code aether.debugShots} is set (runClient passes it with
 * {@code -PaetherDebugShots=<value>}).
 * <p>
 * The value is either {@code true} (window keeps its current size) or a
 * {@code WIDTHxHEIGHT} pair such as {@code 1920x1080}: the hook then resizes the
 * window on the first tick, which is how the GUI is verified at several resolutions.
 * It drives the client through every new screen and saves real screenshots into
 * {@code run/screenshots}:
 * <ol>
 *   <li>{@code debug-aether-mainmenu};</li>
 *   <li>{@code debug-aether-modules} - the Modules screen;</li>
 *   <li>{@code debug-aether-modules-scrolled} - after scrolling the card list;</li>
 *   <li>{@code debug-aether-module-settings} - one module's configuration panel;</li>
 *   <li>{@code debug-aether-cosmetics} - the cosmetics gallery: filters, search, cards, preview;</li>
 *   <li>{@code debug-aether-cosmetics-scrolled} - the gallery after one scroll;</li>
 *   <li>{@code debug-aether-cosmetics-import} - the import popover open;</li>
 *   <li>{@code debug-aether-themes} - the theme picker as it opens, no palette equipped;</li>
 *   <li>{@code debug-aether-themes-equipped} - after selecting a theme, so the equipped pill and
 *       the palette it applies are in the shot;</li>
 *   <li>{@code debug-aether-themes-scrolled} - the same screen on its second page;</li>
 *   <li>{@code debug-aether-hud} - the HUD editor;</li>
 *   <li>{@code debug-aether-settings} - the client settings screen.</li>
 * </ol>
 * Each step fires the moment its screen is visible (after a settle delay), and the
 * sequence finishes with a marker file so an outside observer can pick the images up.
 * Remove once the UI renders as designed.
 */
public final class AetherVisualDebugHook {

    private enum Step {
        RESIZE, WAIT_MENU, SHOOT_MENU, OPEN_MODULES, SHOOT_MODULES, SCROLL_MODULES,
        SHOOT_SCROLLED, OPEN_SETTINGS_PANEL, SHOOT_MODULE_SETTINGS, OPEN_COSMETICS,
        SHOOT_COSMETICS, SCROLL_COSMETICS, SHOOT_COSMETICS_SCROLLED, SHOOT_COSMETICS_IMPORT,
        SHOOT_THEMES_PAGE, SELECT_THEME, SHOOT_THEMES_EQUIPPED, SCROLL_THEMES,
        SHOOT_THEMES_SCROLLED, OPEN_HUD, SHOOT_HUD, OPEN_CLIENT_SETTINGS, SHOOT_CLIENT_SETTINGS,
        LOAD_WORLD, WAIT_WORLD, SHOOT_PREVIEW, DONE
    }

    private final AetherClient client;
    private final int targetWidth;
    private final int targetHeight;
    private Step step = Step.RESIZE;
    private int settleTicks;
    private int idleTicks;

    public AetherVisualDebugHook(AetherClient client) {
        this.client = client;
        String value = System.getProperty("aether.debugShots", "true");
        int width = 0;
        int height = 0;
        int x = value.toLowerCase().indexOf('x');
        if (x > 0 && x < value.length() - 1) {
            try {
                width = Integer.parseInt(value.substring(0, x).trim());
                height = Integer.parseInt(value.substring(x + 1).trim());
            } catch (NumberFormatException malformed) {
                width = 0;
                height = 0;
            }
        }
        this.targetWidth = width >= 320 ? width : 0;
        this.targetHeight = height >= 240 ? height : 0;
        System.out.println("[AetherVisualDebug] enabled, resolution target: "
            + (targetWidth > 0 ? targetWidth + "x" + targetHeight : "current window"));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || step == Step.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();

        switch (step) {
            case RESIZE:
                if (targetWidth > 0) {
                    try {
                        Display.setResizable(true);
                        if (Display.getWidth() != targetWidth || Display.getHeight() != targetHeight) {
                            Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(targetWidth, targetHeight));
                        }
                    } catch (org.lwjgl.LWJGLException failed) {
                        System.out.println("[AetherVisualDebug] window resize failed: " + failed);
                    }
                    // A windowed LWJGL window can ignore setDisplayMode, and Minecraft only adopts
                    // Display's size when Display.wasResized() is set - so tell it outright. The
                    // screenshots come from the framebuffer, which is what this resizes, and a
                    // framebuffer that is not the window size is exactly what makes the saved
                    // images comparable between machines.
                    try {
                        mc.resize(targetWidth, targetHeight);
                        System.out.println("[AetherVisualDebug] framebuffer " + mc.displayWidth
                            + "x" + mc.displayHeight);
                    } catch (Throwable failed) {
                        System.out.println("[AetherVisualDebug] framebuffer resize failed: " + failed);
                    }
                }
                step = Step.WAIT_MENU;
                settleTicks = 60;
                break;

            case WAIT_MENU:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherMainMenuScreen) {
                    step = Step.SHOOT_MENU;
                    settleTicks = 30;
                }
                break;

            case SHOOT_MENU:
                if (--settleTicks <= 0) {
                    shoot(mc, "debug-aether-mainmenu");
                    open(mc, AetherGui.modules(client), Step.SHOOT_MODULES);
                }
                break;

            case SHOOT_MODULES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules");
                    step = Step.SCROLL_MODULES;
                    settleTicks = 10;
                }
                break;

            case SCROLL_MODULES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    ((AetherModScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_SCROLLED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules-scrolled");
                    ((AetherModScreen) mc.currentScreen).debugOpenFirstModuleSettings();
                    step = Step.SHOOT_MODULE_SETTINGS;
                    settleTicks = 20;
                }
                break;

            case SHOOT_MODULE_SETTINGS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof dev.aether.gui.screens.AetherModuleSettingsScreen) {
                    shoot(mc, "debug-aether-module-settings");
                    open(mc, AetherGui.cosmetics(client), Step.SHOOT_COSMETICS);
                }
                break;

            case SHOOT_COSMETICS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics");
                    step = Step.SCROLL_COSMETICS;
                    settleTicks = 10;
                }
                break;

            case SCROLL_COSMETICS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    ((AetherCosmeticScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_COSMETICS_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_SCROLLED:
                // One viewport down: proves the gallery scrolls and the thumb moves.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-scrolled");
                    ((AetherCosmeticScreen) mc.currentScreen).debugToggleImportMenu();
                    step = Step.SHOOT_COSMETICS_IMPORT;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_IMPORT:
                // The import popover: open folder / type folder / drop import / choose file.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-import");
                    ((AetherCosmeticScreen) mc.currentScreen).debugToggleImportMenu();
                    resetThemes();
                    open(mc, AetherGui.themes(client), Step.SHOOT_THEMES_PAGE);
                }
                break;

            case SHOOT_THEMES_PAGE:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherThemesScreen) {
                    shoot(mc, "debug-aether-themes");
                    step = Step.SELECT_THEME;
                    settleTicks = 10;
                }
                break;

            case SELECT_THEME:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherThemesScreen) {
                    ((AetherThemesScreen) mc.currentScreen).debugSelect(0);
                    step = Step.SHOOT_THEMES_EQUIPPED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_THEMES_EQUIPPED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherThemesScreen) {
                    shoot(mc, "debug-aether-themes-equipped");
                    step = Step.SCROLL_THEMES;
                    settleTicks = 10;
                }
                break;

            case SCROLL_THEMES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherThemesScreen) {
                    ((AetherThemesScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_THEMES_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_THEMES_SCROLLED:
                // Page two: proves the pager works and moves the scrollbar thumb down its track.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherThemesScreen) {
                    shoot(mc, "debug-aether-themes-scrolled");
                    open(mc, AetherGui.hudEditor(client), Step.SHOOT_HUD);
                }
                break;

            case SHOOT_HUD:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherHudEditorScreen) {
                    shoot(mc, "debug-aether-hud");
                    open(mc, AetherGui.settings(client), Step.SHOOT_CLIENT_SETTINGS);
                }
                break;

            case SHOOT_CLIENT_SETTINGS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherClientSettingsScreen) {
                    shoot(mc, "debug-aether-settings");
                    step = Step.LOAD_WORLD;
                    settleTicks = 10;
                }
                break;

            case LOAD_WORLD:
                if (--settleTicks <= 0) {
                    try {
                        // Loads the existing singleplayer save so the player preview has
                        // a real player to render.
                        mc.launchIntegratedServer("New World", "New World", null);
                        step = Step.WAIT_WORLD;
                        settleTicks = 400; // up to 20s for the server to come up
                    } catch (Throwable failed) {
                        System.out.println("[AetherVisualDebug] world load failed: " + failed);
                        step = Step.DONE;
                        finish(mc);
                    }
                }
                break;

            case WAIT_WORLD:
                if (mc.theWorld != null && mc.thePlayer != null) {
                    // Let the terrain finish its initial upload burst before opening the
                    // GUI over the world - chunk uploads at first join are flaky here.
                    if (--settleTicks <= 0) {
                        // Park the cursor right of centre so the preview turns and the
                        // equipped cape (worn on the back) comes into view, like a real
                        // user would see by moving the mouse.
                        try {
                            org.lwjgl.input.Mouse.setCursorPosition(
                                Display.getX() + Display.getWidth() / 2 + 300,
                                Display.getY() + Display.getHeight() / 2 - 40);
                        } catch (Throwable ignored) {
                        }
                        open(mc, AetherGui.cosmetics(client), Step.SHOOT_PREVIEW);
                    }
                } else if (--settleTicks <= 0) {
                    // The server never came up - give up gracefully.
                    System.out.println("[AetherVisualDebug] world never loaded, skipping preview");
                    step = Step.DONE;
                    finish(mc);
                }
                break;

            case SHOOT_PREVIEW:
                if (mc.currentScreen instanceof AetherCosmeticScreen) {
                    if (--settleTicks <= 0) {
                        shoot(mc, "debug-aether-preview");
                        step = Step.DONE;
                        try {
                            if (mc.theWorld != null) {
                                mc.theWorld.sendQuittingDisconnectingPacket();
                                mc.loadWorld(null);
                            }
                        } catch (Throwable ignored) {
                        }
                        finish(mc);
                    }
                } else if (--settleTicks <= 0) {
                    // The focus-loss pause menu (or the world join flow) replaced the
                    // screen; put the preview back up and try once more.
                    System.out.println("[AetherVisualDebug] preview screen gone (current: "
                        + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName())
                        + "), reopening");
                    open(mc, AetherGui.cosmetics(client), Step.SHOOT_PREVIEW);
                }
                break;

            default:
                break;
        }

        // Heartbeat: proves the tick handler is alive and says what it sees.
        if (idleTicks % 100 == 0) {
            System.out.println("[AetherVisualDebug] tick " + idleTicks + " step=" + step
                + " world=" + (mc.theWorld != null) + " screen="
                + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName()));
        }

        // Safety: give up after ~2.5 minutes of nothing so a blocked step cannot spin forever.
        idleTicks++;
        if (idleTicks > 3000) {
            step = Step.DONE;
            finish(mc);
        }
    }

    /**
     * Puts the theme picker back on the default palette before its shots. The client saves its
     * configuration, so without this the picker's "before" and "after" images would depend on what
     * the previous run happened to select.
     */
    private void resetThemes() {
        int reset = 0;
        for (dev.aether.module.ClientModule module : client.modules().all()) {
            if (module instanceof dev.aether.theme.ThemeModule
                && module.state() == dev.aether.module.ClientModule.ModuleState.ENABLED) {
                client.modules().setEnabled(module.metadata().id(), false);
                reset++;
            }
        }
        System.out.println("[AetherVisualDebug] reset " + reset + " theme(s) to the default palette");
    }

    private void open(Minecraft mc, net.minecraft.client.gui.GuiScreen screen, Step next) {
        System.out.println("[AetherVisualDebug] open " + screen.getClass().getSimpleName()
            + " -> step " + next + ", world=" + (mc.theWorld != null));
        mc.displayGuiScreen(screen);
        step = next;
        settleTicks = 30;
    }

    private static void shoot(Minecraft mc, String name) {
        try {
            // ScreenShotHelper writes the name verbatim, so the extension is added here: these
            // files are meant to be opened and looked at.
            String file = name.endsWith(".png") ? name : name + ".png";
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, file,
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            System.out.println("[AetherVisualDebug] saved " + file);
        } catch (Exception exception) {
            System.out.println("[AetherVisualDebug] screenshot failed: " + exception);
        }
    }

    private static void finish(Minecraft mc) {
        try {
            File done = new File(mc.mcDataDir, "aether-debug-shots-done.txt");
            if (!done.exists()) {
                done.createNewFile();
            }
            System.out.println("[AetherVisualDebug] sequence complete");
        } catch (Exception ignored) {
        }
    }
}
