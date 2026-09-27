package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Prefixes incoming chat with a coloured timestamp.
 * <p>
 * The colour is picked from the full ARGB palette in the GUI and mapped to the closest of
 * Minecraft's sixteen legacy chat colours when the line is rewritten, because 1.8 chat cannot
 * render a custom RGB value.
 */
public class ChatCustomizationModule extends AbstractModule {
    public static final String ID = "interface.chat_customization";

    public ChatCustomizationModule() {
        super(ModuleMetadata.builder(ID, "Chat Timestamps")
            .category(ModuleCategory.INTERFACE)
            .description("Adds a coloured timestamp to every incoming chat line.")
            .build());

        addBool("timestamps", "Timestamps", true);
        addColor("timestamp_color", "Timestamp Color", 0xFF52BEEB);
        addBool("twenty_four_hour", "24 Hour Clock", true);
    }
}
