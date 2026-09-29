package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.gui.AetherGui;
import dev.aether.gui.screens.AetherCosmeticScreen;
import dev.aether.gui.screens.AetherHudEditorScreen;
import dev.aether.gui.screens.AetherModScreen;
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
 *   <li>{@code debug-aether-cosmetics} - category list + player preview;</li>
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
        SHOOT_COSMETICS, OPEN_HUD, SHOOT_HUD, OPEN_CLIENT_SETTINGS, SHOOT_CLIENT_SETTINGS,
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
                        if (Display.getWidth() != targetWidth || Display.getHeight() != targetHeight) {
                            Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(targetWidth, targetHeight));
                            Display.setResizable(true);
                        }
                    } catch (org.lwjgl.LWJGLException failed) {
                        System.out.println("[AetherVisualDebug] resize failed: " + failed);
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
                    ((AetherModScreen) mc.currentScreen).debugScrollTo(220.0D);
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
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-module-settings");
                    open(mc, AetherGui.cosmetics(client), Step.SHOOT_COSMETICS);
                }
                break;

            case SHOOT_COSMETICS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics");
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

    private void open(Minecraft mc, net.minecraft.client.gui.GuiScreen screen, Step next) {
        System.out.println("[AetherVisualDebug] open " + screen.getClass().getSimpleName()
            + " -> step " + next + ", world=" + (mc.theWorld != null));
        mc.displayGuiScreen(screen);
        step = next;
        settleTicks = 30;
    }

    private static void shoot(Minecraft mc, String name) {
        try {
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, name,
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            System.out.println("[AetherVisualDebug] saved " + name);
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
