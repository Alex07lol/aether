package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * TEMPORARY dev-only visual debugger, active only when the JVM property
 * {@code aether.debugShots} is set (runClient passes it with
 * {@code -PaetherDebugShots=true}). Drives the client through the screens that need
 * visual verification and saves real screenshots into run/screenshots:
 * <ol>
 *   <li>{@code debug-aether-mainmenu} - the Aether main menu;</li>
 *   <li>{@code debug-aether-clickgui} - the Control Center as it opens;</li>
 *   <li>{@code debug-aether-clickgui-scrolled} - the same deck after scrolling the
 *       module list, proving the scroll pipeline end to end.</li>
 * </ol>
 * Each step fires the moment its screen is visible (after a settle delay for the first
 * frame) and the sequence finishes with a marker file so an outside observer can pick
 * the images up. Remove once the UI renders as designed.
 */
public final class AetherVisualDebugHook {

    private enum Step {
        WAIT_MENU, SHOOT_MENU, OPEN_DECK, SHOOT_DECK, SCROLL_DECK, SHOOT_SCROLLED, DONE
    }

    private final AetherClient client;
    private Step step = Step.WAIT_MENU;
    private int settleTicks;
    private int idleTicks;

    public AetherVisualDebugHook(AetherClient client) {
        this.client = client;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || step == Step.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            return; // only drive the menu flow
        }

        switch (step) {
            case WAIT_MENU:
                if (mc.currentScreen instanceof AetherMainMenuScreen) {
                    step = Step.SHOOT_MENU;
                    settleTicks = 30;
                }
                break;

            case SHOOT_MENU:
                if (--settleTicks <= 0) {
                    shoot(mc, "debug-aether-mainmenu");
                    step = Step.OPEN_DECK;
                    settleTicks = 20;
                }
                break;

            case OPEN_DECK:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherMainMenuScreen) {
                    mc.displayGuiScreen(new AetherClickGuiScreen(client, (AetherMainMenuScreen) mc.currentScreen));
                    step = Step.SHOOT_DECK;
                    settleTicks = 30;
                }
                break;

            case SHOOT_DECK:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherClickGuiScreen) {
                    shoot(mc, "debug-aether-clickgui");
                    step = Step.SCROLL_DECK;
                    settleTicks = 10;
                }
                break;

            case SCROLL_DECK:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherClickGuiScreen) {
                    ((AetherClickGuiScreen) mc.currentScreen).setScroll(140f);
                    step = Step.SHOOT_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_SCROLLED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherClickGuiScreen) {
                    shoot(mc, "debug-aether-clickgui-scrolled");
                    step = Step.DONE;
                    finish(mc);
                }
                break;

            default:
                break;
        }

        // Safety: give up after ~30s of nothing so a blocked step cannot spin forever.
        idleTicks++;
        if (idleTicks > 600) {
            step = Step.DONE;
            finish(mc);
        }
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
