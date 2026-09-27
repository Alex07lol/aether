package dev.aether.module.impl.performance;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * The module switch behind the async screenshot pipeline: no framebuffer work happens while it is
 * off, and the notification behaviour is a module setting rather than a GUI special case.
 */
public class ScreenshotModule extends AbstractModule {
    public static final String ID = "performance.screenshot";

    public ScreenshotModule() {
        super(ModuleMetadata.builder(ID, "Screenshot")
            .category(ModuleCategory.PERFORMANCE)
            .description("Async screenshots: pixels leave the GPU on the render thread, encoding and disk writes happen on a worker thread.")
            .build());

        addChoice("notification", "After capture", "notification", "notification", "silent");
    }
}
