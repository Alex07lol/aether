package dev.aether.forge189;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

final class ForgeKeyBindings {
    private static final int KEY_F12 = 88;
    private static final int KEY_RIGHT_SHIFT = 54;

    private final KeyBinding developerOverlay = new KeyBinding("key.aether.developer_overlay", KEY_F12, ForgeAetherConstants.KEY_CATEGORY);
    private final KeyBinding modMenu = new KeyBinding("key.aether.mod_menu", KEY_RIGHT_SHIFT, ForgeAetherConstants.KEY_CATEGORY);

    void register() {
        ClientRegistry.registerKeyBinding(developerOverlay);
        ClientRegistry.registerKeyBinding(modMenu);
    }

    KeyBinding developerOverlay() {
        return developerOverlay;
    }

    KeyBinding modMenu() {
        return modMenu;
    }
}
