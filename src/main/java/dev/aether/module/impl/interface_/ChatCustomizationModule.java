package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ChatCustomizationModule extends AbstractModule {
    public ChatCustomizationModule() {
        super(ModuleMetadata.builder("interface.chat_customization", "Chat Customization")
            .category(ModuleCategory.INTERFACE)
            .description("Customizes chat readability and presentation.")
            .build());

        addBool("show_background", "Show Background", true);
        addBool("text_shadow", "Text Shadow", true);
        addBool("timestamps", "Timestamps", false);
        addNumber("opacity", "Opacity", 70);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("timestamp_color", "Timestamp Color", 0xFF52BEEB);
    }
}
