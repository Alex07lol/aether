package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.hud.HudElement;
import dev.aether.module.ClientModule.ModuleState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;

import java.awt.Dimension;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ForgeHudRenderer {
    /**
     * Fallbacks for HUD elements that carry no colour setting of their own. Both are
     * refreshed from the shared {@link AetherUi} theme tokens every frame, so the HUD
     * follows the active theme module like the screens do.
     */
    private int textColor = 0xFFF5FBFF;
    private int accentColor = 0xFF52BEEB;

    private static final long FADE_FALLBACK_MILLIS = 120L;

    private final AetherClient client;
    /** Provides live combat / toggle state; null when the renderer runs in the HUD editor. */
    private final ForgeClientEventBridge bridge;
    private final java.text.SimpleDateFormat clockFormat24 = new java.text.SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private final java.text.SimpleDateFormat clockFormat12 = new java.text.SimpleDateFormat("h:mm a", Locale.ENGLISH);
    private String cachedClockFormatId = "24h";
    private String cachedClockSample = "";
    /** Per-key release timestamps so hud.keystrokes can fade a press out. */
    private final Map<String, Long> keyReleaseTimes = new HashMap<String, Long>();
    private final Map<String, Boolean> keyDownStates = new HashMap<String, Boolean>();

    // Optimized primitive click tracking buffers (zero GC)
    private final long[] leftClickTimes = new long[128];
    private int leftClickCount = 0;
    private final long[] rightClickTimes = new long[128];
    private int rightClickCount = 0;
    private boolean lastAttackDown;
    private boolean lastUseDown;

    // Optimized FPS history ring buffer (zero GC)
    private final int[] fpsHistory = new int[60];
    private int fpsHistoryCount = 0;

    // Cached render strings (zero GC)
    private long lastClockSec = -1;
    private String cachedClockText = "Time 00:00";
    private long lastMemUpdate = -1;
    private String cachedMemText = "Mem 0/0MB";
    private String cachedDevOverlayText = "";
    private int lastFpsValue = -1;
    private String cachedFpsText = "FPS 0";
    /** The looked-at block's name only changes when the block does; no ItemStack per frame. */
    private String cachedBlockKey = "";
    private int cachedBlockMeta = Integer.MIN_VALUE;
    private String cachedBlockName = "";

    // Multi-line HUD blocks (coordinates) reuse these so we never allocate per frame.
    private final List<String> reusableHudLines = new ArrayList<String>(4);
    private final List<Integer> reusableHudColors = new ArrayList<Integer>(4);

    // Scoreboard reusable collections (zero GC)
    private final List<net.minecraft.scoreboard.Score> reusableFilteredScores = new ArrayList<net.minecraft.scoreboard.Score>(16);
    private final List<String> reusableFormattedLines = new ArrayList<String>(16);
    private final List<String> reusableFormattedScores = new ArrayList<String>(16);

    ForgeHudRenderer(AetherClient client) {
        this(client, null);
    }

    ForgeHudRenderer(AetherClient client, ForgeClientEventBridge bridge) {
        this.client = client;
        this.bridge = bridge;
    }

    void render() {
        Object minecraft = Mc189Compat.minecraft();
        Object fontRenderer = Mc189Compat.fontRenderer(minecraft);
        Object gameSettings = Mc189Compat.gameSettings(minecraft);
        if (minecraft == null || fontRenderer == null || Mc189Compat.hideGui(gameSettings)) {
            return;
        }

        syncTheme();
        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        Mc189Compat.enableTexture2D();
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);

        renderFps(fontRenderer);
        renderCoordinates(fontRenderer, minecraft);
        updateClickCounters(gameSettings);
        renderKeystrokes(fontRenderer, gameSettings);
        renderCps(fontRenderer);
        renderToggleSprint(fontRenderer, gameSettings);
        renderToggleSneak(fontRenderer);
        renderCombo(fontRenderer);
        renderMemory(fontRenderer);
        renderDayCounter(fontRenderer, minecraft);

        renderClock(fontRenderer);
        renderDeveloperOverlay(fontRenderer);
        renderBlockInfo(fontRenderer, minecraft);
        renderArmorStatus(fontRenderer, minecraft);
        renderPotionStatus(fontRenderer, minecraft);

        renderCustomCrosshair(fontRenderer, minecraft);
        renderPing(fontRenderer, minecraft);
        renderReachDisplay(fontRenderer);
        renderSpeedIndicator(fontRenderer, minecraft);
        renderServerAddress(fontRenderer, minecraft);
        renderDirection(fontRenderer, minecraft);
        renderFpsGraph(fontRenderer);

        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
        Mc189Compat.enableTexture2D();
    }

    void renderForEditor() {
        Object minecraft = Mc189Compat.minecraft();
        Object fontRenderer = Mc189Compat.fontRenderer(minecraft);
        if (minecraft == null || fontRenderer == null) {
            return;
        }

        syncTheme();

        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        Mc189Compat.enableTexture2D();
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawEditorPreview(fontRenderer, "hud.fps", "FPS 120");
        drawEditorPreview(fontRenderer, "hud.coordinates", "XYZ 100.0 / 64.0 / -200.0 North");
        drawEditorPreview(fontRenderer, "hud.cps", "CPS: 12 | 12");
        drawEditorPreview(fontRenderer, "pvp.toggle_sprint", "Sprint (Toggled)");
        drawEditorPreview(fontRenderer, "hud.memory", "Mem: 42% 1024MB");
        drawEditorPreview(fontRenderer, "hud.clock", "12:30 PM");
        drawEditorPreview(fontRenderer, "developer.overlay", "Aether v1.0 Dev Overlay");
        drawEditorPreview(fontRenderer, "hud.block_info", "Grass Block");
        drawEditorPreview(fontRenderer, "hud.ping", "24ms");
        drawEditorPreview(fontRenderer, "hud.reach_display", "3.00m");
        drawEditorPreview(fontRenderer, "hud.speed_indicator", "15.2 BPS");
        drawEditorPreview(fontRenderer, "hud.server_address", "mc.hypixel.net");
        drawEditorPreview(fontRenderer, "hud.direction", "South [S]");
        drawEditorPreview(fontRenderer, "hud.potions", "Speed II (0:30)");
        drawEditorPreview(fontRenderer, "hud.armor", "Armor Status");
        drawEditorPreview(fontRenderer, "hud.combo", "Combo 5");
        drawEditorPreview(fontRenderer, "hud.day_counter", "Day 42");
        drawEditorPreview(fontRenderer, "pvp.toggle_sneak", "Sneak (Toggled)");

        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
        Mc189Compat.enableTexture2D();
    }

    private void drawEditorPreview(Object fontRenderer, String id, String previewText) {
        HudElement element = client.hudLayout().get(id);
        if (element == null) return;
        boolean isEnabled = enabled(id);
        int textColor = isEnabled ? settingColor(id, "text_color", accentColor) : 0xAA888888;
        if (settingBool(id, "show_background", true)) {
            drawBackground(fontRenderer, previewText, element, isEnabled ? settingColor(id, "background_color", 0x6F000000) : 0x44222222);
        }
        draw(fontRenderer, previewText, element, textColor);
    }

    private void renderFps(Object fontRenderer) {
        if (!enabled("hud.fps")) {
            return;
        }
        int currentFps = Mc189Compat.debugFps();
        if (currentFps != lastFpsValue) {
            lastFpsValue = currentFps;
            cachedFpsText = "FPS " + currentFps;
        }
        HudElement element = client.hudLayout().get("hud.fps");
        if (settingBool("hud.fps", "show_background", true)) {
            drawBackground(fontRenderer, cachedFpsText, element, settingColor("hud.fps", "background_color", 0x6F000000));
        }
        draw(fontRenderer, cachedFpsText, element, settingColor("hud.fps", "text_color", accentColor));
    }

    private void renderCoordinates(Object fontRenderer, Object minecraft) {
        Object player = Mc189Compat.player(minecraft);
        if (!enabled("hud.coordinates") || player == null) {
            return;
        }
        boolean showCoordinates = settingBool("hud.coordinates", "show_coordinates", true);
        boolean hideY = settingBool("hud.coordinates", "hide_y", false);
        boolean vertical = "Vertical".equalsIgnoreCase(settingString("hud.coordinates", "mode", "Horizontal"));
        boolean showDirection = settingBool("hud.coordinates", "show_direction", true);
        String customLine = settingString("hud.coordinates", "custom_line", "");
        int coordinateColor = settingColor("hud.coordinates", "coordinates_color", textColor);
        int directionColor = settingColor("hud.coordinates", "direction_color", textColor);

        reusableHudLines.clear();
        reusableHudColors.clear();
        if (showCoordinates) {
            if (vertical) {
                if (hideY) {
                    reusableHudLines.add(String.format(Locale.ENGLISH, "X %.1f", Mc189Compat.posX(player)));
                    reusableHudColors.add(Integer.valueOf(coordinateColor));
                    reusableHudLines.add(String.format(Locale.ENGLISH, "Z %.1f", Mc189Compat.posZ(player)));
                    reusableHudColors.add(Integer.valueOf(coordinateColor));
                } else {
                    reusableHudLines.add(String.format(Locale.ENGLISH, "X %.1f", Mc189Compat.posX(player)));
                    reusableHudColors.add(Integer.valueOf(coordinateColor));
                    reusableHudLines.add(String.format(Locale.ENGLISH, "Y %.1f", Mc189Compat.posY(player)));
                    reusableHudColors.add(Integer.valueOf(coordinateColor));
                    reusableHudLines.add(String.format(Locale.ENGLISH, "Z %.1f", Mc189Compat.posZ(player)));
                    reusableHudColors.add(Integer.valueOf(coordinateColor));
                }
            } else {
                reusableHudLines.add(hideY
                    ? String.format(Locale.ENGLISH, "XZ %.1f / %.1f", Mc189Compat.posX(player), Mc189Compat.posZ(player))
                    : String.format(Locale.ENGLISH, "XYZ %.1f / %.1f / %.1f", Mc189Compat.posX(player), Mc189Compat.posY(player), Mc189Compat.posZ(player)));
                reusableHudColors.add(Integer.valueOf(coordinateColor));
            }
        }
        if (customLine != null && !customLine.trim().isEmpty()) {
            reusableHudLines.add(customLine);
            reusableHudColors.add(Integer.valueOf(coordinateColor));
        }
        String direction = showDirection ? directionText(Mc189Compat.rotationYaw(player), "Compass") : null;
        boolean inlineDirection = direction != null && !vertical && !reusableHudLines.isEmpty();
        if (direction != null && !inlineDirection) {
            reusableHudLines.add(direction);
            reusableHudColors.add(Integer.valueOf(directionColor));
        }
        if (reusableHudLines.isEmpty()) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.coordinates");
        if (settingBool("hud.coordinates", "show_background", false)) {
            int blockWidth = 0;
            for (int i = 0; i < reusableHudLines.size(); i++) {
                blockWidth = Math.max(blockWidth, Mc189Compat.stringWidth(fontRenderer, reusableHudLines.get(i)));
            }
            if (inlineDirection) {
                blockWidth += 4 + Mc189Compat.stringWidth(fontRenderer, direction);
            }
            drawBackgroundBlock(fontRenderer, element, blockWidth + 6, reusableHudLines.size() * 10 + 6,
                settingColor("hud.coordinates", "background_color", 0x6F000000));
        }

        int lineY = element.y();
        for (int i = 0; i < reusableHudLines.size(); i++) {
            String line = reusableHudLines.get(i);
            int color = reusableHudColors.get(i).intValue();
            draw(fontRenderer, line, element, lineY, color);
            if (i == 0 && inlineDirection) {
                draw(fontRenderer, direction, element.x() + Mc189Compat.stringWidth(fontRenderer, line) + 4, lineY, directionColor, element);
            }
            lineY += 10;
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
        int textColor = settingColor("hud.keystrokes", "text_color", this.textColor);
        int backgroundColor = settingColor("hud.keystrokes", "background_color", 0x6F000000);
        int pressedColor = settingColor("hud.keystrokes", "pressed_color", accentColor);
        boolean background = settingBool("hud.keystrokes", "show_background", true);
        boolean arrows = settingBool("hud.keystrokes", "arrows", false);
        int fadeTime = clamp(settingInt("hud.keystrokes", "fade_time", 75), 0, 500);
        int x = element.x();
        int y = element.y();

        if (settingBool("hud.keystrokes", "show_movement_keys", true)) {
            drawKeyBox(fontRenderer, arrows ? "^" : "W", Mc189Compat.keyForward(gameSettings), x + size + gap, y, size, size, background, backgroundColor, pressedColor, textColor, fadeTime);
            int rowY = y + size + gap;
            drawKeyBox(fontRenderer, arrows ? "<" : "A", Mc189Compat.keyLeft(gameSettings), x, rowY, size, size, background, backgroundColor, pressedColor, textColor, fadeTime);
            drawKeyBox(fontRenderer, arrows ? "v" : "S", Mc189Compat.keyBack(gameSettings), x + size + gap, rowY, size, size, background, backgroundColor, pressedColor, textColor, fadeTime);
            drawKeyBox(fontRenderer, arrows ? ">" : "D", Mc189Compat.keyRight(gameSettings), x + (size + gap) * 2, rowY, size, size, background, backgroundColor, pressedColor, textColor, fadeTime);
            y = rowY + size + gap;
        }

        if (settingBool("hud.keystrokes", "show_clicks", true)) {
            int totalMovementWidth = size * 3 + gap * 2;
            int clickWidth = (totalMovementWidth - gap) / 2;
            drawKeyBox(fontRenderer, "LMB", Mc189Compat.keyAttack(gameSettings), x, y, clickWidth, clickHeight, background, backgroundColor, pressedColor, textColor, fadeTime);
            drawKeyBox(fontRenderer, "RMB", Mc189Compat.keyUseItem(gameSettings), x + clickWidth + gap, y, clickWidth, clickHeight, background, backgroundColor, pressedColor, textColor, fadeTime);
            y += clickHeight + gap;
        }

        if (settingBool("hud.keystrokes", "show_spacebar", false)) {
            drawKeyBox(fontRenderer, "SPACE", Mc189Compat.keyJump(gameSettings), x, y, size * 3 + gap * 2, spacebarHeight, background, backgroundColor, pressedColor, textColor, fadeTime);
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
            addClick(this.leftClickTimes, this.leftClickCount++);
            if (this.leftClickCount >= this.leftClickTimes.length) this.leftClickCount = this.leftClickTimes.length;
        }
        if (useDown && !this.lastUseDown) {
            addClick(this.rightClickTimes, this.rightClickCount++);
            if (this.rightClickCount >= this.rightClickTimes.length) this.rightClickCount = this.rightClickTimes.length;
        }
        this.lastAttackDown = attackDown;
        this.lastUseDown = useDown;

        this.leftClickCount = pruneClicks(this.leftClickTimes, this.leftClickCount, now);
        this.rightClickCount = pruneClicks(this.rightClickTimes, this.rightClickCount, now);
    }

    private static void addClick(long[] array, int index) {
        if (index < array.length) {
            array[index] = System.currentTimeMillis();
        }
    }

    private static int pruneClicks(long[] array, int count, long now) {
        int valid = 0;
        for (int i = 0; i < count; i++) {
            if (now - array[i] <= 1000L) {
                array[valid++] = array[i];
            }
        }
        return valid;
    }

    private void renderCps(Object fontRenderer) {
        if (!enabled("hud.cps")) {
            return;
        }
        String countText = settingBool("hud.cps", "right_click", false)
            ? this.leftClickCount + " | " + this.rightClickCount
            : String.valueOf(this.leftClickCount);
        String text = labelValue("CPS", countText, settingString("hud.cps", "mode", "Modern"));
        HudElement element = client.hudLayout().get("hud.cps");
        if (settingBool("hud.cps", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.cps", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.cps", "text_color", textColor));
    }

    private void renderToggleSprint(Object fontRenderer, Object gameSettings) {
        if (!enabled("pvp.toggle_sprint") || !settingBool("pvp.toggle_sprint", "show_status", true) || gameSettings == null) {
            return;
        }
        // The bridge owns the toggle state, because the sprint key binding alone cannot
        // tell a forced sprint apart from the vanilla key press.
        boolean active = this.bridge != null
            ? this.bridge.toggleSprintActive()
            : Mc189Compat.keyDown(Mc189Compat.keySprint(gameSettings));
        if (!active) {
            return;
        }
        String text = "Legacy".equalsIgnoreCase(settingString("pvp.toggle_sprint", "mode", "Modern"))
            ? "[Sprinting]"
            : "Sprint (Toggled)";
        HudElement element = client.hudLayout().get("pvp.toggle_sprint");
        if (settingBool("pvp.toggle_sprint", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("pvp.toggle_sprint", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("pvp.toggle_sprint", "text_color", textColor));
    }

    private void renderDirection(Object fontRenderer, Object minecraft) {
        Object player = Mc189Compat.player(minecraft);
        if (!enabled("hud.direction") || player == null) {
            return;
        }
        boolean compass = !"Simple".equalsIgnoreCase(settingString("hud.direction", "style", "Compass"));
        String text = compass
            ? "Facing " + directionText(Mc189Compat.rotationYaw(player), "Compass")
            : directionName(Mc189Compat.rotationYaw(player));
        HudElement element = client.hudLayout().get("hud.direction");
        if (settingBool("hud.direction", "show_background", false)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.direction", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.direction", "text_color", textColor));
    }

    private void renderMemory(Object fontRenderer) {
        if (!enabled("hud.memory")) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastMemUpdate > 500L) {
            lastMemUpdate = now;
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long totalMb = runtime.totalMemory() / (1024L * 1024L);
            int percent = totalMb <= 0L ? 0 : (int) (usedMb * 100L / totalMb);
            cachedMemText = settingBool("hud.memory", "show_percent", true)
                ? "Mem " + percent + "% " + usedMb + "/" + totalMb + "MB"
                : "Mem " + usedMb + "/" + totalMb + "MB";
        }
        HudElement element = client.hudLayout().get("hud.memory");
        if (settingBool("hud.memory", "show_background", false)) {
            drawBackground(fontRenderer, cachedMemText, element, settingColor("hud.memory", "background_color", 0x6F000000));
        }
        draw(fontRenderer, cachedMemText, element, settingColor("hud.memory", "text_color", textColor));
    }


    private void renderClock(Object fontRenderer) {
        if (!enabled("hud.clock")) {
            return;
        }
        String formatId = settingString("hud.clock", "format", "24h");
        long sec = System.currentTimeMillis() / 1000L;
        if (sec != lastClockSec || !formatId.equalsIgnoreCase(cachedClockFormatId)) {
            lastClockSec = sec;
            cachedClockFormatId = formatId;
            cachedClockSample = ("12h".equalsIgnoreCase(formatId) ? clockFormat12 : clockFormat24)
                .format(new Date(sec * 1000L));
            cachedClockText = "Time " + cachedClockSample;
        }
        HudElement element = client.hudLayout().get("hud.clock");
        if (settingBool("hud.clock", "show_background", false)) {
            drawBackground(fontRenderer, cachedClockText, element, settingColor("hud.clock", "background_color", 0x6F000000));
        }
        draw(fontRenderer, cachedClockText, element, settingColor("hud.clock", "text_color", textColor));
    }

    private void renderCombo(Object fontRenderer) {
        if (!enabled("hud.combo") || this.bridge == null) {
            return;
        }
        int chance = clamp(settingInt("hud.combo", "reset_time", 2000), 250, 10000);
        if (!this.bridge.comboActive(chance)) {
            return;
        }
        String value = String.valueOf(this.bridge.comboCount());
        String text = settingBool("hud.combo", "show_label", true)
            ? labelValue("Combo", value, settingString("hud.combo", "mode", "Modern"))
            : value;
        HudElement element = client.hudLayout().get("hud.combo");
        if (settingBool("hud.combo", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.combo", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.combo", "text_color", textColor));
    }

    private void renderDayCounter(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.day_counter")) {
            return;
        }
        Object world = Mc189Compat.world(minecraft);
        if (world == null) {
            return;
        }
        long day = Mc189Compat.worldTime(world) / 24000L + 1L;
        String text = labelValue("Day", String.valueOf(day), settingString("hud.day_counter", "mode", "Modern"));
        HudElement element = client.hudLayout().get("hud.day_counter");
        if (settingBool("hud.day_counter", "show_background", false)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.day_counter", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.day_counter", "text_color", textColor));
    }

    private void renderToggleSneak(Object fontRenderer) {
        if (!enabled("pvp.toggle_sneak") || !settingBool("pvp.toggle_sneak", "show_status", true)) {
            return;
        }
        if (this.bridge == null || !this.bridge.toggleSneakActive()) {
            return;
        }
        String text = "Legacy".equalsIgnoreCase(settingString("pvp.toggle_sneak", "mode", "Modern"))
            ? "[Sneaking]"
            : "Sneak (Toggled)";
        HudElement element = client.hudLayout().get("pvp.toggle_sneak");
        if (settingBool("pvp.toggle_sneak", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("pvp.toggle_sneak", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("pvp.toggle_sneak", "text_color", textColor));
    }

    private void renderDeveloperOverlay(Object fontRenderer) {
        if (!enabled("developer.overlay")) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastMemUpdate > 500L || cachedDevOverlayText.isEmpty()) {
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long totalMb = runtime.totalMemory() / (1024L * 1024L);
            cachedDevOverlayText = "Aether " + client.version().name()
                + " | MC " + client.version().primaryGameVersion().displayName()
                + " | " + client.platform().displayName()
                + " | Mem " + usedMb + "/" + totalMb + "MB";
        }
        draw(fontRenderer, cachedDevOverlayText, client.hudLayout().get("developer.overlay"), accentColor);
    }

    private void renderBlockInfo(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.block_info")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        MovingObjectPosition mouseOver = (MovingObjectPosition) Mc189Compat.objectMouseOver(mc);
        if (mouseOver != null && Mc189Compat.typeOfHit(mouseOver) == MovingObjectPosition.MovingObjectType.BLOCK) {
            net.minecraft.util.BlockPos blockPos = (net.minecraft.util.BlockPos) Mc189Compat.blockPos(mouseOver);
            WorldClient world = Mc189Compat.theWorld(mc);
            if (world == null) return;
            IBlockState state = world.getBlockState(blockPos);
            Block block = state.getBlock();

            if (block != null && block != Blocks.air) {
                int meta = block.getMetaFromState(state);
                String blockKey = String.valueOf(block.getUnlocalizedName());
                if (!blockKey.equals(this.cachedBlockKey) || meta != this.cachedBlockMeta) {
                    this.cachedBlockKey = blockKey;
                    this.cachedBlockMeta = meta;
                    ItemStack stack = new ItemStack(block, 1, meta);
                    String name = stack.getDisplayName();
                    if (stack.getItem() == null) {
                        name = block.getLocalizedName();
                    }
                    this.cachedBlockName = name;
                }
                String blockName = this.cachedBlockName;

                HudElement element = client.hudLayout().get("hud.block_info");
                if (settingBool("hud.block_info", "show_background", true)) {
                    drawBackground(fontRenderer, blockName, element, settingColor("hud.block_info", "background_color", 0x6F000000));
                }
                draw(fontRenderer, blockName, element, settingColor("hud.block_info", "text_color", textColor));
            }
        }
    }

    private void renderArmorStatus(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.armor")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        EntityPlayerSP player = Mc189Compat.thePlayer(mc);
        if (player == null) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.armor");
        int x = element.x();
        int y = element.y();

        Mc189Compat.enableGUIStandardItemLighting();
        Mc189Compat.enableRescaleNormal();

        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = player.inventory.armorInventory[3 - i];
            if (itemStack == null) {
                continue;
            }
            
            int currentY = y + i * 18;

            Mc189Compat.renderItemAndEffectIntoGUI(itemStack, x, currentY);

            boolean showDurability = settingBool("hud.armor", "show_durability", true);
            boolean showDamage = settingBool("hud.armor", "show_damage", true);
            int maxDamage = itemStack.getMaxDamage();
            if ((showDurability || showDamage) && maxDamage > 0) {
                int damage = itemStack.getItemDamage();
                int durability = maxDamage - damage;
                int damagePercent = Math.round(damage * 100.0F / (float) maxDamage);
                String text = null;
                if (showDurability && showDamage) {
                    text = durability + " (" + damagePercent + "%)";
                } else if (showDurability) {
                    text = String.valueOf(durability);
                } else {
                    text = damagePercent + "% used";
                }
                float hue = Math.max(0.0F, durability / (float) maxDamage);
                int color = (int) (0xFF000000 | (java.awt.Color.HSBtoRGB(hue / 3.0F, 1.0F, 1.0F) & 0x00FFFFFF));
                Mc189Compat.drawStringWithShadow(fontRenderer, text, x + 20, currentY + 4, color);
            }
        }

        Mc189Compat.disableRescaleNormal();
        Mc189Compat.disableStandardItemLighting();
    }

    public void renderScoreboard() {
        if (!enabled("interface.scoreboard_customization")) {
            return;
        }

        Object minecraft = Mc189Compat.minecraft();
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        WorldClient world = Mc189Compat.theWorld(mc);
        if (world == null) {
            return;
        }

        net.minecraft.scoreboard.Scoreboard scoreboard = Mc189Compat.scoreboard(world);
        if (scoreboard == null) {
            return;
        }

        // Sidebar selection matches Forge's own: the player's team-colour sidebar, when one is
        // set, wins over the general sidebar in slot 1. The old code read slot 1 only, so on
        // servers using a coloured team sidebar this drew the wrong objective.
        net.minecraft.scoreboard.ScoreObjective objective = null;
        net.minecraft.client.entity.EntityPlayerSP ownPlayer = Mc189Compat.thePlayer((net.minecraft.client.Minecraft) minecraft);
        if (ownPlayer != null) {
            net.minecraft.scoreboard.ScorePlayerTeam ownTeam = Mc189Compat.playersTeam(scoreboard, ownPlayer.getName());
            if (ownTeam != null && ownTeam.getChatFormat() != null
                && ownTeam.getChatFormat().getColorIndex() >= 0) {
                objective = Mc189Compat.objectiveInDisplaySlot(scoreboard, 3 + ownTeam.getChatFormat().getColorIndex());
            }
        }
        if (objective == null) {
            objective = Mc189Compat.objectiveInDisplaySlot(scoreboard, 1);
        }
        if (objective == null) {
            return;
        }

        Collection<net.minecraft.scoreboard.Score> scores = Mc189Compat.sortedScores(scoreboard, objective);
        reusableFilteredScores.clear();
        for (net.minecraft.scoreboard.Score score : scores) {
            String pName = Mc189Compat.scorePlayerName(score);
            if (pName != null && !pName.startsWith("#")) {
                reusableFilteredScores.add(score);
            }
        }

        if (reusableFilteredScores.size() > 15) {
            int toRemove = reusableFilteredScores.size() - 15;
            for (int i = 0; i < toRemove; i++) {
                reusableFilteredScores.remove(0);
            }
        }

        Object fontRenderer = Mc189Compat.fontRenderer(minecraft);
        String title = Mc189Compat.objectiveDisplayName(objective);

        boolean showBackground = settingBool("interface.scoreboard_customization", "show_background", true);
        boolean textShadow = settingBool("interface.scoreboard_customization", "text_shadow", true);
        boolean hideRedNumbers = settingBool("interface.scoreboard_customization", "hide_red_numbers", false);
        int scaleSetting = settingInt("interface.scoreboard_customization", "scale", 100);
        int titleColor = settingColor("interface.scoreboard_customization", "title_color", 0xFFFFFFFF);
        int textColor = settingColor("interface.scoreboard_customization", "text_color", 0xFFFFFFFF);
        int bgColor = settingColor("interface.scoreboard_customization", "background_color", 0x6F000000);

        int maxLineWidth = Mc189Compat.stringWidth(fontRenderer, title);
        reusableFormattedLines.clear();
        reusableFormattedScores.clear();

        for (net.minecraft.scoreboard.Score score : reusableFilteredScores) {
            String pName = Mc189Compat.scorePlayerName(score);
            net.minecraft.scoreboard.ScorePlayerTeam team = Mc189Compat.playersTeam(scoreboard, pName);
            String name = Mc189Compat.formatPlayerName(team, pName);
            reusableFormattedLines.add(name);
            String scoreVal = net.minecraft.util.EnumChatFormatting.RED + "" + Mc189Compat.scorePoints(score);
            reusableFormattedScores.add(scoreVal);
            int lineWidth = Mc189Compat.stringWidth(fontRenderer, name) + (hideRedNumbers ? 0 : Mc189Compat.stringWidth(fontRenderer, "  " + scoreVal));
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }

        int fontHeight = 9;
        int totalHeight = (reusableFilteredScores.size() + 1) * fontHeight;
        int boxWidth = maxLineWidth + 6;

        net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(mc);
        int scaledW = Mc189Compat.scaledWidth(res);
        int scaledH = Mc189Compat.scaledHeight(res);

        float scaleFactor = Math.max(0.5F, Math.min(1.5F, scaleSetting / 100.0F));
        boolean isScaled = scaleFactor != 1.0F;

        if (isScaled) {
            Mc189Compat.pushMatrix();
            Mc189Compat.scale(scaleFactor, scaleFactor, 1.0F);
        }

        int rightX = (int) ((scaledW - 3) / scaleFactor);
        int startY = (int) ((scaledH / 2 - totalHeight / 2) / scaleFactor);
        int leftX = rightX - boxWidth;

        // Render Background Card
        if (showBackground) {
            Mc189Compat.drawRoundedRectangle(leftX, startY, boxWidth, totalHeight + fontHeight, 3, bgColor, 0);
        }

        // Render Title
        int titleX = leftX + (boxWidth - Mc189Compat.stringWidth(fontRenderer, title)) / 2;
        if (textShadow) {
            Mc189Compat.drawStringWithShadow(fontRenderer, title, titleX, startY + 1, titleColor);
        } else {
            drawNoShadowString(fontRenderer, title, titleX, startY + 1, titleColor);
        }

        // Render Scores
        for (int i = 0; i < reusableFilteredScores.size(); i++) {
            int lineIdx = reusableFilteredScores.size() - 1 - i;
            String lineName = reusableFormattedLines.get(lineIdx);
            String lineScore = reusableFormattedScores.get(lineIdx);
            int lineY = startY + (i + 1) * fontHeight;

            if (textShadow) {
                Mc189Compat.drawStringWithShadow(fontRenderer, lineName, leftX + 2, lineY + 1, textColor);
                if (!hideRedNumbers) {
                    int scoreX = rightX - 2 - Mc189Compat.stringWidth(fontRenderer, lineScore);
                    Mc189Compat.drawStringWithShadow(fontRenderer, lineScore, scoreX, lineY + 1, 0xFFFF5555);
                }
            } else {
                drawNoShadowString(fontRenderer, lineName, leftX + 2, lineY + 1, textColor);
                if (!hideRedNumbers) {
                    int scoreX = rightX - 2 - Mc189Compat.stringWidth(fontRenderer, lineScore);
                    drawNoShadowString(fontRenderer, lineScore, scoreX, lineY + 1, 0xFFFF5555);
                }
            }
        }

        if (isScaled) {
            Mc189Compat.popMatrix();
        }
    }

    private void drawNoShadowString(Object fontRenderer, String text, float x, float y, int color) {
        Mc189Compat.drawString(fontRenderer, text, x, y, color, false);
    }

    /** @return the number of effect lines drawn, so the HUD editor box can size itself. */
    private int renderPotionStatus(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.potions")) {
            return 0;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        EntityPlayerSP player = Mc189Compat.thePlayer(mc);
        if (player == null || player.getActivePotionEffects().isEmpty()) {
            return 0;
        }

        HudElement element = client.hudLayout().get("hud.potions");
        int x = element.x();
        int y = element.y();
        String mode = settingString("hud.potions", "mode", "Compact");
        boolean hideAmbient = settingBool("hud.potions", "hide_ambient", false);
        boolean background = settingBool("hud.potions", "show_background", false);
        int textColor = settingColor("hud.potions", "text_color", this.textColor);
        int backgroundColor = settingColor("hud.potions", "background_color", 0x6F000000);
        int lines = 0;

        for (PotionEffect effect : (Collection<PotionEffect>) player.getActivePotionEffects()) {
            // Beacon/conduit style effects last only as long as you stand in range; hiding
            // them keeps the HUD to the effects the player actually chose to carry.
            if (hideAmbient && Mc189Compat.isAmbientEffect(effect)) {
                continue;
            }
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            String name = StatCollector.translateToLocal(potion.getName());
            if (effect.getAmplifier() > 0) {
                name += " " + (effect.getAmplifier() + 1);
            }
            String text = name;
            if ("Detailed".equalsIgnoreCase(mode)) {
                text += ": " + Potion.getDurationString(effect);
            }
            if (background) {
                drawBackground(fontRenderer, text, element, backgroundColor);
            }
            int color = potion.isBadEffect() ? 0xFF5555 : textColor;
            Mc189Compat.drawStringWithShadow(fontRenderer, text, x, y, color);
            y += 10;
            lines++;
        }
        return lines;
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


    private void renderPing(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.ping")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.ping");
        int ping = Mc189Compat.playerPing(minecraft);
        String text = labelValue("Ping", (ping < 0 ? 0 : ping) + "ms", settingString("hud.ping", "mode", "Modern"));
        if (settingBool("hud.ping", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.ping", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.ping", "text_color", accentColor));
    }

    private void renderReachDisplay(Object fontRenderer) {
        if (!enabled("hud.reach_display")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.reach_display");
        String text = labelValue("Reach", String.format(Locale.ENGLISH, "%.2fm", Mc189Compat.lastReach()),
            settingString("hud.reach_display", "mode", "Modern"));
        if (settingBool("hud.reach_display", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.reach_display", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.reach_display", "text_color", accentColor));
    }

    private void renderSpeedIndicator(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.speed_indicator")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.speed_indicator");
        double bps = Mc189Compat.playerBps(minecraft);
        String text = labelValue("Speed", String.format(Locale.ENGLISH, "%.2f BPS", bps),
            settingString("hud.speed_indicator", "mode", "Modern"));
        if (settingBool("hud.speed_indicator", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.speed_indicator", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.speed_indicator", "text_color", accentColor));
    }

    private void renderServerAddress(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.server_address")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.server_address");
        String server = Mc189Compat.serverAddress(minecraft);
        String text = server == null || server.isEmpty() ? "Singleplayer" : server;
        if (!"Legacy".equalsIgnoreCase(settingString("hud.server_address", "mode", "Modern"))) {
            text = "Server " + text;
        }
        if (settingBool("hud.server_address", "show_background", true)) {
            drawBackground(fontRenderer, text, element, settingColor("hud.server_address", "background_color", 0x6F000000));
        }
        draw(fontRenderer, text, element, settingColor("hud.server_address", "text_color", accentColor));
    }


    private void renderFpsGraph(Object fontRenderer) {
        if (!enabled("hud.fps_graph")) {
            return;
        }
        HudElement element = client.hudLayout().get("hud.fps_graph");
        int fps = Mc189Compat.debugFps();

        if (fpsHistoryCount < 60) {
            fpsHistory[fpsHistoryCount++] = fps;
        } else {
            System.arraycopy(fpsHistory, 1, fpsHistory, 0, 59);
            fpsHistory[59] = fps;
        }

        int width = clamp(settingInt("hud.fps_graph", "graph_width", 80), 40, 200);
        int height = clamp(settingInt("hud.fps_graph", "graph_height", 24), 16, 80);
        int lineColor = settingColor("hud.fps_graph", "line_color", accentColor);
        int bgColor = settingColor("hud.fps_graph", "background_color", 0x6F000000);

        if (settingBool("hud.fps_graph", "show_background", true)) {
            Mc189Compat.drawRect(element.x() - 3, element.y() - 3, element.x() + width + 3, element.y() + height + 3, bgColor);
        }

        if (fpsHistoryCount < 2) return;

        int maxFps = 60;
        for (int i = 0; i < fpsHistoryCount; i++) {
            if (fpsHistory[i] > maxFps) maxFps = fpsHistory[i];
        }

        if ("Bar Chart".equalsIgnoreCase(settingString("hud.fps_graph", "graph_mode", "Sparkline"))) {
            float barWidth = (float) width / (float) fpsHistoryCount;
            for (int i = 0; i < fpsHistoryCount; i++) {
                int barHeight = Math.round((float) fpsHistory[i] / (float) maxFps * height);
                if (barHeight <= 0) continue;
                int left = Math.round(element.x() + i * barWidth);
                int right = Math.round(element.x() + (i + 1) * barWidth);
                Mc189Compat.drawRect(left, element.y() + height - barHeight, Math.max(left + 1, right), element.y() + height + 1, lineColor);
            }
            return;
        }

        float stepX = (float) width / (float) (fpsHistoryCount - 1);
        for (int i = 0; i < fpsHistoryCount - 1; i++) {
            int currentFps = fpsHistory[i];
            int nextFps = fpsHistory[i + 1];

            int x1 = Math.round(element.x() + (i * stepX));
            int y1 = Math.round(element.y() + height - ((float) currentFps / maxFps * height));
            int x2 = Math.round(element.x() + ((i + 1) * stepX));
            int y2 = Math.round(element.y() + height - ((float) nextFps / maxFps * height));

            Mc189Compat.drawRect(x1, y1, x2 + 1, y1 + 1, lineColor);
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

    private void drawKeyBox(Object fontRenderer, String label, Object keyBinding, int x, int y, int width, int height,
                            boolean background, int backgroundColor, int pressedColor, int textColor, int fadeTime) {
        boolean down = Mc189Compat.keyDown(keyBinding);
        long now = System.currentTimeMillis();
        float intensity = down ? 1.0F : 0.0F;

        if (down) {
            this.keyDownStates.put(label, Boolean.TRUE);
            this.keyReleaseTimes.remove(label);
        } else {
            if (Boolean.TRUE.equals(this.keyDownStates.put(label, Boolean.FALSE))) {
                this.keyReleaseTimes.put(label, Long.valueOf(now));
            }
            Long releasedAt = this.keyReleaseTimes.get(label);
            if (releasedAt != null) {
                long age = now - releasedAt.longValue();
                long fade = fadeTime > 0 ? fadeTime : FADE_FALLBACK_MILLIS;
                if (age >= fade) {
                    this.keyReleaseTimes.remove(label);
                } else if (age >= 0L) {
                    intensity = 1.0F - (float) age / (float) fade;
                }
            }
        }

        if (background || intensity > 0.0F) {
            int boxColor = intensity > 0.0F ? blend(backgroundColor, pressedColor, intensity) : backgroundColor;
            Mc189Compat.drawRoundedRectangle(x, y, width, height, 2, boxColor, 0);
        }
        int textX = x + (width - Mc189Compat.stringWidth(fontRenderer, label)) / 2;
        int textY = y + height / 2 - 4;
        Mc189Compat.drawStringWithShadow(fontRenderer, label, textX, textY, textColor);
    }

    /** Modern HUDs use "Label value", the legacy layouts use "Label: value". */
    private static String labelValue(String label, String value, String mode) {
        return "Legacy".equalsIgnoreCase(mode) ? label + ": " + value : label + " " + value;
    }

    /** Compass reads as "South [S]", plain text is just the direction name. */
    private static String directionText(float yaw, String style) {
        String name = directionName(yaw);
        if ("Simple".equalsIgnoreCase(style) || "Text".equalsIgnoreCase(style)) {
            return name;
        }
        return name + " [" + directionCode(name) + "]";
    }

    private static String directionCode(String name) {
        StringBuilder code = new StringBuilder(3);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0 || name.charAt(i - 1) == ' ') {
                code.append(Character.toUpperCase(c));
            }
        }
        return code.toString();
    }

    /** Linear ARGB blend, {@code t = 0} keeps {@code from}, {@code t = 1} returns {@code to}. */
    private static int blend(int from, int to, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * clamped);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * clamped);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * clamped);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
        draw(font, text, element.x(), element.y(), color, element);
    }

    /** Same as {@link #draw(Object, String, HudElement, int)} but on an explicit line of a block. */
    private static void draw(Object font, String text, HudElement element, int y, int color) {
        draw(font, text, element.x(), y, color, element);
    }

    private static void draw(Object font, String text, int x, int y, int color, HudElement element) {
        float scale = element.scale();
        float opacity = element.opacity();
        int alpha = Math.round(((color >> 24) & 0xFF) * opacity);
        if (alpha <= 0) alpha = Math.round(255 * opacity);
        int finalColor = (alpha << 24) | (color & 0x00FFFFFF);

        if (Math.abs(scale - 1.0F) > 0.001F) {
            Mc189Compat.pushMatrix();
            Mc189Compat.scale(scale, scale, 1.0F);
            Mc189Compat.drawStringWithShadow(font, text, x / scale, y / scale, finalColor);
            Mc189Compat.popMatrix();
        } else {
            Mc189Compat.drawStringWithShadow(font, text, x, y, finalColor);
        }
    }

    /** Card behind a multi-line block (coordinates with the direction beside them). */
    private static void drawBackgroundBlock(Object font, HudElement element, int width, int height, int color) {
        float scale = element.scale();
        float opacity = element.opacity();
        int alpha = Math.round(((color >> 24) & 0xFF) * opacity);
        if (alpha <= 0) alpha = Math.round(111 * opacity);
        int finalColor = (alpha << 24) | (color & 0x00FFFFFF);

        if (Math.abs(scale - 1.0F) > 0.001F) {
            Mc189Compat.pushMatrix();
            Mc189Compat.scale(scale, scale, 1.0F);
            int unscaledX = Math.round(element.x() / scale);
            int unscaledY = Math.round(element.y() / scale);
            Mc189Compat.drawRoundedRectangle(unscaledX - 3, unscaledY - 3, width + 3, height, 2, finalColor, 0);
            Mc189Compat.popMatrix();
        } else {
            Mc189Compat.drawRoundedRectangle(element.x() - 3, element.y() - 3, width + 3, height, 2, finalColor, 0);
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
            Mc189Compat.drawRoundedRectangle(unscaledX - 3, unscaledY - 3, width + 3, 14, 2, finalColor, 0);
            Mc189Compat.popMatrix();
        } else {
            Mc189Compat.drawRoundedRectangle(element.x() - 3, element.y() - 3, width + 3, 14, 2, finalColor, 0);
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

    private void syncTheme() {
        AetherUi.syncTheme();
        this.textColor = AetherUi.TEXT_PRIMARY;
        this.accentColor = AetherUi.ACCENT;
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
            case "hud.fps_graph": {
                width = clamp(settingInt("hud.fps_graph", "graph_width", 80), 40, 200) + 6;
                height = clamp(settingInt("hud.fps_graph", "graph_height", 24), 16, 80) + 6;
                break;
            }
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
                boolean vertical = "Vertical".equalsIgnoreCase(settingString("hud.coordinates", "mode", "Horizontal"));
                if (vertical) {
                    width = Mc189Compat.stringWidth(fontRenderer, "X 100.0");
                    height = 10 * (settingBool("hud.coordinates", "hide_y", false) ? 2 : 3);
                } else {
                    String text = player != null
                        ? String.format(Locale.ENGLISH, "XYZ %.1f / %.1f / %.1f South [S]", Mc189Compat.posX(player), Mc189Compat.posY(player), Mc189Compat.posZ(player))
                        : "XYZ 100.0 / 64.0 / -200.0 South [S]";
                    width = Mc189Compat.stringWidth(fontRenderer, text);
                    height = 8;
                }
                if (settingBool("hud.coordinates", "show_background", false)) {
                    width += 6;
                    height += 6;
                }
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
            case "hud.ping":
            case "hud.reach_display":
            case "hud.speed_indicator":
            case "hud.server_address":
            case "hud.combo":
            case "hud.day_counter":
            case "pvp.toggle_sprint":
            case "pvp.toggle_sneak":
            case "hud.block_info": {
                // Every simple text element shares the same "text + optional card" box.
                String text = "Sample Text 123"; // Approximation
                if (id.equals("hud.cps")) text = "CPS 10 | 10";
                if (id.equals("hud.ping")) text = "Ping 24ms";
                if (id.equals("hud.reach_display")) text = "Reach 3.00m";
                if (id.equals("hud.speed_indicator")) text = "Speed 15.20 BPS";
                if (id.equals("hud.server_address")) text = "mc.hypixel.net";
                if (id.equals("hud.combo")) text = "Combo 5";
                if (id.equals("hud.day_counter")) text = "Day 42";
                if (id.equals("pvp.toggle_sprint")) text = "Sprint (Toggled)";
                if (id.equals("pvp.toggle_sneak")) text = "Sneak (Toggled)";
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
            case "hud.clock":
            case "developer.overlay": {
                String text = "Sample Text 123"; // Approximation
                if (id.equals("hud.direction")) text = "Facing South [S]";
                if (id.equals("hud.memory")) text = "Mem 42% 1024/2048MB";
                if (id.equals("hud.clock")) text = "Time 12:30 PM";
                width = Mc189Compat.stringWidth(fontRenderer, text);
                height = 8;
                if (settingBool(id, "show_background", false)) {
                    width += 6;
                    height = 14;
                }
                break;
            }
            case "hud.armor": {
                width = 16;
                height = 4 * 18;
                boolean showDurability = settingBool("hud.armor", "show_durability", true);
                boolean showDamage = settingBool("hud.armor", "show_damage", true);
                if (showDurability || showDamage) {
                    String sample = showDurability ? (showDamage ? "100 (0%)" : "100") : "0% used";
                    width += 4 + Mc189Compat.stringWidth(fontRenderer, sample);
                }
                break;
            }
            case "hud.potions": {
                EntityPlayerSP playerSp = mc != null ? Mc189Compat.thePlayer(mc) : null;
                Collection<PotionEffect> effects = playerSp != null ? playerSp.getActivePotionEffects() : null;
                if (effects == null || effects.isEmpty()) {
                    width = Mc189Compat.stringWidth(fontRenderer, "Speed II: 0:30");
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
