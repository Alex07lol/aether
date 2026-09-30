package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Target Info: the entity the crosshair is on (or the one just hit) as a small card with its real
 * skin, its name and an animated health bar.
 * <p>
 * The module holds configuration only - every number below is read by
 * {@code ForgeHudRenderer.renderTargetInfo} and by the widget's layout maths, which is also what
 * the HUD editor measures, so a setting can never drift away from what is drawn.
 */
public class TargetInfoModule extends AbstractModule {
    public static final String ID = "hud.target_info";

    public TargetInfoModule() {
        super(ModuleMetadata.builder(ID, "Target Info")
            .category(ModuleCategory.HUD)
            .description("Shows the entity you are aiming at: skin, name and health.")
            .favoriteByDefault(true)
            .build());

        addBool("show_skin", "Show Skin", true);
        addBool("show_name", "Show Name", true);
        addBool("show_health_bar", "Show Health Bar", true);
        addBool("show_health_text", "Show Health Numbers", true);
        addChoice("health_mode", "Health Format", "Value", "Value", "Percent", "Both");
        addBool("show_background", "Show Background", true);
        addNumber("bar_width", "Health Bar Width", 72, 30, 140, 2);
        addNumber("animation_time", "Open / Close Time", 220, 60, 600, 10);
        addNumber("damage_flash_time", "Damage Flash Time", 320, 80, 1200, 20);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("bar_color", "Health Bar Color", 0xFF52BEEB);
        addColor("bar_background_color", "Health Bar Background", 0x8A101014);
        addColor("damage_color", "Damage Flash Color", 0xFFFF4D4D);
    }
}
