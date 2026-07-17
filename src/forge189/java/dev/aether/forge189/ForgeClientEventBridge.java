package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.platform.ClientTickEvent;
import dev.aether.platform.KeyInputEvent;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

final class ForgeClientEventBridge {
    private final AetherClient client;
    private final ForgeKeyBindings keyBindings;
    private Float originalGamma;
    private Integer originalParticles;
    private Boolean originalFancyGraphics;
    private Boolean originalUseVbo;
    private Integer originalLimitFramerate;
    private Integer originalRenderDistance;
    private Float originalFov;
    private Integer originalPerspective;
    private boolean toggleSprintActive;
    private boolean toggleSprintKeyDown;
    private boolean freelookActive;
    private float freelookYaw;
    private float freelookPitch;
    private long nextMemoryCleanupMillis;
    private Object hurtCameraEntity;
    private Integer originalHurtTime;
    private Integer originalMaxHurtTime;
    private Float originalAttackedAtYaw;

    ForgeClientEventBridge(AetherClient client, ForgeKeyBindings keyBindings) {
        this.client = client;
        this.keyBindings = keyBindings;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        client.eventBus().publish(new ClientTickEvent(Mc189Compat.tickTimeMillis()));
        applyToggleSprint();
        applyToggleSneak();
        applySnaplook();
        applyClientEffects();
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            applyNoHurtCam();
        } else if (event.phase == TickEvent.Phase.END) {
            restoreNoHurtCam();
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        Object minecraft = Mc189Compat.minecraft();
        if (Mc189Compat.currentScreen(minecraft) != null) {
            return;
        }
        if (enabled("pvp.zoom") && Mc189Compat.keyboardKeyDown(settingInt("pvp.zoom", "keybind", 0)) && settingBool("pvp.zoom", "scroll_to_zoom", true)) {
            int delta = Mc189Compat.mouseWheelDelta();
            if (delta == 0) return;
            updateZoomFromScroll(delta > 0 ? 1 : -1);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!this.freelookActive || event.dx == 0 && event.dy == 0) {
            return;
        }
        String moduleId = freelookModuleId();
        if (moduleId == null) {
            return;
        }
        float sensitivity = clamp(settingInt(moduleId, "sensitivity", 100), 10, 250) / 100.0F;
        this.freelookYaw += event.dx * 0.125F * sensitivity;
        float pitchDelta = event.dy * 0.125F * sensitivity;
        this.freelookPitch += settingBool(moduleId, "invert_y", false) ? -pitchDelta : pitchDelta;
        this.freelookPitch = clamp(this.freelookPitch, -90.0F, 90.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!this.freelookActive) {
            return;
        }
        event.yaw = this.freelookYaw;
        event.pitch = this.freelookPitch;
        event.roll = 0.0F;
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        applyAttackParticles(event.target);
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (!enabled("pvp.block_overlay") || Mc189Compat.typeOfHit(event.target) != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        event.setCanceled(true);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        float thickness = (float) settingInt("pvp.block_overlay", "thickness", 2);
        Mc189Compat.glLineWidth(thickness);

        net.minecraft.util.BlockPos blockpos = (net.minecraft.util.BlockPos) Mc189Compat.blockPos(event.target);
        Object world = Mc189Compat.world(Mc189Compat.minecraft());
        net.minecraft.block.state.IBlockState state = ((net.minecraft.world.World) world).getBlockState(blockpos);
        net.minecraft.block.Block block = state.getBlock();

        if (block.getMaterial() != net.minecraft.block.material.Material.air && ((net.minecraft.world.World) world).getWorldBorder().contains(blockpos)) {
            double d0 = event.player.lastTickPosX + (event.player.posX - event.player.lastTickPosX) * (double) event.partialTicks;
            double d1 = event.player.lastTickPosY + (event.player.posY - event.player.lastTickPosY) * (double) event.partialTicks;
            double d2 = event.player.lastTickPosZ + (event.player.posZ - event.player.lastTickPosZ) * (double) event.partialTicks;
            AxisAlignedBB boundingBox = block.getSelectedBoundingBox((net.minecraft.world.World) world, blockpos).expand(0.002, 0.002, 0.002).offset(-d0, -d1, -d2);

            if (settingBool("pvp.block_overlay", "fill", true)) {
                int color = settingColor("pvp.block_overlay", "fill_color", 0x4452BEEB);
                float a = (float)(color >> 24 & 255) / 255.0F;
                float r = (float)(color >> 16 & 255) / 255.0F;
                float g = (float)(color >> 8 & 255) / 255.0F;
                float b = (float)(color & 255) / 255.0F;
                GlStateManager.color(r, g, b, a);
                drawFilledBoundingBox(boundingBox);
            }

            if (settingBool("pvp.block_overlay", "outline", true)) {
                int color = settingColor("pvp.block_overlay", "outline_color", 0x8852BEEB);
                float a = (float)(color >> 24 & 255) / 255.0F;
                float r = (float)(color >> 16 & 255) / 255.0F;
                float g = (float)(color >> 8 & 255) / 255.0F;
                float b = (float)(color & 255) / 255.0F;
                GlStateManager.color(r, g, b, a);
                RenderGlobal.drawSelectionBoundingBox(boundingBox);
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        if (enabled("graphics.hit_color") && event.entity.hurtTime > 0) {
            int color = settingColor("graphics.hit_color", "color", 0xFFFF5555);
            float a = (float)(color >> 24 & 255) / 255.0F;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.color(r, g, b, a);
            RenderGlobal.drawSelectionBoundingBox(event.entity.getEntityBoundingBox().offset(-event.entity.posX, -event.entity.posY, -event.entity.posZ).offset(event.x, event.y, event.z));
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Mc189Compat.keyPressed(keyBindings.developerOverlay())) {
            boolean enabled = client.modules().get("developer.overlay").state() != ModuleState.ENABLED;
            client.modules().setEnabled("developer.overlay", enabled);
            client.eventBus().publish(new KeyInputEvent("developer.overlay", Mc189Compat.keyCode(keyBindings.developerOverlay())));
        }
        if (Mc189Compat.keyPressed(keyBindings.modMenu())) {
            client.eventBus().publish(new KeyInputEvent("mod_menu", Mc189Compat.keyCode(keyBindings.modMenu())));
            Mc189Compat.displayGuiScreen(new AetherQuickNavScreen(client));
        }
    }

    private void applyToggleSprint() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sprint")) {
            this.toggleSprintActive = false;
            this.toggleSprintKeyDown = false;
            if (gameSettings != null) {
                Mc189Compat.setKeyBindState(Mc189Compat.keySprint(gameSettings), false);
            }
            return;
        }
        boolean keyDown = Mc189Compat.keyboardKeyDown(settingInt("pvp.toggle_sprint", "keybind", 29));
        if (keyDown && !this.toggleSprintKeyDown) {
            this.toggleSprintActive = !this.toggleSprintActive;
        }
        this.toggleSprintKeyDown = keyDown;

        Object player = Mc189Compat.player(minecraft);
        if (player == null || gameSettings == null) {
            return;
        }
        Mc189Compat.setKeyBindState(Mc189Compat.keySprint(gameSettings), this.toggleSprintActive);
        if (this.toggleSprintActive && Mc189Compat.keyDown(Mc189Compat.keyForward(gameSettings)) && !Mc189Compat.sneaking(player)) {
            Mc189Compat.setSprinting(player, true);
        }
    }

    private boolean toggleSneakActive;
    private boolean toggleSneakKeyDown;

    private void applyToggleSneak() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sneak")) {
            this.toggleSneakActive = false;
            this.toggleSneakKeyDown = false;
            if (gameSettings != null) {
                Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), false);
            }
            return;
        }
        boolean keyDown = Mc189Compat.keyboardKeyDown(settingInt("pvp.toggle_sneak", "keybind", 42));
        if (keyDown && !this.toggleSneakKeyDown) {
            this.toggleSneakActive = !this.toggleSneakActive;
        }
        this.toggleSneakKeyDown = keyDown;

        Object player = Mc189Compat.player(minecraft);
        if (player == null || gameSettings == null) {
            return;
        }
        Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), this.toggleSneakActive);
    }

    private boolean snaplookActive;

    private void applySnaplook() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.snaplook") || gameSettings == null) {
            if (this.snaplookActive) {
                Mc189Compat.setThirdPersonView(gameSettings, 0);
                this.snaplookActive = false;
            }
            return;
        }
        boolean keyDown = Mc189Compat.keyboardKeyDown(settingInt("pvp.snaplook", "keybind", 33));
        if (keyDown && !this.snaplookActive) {
            this.snaplookActive = true;
            Mc189Compat.setThirdPersonView(gameSettings, 1);
        } else if (!keyDown && this.snaplookActive) {
            this.snaplookActive = false;
            Mc189Compat.setThirdPersonView(gameSettings, 0);
        }
    }

    private void applyClientEffects() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (gameSettings != null) {
            applyFullbright(gameSettings);
            applyFpsOptimizer(gameSettings);
            applyZoom(gameSettings);
            applyFreelook(gameSettings);
        }
        applyWeatherToggle(minecraft);
    }

    private void applyFullbright(Object gameSettings) {
        if (enabled("graphics.fullbright")) {
            if (originalGamma == null) {
                originalGamma = Float.valueOf(Mc189Compat.gammaSetting(gameSettings));
            }
            Mc189Compat.setGammaSetting(gameSettings, settingInt("graphics.fullbright", "brightness", 100));
            return;
        }
        if (originalGamma != null) {
            Mc189Compat.setGammaSetting(gameSettings, originalGamma.floatValue());
            originalGamma = null;
        }
    }

    private void applyFpsOptimizer(Object gameSettings) {
        boolean active = enabled("performance.fps_optimizer");
        if (active) {
            if (settingBool("performance.fps_optimizer", "limit_particles", true)) {
                if (this.originalParticles == null) {
                    this.originalParticles = Integer.valueOf(Mc189Compat.particleSetting(gameSettings));
                }
                Mc189Compat.setParticleSetting(gameSettings, 2);
            } else {
                restoreParticles(gameSettings);
            }
            if (settingBool("performance.fps_optimizer", "fast_graphics", true)) {
                if (this.originalFancyGraphics == null) {
                    this.originalFancyGraphics = Boolean.valueOf(Mc189Compat.fancyGraphics(gameSettings));
                }
                Mc189Compat.setFancyGraphics(gameSettings, false);
            } else {
                restoreFancyGraphics(gameSettings);
            }
            if (settingBool("performance.fps_optimizer", "use_vbo", true)) {
                if (this.originalUseVbo == null) {
                    this.originalUseVbo = Boolean.valueOf(Mc189Compat.useVbo(gameSettings));
                }
                Mc189Compat.setUseVbo(gameSettings, true);
            } else {
                restoreUseVbo(gameSettings);
            }
            int fpsLimit = clamp(settingInt("performance.fps_optimizer", "fps_limit", 260), 30, 1000);
            if (this.originalLimitFramerate == null) {
                this.originalLimitFramerate = Integer.valueOf(Mc189Compat.limitFramerate(gameSettings));
            }
            if (Mc189Compat.limitFramerate(gameSettings) < fpsLimit) {
                Mc189Compat.setLimitFramerate(gameSettings, fpsLimit);
            }
            int maxDist = clamp(settingInt("performance.fps_optimizer", "max_render_distance", 8), 2, 32);
            if (maxDist > 0) {
                if (this.originalRenderDistance == null) {
                    this.originalRenderDistance = Integer.valueOf(Mc189Compat.renderDistanceChunks(gameSettings));
                }
                int current = Mc189Compat.renderDistanceChunks(gameSettings);
                Mc189Compat.setRenderDistanceChunks(gameSettings, Math.max(2, Math.min(current, maxDist)));
            } else {
                restoreRenderDistance(gameSettings);
            }

            if (settingBool("performance.fps_optimizer", "memory_cleanup", true)) {
                long now = System.currentTimeMillis();
                if (now >= this.nextMemoryCleanupMillis) {
                    this.nextMemoryCleanupMillis = now + 45000L;
                    Runtime runtime = Runtime.getRuntime();
                    long used = runtime.totalMemory() - runtime.freeMemory();
                    long threshold = clamp(settingInt("performance.fps_optimizer", "memory_threshold", 72), 1, 95);
                    if (used > runtime.totalMemory() * threshold / 100L) {
                        System.gc();
                    }
                }
            }
            return;
        }

        restoreParticles(gameSettings);
        restoreFancyGraphics(gameSettings);
        restoreUseVbo(gameSettings);
        restoreLimitFramerate(gameSettings);
        restoreRenderDistance(gameSettings);
    }

    private void restoreParticles(Object gameSettings) {
        if (this.originalParticles != null) {
            Mc189Compat.setParticleSetting(gameSettings, this.originalParticles.intValue());
            this.originalParticles = null;
        }
    }

    private void restoreFancyGraphics(Object gameSettings) {
        if (this.originalFancyGraphics != null) {
            Mc189Compat.setFancyGraphics(gameSettings, this.originalFancyGraphics.booleanValue());
            this.originalFancyGraphics = null;
        }
    }

    private void restoreUseVbo(Object gameSettings) {
        if (this.originalUseVbo != null) {
            Mc189Compat.setUseVbo(gameSettings, this.originalUseVbo.booleanValue());
            this.originalUseVbo = null;
        }
    }

    private void restoreLimitFramerate(Object gameSettings) {
        if (this.originalLimitFramerate != null) {
            Mc189Compat.setLimitFramerate(gameSettings, this.originalLimitFramerate.intValue());
            this.originalLimitFramerate = null;
        }
    }

    private void restoreRenderDistance(Object gameSettings) {
        if (this.originalRenderDistance != null) {
            Mc189Compat.setRenderDistanceChunks(gameSettings, this.originalRenderDistance.intValue());
            this.originalRenderDistance = null;
        }
    }

    private void applyWeatherToggle(Object minecraft) {
        if (!enabled("graphics.weather_toggle")) {
            return;
        }
        Object world = Mc189Compat.world(minecraft);
        if (world != null) {
            Mc189Compat.setWorldRain(world, false);
        }
    }

    private void applyFreelook(Object gameSettings) {
        String moduleId = freelookModuleId();
        Object minecraft = Mc189Compat.minecraft();
        boolean active = moduleId != null
            && Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(settingInt(moduleId, "keybind", 56));
        if (active) {
            startFreelook(gameSettings, minecraft);
            if (this.originalPerspective == null) {
                this.originalPerspective = Integer.valueOf(Mc189Compat.thirdPersonView(gameSettings));
            }
            Mc189Compat.setThirdPersonView(gameSettings, 1);
            return;
        }
        stopFreelook(gameSettings);
    }

    private void startFreelook(Object gameSettings, Object minecraft) {
        if (this.freelookActive) {
            return;
        }
        Object player = Mc189Compat.player(minecraft);
        if (player == null) {
            return;
        }
        if (this.originalPerspective == null) {
            this.originalPerspective = Integer.valueOf(Mc189Compat.thirdPersonView(gameSettings));
        }
        this.freelookYaw = Mc189Compat.rotationYaw(player) + 180.0F;
        this.freelookPitch = Mc189Compat.rotationPitch(player);
        this.freelookActive = true;
    }

    private void stopFreelook(Object gameSettings) {
        this.freelookActive = false;
        if (this.originalPerspective != null) {
            Mc189Compat.setThirdPersonView(gameSettings, this.originalPerspective.intValue());
            this.originalPerspective = null;
        }
    }

    private void applyZoom(Object gameSettings) {
        boolean active = enabled("pvp.zoom")
            && Mc189Compat.keyboardKeyDown(settingInt("pvp.zoom", "keybind", 0));
        if (active) {
            if (originalFov == null) {
                originalFov = Float.valueOf(Mc189Compat.fovSetting(gameSettings));
            }
            int percent = Math.max(10, Math.min(100, settingInt("pvp.zoom", "zoom_percent", 40)));
            Mc189Compat.setFovSetting(gameSettings, Math.max(0.05F, originalFov.floatValue() * percent / 100.0F));
            return;
        }
        if (originalFov != null) {
            Mc189Compat.setFovSetting(gameSettings, originalFov.floatValue());
            originalFov = null;
        }
    }

    private void updateZoomFromScroll(int direction) {
        int step = clamp(settingInt("pvp.zoom", "scroll_step", 5), 1, 25);
        int current = settingInt("pvp.zoom", "zoom_percent", 40);
        int next = current + direction * step;
        int min = clamp(settingInt("pvp.zoom", "min_zoom_percent", 15), 5, 100);
        int max = clamp(settingInt("pvp.zoom", "max_zoom_percent", 90), min, 100);
        setSettingInt("pvp.zoom", "zoom_percent", Math.max(min, Math.min(max, next)));
        saveQuietly();
    }

    private void applyNoHurtCam() {
        if (!enabled("graphics.no_hurt_cam")) {
            return;
        }
        int shake = clamp(settingInt("graphics.no_hurt_cam", "shake_amount", 100), 0, 100);
        if (shake >= 100) {
            return;
        }
        Object minecraft = Mc189Compat.minecraft();
        Object entity = Mc189Compat.renderViewEntity(minecraft);
        if (entity == null) {
            entity = Mc189Compat.player(minecraft);
        }
        if (entity == null) {
            return;
        }
        float scale = shake / 100.0F;
        this.hurtCameraEntity = entity;
        this.originalHurtTime = Integer.valueOf(Mc189Compat.hurtTime(entity));
        this.originalMaxHurtTime = Integer.valueOf(Mc189Compat.maxHurtTime(entity));
        this.originalAttackedAtYaw = Float.valueOf(Mc189Compat.attackedAtYaw(entity));
        Mc189Compat.setHurtTime(entity, Math.round(this.originalHurtTime.intValue() * scale));
        Mc189Compat.setMaxHurtTime(entity, Math.round(this.originalMaxHurtTime.intValue() * scale));
        Mc189Compat.setAttackedAtYaw(entity, this.originalAttackedAtYaw.floatValue() * scale);
    }

    private void restoreNoHurtCam() {
        if (this.hurtCameraEntity == null) {
            return;
        }
        if (this.originalHurtTime != null) {
            Mc189Compat.setHurtTime(this.hurtCameraEntity, this.originalHurtTime.intValue());
        }
        if (this.originalMaxHurtTime != null) {
            Mc189Compat.setMaxHurtTime(this.hurtCameraEntity, this.originalMaxHurtTime.intValue());
        }
        if (this.originalAttackedAtYaw != null) {
            Mc189Compat.setAttackedAtYaw(this.hurtCameraEntity, this.originalAttackedAtYaw.floatValue());
        }
        this.hurtCameraEntity = null;
        this.originalHurtTime = null;
        this.originalMaxHurtTime = null;
        this.originalAttackedAtYaw = null;
    }

    private void applyAttackParticles(Object target) {
        if (!enabled("graphics.particles") || target == null) {
            return;
        }
        Object player = Mc189Compat.player(Mc189Compat.minecraft());
        if (player == null) {
            return;
        }
        int amount = clamp(settingInt("graphics.particles", "particle_amount", 5), 1, 25);
        String criticals = settingString("graphics.particles", "show_criticals", "Vanilla");
        String sharpness = settingString("graphics.particles", "show_sharpness", "Vanilla");
        boolean vanillaCritical = isVanillaCritical(player);

        for (int i = 0; i < amount; i++) {
            if ("Always".equalsIgnoreCase(sharpness)) {
                Mc189Compat.onEnchantmentCritical(player, target);
            }
            if ("Always".equalsIgnoreCase(criticals) || "Vanilla".equalsIgnoreCase(criticals) && vanillaCritical) {
                Mc189Compat.onCriticalHit(player, target);
            }
        }
    }

    private boolean isVanillaCritical(Object player) {
        return Mc189Compat.fallDistance(player) > 0.0F
            && !Mc189Compat.onGround(player)
            && !Mc189Compat.onLadder(player)
            && !Mc189Compat.inWater(player)
            && !Mc189Compat.riding(player);
    }

    private boolean enabled(String id) {
        try {
            return client.modules().get(id).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String freelookModuleId() {
        if (enabled("pvp.freelook")) {
            return "pvp.freelook";
        }
        return null;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setSettingInt(String moduleId, String settingId, int value) {
        try {
            for (Setting setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    setting.setValue(Integer.valueOf(value));
                    return;
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
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

    private int settingColor(String moduleId, String settingId, int fallback) {
        return settingInt(moduleId, settingId, fallback);
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (java.io.IOException ignored) {
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawFilledBoundingBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        tessellator.draw();
    }
}
