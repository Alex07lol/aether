package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.impl.graphics.NameTagModule;
import dev.aether.module.impl.interface_.NickHiderModule;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Custom name tags: cancels the vanilla tag pass and draws the tag itself, so scale, colours,
 * background and the optional armour value all come from the module settings.
 * <p>
 * Tags are drawn in the world-last pass billboarded to the current view, the same way vanilla
 * orients them, and depth testing stays on so a tag behind a wall stays hidden.
 */
final class ForgeNameTagRenderer {
    private static final double TAG_RANGE = 64.0D;

    private final AetherClient client;

    ForgeNameTagRenderer(AetherClient client) {
        this.client = client;
    }

    /** @return true when the vanilla tag should be suppressed because this class draws one. */
    boolean suppressesVanillaTag(Object entity) {
        return enabled(NameTagModule.ID) && showsTag(entity);
    }

    void onRenderWorldLast(float partialTicks) {
        if (!enabled(NameTagModule.ID)) {
            return;
        }
        Object minecraft = Mc189Compat.minecraft();
        Object viewer = Mc189Compat.player(minecraft);
        Object world = Mc189Compat.world(minecraft);
        if (viewer == null || world == null) {
            return;
        }
        Object font = Mc189Compat.fontRenderer(minecraft);
        if (font == null) {
            return;
        }

        double cameraX = interpolated(viewer, 0, partialTicks);
        double cameraY = interpolated(viewer, 1, partialTicks);
        double cameraZ = interpolated(viewer, 2, partialTicks);
        float viewYaw = Mc189Compat.rotationYaw(Mc189Compat.renderViewEntity(minecraft));
        float viewPitch = Mc189Compat.rotationPitch(Mc189Compat.renderViewEntity(minecraft));

        for (Object candidate : Mc189Compat.worldPlayers(world)) {
            if (!showsTag(candidate)) {
                continue;
            }
            drawTag(font, candidate, cameraX, cameraY, cameraZ, partialTicks, viewYaw, viewPitch);
        }
    }

    private void drawTag(Object font, Object entity, double cameraX, double cameraY, double cameraZ,
                         float partialTicks, float viewYaw, float viewPitch) {
        String label = tagText(entity);
        if (label.isEmpty()) {
            return;
        }
        double height = Mc189Compat.entityHeight(entity);
        double x = interpolated(entity, 0, partialTicks) - cameraX;
        double y = interpolated(entity, 1, partialTicks) - cameraY + height + 0.45D;
        double z = interpolated(entity, 2, partialTicks) - cameraZ;
        double scale = 0.025D * clamp(settingInt(NameTagModule.ID, "scale", 100), 50, 150) / 100.0D;
        int textColor = settingColor(NameTagModule.ID, "text_color", 0xFFFFFFFF);
        boolean background = settingBool(NameTagModule.ID, "show_background", true);
        int backgroundColor = settingColor(NameTagModule.ID, "background_color", 0x6F000000);
        int width = Mc189Compat.stringWidth(font, label);

        Mc189Compat.pushMatrix();
        try {
            Mc189Compat.translate((float) x, (float) y, (float) z);
            Mc189Compat.rotate(-viewYaw, 0.0F, 1.0F, 0.0F);
            Mc189Compat.rotate(viewPitch, 1.0F, 0.0F, 0.0F);
            Mc189Compat.scale((float) -scale, (float) -scale, (float) scale);
            Mc189Compat.disableLighting();
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
            Mc189Compat.depthMask(false);
            if (background) {
                Mc189Compat.drawRect(-width / 2 - 2, -2, width / 2 + 2, 9, backgroundColor);
            }
            Mc189Compat.drawStringWithShadow(font, label, -width / 2.0F, 0.0F, textColor);
        } finally {
            Mc189Compat.depthMask(true);
            Mc189Compat.resetColor();
            Mc189Compat.disableBlend();
            Mc189Compat.enableLighting();
            Mc189Compat.popMatrix();
        }
    }

    /** @return the tag text: the entity's name, a nickname when nick hiding is on, and armour. */
    private String tagText(Object entity) {
        Object viewer = Mc189Compat.player(Mc189Compat.minecraft());
        String name = Mc189Compat.displayName(entity);
        if (entity == viewer && enabled(NickHiderModule.ID)) {
            String nickname = settingString(NickHiderModule.ID, "nickname", "You");
            if (nickname != null && !nickname.trim().isEmpty()) {
                name = nickname;
            }
        }
        if (settingBool(NameTagModule.ID, "show_armor", false)) {
            int armour = Mc189Compat.armourValue(entity);
            if (armour > 0) {
                name = name + " \u00A77[" + armour + "]";
            }
        }
        return name;
    }

    private boolean showsTag(Object entity) {
        Object minecraft = Mc189Compat.minecraft();
        Object viewer = Mc189Compat.player(minecraft);
        if (viewer == null || entity == null || entity == viewer) {
            // The local player never sees their own tag; the camera sits inside the model.
            return false;
        }
        if (Mc189Compat.isInvisible(entity)) {
            return false;
        }
        if (settingBool(NameTagModule.ID, "hide_sneaking", true) && Mc189Compat.isSneaking(entity)) {
            return false;
        }
        if (!(entity instanceof EntityPlayer) && !Mc189Compat.hasCustomName(entity)) {
            return false;
        }
        return Mc189Compat.distanceTo(entity, Mc189Compat.posX(viewer), Mc189Compat.posY(viewer), Mc189Compat.posZ(viewer))
            <= TAG_RANGE;
    }

    private static double interpolated(Object entity, int axis, float partialTicks) {
        double now = axis == 0 ? Mc189Compat.posX(entity) : axis == 1 ? Mc189Compat.posY(entity) : Mc189Compat.posZ(entity);
        double previous = axis == 0 ? Mc189Compat.lastTickPosX(entity)
            : axis == 1 ? Mc189Compat.lastTickPosY(entity) : Mc189Compat.lastTickPosZ(entity);
        return previous + (now - previous) * partialTicks;
    }

    private boolean enabled(String id) {
        try {
            return client.modules().get(id).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean settingBool(String moduleId, String settingId, boolean fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Boolean) {
                    return ((Boolean) setting.value()).booleanValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private int settingInt(String moduleId, String settingId, int fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    return ((Number) setting.value()).intValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private int settingColor(String moduleId, String settingId, int fallback) {
        return settingInt(moduleId, settingId, fallback);
    }

    private String settingString(String moduleId, String settingId, String fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof String) {
                    return (String) setting.value();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
