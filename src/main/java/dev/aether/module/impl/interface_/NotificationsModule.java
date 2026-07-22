package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NotificationsModule extends AbstractModule {
    public NotificationsModule() {
        super(ModuleMetadata.builder("interface.notifications", "Notifications")
            .category(ModuleCategory.INTERFACE)
            .description("Displays in-game toast notifications for events, messages, and module state changes.")
            .build());

        addBool("show_toasts", "Show Toasts", true);
        addNumber("display_time", "Display Time", 3);
        addNumber("max_notifications", "Max Notifications", 5);
    }
}
