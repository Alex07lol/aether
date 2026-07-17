package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ScrollTooltipsModule extends AbstractModule {
    public ScrollTooltipsModule() {
        super(ModuleMetadata.builder("interface.scroll_tooltips", "Scroll Tooltips")
            .category(ModuleCategory.INTERFACE)
            .description("Allows scrolling within item tooltips.")
            .build());
    }
}
