package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.hud.HudElement;
import dev.aether.module.ClientModule.ModuleState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;

import java.awt.Dimension;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

final class ForgeHudRenderer {
    private static final int TEXT_COLOR = 0xF5FBFF;
    private static final int ACCENT_COLOR = 0x52BEEB;

    private final AetherClient client;
    private final long sessionStartedMillis = System.currentTimeMillis();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private final List<Long> leftClicks = new ArrayList<Long>();
    private final List<Long> rightClicks = new ArrayList<Long>();
    private boolean lastAttackDown;
    private boolean lastUseDown;

    ForgeHudRenderer(AetherClient client) {
        this.client = client;
    }

    void render() {
        Object minecraft = Mc189Compat.minecraft();
        Object fontRenderer = Mc189Compat.fontRenderer(minecraft);
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (minecraft == null || fontRenderer == null || Mc189Compat.hideGui(gameSettings)) {
            return;
        }

        renderFps(fontRenderer);
        renderCoordinates(fontRenderer, minecraft);
        updateClickCounters(gameSettings);
        renderKeystrokes(fontRenderer, gameSettings);
        renderCps(fontRenderer);
        renderToggleSprint(fontRenderer, gameSettings);
        renderDirection(fontRenderer, minecraft);
        renderMemory(fontRenderer);
        renderSessionTime(fontRenderer);
        renderClock(fontRenderer);
        renderDeveloperOverlay(fontRenderer);
        renderBlockInfo(fontRenderer, minecraft);
        renderArmorStatus(fontRenderer, minecraft);
        renderPotionStatus(fontRenderer, minecraft);
        renderCustomCrosshair(fontRenderer, minecraft);
        renderDayCounter(fontRenderer, minecraft);
        renderPing(fontRenderer, minecraft);
        renderReachDisplay(fontRenderer);
        renderSpeedIndicator(fontRenderer, minecraft);
        renderServerAddress(fontRenderer, minecraft);
        renderToggleSneak(fontRenderer, gameSettings);
    }

    private void renderFps(Object fontRenderer) {
        if (!enabled("hud.fps")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.fps");
        String text = "FPS " + Mc189Compat.debugFps();
        if (settingBool("hud.fps", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.fps", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.fps", "text_color", ACCENT_COLOR));
    }

    private void renderCoordinates(Object fontRenderer, Object minecraft) {
        Object player = Mc189Compat.player(minecraft);
        if (!enabled("hud.coordinates") || player == null) {
            return;
        }
        boolean showCoordinates = settingBool("hud.coordinates", "show_coordinates", true);
        boolean hideY = settingBool("hud.coordinates", "hide_y", false);
        String text;
        if (showCoordinates && hideY) {
            text = String.format(Locale.ENGLISH, "XZ %.1f / %.1f", Mc189Compat.posX(player), Mc189Compat.posZ(player));
        } else if (showCoordinates) {
            text = String.format(Locale.ENGLISH, "XYZ %.1f / %.1f / %.1f",
                Mc189Compat.posX(player),
                Mc189Compat.posY(player),
                Mc189Compat.posZ(player)
            );
        } else {
            text = "";
        }
        if (settingBool("hud.coordinates", "show_direction", true)) {
            String direction = directionName(Mc189Compat.rotationYaw(player));
            text = text.length() == 0 ? direction : text + " " + direction;
        }
        if (text.length() > 0) {
            draw(fontRenderer, text, client.hudLayout().get("hud.coordinates"), settingColor("hud.coordinates", "coordinates_color", TEXT_COLOR));
        }
    }

    private void renderKeystrokes(Object fontRenderer, Object gameSettings) {
        if (!enabled("hud.keystrokes") || gameSettings == null) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.keystrokes");
        int size = clamp(settingInt("hud.keystrokes", "box_size", 18), 14, 34);
        int clickHeight = clamp(settingInt("hud.keystrokes", "click_size", 18), 14, 34);
        int spacebarHeight = clamp(settingInt("hud.keystrokes", "spacebar_height", 15), 8, 24);
        int gap = clamp(settingInt("hud.keystrokes", "gap", 1), 0, 8);
        int textColor = settingColor("hud.keystrokes", "text_color", TEXT_COLOR);
        int backgroundColor = settingColor("hud.keystrokes", "background_color", 0x6F000000);
        int pressedColor = settingColor("hud.keystrokes", "pressed_color", ACCENT_COLOR);
        boolean background = settingBool("hud.keystrokes", "show_background", true);
        boolean arrows = settingBool("hud.keystrokes", "arrows", false);
        int x = element.x();
        int y = element.y();

        if (settingBool("hud.keystrokes", "show_movement_keys", true)) {
            drawKeyBox(fontRenderer, arrows ? "^" : "W", Mc189Compat.keyForward(gameSettings), x + size + gap, y, size, size, background, backgroundColor, pressedColor, textColor);
            int rowY = y + size + gap;
            drawKeyBox(fontRenderer, arrows ? "<" : "A", Mc189Compat.keyLeft(gameSettings), x, rowY, size, size, background, backgroundColor, pressedColor, textColor);
            drawKeyBox(fontRenderer, arrows ? "v" : "S", Mc189Compat.keyBack(gameSettings), x + size + gap, rowY, size, size, background, backgroundColor, pressedColor, textColor);
            drawKeyBox(fontRenderer, arrows ? ">" : "D", Mc189Compat.keyRight(gameSettings), x + (size + gap) * 2, rowY, size, size, background, backgroundColor, pressedColor, textColor);
            y = rowY + size + gap;
        }

        if (settingBool("hud.keystrokes", "show_clicks", true)) {
            int totalMovementWidth = size * 3 + gap * 2;
            int clickWidth = (totalMovementWidth - gap) / 2;
            drawKeyBox(fontRenderer, "LMB", Mc189Compat.keyAttack(gameSettings), x, y, clickWidth, clickHeight, background, backgroundColor, pressedColor, textColor);
            drawKeyBox(fontRenderer, "RMB", Mc189Compat.keyUseItem(gameSettings), x + clickWidth + gap, y, clickWidth, clickHeight, background, backgroundColor, pressedColor, textColor);
            y += clickHeight + gap;
        }

        if (settingBool("hud.keystrokes", "show_spacebar", false)) {
            drawKeyBox(fontRenderer, "SPACE", Mc189Compat.keyJump(gameSettings), x, y, size * 3 + gap * 2, spacebarHeight, background, backgroundColor, pressedColor, textColor);
        }
    }

    private void updateClickCounters(Object gameSettings) {
        if (gameSettings == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean attackDown = Mc189Compat.keyDown(Mc189Compat.keyAttack(gameSettings));
        boolean useDown = Mc189Compat.keyDown(Mc189Compat.keyUseItem(gameSettings));
        if (attackDown && !this.lastAttackDown) {
            this.leftClicks.add(Long.valueOf(now));
        }
        if (useDown && !this.lastUseDown) {
            this.rightClicks.add(Long.valueOf(now));
        }
        this.lastAttackDown = attackDown;
        this.lastUseDown = useDown;
        trimClicks(this.leftClicks, now);
        trimClicks(this.rightClicks, now);
    }

    private void renderCps(Object fontRenderer) {
        if (!enabled("hud.cps")) {
            return;
        }
        String text = settingBool("hud.cps", "right_click", false)
            ? "CPS " + this.leftClicks.size() + " | " + this.rightClicks.size()
            : "CPS " + this.leftClicks.size();
        HudElement element = client.hudLayout().get("hud.cps");
        if (settingBool("hud.cps", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.cps", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.cps", "text_color", TEXT_COLOR));
    }

    private void renderToggleSprint(Object fontRenderer, Object gameSettings) {
        if (!enabled("pvp.toggle_sprint") || !settingBool("pvp.toggle_sprint", "show_status", true) || gameSettings == null) {
            return;
        }
        if (!Mc189Compat.keyDown(Mc189Compat.keySprint(gameSettings))) {
            return;
        }
        String text = "Sprint toggled";
        HudElement element = client.hudLayout().get("pvp.toggle_sprint");
        if (settingBool("pvp.toggle_sprint", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("pvp.toggle_sprint", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("pvp.toggle_sprint", "text_color", TEXT_COLOR));
    }

    private void renderDirection(Object fontRenderer, Object minecraft) {
        Object player = Mc189Compat.player(minecraft);
        if (!enabled("hud.direction") || player == null) {
            return;
        }
        draw(fontRenderer, "Facing " + directionName(Mc189Compat.rotationYaw(player)), client.hudLayout().get("hud.direction"), settingColor("hud.direction", "text_color", TEXT_COLOR));
    }

    private void renderMemory(Object fontRenderer) {
        if (!enabled("hud.memory")) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long totalMb = runtime.totalMemory() / (1024L * 1024L);
        draw(fontRenderer, "Mem " + usedMb + "/" + totalMb + "MB", client.hudLayout().get("hud.memory"), TEXT_COLOR);
    }

    private void renderSessionTime(Object fontRenderer) {
        if (!enabled("hud.session_time")) {
            return;
        }
        long elapsedSeconds = (System.currentTimeMillis() - sessionStartedMillis) / 1000L;
        long minutes = elapsedSeconds / 60L;
        long seconds = elapsedSeconds % 60L;
        draw(fontRenderer, String.format(Locale.ENGLISH, "Session %02d:%02d", Long.valueOf(minutes), Long.valueOf(seconds)), client.hudLayout().get("hud.session_time"), TEXT_COLOR);
    }

    private void renderClock(Object fontRenderer) {
        if (!enabled("hud.clock")) {
            return;
        }
        draw(fontRenderer, "Time " + clockFormat.format(new Date()), client.hudLayout().get("hud.clock"), TEXT_COLOR);
    }

    private void renderDeveloperOverlay(Object fontRenderer) {
        if (!enabled("developer.overlay")) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long totalMb = runtime.totalMemory() / (1024L * 1024L);
        String text = "Aether " + client.version().name()
            + " | MC " + client.version().primaryGameVersion().displayName()
            + " | " + client.platform().displayName()
            + " | Mem " + usedMb + "/" + totalMb + "MB";
        draw(fontRenderer, text, client.hudLayout().get("developer.overlay"), ACCENT_COLOR);
    }

    private void renderBlockInfo(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.block_info")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        MovingObjectPosition mouseOver = (MovingObjectPosition) Mc189Compat.objectMouseOver(mc);
        if (mouseOver != null && Mc189Compat.typeOfHit(mouseOver) == MovingObjectPosition.MovingObjectType.BLOCK) {
            net.minecraft.util.BlockPos blockPos = (net.minecraft.util.BlockPos) Mc189Compat.blockPos(mouseOver);
            IBlockState state = mc.theWorld.getBlockState(blockPos);
            Block block = state.getBlock();

            if (block != null && block != Blocks.air) {
                ItemStack stack = new ItemStack(block, 1, block.getMetaFromState(state));
                String blockName = stack.getDisplayName();
                if (stack.getItem() == null) {
                    blockName = block.getLocalizedName();
                }

                HudElement element = client.hudLayout().get("hud.block_info");
                if (settingBool("hud.block_info", "show_background", true)) {
                    drawBackground(fontRenderer, blockName, element, settingColor("hud.block_info", "background_color", 0x6F000000));
                }
                draw(fontRenderer, blockName, element, settingColor("hud.block_info", "text_color", TEXT_COLOR));
            }
        }
    }

    private void renderArmorStatus(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.armor")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        if (mc.thePlayer == null) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.armor");
        int x = element.x();
        int y = element.y();

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();

        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.armorInventory[3 - i];
            if (itemStack == null) {
                continue;
            }
            
            int currentY = y + i * 18;

            mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, currentY);

            if (settingBool("hud.armor", "show_durability", true) && itemStack.isItemDamaged()) {
                int durability = itemStack.getMaxDamage() - itemStack.getItemDamage();
                String text = String.valueOf(durability);
                float hue = Math.max(0.0F, (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) / (float) itemStack.getMaxDamage());
                int color = java.awt.Color.HSBtoRGB(hue / 3.0F, 1.0F, 1.0F);
                Mc189Compat.drawStringWithShadow(fontRenderer, text, x + 20, currentY + 4, color);
            }
        }

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void renderPotionStatus(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.potions")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        if (mc.thePlayer == null || mc.thePlayer.getActivePotionEffects().isEmpty()) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.potions");
        int x = element.x();
        int y = element.y();
        String mode = settingString("hud.potions", "mode", "Compact");

        for (PotionEffect effect : (Collection<PotionEffect>) mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            String name = StatCollector.translateToLocal(potion.getName());
            if (effect.getAmplifier() > 0) {
                name += " " + (effect.getAmplifier() + 1);
            }
            String text = name;
            if ("Detailed".equalsIgnoreCase(mode)) {
                text += ": " + Potion.getDurationString(effect);
            }
            int color = potion.isBadEffect() ? 0xFF5555 : 0xFFFFFF;
            Mc189Compat.drawStringWithShadow(fontRenderer, text, x, y, color);
            y += 10;
        }
    }

    private void renderCustomCrosshair(Object fontRenderer, Object minecraft) {
        if (!enabled("graphics.custom_crosshair")) {
            return;
        }
        Object resolution = new net.minecraft.client.gui.ScaledResolution((net.minecraft.client.Minecraft) minecraft);
        int width = Mc189Compat.scaledWidth(resolution);
        int height = Mc189Compat.scaledHeight(resolution);
        int color = settingColor("graphics.custom_crosshair", "color", 0xFFFFFFFF);
        int size = settingInt("graphics.custom_crosshair", "size", 8);
        int gap = settingInt("graphics.custom_crosshair", "gap", 4);
        int thickness = settingInt("graphics.custom_crosshair", "thickness", 1);
        boolean showDot = settingBool("graphics.custom_crosshair", "show_dot", false);
        String shape = settingString("graphics.custom_crosshair", "shape", "Cross");

        int centerX = width / 2;
        int centerY = height / 2;

        int halfThickness = thickness / 2;
        int remainder = thickness % 2;

        if ("Dot".equalsIgnoreCase(shape)) {
            drawCrosshairDot(centerX, centerY, thickness, color);
            return;
        }
        if ("Circle".equalsIgnoreCase(shape)) {
            Mc189Compat.drawRect(centerX - size, centerY - size, centerX + size + 1, centerY - size + thickness, color);
            Mc189Compat.drawRect(centerX - size, centerY + size - thickness + 1, centerX + size + 1, centerY + size + 1, color);
            Mc189Compat.drawRect(centerX - size, centerY - size, centerX - size + thickness, centerY + size + 1, color);
            Mc189Compat.drawRect(centerX + size - thickness + 1, centerY - size, centerX + size + 1, centerY + size + 1, color);
        } else {
            Mc189Compat.drawRect(centerX - size - gap, centerY - halfThickness, centerX - gap, centerY + halfThickness + remainder, color);
            Mc189Compat.drawRect(centerX + gap, centerY - halfThickness, centerX + size + gap, centerY + halfThickness + remainder, color);
            Mc189Compat.drawRect(centerX - halfThickness, centerY - size - gap, centerX + halfThickness + remainder, centerY - gap, color);
            Mc189Compat.drawRect(centerX - halfThickness, centerY + gap, centerX + halfThickness + remainder, centerY + size + gap, color);
        }

        if (showDot) {
            drawCrosshairDot(centerX, centerY, thickness, color);
        }
    }

    private void renderDayCounter(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.day_counter")) {
            return;
        }
        Object world = Mc189Compat.world(minecraft);
        if (world == null) return;
        HudElement element = client.hudLayout().get("hud.day_counter");
        long totalTime = Mc189Compat.worldTime(world);
        long day = totalTime / 24000L;
        String text = "Day " + day;
        if (settingBool("hud.day_counter", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.day_counter", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.day_counter", "text_color", ACCENT_COLOR));
    }

    private void renderPing(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.ping")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.ping");
        int ping = Mc189Compat.playerPing(minecraft);
        String text = "Ping " + (ping < 0 ? 0 : ping) + "ms";
        if (settingBool("hud.ping", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.ping", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.ping", "text_color", ACCENT_COLOR));
    }

    private void renderReachDisplay(Object fontRenderer) {
        if (!enabled("hud.reach_display")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.reach_display");
        String text = String.format(Locale.ENGLISH, "Reach %.2fm", Mc189Compat.lastReach());
        if (settingBool("hud.reach_display", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.reach_display", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.reach_display", "text_color", ACCENT_COLOR));
    }

    private void renderSpeedIndicator(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.speed_indicator")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.speed_indicator");
        double bps = Mc189Compat.playerBps(minecraft);
        String text = String.format(Locale.ENGLISH, "Speed %.2f BPS", bps);
        if (settingBool("hud.speed_indicator", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.speed_indicator", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.speed_indicator", "text_color", ACCENT_COLOR));
    }

    private void renderServerAddress(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.server_address")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.server_address");
        String server = Mc189Compat.serverAddress(minecraft);
        String text = server == null || server.isEmpty() ? "Singleplayer" : server;
        if (settingBool("hud.server_address", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.server_address", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.server_address", "text_color", ACCENT_COLOR));
    }

    private void renderToggleSneak(Object fontRenderer, Object gameSettings) {
        if (!enabled("pvp.toggle_sneak")) {
            return;
        }
        HudElement element = client.hudLayout().get("pvp.toggle_sneak");
        Object keySneak = Mc189Compat.keyBindSneak(gameSettings);
        if (keySneak != null && Mc189Compat.keyDown(keySneak)) {
            String text = "[Sneaking (Toggled)]";
            if (settingBool("pvp.toggle_sneak", "show_background", true)) {
                drawBackground(fontRenderer, text, element, settingColor("pvp.toggle_sneak", "background_color", 0x6F000000));
            }
            draw(fontRenderer, text, element, settingColor("pvp.toggle_sneak", "text_color", ACCENT_COLOR));
        }
    }

    private void drawCrosshairDot(int centerX, int centerY, int thickness, int color) {
            int dotHalf = thickness / 2;
            int dotRem = thickness % 2;
            Mc189Compat.drawRect(centerX - dotHalf, centerY - dotHalf, centerX + dotHalf + dotRem, centerY + dotHalf + dotRem, color);
    }

    boolean enabled(String moduleId) {
        return client.modules().get(moduleId).state() == ModuleState.ENABLED;
    }

    private void drawKeyBox(Object fontRenderer, String label, Object keyBinding, int x, int y, int width, int height, boolean background, int backgroundColor, int pressedColor, int textColor) {
        boolean down = Mc189Compat.keyDown(keyBinding);
        if (background || down) {
            Mc189Compat.drawRect(x, y, x + width, y + height, down ? pressedColor : backgroundColor);
        }
        int textX = x + (width - Mc189Compat.stringWidth(fontRenderer, label)) / 2;
        int textY = y + height / 2 - 4;
        Mc189Compat.drawStringWithShadow(fontRenderer, label, textX, textY, textColor);
    }

    private int settingColor(String moduleId, String settingId, int fallback) {
        return settingInt(moduleId, settingId, fallback);
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

    private static String directionName(float yaw) {
        int index = Math.round((((yaw % 360.0F) + 360.0F) % 360.0F) / 45.0F) & 7;
        String[] directions = {"South", "South West", "West", "North West", "North", "North East", "East", "South East"};
        return directions[index];
    }

    private static void draw(Object font, String text, HudElement element, int color) {
        float scale = element.scale();
        float opacity = element.opacity();
        int alpha = Math.round(((color >> 24) & 0xFF) * opacity);
        if (alpha <= 0) alpha = Math.round(255 * opacity);
        int finalColor = (alpha << 24) | (color & 0x00FFFFFF);

        if (Math.abs(scale - 1.0F) > 0.001F) {
            Mc189Compat.pushMatrix();
            Mc189Compat.scale(scale, scale, 1.0F);
            float unscaledX = element.x() / scale;
            float unscaledY = element.y() / scale;
            Mc189Compat.drawStringWithShadow(font, text, unscaledX, unscaledY, finalColor);
            Mc189Compat.popMatrix();
        } else {
            Mc189Compat.drawStringWithShadow(font, text, element.x(), element.y(), finalColor);
        }
    }

    private static void drawBackground(Object font, String text, HudElement element, int color) {
        float scale = element.scale();
        float opacity = element.opacity();
        int alpha = Math.round(((color >> 24) & 0xFF) * opacity);
        if (alpha <= 0) alpha = Math.round(111 * opacity);
        int finalColor = (alpha << 24) | (color & 0x00FFFFFF);

        int width = Mc189Compat.stringWidth(font, text) + 6;
        if (Math.abs(scale - 1.0F) > 0.001F) {
            Mc189Compat.pushMatrix();
            Mc189Compat.scale(scale, scale, 1.0F);
            int unscaledX = Math.round(element.x() / scale);
            int unscaledY = Math.round(element.y() / scale);
            Mc189Compat.drawRect(unscaledX - 3, unscaledY - 3, unscaledX + width, unscaledY + 11, finalColor);
            Mc189Compat.popMatrix();
        } else {
            Mc189Compat.drawRect(element.x() - 3, element.y() - 3, element.x() + width, element.y() + 11, finalColor);
        }
    }

    private static void trimClicks(List<Long> clicks, long now) {
        Iterator<Long> iterator = clicks.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().longValue() > 1000L) {
                iterator.remove();
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    Dimension getDimensions(String id, Object fontRenderer, Object minecraft) {
        int width = 70;
        int height = 12;
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        Object player = Mc189Compat.player(minecraft);

        switch (id) {
            case "hud.fps": {
                String text = "FPS " + Mc189Compat.debugFps();
                width = Mc189Compat.stringWidth(fontRenderer, text);
                height = 8;
                if (settingBool("hud.fps", "show_background", true)) {
                    width += 6;
                    height = 14;
                }
                break;
            }
            case "hud.coordinates": {
                if (player == null) break;
                // This is an approximation as it depends on settings
                String text = String.format(Locale.ENGLISH, "XYZ %.1f / %.1f / %.1f %s", Mc189Compat.posX(player), Mc189Compat.posY(player), Mc189Compat.posZ(player), "North West");
                width = Mc189Compat.stringWidth(fontRenderer, text);
                height = 8;
                break;
            }
            case "hud.keystrokes": {
                int size = clamp(settingInt("hud.keystrokes", "box_size", 18), 14, 34);
                int clickHeight = clamp(settingInt("hud.keystrokes", "click_size", 18), 14, 34);
                int spacebarHeight = clamp(settingInt("hud.keystrokes", "spacebar_height", 15), 8, 24);
                int gap = clamp(settingInt("hud.keystrokes", "gap", 1), 0, 8);
                width = size * 3 + gap * 2;
                height = 0;
                if (settingBool("hud.keystrokes", "show_movement_keys", true)) {
                    height += size * 2 + gap;
                }
                if (settingBool("hud.keystrokes", "show_clicks", true)) {
                    height += clickHeight + gap;
                }
                if (settingBool("hud.keystrokes", "show_spacebar", false)) {
                    height += spacebarHeight + gap;
                }
                break;
            }
            case "hud.cps":
            case "pvp.toggle_sprint":
            case "hud.block_info": {
                // These are all similar text + optional background
                String text = "Sample Text 123"; // Approximation
                if (id.equals("hud.cps")) text = "CPS: 10 | 10";
                if (id.equals("pvp.toggle_sprint")) text = "Sprint toggled";
                if (id.equals("hud.block_info")) text = "Stone";
                width = Mc189Compat.stringWidth(fontRenderer, text);
                height = 8;
                if (settingBool(id, "show_background", true)) {
                    width += 6;
                    height = 14;
                }
                break;
            }
            case "hud.direction":
            case "hud.memory":
            case "hud.session_time":
            case "hud.clock":
            case "developer.overlay": {
                String text = "Sample Text 123"; // Approximation
                width = Mc189Compat.stringWidth(fontRenderer, text);
                height = 8;
                break;
            }
            case "hud.armor": {
                width = 16;
                height = 4 * 18;
                if (settingBool("hud.armor", "show_durability", true)) {
                    width += 4 + Mc189Compat.stringWidth(fontRenderer, "100");
                }
                break;
            }
            case "hud.potions": {
                if (mc.thePlayer == null) break;
                Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
                if (effects.isEmpty()) {
                    width = Mc189Compat.stringWidth(fontRenderer, "Speed II: 0:00");
                    height = 10;
                } else {
                    width = 0;
                    height = effects.size() * 10;
                    for (PotionEffect effect : effects) {
                        String name = StatCollector.translateToLocal(Potion.potionTypes[effect.getPotionID()].getName());
                        if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
                        String text = name + ": " + Potion.getDurationString(effect);
                        width = Math.max(width, Mc189Compat.stringWidth(fontRenderer, text));
                    }
                }
                break;
            }
        }

        return new Dimension(width, height);
    }
}
