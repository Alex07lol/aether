package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.forge189.mixin.EntityPlayerSPMixin;
import dev.aether.forge189.mixin.EntityRendererMixin;
import dev.aether.forge189.mixin.ItemRendererMixin;
import dev.aether.forge189.mixin.RendererLivingEntityMixin;
import dev.aether.forge189.mixin.WorldMixin;
import dev.aether.graphics.FreelookMath;
import dev.aether.graphics.HurtCamMath;
import dev.aether.graphics.ZoomMath;
import dev.aether.module.state.ToggleKey;
import dev.aether.module.state.ValueHold;
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

    /** Chat timestamp formatters: built once and reused for every message. */
    private final SimpleDateFormat clockFormat24 = new SimpleDateFormat("HH:mm", Locale.ROOT);
    private final SimpleDateFormat clockFormat12 = new SimpleDateFormat("hh:mm a", Locale.ROOT);

    private final AetherClient client;
    private final ForgeKeyBindings keyBindings;
    private final ForgeCosmeticRenderer cosmetics;
    private final ForgeNameTagRenderer nametags;
    private final ForgeNotifications notifications;
    /**
     * Held vanilla values. One {@link ValueHold} per value the client is allowed to override, so
     * every "put it back" path captures exactly once and restores exactly once - the three bugs
     * this replaces (capturing an override, restoring something never owned and leaking across a
     * re-enable) are all closed in one place instead of once per setting.
     */
    private final ValueHold<Float> gammaHold = new ValueHold<Float>("gamma");
    private final ValueHold<Boolean> fancyGraphicsHold = new ValueHold<Boolean>("fancy graphics");
    private final ValueHold<Boolean> vboHold = new ValueHold<Boolean>("vbo");
    private final ValueHold<Integer> ambientOcclusionHold = new ValueHold<Integer>("ambient occlusion");
    private final ValueHold<Integer> cloudsHold = new ValueHold<Integer>("clouds");
    private final ValueHold<Integer> freelookPerspectiveHold = new ValueHold<Integer>("freelook perspective");
    private final ValueHold<Integer> snaplookPerspectiveHold = new ValueHold<Integer>("snaplook perspective");
    private final ToggleKey toggleSprint = new ToggleKey();
    private final ToggleKey toggleSneak = new ToggleKey();
    /** True while the module owns the sprint/sneak key, so it is only handed back once. */
    private boolean sprintKeyForced;
    private boolean sneakKeyForced;
    private long zoomPersistAtMillis;
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
        flushZoomPersist();
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
        if (!enabled("pvp.zoom") || !configuredBool("pvp.zoom", "scroll_to_zoom")) {
            return;
        }
        if (!Mc189Compat.keyboardKeyDown(configuredInt("pvp.zoom", "keybind"))) {
            return;
        }
        int delta = Mc189Compat.mouseWheelDelta();
        if (delta == 0) {
            return;
        }
        updateZoomFromScroll(delta > 0 ? 1 : -1);
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
        // Vanilla's own sensitivity curve, scaled by the module's dial: at the default slider and
        // a sensitivity of 100 the camera turns exactly as fast as the player's head would.
        float mouseSensitivity = Mc189Compat.mouseSensitivity(Mc189Compat.gameSettings(Mc189Compat.minecraft()));
        float scale = FreelookMath.moduleScale(configuredInt(moduleId, "sensitivity"));
        this.freelookYaw = FreelookMath.yawAfter(this.freelookYaw, event.dx, mouseSensitivity, scale);
        this.freelookPitch = FreelookMath.pitchAfter(this.freelookPitch, event.dy, mouseSensitivity, scale,
            configuredBool(moduleId, "invert_y"));
        // The player is frozen inside the mixin as well; cancelling here is the primary guard so
        // vanilla never even reads the delta.
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
        // One formatter per format, built once: a chat line should not build a SimpleDateFormat and
        // a pattern parser just to print a five character clock.
        SimpleDateFormat format = settingBool(ChatCustomizationModule.ID, "twenty_four_hour", true)
            ? this.clockFormat24
            : this.clockFormat12;
        String clock = format.format(new Date());
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

    /**
     * The hit outline half of {@code graphics.hit_color}. Everything it changes is global render
     * state, so the whole draw is wrapped in {@code try/finally}: the colour, line width, depth
     * writes, texture and blend flags go back even if the draw throws, and nothing is left tinted
     * for the next renderer that draws into the same frame.
     */
    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        if (!enabled("graphics.hit_color") || Mc189Compat.hurtTime(event.entity) <= 0) {
            return;
        }
        AxisAlignedBB box = Mc189Compat.getEntityBoundingBox(event.entity);
        if (box == null) {
            return;
        }
        int color = settingColor("graphics.hit_color", "color", 0xFFFF5555);
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        AxisAlignedBB offset = box
            .offset(-Mc189Compat.posX(event.entity), -Mc189Compat.posY(event.entity), -Mc189Compat.posZ(event.entity))
            .offset(event.x, event.y, event.z);

        Mc189Compat.pushMatrix();
        try {
            // Vanilla's own block-highlight state, so the outline reads the same as the vanilla one.
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
            Mc189Compat.disableTexture2D();
            Mc189Compat.depthMask(false);
            Mc189Compat.glLineWidth(2.0F);
            Mc189Compat.color(r, g, b, a);
            Mc189Compat.drawSelectionBoundingBox(offset);
        } finally {
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            Mc189Compat.glLineWidth(1.0F);
            Mc189Compat.depthMask(true);
            Mc189Compat.enableTexture2D();
            Mc189Compat.disableBlend();
            Mc189Compat.popMatrix();
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

    /**
     * Toggle sprint owns the sprint key only while the module is on and the toggle is engaged.
     * <p>
     * Three rules keep it from sticking: the key press is an edge (a held key cannot re-toggle),
     * the forced key state is published only when it actually changes (so vanilla's own key handling
     * keeps working in between, and nothing is rewritten every tick), and disabling the module hands
     * the key back exactly once and forgets both the toggle and the key latch. The key is also
     * ignored while a screen is open, so typing in chat cannot flip sprint.
     */
    private void applyToggleSprint() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sprint")) {
            this.toggleSprint.reset();
            if (this.sprintKeyForced) {
                this.sprintKeyForced = false;
                if (gameSettings != null) {
                    Mc189Compat.setKeyBindState(Mc189Compat.keySprint(gameSettings), false);
                }
            }
            return;
        }
        boolean keyDown = Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(configuredInt("pvp.toggle_sprint", "keybind"));
        if (this.toggleSprint.update(keyDown)) {
            this.notifications.push("Toggle Sprint " + (this.toggleSprint.active() ? "ON" : "OFF"));
        }

        Object player = Mc189Compat.player(minecraft);
        if (gameSettings == null || player == null) {
            return;
        }
        boolean wanted = this.toggleSprint.active();
        if (this.sprintKeyForced != wanted) {
            this.sprintKeyForced = wanted;
            Mc189Compat.setKeyBindState(Mc189Compat.keySprint(gameSettings), wanted);
        }
        if (wanted && !Mc189Compat.sprinting(player)
                && Mc189Compat.keyDown(Mc189Compat.keyForward(gameSettings))
                && !Mc189Compat.sneaking(player)) {
            Mc189Compat.setSprinting(player, true);
        }
    }


    /**
     * Mirrors {@link #applyToggleSprint()} for the sneak key, with the same edge detection, the same
     * publish-once key ownership and the same clean reset on disable.
     * <p>
     * Containers are deliberately not special-cased: the module holds the sneak key exactly like a
     * held keyboard key would, and vanilla's own container behaviour (sneak is not what closes a
     * chest - the GUI key is) is left alone. The one guard is the same as sprint's: a screen swallows
     * key presses, so the toggle cannot flip while the player is typing.
     */
    private void applyToggleSneak() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (!enabled("pvp.toggle_sneak")) {
            this.toggleSneak.reset();
            if (this.sneakKeyForced) {
                this.sneakKeyForced = false;
                if (gameSettings != null) {
                    Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), false);
                }
            }
            return;
        }

        // Never toggle while a screen is open, or typing in chat would flip sneak.
        boolean keyDown = Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(configuredInt("pvp.toggle_sneak", "keybind"));
        if (this.toggleSneak.update(keyDown)) {
            this.notifications.push("Toggle Sneak " + (this.toggleSneak.active() ? "ON" : "OFF"));
        }

        if (gameSettings == null || Mc189Compat.player(minecraft) == null) {
            return;
        }
        boolean wanted = this.toggleSneak.active();
        if (this.sneakKeyForced != wanted) {
            this.sneakKeyForced = wanted;
            Mc189Compat.setKeyBindState(Mc189Compat.keyBindSneak(gameSettings), wanted);
        }
    }

    private void registerComboHit() {
        if (!enabled("hud.combo")) {
            return;
        }
        long now = System.currentTimeMillis();
        int resetMillis = Math.max(50, settingRangeValue("hud.combo", "reset_time"));
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
        return this.toggleSprint.active();
    }

    boolean toggleSneakActive() {
        return this.toggleSneak.active();
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

    /**
     * Snaplook holds the camera behind the player while its key is down. The perspective the player
     * was already using is captured once and restored once, so releasing the key returns to that
     * perspective - a player who was already in third person does not get dropped into first person
     * by a module that was only supposed to peek.
     */
    private void applySnaplook() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (gameSettings == null) {
            this.snaplookPerspectiveHold.forget();
            return;
        }
        boolean active = enabled("pvp.snaplook")
            && Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(configuredInt("pvp.snaplook", "keybind"));
        if (active) {
            this.snaplookPerspectiveHold.capture(Integer.valueOf(Mc189Compat.thirdPersonView(gameSettings)));
            if (Mc189Compat.thirdPersonView(gameSettings) != 1) {
                Mc189Compat.setThirdPersonView(gameSettings, 1);
            }
            return;
        }
        Integer restore = this.snaplookPerspectiveHold.release();
        if (restore != null && Mc189Compat.thirdPersonView(gameSettings) != restore.intValue()) {
            Mc189Compat.setThirdPersonView(gameSettings, restore.intValue());
        }
    }

    private void applyClientEffects() {
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (gameSettings != null) {
            applyFullbright(gameSettings);
            applyFpsOptimizer(gameSettings);
        }
        // Zoom and freelook publish their own render-time state instead of writing game settings.
        applyZoom();
        applyFreelook();
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
        this.cloudsHold.capture(Integer.valueOf(Mc189Compat.clouds(gameSettings)));
        if (Mc189Compat.clouds(gameSettings) != wanted) {
            Mc189Compat.setClouds(gameSettings, wanted);
        }
    }

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (enabled("graphics.sky_customization") && settingBool("graphics.sky_customization", "hide_fog", false)) {
            // Density 0 with the event cancelled clears the distance, water and lava fog.
            event.density = 0.0F;
            event.setCanceled(true);
        }
    }

    /**
     * Fullbright raises the gamma the world is lit with. The player's own gamma is captured exactly
     * once - on the first tick the module is active - and written back exactly once on the way out,
     * which is what makes enable/disable/enable cycles idempotent: the module can never capture its
     * own boosted value and leave the user's slider at 100.
     */
    private void applyFullbright(Object gameSettings) {
        if (gameSettings == null) {
            return;
        }
        if (!enabled("graphics.fullbright")) {
            Float restore = this.gammaHold.release();
            if (restore != null && Mc189Compat.gammaSetting(gameSettings) != restore.floatValue()) {
                Mc189Compat.setGammaSetting(gameSettings, restore.floatValue());
            }
            return;
        }
        this.gammaHold.capture(Float.valueOf(Mc189Compat.gammaSetting(gameSettings)));
        float wanted = (float) settingRangeValue("graphics.fullbright", "brightness");
        if (Mc189Compat.gammaSetting(gameSettings) != wanted) {
            Mc189Compat.setGammaSetting(gameSettings, wanted);
        }
    }

    private void applyFpsOptimizer(Object gameSettings) {
        if (gameSettings == null) {
            return;
        }
        if (enabled("performance.fps_optimizer")) {
            if (configuredBool("performance.fps_optimizer", "fast_graphics")) {
                this.fancyGraphicsHold.capture(Boolean.valueOf(Mc189Compat.fancyGraphics(gameSettings)));
                if (Mc189Compat.fancyGraphics(gameSettings)) {
                    Mc189Compat.setFancyGraphics(gameSettings, false);
                }
            } else {
                restoreFancyGraphics(gameSettings);
            }

            if (configuredBool("performance.fps_optimizer", "use_vbo")) {
                this.vboHold.capture(Boolean.valueOf(Mc189Compat.useVbo(gameSettings)));
                if (!Mc189Compat.useVbo(gameSettings)) {
                    Mc189Compat.setUseVbo(gameSettings, true);
                }
            } else {
                restoreUseVbo(gameSettings);
            }

            if (configuredBool("performance.fps_optimizer", "fast_lighting")) {
                this.ambientOcclusionHold.capture(Integer.valueOf(Mc189Compat.ambientOcclusion(gameSettings)));
                if (Mc189Compat.ambientOcclusion(gameSettings) != 0) {
                    Mc189Compat.setAmbientOcclusion(gameSettings, 0);
                }
            } else {
                restoreAmbientOcclusion(gameSettings);
            }

            if (configuredBool("performance.fps_optimizer", "memory_cleanup")) {
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

    private void restoreClouds(Object gameSettings) {
        Integer restore = this.cloudsHold.release();
        if (restore != null && Mc189Compat.clouds(gameSettings) != restore.intValue()) {
            Mc189Compat.setClouds(gameSettings, restore.intValue());
        }
    }

    private void restoreAmbientOcclusion(Object gameSettings) {
        Integer restore = this.ambientOcclusionHold.release();
        if (restore != null && Mc189Compat.ambientOcclusion(gameSettings) != restore.intValue()) {
            Mc189Compat.setAmbientOcclusion(gameSettings, restore.intValue());
        }
    }

    private void restoreFancyGraphics(Object gameSettings) {
        Boolean restore = this.fancyGraphicsHold.release();
        if (restore != null && Mc189Compat.fancyGraphics(gameSettings) != restore.booleanValue()) {
            Mc189Compat.setFancyGraphics(gameSettings, restore.booleanValue());
        }
    }

    private void restoreUseVbo(Object gameSettings) {
        Boolean restore = this.vboHold.release();
        if (restore != null && Mc189Compat.useVbo(gameSettings) != restore.booleanValue()) {
            Mc189Compat.setUseVbo(gameSettings, restore.booleanValue());
        }
    }

    /**
     * Hides rain and thunder without writing the client world: {@code World.getRainStrength} is
     * reported as zero, which is what the rain renderer, the sky colour, the fog and the rain sound
     * all read. The server's weather is untouched, so switching the module off restores normal
     * weather on the very next frame - there is no captured state and nothing to lose.
     */
    private void applyWeatherToggle(Object minecraft) {
        WorldMixin.rainVisualSuppressed = enabled("graphics.weather_toggle")
            && Mc189Compat.world(minecraft) != null;
    }

    /**
     * Moves the sky instead of the world clock. The real world time is sampled once per tick and
     * published together with the configured offset; the mixin recomputes the celestial angle from
     * those. Every other reader of world time - the server, the scoreboard, other mods, F3 - keeps
     * seeing the real time, and the day count is never disturbed.
     */
    private void applyTimeChanger(Object minecraft) {
        Object world = Mc189Compat.world(minecraft);
        if (!enabled("graphics.time_changer") || world == null) {
            WorldMixin.visualTimeActive = false;
            return;
        }
        Integer dimension = Mc189Compat.worldDimension(world);
        if (dimension != null && dimension.intValue() != 0) {
            // Only the overworld draws a sky; the Nether and the End keep vanilla's fixed light.
            WorldMixin.visualTimeActive = false;
            return;
        }
        WorldMixin.visualTimeWorldSnapshot = Mc189Compat.worldTime(world);
        WorldMixin.visualTimeOffset = configuredInt("graphics.time_changer", "offset");
        WorldMixin.visualTimeActive = true;
    }

    /** Saves the scroll-to-zoom setting once the wheel has been still for a moment. */
    private void flushZoomPersist() {
        if (this.zoomPersistAtMillis == 0L || System.currentTimeMillis() < this.zoomPersistAtMillis) {
            return;
        }
        this.zoomPersistAtMillis = 0L;
        saveQuietly();
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

    /**
     * Freelook holds a camera that is independent of the player: the module keeps its own yaw and
     * pitch, the mouse hook feeds them vanilla's own sensitivity maths, and the camera setup hook
     * hands them straight to the renderer. The player's rotation is never written - it is frozen for
     * as long as the key is held, and the perspective the player was already using is captured once
     * and handed back on release.
     */
    private void applyFreelook() {
        String moduleId = freelookModuleId();
        Object minecraft = Mc189Compat.minecraft();
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (gameSettings == null) {
            EntityPlayerSPMixin.freelookFreezesRotation = false;
            return;
        }
        boolean active = moduleId != null
            && Mc189Compat.currentScreen(minecraft) == null
            && Mc189Compat.keyboardKeyDown(configuredInt(moduleId, "keybind"));
        if (!active) {
            stopFreelook(gameSettings);
            return;
        }
        if (!this.freelookActive) {
            startFreelook(gameSettings, minecraft);
        }
        if (EntityPlayerSPMixin.freelookFreezesRotation != this.freelookActive) {
            EntityPlayerSPMixin.freelookFreezesRotation = this.freelookActive;
        }
        if (this.freelookActive && Mc189Compat.thirdPersonView(gameSettings) != 1) {
            Mc189Compat.setThirdPersonView(gameSettings, 1);
        }
    }

    private void startFreelook(Object gameSettings, Object minecraft) {
        Object player = Mc189Compat.player(minecraft);
        if (player == null) {
            return;
        }
        this.freelookPerspectiveHold.capture(Integer.valueOf(Mc189Compat.thirdPersonView(gameSettings)));
        // The camera starts where the player is already looking: vanilla orients its third-person
        // camera from the player's yaw plus 180 degrees, and the pitch is the player's own pitch.
        this.freelookYaw = FreelookMath.thirdPersonCameraYaw(Mc189Compat.rotationYaw(player));
        this.freelookPitch = FreelookMath.clamp(Mc189Compat.rotationPitch(player),
            -FreelookMath.PITCH_LIMIT, FreelookMath.PITCH_LIMIT);
        this.freelookActive = true;
    }

    private void stopFreelook(Object gameSettings) {
        if (this.freelookActive) {
            this.freelookActive = false;
            // The camera angle belongs to the hold: a fresh hold recaptures from the player again.
            this.freelookYaw = 0.0F;
            this.freelookPitch = 0.0F;
        }
        if (EntityPlayerSPMixin.freelookFreezesRotation) {
            EntityPlayerSPMixin.freelookFreezesRotation = false;
        }
        Integer restore = this.freelookPerspectiveHold.release();
        if (restore != null && Mc189Compat.thirdPersonView(gameSettings) != restore.intValue()) {
            Mc189Compat.setThirdPersonView(gameSettings, restore.intValue());
        }
    }

    /**
     * Zoom publishes a target scale and lets the renderer apply it to the field of view it is about
     * to use (see {@code EntityRendererMixin}). Nothing is written to {@code GameSettings}: the
     * player's FOV slider keeps its value, there is no captured FOV to restore, and an external FOV
     * change is scaled rather than overwritten.
     */
    private void applyZoom() {
        if (!enabled("pvp.zoom")) {
            EntityRendererMixin.resetZoomAnimation();
            return;
        }
        int percent = settingRangeValue("pvp.zoom", "zoom_percent");
        int floor = settingMin("pvp.zoom", "zoom_percent", 5);
        EntityRendererMixin.zoomTargetScale = Mc189Compat.keyboardKeyDown(configuredInt("pvp.zoom", "keybind"))
            ? ZoomMath.scaleFromPercent(percent, floor)
            : ZoomMath.NO_ZOOM;
    }

    /**
     * Scroll-to-zoom rewrites the module's own preference. The bounds come from the settings' own
     * metadata (the zoom percentage's declared range, narrowed by the configured minimum and
     * maximum), and the write to disk is debounced so a fast wheel does not save the config file
     * several times a second.
     */
    private void updateZoomFromScroll(int direction) {
        int step = Math.max(1, configuredInt("pvp.zoom", "scroll_step"));
        int low = Math.max(settingMin("pvp.zoom", "zoom_percent", 5),
            configuredInt("pvp.zoom", "min_zoom_percent"));
        int high = Math.min(settingMax("pvp.zoom", "zoom_percent", 100),
            configuredInt("pvp.zoom", "max_zoom_percent"));
        int next = ZoomMath.scrollTarget(configuredInt("pvp.zoom", "zoom_percent"), direction, step, low, high);
        setSettingInt("pvp.zoom", "zoom_percent", next);
        this.zoomPersistAtMillis = System.currentTimeMillis() + 800L;
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
            ? HurtCamMath.scaleFromPercent(settingRangeValue("graphics.no_hurt_cam", "shake_amount"))
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

    /**
     * The module's extra hit particles. The modes are explicit and cheap to satisfy: {@code Never}
     * spawns nothing at all (no loop, no vanilla particle, no player lookups), {@code Always} spawns
     * the burst on every hit and {@code Vanilla} only when the hit would have crit anyway. The
     * amount is read from the setting's own declared range, so a configured 0 means "no extra
     * particles" instead of being clamped up to a hard-coded minimum.
     */
    private void applyAttackParticles(Object target) {
        if (!enabled("graphics.particles") || target == null) {
            return;
        }
        int amount = settingRangeValue("graphics.particles", "particle_amount");
        String criticals = configuredString("graphics.particles", "show_criticals");
        String sharpness = configuredString("graphics.particles", "show_sharpness");
        boolean sharpnessAlways = "Always".equalsIgnoreCase(sharpness);
        boolean criticalsAlways = "Always".equalsIgnoreCase(criticals);
        boolean criticalsVanilla = "Vanilla".equalsIgnoreCase(criticals);
        if (amount <= 0 || !sharpnessAlways && !criticalsAlways && !criticalsVanilla) {
            return;
        }
        Object player = Mc189Compat.player(Mc189Compat.minecraft());
        if (player == null) {
            return;
        }
        boolean spawnCritical = criticalsAlways || criticalsVanilla && isVanillaCritical(player);
        if (!sharpnessAlways && !spawnCritical) {
            return;
        }

        for (int i = 0; i < amount; i++) {
            if (sharpnessAlways) {
                Mc189Compat.onEnchantmentCritical(player, target);
            }
            if (spawnCritical) {
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

    /** @return the module's setting, or {@code null} when the module or setting is not registered. */
    private Setting<?> setting(String moduleId, String settingId) {
        if (moduleId == null || settingId == null) {
            return null;
        }
        try {
            for (Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id())) {
                    return setting;
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /**
     * The configured number, or the setting's own declared default. Reading the default from the
     * metadata keeps the bridge free of a second copy of every default value.
     */
    private int configuredInt(String moduleId, String settingId) {
        Setting<?> setting = setting(moduleId, settingId);
        if (setting == null) {
            return 0;
        }
        if (setting.value() instanceof Number) {
            return ((Number) setting.value()).intValue();
        }
        return setting.defaultValue() instanceof Number ? ((Number) setting.defaultValue()).intValue() : 0;
    }

    private boolean configuredBool(String moduleId, String settingId) {
        Setting<?> setting = setting(moduleId, settingId);
        if (setting == null) {
            return false;
        }
        if (setting.value() instanceof Boolean) {
            return ((Boolean) setting.value()).booleanValue();
        }
        return setting.defaultValue() instanceof Boolean && ((Boolean) setting.defaultValue()).booleanValue();
    }

    private String configuredString(String moduleId, String settingId) {
        Setting<?> setting = setting(moduleId, settingId);
        if (setting == null) {
            return "";
        }
        if (setting.value() instanceof String) {
            return (String) setting.value();
        }
        return setting.defaultValue() instanceof String ? (String) setting.defaultValue() : "";
    }

    /**
     * The configured number clamped onto the setting's own declared range. This is what keeps the
     * behaviour of a module in step with the slider the deck shows: the range lives in exactly one
     * place, the setting.
     */
    private int settingRangeValue(String moduleId, String settingId) {
        Setting<?> setting = setting(moduleId, settingId);
        if (setting == null) {
            return 0;
        }
        int value = configuredInt(moduleId, settingId);
        return setting.hasRange() ? setting.range().snap(value) : value;
    }

    /** The lower bound a setting declares for itself. */
    private int settingMin(String moduleId, String settingId, int fallback) {
        Setting<?> setting = setting(moduleId, settingId);
        return setting != null && setting.hasRange() ? setting.range().min() : fallback;
    }

    /** The upper bound a setting declares for itself. */
    private int settingMax(String moduleId, String settingId, int fallback) {
        Setting<?> setting = setting(moduleId, settingId);
        return setting != null && setting.hasRange() ? setting.range().max() : fallback;
    }

    private boolean settingBool(String moduleId, String settingId, boolean fallback) {
        Setting<?> setting = setting(moduleId, settingId);
        return setting != null && setting.value() instanceof Boolean
            ? ((Boolean) setting.value()).booleanValue()
            : fallback;
    }

    private int settingInt(String moduleId, String settingId, int fallback) {
        Setting<?> setting = setting(moduleId, settingId);
        return setting != null && setting.value() instanceof Number
            ? ((Number) setting.value()).intValue()
            : fallback;
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
        Setting<?> setting = setting(moduleId, settingId);
        return setting != null && setting.value() instanceof String ? (String) setting.value() : fallback;
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
