package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.hud.HudElement;
import dev.aether.module.ClientModule.ModuleState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderHelper;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

final class ForgeHudRenderer {
    private static final int TEXT_COLOR = 0xF5FBFF;
    private static final int ACCENT_COLOR = 0x52BEEB;

    private final AetherClient client;
    private final java.text.SimpleDateFormat clockFormat = new java.text.SimpleDateFormat("HH:mm", Locale.ENGLISH);

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

    // Scoreboard reusable collections (zero GC)
    private final List<net.minecraft.scoreboard.Score> reusableFilteredScores = new ArrayList<net.minecraft.scoreboard.Score>(16);
    private final List<String> reusableFormattedLines = new ArrayList<String>(16);
    private final List<String> reusableFormattedScores = new ArrayList<String>(16);

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
        renderMemory(fontRenderer);

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

        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
        Mc189Compat.enableTexture2D();
    }

    private void drawEditorPreview(Object fontRenderer, String id, String previewText) {
        HudElement element = client.hudLayout().get(id);
        if (element == null) return;
        boolean isEnabled = enabled(id);
        int textColor = isEnabled ? settingColor(id, "text_color", ACCENT_COLOR) : 0xAA888888;
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
        draw(fontRenderer, cachedFpsText, element, settingColor("hud.fps", "text_color", ACCENT_COLOR));
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
            HudElement element = client.hudLayout().get("hud.coordinates");
            if (settingBool("hud.coordinates", "show_background", false)) {
                drawBackground(fontRenderer, text, element, settingColor("hud.coordinates", "background_color", 0x6F000000));
            }
            draw(fontRenderer, text, element, settingColor("hud.coordinates", "coordinates_color", TEXT_COLOR));
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
        String text = settingBool("hud.cps", "right_click", false)
            ? "CPS " + this.leftClickCount + " | " + this.rightClickCount
            : "CPS " + this.leftClickCount;
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
        // keyDown on the sprint key binding reflects the toggled state because
        // applyToggleSprint calls setKeyBindState to keep it in sync.
        Object sprintKey = Mc189Compat.keySprint(gameSettings);
        if (sprintKey == null || !Mc189Compat.keyDown(sprintKey)) {
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
        long now = System.currentTimeMillis();
        if (now - lastMemUpdate > 500L) {
            lastMemUpdate = now;
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
            long totalMb = runtime.totalMemory() / (1024L * 1024L);
            cachedMemText = "Mem " + usedMb + "/" + totalMb + "MB";
        }
        draw(fontRenderer, cachedMemText, client.hudLayout().get("hud.memory"), TEXT_COLOR);
    }


    private void renderClock(Object fontRenderer) {
        if (!enabled("hud.clock")) {
            return;
        }
        long sec = System.currentTimeMillis() / 1000L;
        if (sec != lastClockSec) {
            lastClockSec = sec;
            cachedClockText = "Time " + clockFormat.format(new Date(sec * 1000L));
        }
        draw(fontRenderer, cachedClockText, client.hudLayout().get("hud.clock"), TEXT_COLOR);
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
        draw(fontRenderer, cachedDevOverlayText, client.hudLayout().get("developer.overlay"), ACCENT_COLOR);
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
        EntityPlayerSP player = Mc189Compat.thePlayer(mc);
        if (player == null) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.armor");
        int x = element.x();
        int y = element.y();

        RenderHelper.enableGUIStandardItemLighting();
        Mc189Compat.enableRescaleNormal();

        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = player.inventory.armorInventory[3 - i];
            if (itemStack == null) {
                continue;
            }
            
            int currentY = y + i * 18;

            Mc189Compat.renderItemAndEffectIntoGUI(itemStack, x, currentY);

            if (settingBool("hud.armor", "show_durability", true) && itemStack.isItemDamaged()) {
                int durability = itemStack.getMaxDamage() - itemStack.getItemDamage();
                String text = String.valueOf(durability);
                float hue = Math.max(0.0F, (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) / (float) itemStack.getMaxDamage());
                int color = java.awt.Color.HSBtoRGB(hue / 3.0F, 1.0F, 1.0F);
                Mc189Compat.drawStringWithShadow(fontRenderer, text, x + 20, currentY + 4, color);
            }
        }

        Mc189Compat.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
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

        net.minecraft.scoreboard.ScoreObjective objective = Mc189Compat.objectiveInDisplaySlot(scoreboard, 1);
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

    private void renderPotionStatus(Object fontRenderer, Object minecraft) {
        if (!enabled("hud.potions")) {
            return;
        }
        net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft) minecraft;
        EntityPlayerSP player = Mc189Compat.thePlayer(mc);
        if (player == null || player.getActivePotionEffects().isEmpty()) {
            return;
        }

        HudElement element = client.hudLayout().get("hud.potions");
        int x = element.x();
        int y = element.y();
        String mode = settingString("hud.potions", "mode", "Compact");

        for (PotionEffect effect : (Collection<PotionEffect>) player.getActivePotionEffects()) {
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
        int lineColor = settingColor("hud.fps_graph", "line_color", ACCENT_COLOR);
        int bgColor = settingColor("hud.fps_graph", "background_color", 0x6F000000);

        if (settingBool("hud.fps_graph", "show_background", true)) {
            Mc189Compat.drawRect(element.x() - 3, element.y() - 3, element.x() + width + 3, element.y() + height + 3, bgColor);
        }

        if (fpsHistoryCount < 2) return;

        int maxFps = 60;
        for (int i = 0; i < fpsHistoryCount; i++) {
            if (fpsHistory[i] > maxFps) maxFps = fpsHistory[i];
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

    private void drawKeyBox(Object fontRenderer, String label, Object keyBinding, int x, int y, int width, int height, boolean background, int backgroundColor, int pressedColor, int textColor) {
        boolean down = Mc189Compat.keyDown(keyBinding);
        if (background || down) {
            Mc189Compat.drawRoundedRectangle(x, y, width, height, 2, down ? pressedColor : backgroundColor, 0);
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
                String text = player != null
                    ? String.format(Locale.ENGLISH, "XYZ %.1f / %.1f / %.1f %s", Mc189Compat.posX(player), Mc189Compat.posY(player), Mc189Compat.posZ(player), "North")
                    : "XYZ 100.0 / 64.0 / -200.0 North";
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
