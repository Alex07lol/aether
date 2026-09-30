package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Mouse Display: a small pad whose indicator drifts in the direction the mouse is moving and
 * springs back to centre when the mouse stops, like Soar v4's mouse display.
 * <p>
 * The module holds configuration only - every number below is read by
 * {@code ForgeHudRenderer.renderMouseDisplay} and by the widget's layout maths, which is also what
 * the HUD editor measures, so a setting can never drift away from what is drawn.
 */
public class MouseDisplayModule extends AbstractModule {
    public static final String ID = "hud.mouse_display";

    public MouseDisplayModule() {
        super(ModuleMetadata.builder(ID, "Mouse Display")
            .category(ModuleCategory.HUD)
            .description("A pad whose indicator drifts with your mouse movement and recentres when you stop.")
            .build());

        addBool("show_background", "Show Background", true);
        addBool("show_direction", "Show Direction Text", false);
        addNumber("size", "Pad Size", 24, 14, 48, 2);
        addNumber("movement_speed", "Indicator Speed", 240, 40, 1000, 20);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("indicator_color", "Indicator Color", 0xFFFFFFFF);
    }
}
