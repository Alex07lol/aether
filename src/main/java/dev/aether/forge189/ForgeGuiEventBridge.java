package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

final class ForgeGuiEventBridge {
    private final AetherClient client;

    ForgeGuiEventBridge(AetherClient client) {
        this.client = client;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu && !(event.gui instanceof AetherMainMenuScreen)) {
            event.gui = new AetherMainMenuScreen(client);
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Post event) {
        if (event.gui instanceof AetherModMenuScreen) {
            int dWheel = Mc189Compat.mouseWheelDelta();
            if (dWheel != 0) {
                ((AetherModMenuScreen) event.gui).handleWheelScroll(-dWheel / 8);
            }
        }
    }
}
