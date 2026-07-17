package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NotificationsModule extends AbstractModule {
    public NotificationsModule() {
        super(ModuleMetadata.builder("interface.notifications", "Notifications")
            .category(ModuleCategory.INTERFACE)
            .description("Adds a notification center utility slot.")
            .build());

        addBool("show_toasts", "Show Toasts", true);
    }
}
