package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.platform.HudRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

final class ForgeHudEventBridge {
    private final AetherClient client;
    private final ForgeHudRenderer renderer;

    ForgeHudEventBridge(AetherClient client, ForgeHudRenderer renderer) {
        this.client = client;
        this.renderer = renderer;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            client.eventBus().publish(new HudRenderEvent(
                Mc189Compat.scaledWidth(event.resolution),
                Mc189Compat.scaledHeight(event.resolution),
                event.partialTicks
            ));
            boolean scoreboardCustom = client.modules().get("interface.scoreboard_customization").state() == ClientModule.ModuleState.ENABLED;
            Mc189Compat.setScoreboardDisabled(scoreboardCustom);
            renderer.render();
            if (scoreboardCustom) {
                renderer.renderScoreboard();
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            if (client.modules().get("interface.bossbar").state() == ClientModule.ModuleState.ENABLED) {
                event.setCanceled(true);
            }
        }
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            if (client.modules().get("graphics.custom_crosshair").state() == ClientModule.ModuleState.ENABLED) {
                event.setCanceled(true);
            }
        }
    }
}
