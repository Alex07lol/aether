package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.forge189.mixin.EntityRendererMixin;
import dev.aether.forge189.mixin.ItemRendererMixin;
import dev.aether.forge189.mixin.RendererLivingEntityMixin;
import dev.aether.module.impl.interface_.ChatCustomizationModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.platform.ClientTickEvent;
import dev.aether.platform.KeyInputEvent;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ForgeClientEventBridge {
    private static final int[] LEGACY_COLOURS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final String LEGACY_CODES = "0123456789abcdef";

    private final AetherClient client;
    private final ForgeKeyBindings keyBindings;
    private final ForgeCosmeticRenderer cosmetics;
    private final ForgeNameTagRenderer nametags;
    private final ForgeNotifications notifications;
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
    private boolean toggleSneakActive;
    private boolean toggleSneakKeyDown;
    private int comboCount;
    private long lastComboMillis;
    private boolean freelookActive;
    private float freelookYaw;
    private float freelookPitch;
    private long nextMemoryCleanupMillis;
    private boolean modMenuKeyDown;

    ForgeClientEventBridge(AetherClient client, ForgeKeyBindings keyBindings) {
        this.client = client;
        this.keyBindings = keyBindings;
        this.cosmetics = new ForgeCosmeticRenderer(client);
        this.nametags = new ForgeNameTagRenderer(client);
        this.notifications = new ForgeNotifications(client);
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
        checkModMenuKey();
        applyHudEditorRequest();
        applyThemeSelectorRequest();
        applyCosmeticManagerRequest();
        this.cosmetics.onClientTick();
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
        registerComboHit();
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (!enabled("pvp.block_overlay") || Mc189Compat.typeOfHit(event.target) != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        net.minecraft.util.BlockPos blockpos = (net.minecraft.util.BlockPos) Mc189Compat.blockPos(event.target);
        Object world = Mc189Compat.world(Mc189Compat.minecraft());
        if (world == null || blockpos == null) {
            return;
        }

        net.minecraft.block.state.IBlockState state = Mc189Compat.getBlockState(world, blockpos);
        net.minecraft.block.Block block = Mc189Compat.getBlock(state);
        if (block == null || Mc189Compat.getMaterial(block) == net.minecraft.block.material.Material.air
                || !Mc189Compat.worldBorderContains(Mc189Compat.getWorldBorder(world), blockpos)) {
            return;
        }

        AxisAlignedBB rawBox = Mc189Compat.getSelectedBoundingBox(block, world, blockpos);
        if (rawBox == null) {
            return;
        }

        // All guards passed — cancel the vanilla highlight and draw ours.
        event.setCanceled(true);

        double d0 = Mc189Compat.lastTickPosX(event.player) + (Mc189Compat.posX(event.player) - Mc189Compat.lastTickPosX(event.player)) * (double) event.partialTicks;
        double d1 = Mc189Compat.lastTickPosY(event.player) + (Mc189Compat.posY(event.player) - Mc189Compat.lastTickPosY(event.player)) * (double) event.partialTicks;
        double d2 = Mc189Compat.lastTickPosZ(event.player) + (Mc189Compat.posZ(event.player) - Mc189Compat.lastTickPosZ(event.player)) * (double) event.partialTicks;
        AxisAlignedBB boundingBox = rawBox.expand(0.002, 0.002, 0.002).offset(-d0, -d1, -d2);

        // Set up GL state — MUST be restored in the finally block no matter what.
        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        Mc189Compat.disableTexture2D();
        Mc189Compat.depthMask(false);

        float thickness = (float) settingInt("pvp.block_overlay", "thickness", 2);
        Mc189Compat.glLineWidth(thickness);

        try {
            if (settingBool("pvp.block_overlay", "fill", true)) {
                int color = settingColor("pvp.block_overlay", "fill_color", 0x4452BEEB);
                float a = (float)(color >> 24 & 255) / 255.0F;
                float r = (float)(color >> 16 & 255) / 255.0F;
                float g = (float)(color >>  8 & 255) / 255.0F;
                float b = (float)(color       & 255) / 255.0F;
                Mc189Compat.color(r, g, b, a);
                Mc189Compat.drawFilledBoundingBox(boundingBox);
            }

            if (settingBool("pvp.block_overlay", "outline", true)) {
                int color = settingColor("pvp.block_overlay", "outline_color", 0x8852BEEB);
                float a = (float)(color >> 24 & 255) / 255.0F;
                float r = (float)(color >> 16 & 255) / 255.0F;
                float g = (float)(color >>  8 & 255) / 255.0F;
                float b = (float)(color       & 255) / 255.0F;
                Mc189Compat.color(r, g, b, a);
                Mc189Compat.drawSelectionBoundingBox(boundingBox);
            }
        } finally {
            // Always restore GL state — failure to do so causes subsequent draws
            // (HUD, chat, inventory) to render without textures = solid white.
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            Mc189Compat.glLineWidth(1.0F);
            Mc189Compat.depthMask(true);
            Mc189Compat.enableTexture2D();
            Mc189Compat.disableBlend();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        this.cosmetics.onRenderWorldLast(event.partialTicks);
        this.nametags.onRenderWorldLast(event.partialTicks);
    }

    @SubscribeEvent
    public void onRenderNameTag(RenderLivingEvent.Specials.Pre event) {
        // Cancelling the vanilla pass is what makes the custom scale/colour/background visible.
        if (this.nametags.suppressesVanillaTag(event.entity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type != 0 || event.message == null || !enabled(ChatCustomizationModule.ID)) {
            return;
        }
        if (!settingBool(ChatCustomizationModule.ID, "timestamps", true)) {
            return;
        }
        String line = Mc189Compat.chatFormattedText(event.message);
        if (line == null || line.isEmpty()) {
            return;
        }
        Object stamped = Mc189Compat.chatComponent(timestampPrefix() + line);
        if (stamped instanceof IChatComponent) {
            event.message = (IChatComponent) stamped;
        }
    }

    private String timestampPrefix() {
        String pattern = settingBool(ChatCustomizationModule.ID, "twenty_four_hour", true) ? "HH:mm" : "hh:mm a";
        String clock = new SimpleDateFormat(pattern, Locale.ROOT).format(new Date());
        char colour = legacyColourCode(settingColor(ChatCustomizationModule.ID, "timestamp_color", 0xFF52BEEB));
        return "\u00A78[\u00A7" + colour + clock + "\u00A78] \u00A7r";
    }

    /** 1.8 chat cannot render ARGB, so the picker colour is matched to the nearest legacy colour. */
    private static char legacyColourCode(int argb) {
        int red = argb >> 16 & 255;
        int green = argb >> 8 & 255;
        int blue = argb & 255;
        int best = 15;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < LEGACY_COLOURS.length; i++) {
            int candidate = LEGACY_COLOURS[i];
            int dr = red - (candidate >> 16 & 255);
            int dg = green - (candidate >> 8 & 255);
            int db = blue - (candidate & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return LEGACY_CODES.charAt(best);
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        if (enabled("graphics.hit_color") && Mc189Compat.hurtTime(event.entity) > 0) {
            int color = settingColor("graphics.hit_color", "color", 0xFFFF5555);
            float a = (float)(color >> 24 & 255) / 255.0F;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;

            AxisAlignedBB bb = Mc189Compat.getEntityBoundingBox(event.entity);
            if (bb != null) {
                Mc189Compat.pushMatrix();
                Mc189Compat.disableTexture2D();
                Mc189Compat.color(r, g, b, a);
                Mc189Compat.drawSelectionBoundingBox(bb.offset(-Mc189Compat.posX(event.entity), -Mc189Compat.posY(event.entity), -Mc189Compat.posZ(event.entity)).offset(event.x, event.y, event.z));
                Mc189Compat.enableTexture2D();
                Mc189Compat.popMatrix();
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Mc189Compat.keyPressed(keyBindings.developerOverlay())) {
            boolean enabled = client.modules().get("developer.overlay").state() != ModuleState.ENABLED;
            client.modules().setEnabled("developer.overlay", enabled);
            client.eventBus().publish(new KeyInputEvent("developer.overlay", Mc189Compat.keyCode(keyBindings.developerOverlay())));
        }
        checkModMenuKey();
    }

    private void checkModMenuKey() {
        Object mc = Mc189Compat.minecraft();
        if (Mc189Compat.currentScreen(mc) != null) {
            this.modMenuKeyDown = false;
            return;
        }
        int code = Mc189Compat.keyCode(keyBindings.modMenu());
        boolean isDown = Mc189Compat.keyPressed(keyBindings.modMenu()) || Mc189Compat.keyboardKeyDown(code);
        if (isDown && !this.modMenuKeyDown) {
            client.eventBus().publish(new KeyInputEvent("mod_menu", code));
            Mc189Compat.displayGuiScreen(new AetherClickGuiScreen(client));
        }
        this.modMenuKeyDown = isDown;
    }

    private void applyToggleSprint() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sprint")) {
            // Only clear the forced-sprint state once when transitioning to disabled.
            // Do NOT call setKeyBindState every tick — that overwrites MC's own key
            // state and prevents vanilla sprinting from working.
            if (this.toggleSprintActive && gameSettings != null) {
                Mc189Compat.setKeyBindState(Mc189Compat.keySprint(gameSettings), false);
            }
            this.toggleSprintActive = false;
            this.toggleSprintKeyDown = false;
            return;
        }
        boolean keyDown = Mc189Compat.keyboardKeyDown(settingInt("pvp.toggle_sprint", "keybind", 29));
        if (keyDown && !this.toggleSprintKeyDown) {
            this.toggleSprintActive = !this.toggleSprintActive;
            this.notifications.push("Toggle Sprint " + (this.toggleSprintActive ? "ON" : "OFF"));
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


    /** Mirrors {@link #applyToggleSprint()} for the sneak key. */
    private void applyToggleSneak() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sneak")) {
            // Same rule as toggle sprint: only clear the forced state on the way out,
            // otherwise vanilla sneaking breaks because we keep overwriting its key.
            if (this.toggleSneakActive && gameSettings != null) {
                Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), false);
            }
            this.toggleSneakActive = false;
            this.toggleSneakKeyDown = false;
            return;
        }

        // Never toggle while a screen is open, or typing in chat would flip sneak.
        boolean keyDown = Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(settingInt("pvp.toggle_sneak", "keybind", 42));
        if (keyDown && !this.toggleSneakKeyDown) {
            this.toggleSneakActive = !this.toggleSneakActive;
            this.notifications.push("Toggle Sneak " + (this.toggleSneakActive ? "ON" : "OFF"));
        }
        this.toggleSneakKeyDown = keyDown;

        if (gameSettings == null || Mc189Compat.player(minecraft) == null) {
            return;
        }
        Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), this.toggleSneakActive);
    }

    private void registerComboHit() {
        if (!enabled("hud.combo")) {
            return;
        }
        long now = System.currentTimeMillis();
        int resetMillis = clamp(settingInt("hud.combo", "reset_time", 2000), 250, 10000);
        this.comboCount = now - this.lastComboMillis <= (long) resetMillis ? this.comboCount + 1 : 1;
        this.lastComboMillis = now;
        if (this.comboCount % 5 == 0) {
            this.notifications.push("Combo x" + this.comboCount);
        }
    }

    /**
     * The HUD editor module doubles as a button: enabling it opens the editor and
     * immediately switches itself back off, so the deck's toggle never stays pinned.
     */
    private void applyHudEditorRequest() {
        if (!enabled("interface.hud_editor")) {
            return;
        }
        Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
        client.modules().setEnabled("interface.hud_editor", false);
        saveQuietly();
    }

    /**
     * Same one-shot pattern for the theme selector: it opens the deck on the Interface
     * category, which is where the five theme modules live.
     */
    private void applyThemeSelectorRequest() {
        if (!enabled("interface.theme_selector")) {
            return;
        }
        AetherClickGuiScreen screen = new AetherClickGuiScreen(client);
        screen.focusCategory(ModuleCategory.INTERFACE);
        Mc189Compat.displayGuiScreen(screen);
        client.modules().setEnabled("interface.theme_selector", false);
        saveQuietly();
    }

    /**
     * The cosmetic manager is a one-shot launcher too: switching it on opens the cosmetics
     * screen (where capes are imported and slots are chosen) and the switch falls back off.
     */
    private void applyCosmeticManagerRequest() {
        if (!enabled("cosmetics.manager")) {
            return;
        }
        Mc189Compat.displayGuiScreen(new AetherCosmeticsScreen(client, null));
        client.modules().setEnabled("cosmetics.manager", false);
        saveQuietly();
    }

    boolean toggleSprintActive() {
        return this.toggleSprintActive;
    }

    boolean toggleSneakActive() {
        return this.toggleSneakActive;
    }

    int comboCount() {
        return this.comboCount;
    }

    ForgeNotifications notifications() {
        return this.notifications;
    }

    boolean comboActive(int maxGapMillis) {
        return this.comboCount > 0 && System.currentTimeMillis() - this.lastComboMillis <= (long) maxGapMillis;
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
        applyTimeChanger(minecraft);
        applySkyCustomization(gameSettings);
        applyAnimationState();
        applyNoHurtCamState();
        applyHitColor();
    }

    /**
     * Sky customization owns the cloud style. Vanilla means "leave game settings alone", so the
     * player's own video settings win until they pick a cloud override.
     */
    private void applySkyCustomization(Object gameSettings) {
        if (gameSettings == null) {
            return;
        }
        String clouds = enabled("graphics.sky_customization")
            ? settingString("graphics.sky_customization", "clouds", "Vanilla")
            : "Vanilla";
        int wanted;
        if ("Off".equalsIgnoreCase(clouds)) {
            wanted = 0;
        } else if ("Fast".equalsIgnoreCase(clouds)) {
            wanted = 1;
        } else if ("Fancy".equalsIgnoreCase(clouds)) {
            wanted = 2;
        } else {
            restoreClouds(gameSettings);
            return;
        }
        if (this.originalClouds == null) {
            this.originalClouds = Integer.valueOf(Mc189Compat.clouds(gameSettings));
        }
        Mc189Compat.setClouds(gameSettings, wanted);
    }

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (enabled("graphics.sky_customization") && settingBool("graphics.sky_customization", "hide_fog", false)) {
            // Density 0 with the event cancelled clears the distance, water and lava fog.
            event.density = 0.0F;
            event.setCanceled(true);
        }
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

            if (settingBool("performance.fps_optimizer", "fast_lighting", true)) {
                if (this.originalAmbientOcclusion == null) {
                    this.originalAmbientOcclusion = Integer.valueOf(Mc189Compat.ambientOcclusion(gameSettings));
                }
                Mc189Compat.setAmbientOcclusion(gameSettings, 0);
            } else {
                restoreAmbientOcclusion(gameSettings);
            }

            if (settingBool("performance.fps_optimizer", "memory_cleanup", true)) {
                long now = System.currentTimeMillis();
                if (now >= this.nextMemoryCleanupMillis) {
                    this.nextMemoryCleanupMillis = now + 45000L;
                    Runtime runtime = Runtime.getRuntime();
                    long used = runtime.totalMemory() - runtime.freeMemory();
                    if (used > runtime.totalMemory() * 70L / 100L) {
                        System.gc();
                    }
                }
            }
            return;
        }

        restoreFancyGraphics(gameSettings);
        restoreUseVbo(gameSettings);
        restoreAmbientOcclusion(gameSettings);
    }

    private Boolean originalEntityShadows;
    private Integer originalClouds;
    private Integer originalAmbientOcclusion;

    private void restoreEntityShadows(Object gameSettings) {
        if (this.originalEntityShadows != null) {
            Mc189Compat.setEntityShadows(gameSettings, this.originalEntityShadows.booleanValue());
            this.originalEntityShadows = null;
        }
    }

    private void restoreClouds(Object gameSettings) {
        if (this.originalClouds != null) {
            Mc189Compat.setClouds(gameSettings, this.originalClouds.intValue());
            this.originalClouds = null;
        }
    }

    private void restoreAmbientOcclusion(Object gameSettings) {
        if (this.originalAmbientOcclusion != null) {
            Mc189Compat.setAmbientOcclusion(gameSettings, this.originalAmbientOcclusion.intValue());
            this.originalAmbientOcclusion = null;
        }
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

    private void applyTimeChanger(Object minecraft) {
        if (!enabled("graphics.time_changer")) {
            return;
        }
        Object world = Mc189Compat.world(minecraft);
        if (world != null) {
            long offset = (long) clamp(settingInt("graphics.time_changer", "offset", 12000), 0, 24000);
            long worldTime = Mc189Compat.worldTime(world);
            long dayBase = (worldTime / 24000L) * 24000L;
            // Freeze world time at the configured offset (time-of-day)
            Mc189Compat.setWorldTime(world, dayBase + offset);
        }
    }

    /**
     * Publishes the animation module's four toggles to the item renderer mixin. Each toggle
     * drives one pose: block swing, eat/drink swing, bow draw curve and the held fishing rod.
     */
    private void applyAnimationState() {
        boolean moduleEnabled = enabled("graphics.animation");
        ItemRendererMixin.blockAnimationEnabled =
            moduleEnabled && settingBool("graphics.animation", "block_animation", true);
        ItemRendererMixin.eatDrinkAnimationEnabled =
            moduleEnabled && settingBool("graphics.animation", "eat_drink_animation", true);
        ItemRendererMixin.bowAnimationEnabled =
            moduleEnabled && settingBool("graphics.animation", "bow_animation", true);
        ItemRendererMixin.fishingRodAnimationEnabled =
            moduleEnabled && settingBool("graphics.animation", "rod_animation", true);
        if (moduleEnabled) {
            applyActionSwing();
        }
    }

    /**
     * 1.7 loops the arm swing for the whole eat/drink/bow/block action instead of playing it
     * once, which is the part of CloudClient's animation mod that is not a pose change. Vanilla
     * ends a swing on its own, so restarting only when it has ended keeps the bob continuous
     * without touching vanilla's swing bookkeeping mid-animation.
     */
    private void applyActionSwing() {
        Object player = Mc189Compat.player(Mc189Compat.minecraft());
        if (player == null || !Mc189Compat.usingItem(player) || Mc189Compat.isSwingInProgress(player)) {
            return;
        }
        String action = Mc189Compat.itemUseAction(Mc189Compat.itemInUse(player));
        boolean animate = "BLOCK".equals(action) && settingBool("graphics.animation", "block_animation", true)
            || ("EAT".equals(action) || "DRINK".equals(action))
                && settingBool("graphics.animation", "eat_drink_animation", true)
            || "BOW".equals(action) && settingBool("graphics.animation", "bow_animation", true);
        if (!animate) {
            return;
        }
        Mc189Compat.setSwingProgressInt(player, 0);
        Mc189Compat.setSwingInProgress(player, true);
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

    /**
     * Publishes {@code graphics.no_hurt_cam} to the entity renderer mixin, which scales the
     * camera shake itself. The hurt timers stay untouched, so nothing else that reads them
     * (the hurt overlay, other renderers, other mods) sees a rewritten timer.
     */
    private void applyNoHurtCamState() {
        boolean active = enabled("graphics.no_hurt_cam");
        EntityRendererMixin.hurtCameraScaled = active;
        EntityRendererMixin.hurtCameraScale = active
            ? clamp(settingInt("graphics.no_hurt_cam", "shake_amount", 100), 0, 100) / 100.0F
            : 1.0F;
    }

    /**
     * Publishes {@code graphics.hit_color} to the renderer mixin that tints the damage overlay.
     * The colour's alpha channel is the tint strength, so the deck's colour palette doubles as
     * the opacity control; the hit outline drawn in {@code onRenderLivingPost} uses the same RGB.
     */
    private void applyHitColor() {
        boolean active = enabled("graphics.hit_color");
        RendererLivingEntityMixin.customHitColorEnabled = active;
        if (!active) {
            return;
        }
        int color = settingColor("graphics.hit_color", "color", 0xFFFF5555);
        RendererLivingEntityMixin.hitColorRed = (color >> 16 & 255) / 255.0F;
        RendererLivingEntityMixin.hitColorGreen = (color >> 8 & 255) / 255.0F;
        RendererLivingEntityMixin.hitColorBlue = (color & 255) / 255.0F;
        RendererLivingEntityMixin.hitColorAlpha = (color >>> 24 & 255) / 255.0F;
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
