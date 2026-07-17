package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FpsGraphModule extends AbstractModule {
    public FpsGraphModule() {
        super(ModuleMetadata.builder("hud.fps_graph", "FPS Sparkline Graph")
            .category(ModuleCategory.HUD)
            .description("Renders a live frame-time graph on your HUD showing FPS stability and drops.")
            .favoriteByDefault(true)
            .build());

        addBool("show_background", "Show Background", true);
        addChoice("graph_mode", "Graph Mode", "Sparkline");
        addNumber("graph_width", "Graph Width", 80);
        addNumber("graph_height", "Graph Height", 24);
        addColor("line_color", "Line Color", 0xFF52BEEB);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
