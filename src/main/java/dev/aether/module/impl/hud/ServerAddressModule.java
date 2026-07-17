package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ServerAddressModule extends AbstractModule {
    public ServerAddressModule() {
        super(ModuleMetadata.builder("hud.server_address", "Server Address")
            .category(ModuleCategory.HUD)
            .description("Displays the current server address on the HUD.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
