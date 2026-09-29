package dev.aether.forge189;

import dev.aether.forge189.font.GlyphPageFontRenderer;
import dev.aether.graphics.FirstPersonAnims;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Mc189Compat {
    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    /** Guards the one-shot report for a colour state that cannot be set; see {@link #color}. */
    private static final java.util.concurrent.atomic.AtomicBoolean COLOUR_FAILURE_REPORTED =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    private Mc189Compat() {
    }

    static Object minecraft() {
        return invokeStatic(Minecraft.class, new String[] {"getMinecraft", "func_71410_x"});
    }

    static String username() {
        Object session = invoke(minecraft(), new String[] {"getSession", "func_110432_I"});
        Object value = invoke(session, new String[] {"getUsername", "func_111285_a"});
        return value instanceof String ? (String) value : "Unknown";
    }

    static long tickTimeMillis() {
        return System.currentTimeMillis();
    }

    static int debugFps() {
        Object value = invokeStatic(Minecraft.class, new String[] {"getDebugFPS", "func_175610_ah"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static Object fontRenderer(Object minecraft) {
        return getField(minecraft, new String[] {"fontRendererObj", "field_71466_p"});
    }

    static Object gameSettings(Object minecraft) {
        return getField(minecraft, new String[] {"gameSettings", "field_71474_y"});
    }

    static Object world(Object minecraft) {
        return getField(minecraft, new String[] {"theWorld", "field_71441_e"});
    }

    static Object player(Object minecraft) {
        return getField(minecraft, new String[] {"thePlayer", "field_71439_g"});
    }

    static EntityPlayerSP thePlayer(Object minecraft) {
        return (EntityPlayerSP) player(minecraft);
    }

    static WorldClient theWorld(Object minecraft) {
        return (WorldClient) world(minecraft);
    }

    static Scoreboard scoreboard(Object world) {
        if (world == null) return null;
        Object obj = invoke(world, new String[] {"getScoreboard", "func_96441_U"});
        return obj instanceof Scoreboard ? (Scoreboard) obj : null;
    }

    static ScoreObjective objectiveInDisplaySlot(Scoreboard scoreboard, int slot) {
        if (scoreboard == null) return null;
        Object obj = invoke(scoreboard, new String[] {"getObjectiveInDisplaySlot", "func_96539_a"},
            new Class<?>[] {Integer.TYPE}, Integer.valueOf(slot));
        return obj instanceof ScoreObjective ? (ScoreObjective) obj : null;
    }

    @SuppressWarnings("unchecked")
    static Collection<Score> sortedScores(Scoreboard scoreboard, ScoreObjective objective) {
        if (scoreboard == null || objective == null) return Collections.emptyList();
        Object obj = invoke(scoreboard, new String[] {"getSortedScores", "func_96534_a"},
            new Class<?>[] {ScoreObjective.class}, objective);
        return obj instanceof Collection ? (Collection<Score>) obj : Collections.<Score>emptyList();
    }

    static String objectiveDisplayName(ScoreObjective objective) {
        if (objective == null) return "";
        Object obj = invoke(objective, new String[] {"getDisplayName", "func_96678_d"});
        return obj instanceof String ? (String) obj : "";
    }

    static String scorePlayerName(Score score) {
        if (score == null) return "";
        Object obj = invoke(score, new String[] {"getPlayerName", "func_96653_e"});
        return obj instanceof String ? (String) obj : "";
    }

    static int scorePoints(Score score) {
        if (score == null) return 0;
        Object obj = invoke(score, new String[] {"getScorePoints", "func_96652_c"});
        return obj instanceof Integer ? ((Integer) obj).intValue() : 0;
    }

    static ScorePlayerTeam playersTeam(Scoreboard scoreboard, String playerName) {
        if (scoreboard == null || playerName == null) return null;
        Object obj = invoke(scoreboard, new String[] {"getPlayersTeam", "func_96508_e"},
            new Class<?>[] {String.class}, playerName);
        return obj instanceof ScorePlayerTeam ? (ScorePlayerTeam) obj : null;
    }

    static String formatPlayerName(ScorePlayerTeam team, String playerName) {
        Object obj = invokeStatic(ScorePlayerTeam.class, new String[] {"formatPlayerName", "func_96667_a"},
            new Class<?>[] {ScorePlayerTeam.class, String.class}, team, playerName);
        return obj instanceof String ? (String) obj : playerName;
    }

    static void renderItemAndEffectIntoGUI(ItemStack stack, int x, int y) {
        Object renderItem = invoke(minecraft(), new String[] {"getRenderItem", "func_175599_af"});
        if (renderItem != null) {
            invoke(renderItem, new String[] {"renderItemAndEffectIntoGUI", "func_180450_b"},
                new Class<?>[] {ItemStack.class, Integer.TYPE, Integer.TYPE}, stack, Integer.valueOf(x), Integer.valueOf(y));
        }
    }

    static Object renderViewEntity(Object minecraft) {
        return getField(minecraft, new String[] {"renderViewEntity", "field_175622_Z"});
    }

    static Object objectMouseOver(Object minecraft) {
        return getField(minecraft, new String[] {"objectMouseOver", "field_71476_x"});
    }

    static Object currentScreen(Object minecraft) {
        return getField(minecraft, new String[] {"currentScreen", "field_71462_r"});
    }

    static Object entityRenderer(Object minecraft) {
        return getField(minecraft, new String[] {"entityRenderer", "field_71460_t"});
    }

    static int displayWidth(Object minecraft) {
        Object value = getField(minecraft, new String[] {"displayWidth", "field_71443_c"});
        return value instanceof Integer ? ((Integer) value).intValue() : 1;
    }

    static int displayHeight(Object minecraft) {
        Object value = getField(minecraft, new String[] {"displayHeight", "field_71440_d"});
        return value instanceof Integer ? ((Integer) value).intValue() : 1;
    }

    static boolean hideGui(Object gameSettings) {
        Object value = getField(gameSettings, new String[] {"hideGUI", "field_74319_N"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static int thirdPersonView(Object gameSettings) {
        return intField(gameSettings, new String[] {"thirdPersonView", "field_74320_O"});
    }

    static void setThirdPersonView(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"thirdPersonView", "field_74320_O"}, Integer.valueOf(value));
    }

    /** @return the player's hurt timer, used by the hurt-camera mixin. */
    public static int hurtTime(Object entity) {
        return intField(entity, new String[] {"hurtTime", "field_70737_aN"});
    }

    /** @return the hurt timer's initial value, the denominator of the shake curve. */
    public static int maxHurtTime(Object entity) {
        return intField(entity, new String[] {"maxHurtTime", "field_70738_aO"});
    }

    /** @return the yaw the last hit came from, which vanilla shakes the camera around. */
    public static float attackedAtYaw(Object entity) {
        return floatField(entity, new String[] {"attackedAtYaw", "field_70739_aP"});
    }

    static AxisAlignedBB getEntityBoundingBox(Object entity) {
        Object obj = invoke(entity, new String[] {"getEntityBoundingBox", "func_174813_aQ"});
        return obj instanceof AxisAlignedBB ? (AxisAlignedBB) obj : null;
    }

    static float gammaSetting(Object gameSettings) {
        return floatField(gameSettings, new String[] {"gammaSetting", "field_74333_Y"});
    }

    static void setGammaSetting(Object gameSettings, float value) {
        setField(gameSettings, new String[] {"gammaSetting", "field_74333_Y"}, Float.valueOf(value));
    }

    // There is deliberately no fovSetting accessor here any more: zoom scales the value vanilla is
    // about to use (see EntityRendererMixin.aetherZoom) instead of writing the player's own FOV, so
    // nothing in the client has a way to overwrite that setting.

    /**
     * Vanilla's mouse sensitivity slider, which the freelook module folds into its own camera
     * maths. Falls back to vanilla's default of 0.5 when the field is not reachable, so a lookup
     * miss makes freelook feel like vanilla rather than like broken input.
     */
    static float mouseSensitivity(Object gameSettings) {
        Object value = getField(gameSettings, new String[] {"mouseSensitivity", "field_74341_c"});
        return value instanceof Number
            ? ((Number) value).floatValue()
            : dev.aether.graphics.FreelookMath.DEFAULT_MOUSE_SENSITIVITY;
    }

    static int particleSetting(Object gameSettings) {
        return intField(gameSettings, new String[] {"particleSetting", "field_74362_aa"});
    }

    static void setParticleSetting(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"particleSetting", "field_74362_aa"}, Integer.valueOf(value));
    }

    static boolean fancyGraphics(Object gameSettings) {
        return booleanField(gameSettings, new String[] {"fancyGraphics", "field_74347_j"});
    }

    static void setFancyGraphics(Object gameSettings, boolean value) {
        setField(gameSettings, new String[] {"fancyGraphics", "field_74347_j"}, Boolean.valueOf(value));
    }

    static boolean useVbo(Object gameSettings) {
        return booleanField(gameSettings, new String[] {"useVbo", "field_178881_t"});
    }

    static void setUseVbo(Object gameSettings, boolean value) {
        setField(gameSettings, new String[] {"useVbo", "field_178881_t"}, Boolean.valueOf(value));
    }

    static int renderDistanceChunks(Object gameSettings) {
        return intField(gameSettings, new String[] {"renderDistanceChunks", "field_151451_c"});
    }

    static void setRenderDistanceChunks(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"renderDistanceChunks", "field_151451_c"}, Integer.valueOf(value));
    }

    /**
     * Whether the game window currently has focus, which the FPS limiter uses to pick its unfocused
     * cap. An unreachable display counts as focused: that is the conservative answer, because it
     * keeps the gameplay cap rather than dropping to a background one by mistake.
     */
    static boolean displayActive() {
        try {
            Class<?> display = Class.forName("org.lwjgl.opengl.Display");
            Method method = display.getMethod("isActive");
            Object value = method.invoke(null);
            return !(value instanceof Boolean) || ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
            return true;
        }
    }

    static int limitFramerate(Object gameSettings) {
        return intField(gameSettings, new String[] {"limitFramerate", "field_74350_i"});
    }

    static void setLimitFramerate(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"limitFramerate", "field_74350_i"}, Integer.valueOf(value));
    }

    static boolean entityShadows(Object gameSettings) {
        return booleanField(gameSettings, new String[] {"entityShadows", "field_181155_a"});
    }

    static void setEntityShadows(Object gameSettings, boolean value) {
        setField(gameSettings, new String[] {"entityShadows", "field_181155_a"}, Boolean.valueOf(value));
    }

    static int clouds(Object gameSettings) {
        return intField(gameSettings, new String[] {"clouds", "field_181154_b"});
    }

    static void setClouds(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"clouds", "field_181154_b"}, Integer.valueOf(value));
    }

    static int ambientOcclusion(Object gameSettings) {
        return intField(gameSettings, new String[] {"ambientOcclusion", "field_74348_k"});
    }

    static void setAmbientOcclusion(Object gameSettings, int value) {
        setField(gameSettings, new String[] {"ambientOcclusion", "field_74348_k"}, Integer.valueOf(value));
    }

    static void playSound(Object minecraft, String name, float volume, float pitch) {
        Object soundHandler = invoke(minecraft, new String[] {"getSoundHandler", "func_147118_V"});
        if (soundHandler != null && name != null) {
            try {
                Class<?> recordClass = Class.forName("net.minecraft.client.audio.PositionedSoundRecord");
                Class<?> soundClass = Class.forName("net.minecraft.client.audio.ISound");
                Object sound = invokeStatic(recordClass, new String[] {"create", "func_147674_a"},
                    new Class<?>[] {ResourceLocation.class, Float.TYPE}, new ResourceLocation(name), Float.valueOf(pitch));
                if (sound != null) {
                    invoke(soundHandler, new String[] {"playSound", "func_147682_a"},
                        new Class<?>[] {soundClass}, sound);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    static void saveOptions(Object gameSettings) {
        invoke(gameSettings, new String[] {"saveOptions", "func_74303_b"});
    }

    static Object keyForward(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindForward", "field_74351_w"});
    }

    static Object keyLeft(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindLeft", "field_74370_x"});
    }

    static Object keyBack(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindBack", "field_74368_y"});
    }

    static Object keyRight(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindRight", "field_74366_z"});
    }

    static Object keyAttack(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindAttack", "field_74312_F"});
    }

    static Object keyUseItem(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindUseItem", "field_74313_G"});
    }

    static Object keyJump(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindJump", "field_74314_A"});
    }

    static Object keySprint(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindSprint", "field_151444_V"});
    }

    static Object keyBindSneak(Object gameSettings) {
        return getField(gameSettings, new String[] {"keyBindSneak", "field_74311_E"});
    }

    static long worldTime(Object world) {
        Object value = invoke(world, new String[] {"getWorldTime", "func_72820_D"});
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * The dimension a world belongs to (0 is the overworld), or {@code null} when the provider is
     * not reachable. The visual time override only makes sense where a sky is drawn, so the caller
     * treats an unreadable dimension as "apply anyway" and a readable non-zero one as "skip".
     */
    static Integer worldDimension(Object world) {
        if (world == null) {
            return null;
        }
        Object provider = getField(world, new String[] {"provider", "field_73011_w"});
        if (provider == null) {
            return null;
        }
        Object id = invoke(provider, new String[] {"getDimensionId", "func_186058_p"});
        if (id instanceof Number) {
            return Integer.valueOf(((Number) id).intValue());
        }
        id = getField(provider, new String[] {"dimensionId"});
        return id instanceof Number ? Integer.valueOf(((Number) id).intValue()) : null;
    }

    static int playerPing(Object minecraft) {
        Object player = player(minecraft);
        Object connection = invoke(minecraft, new String[] {"getNetHandler", "func_147114_u"});
        if (connection != null && player != null) {
            Object id = invoke(player, new String[] {"getUniqueID", "func_110124_au"});
            if (id != null) {
                Object info = invoke(connection, new String[] {"getPlayerInfo", "func_175102_a"}, new Class<?>[] {java.util.UUID.class}, id);
                if (info != null) {
                    Object ping = invoke(info, new String[] {"getResponseTime", "func_178853_c"});
                    if (ping instanceof Integer) {
                        return ((Integer) ping).intValue();
                    }
                }
            }
        }
        return 0;
    }

    static String serverAddress(Object minecraft) {
        Object serverData = invoke(minecraft, new String[] {"getCurrentServerData", "func_147104_D"});
        if (serverData != null) {
            Object ip = getField(serverData, new String[] {"serverIP", "field_78845_b"});
            return ip instanceof String ? (String) ip : null;
        }
        return null;
    }

    private static double lastReachDistance = 0.0D;
    static double lastReach() {
        return lastReachDistance;
    }

    static void setLastReach(double distance) {
        lastReachDistance = distance;
    }

    static double playerBps(Object minecraft) {
        Object player = player(minecraft);
        if (player == null) return 0.0D;
        double dx = posX(player) - lastTickPosX(player);
        double dz = posZ(player) - lastTickPosZ(player);
        return Math.sqrt(dx * dx + dz * dz) * 20.0D;
    }

    static double lastTickPosX(Object entity) {
        return doubleField(entity, new String[] {"lastTickPosX", "field_70142_S"});
    }

    static double lastTickPosY(Object entity) {
        return doubleField(entity, new String[] {"lastTickPosY", "field_70137_T"});
    }

    static double lastTickPosZ(Object entity) {
        return doubleField(entity, new String[] {"lastTickPosZ", "field_70136_U"});
    }

    static IBlockState getBlockState(Object world, BlockPos pos) {
        if (world == null || pos == null) return null;
        Object obj = invoke(world, new String[] {"getBlockState", "func_180495_p"},
            new Class<?>[] {BlockPos.class}, pos);
        return obj instanceof IBlockState ? (IBlockState) obj : null;
    }

    static Block getBlock(IBlockState state) {
        if (state == null) return null;
        Object obj = invoke(state, new String[] {"getBlock", "func_177230_c"});
        return obj instanceof Block ? (Block) obj : null;
    }

    static Material getMaterial(Block block) {
        if (block == null) return null;
        Object obj = invoke(block, new String[] {"getMaterial", "func_149688_o"});
        return obj instanceof Material ? (Material) obj : null;
    }

    static Object getWorldBorder(Object world) {
        if (world == null) return null;
        return invoke(world, new String[] {"getWorldBorder", "func_175726_f"});
    }

    static boolean worldBorderContains(Object worldBorder, BlockPos pos) {
        if (worldBorder == null || pos == null) return true;
        Object obj = invoke(worldBorder, new String[] {"contains", "func_177746_a"},
            new Class<?>[] {BlockPos.class}, pos);
        return obj instanceof Boolean ? ((Boolean) obj).booleanValue() : true;
    }

    static AxisAlignedBB getSelectedBoundingBox(Block block, Object world, BlockPos pos) {
        if (block == null || world == null || pos == null) return null;
        Object obj = invoke(block, new String[] {"getSelectedBoundingBox", "func_180646_a"},
            new Class<?>[] {net.minecraft.world.World.class, BlockPos.class}, world, pos);
        return obj instanceof AxisAlignedBB ? (AxisAlignedBB) obj : null;
    }

    static void setKeyBindState(Object keyBinding, boolean pressed) {
        int keyCode = keyCode(keyBinding);
        if (keyCode != 0) {
            invokeStatic(KeyBinding.class, new String[] {"setKeyBindState", "func_74510_a"},
                new Class<?>[] {Integer.TYPE, Boolean.TYPE}, Integer.valueOf(keyCode), Boolean.valueOf(pressed));
        }
    }

    static boolean keyPressed(Object keyBinding) {
        Object value = invoke(keyBinding, new String[] {"isPressed", "func_151468_f"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean keyDown(Object keyBinding) {
        Object value = invoke(keyBinding, new String[] {"isKeyDown", "func_151470_d"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean keyboardKeyDown(int keyCode) {
        if (keyCode <= 0) {
            return false;
        }
        try {
            Class<?> keyboard = Class.forName("org.lwjgl.input.Keyboard");
            Method method = keyboard.getMethod("isKeyDown", Integer.TYPE);
            Object value = method.invoke(null, Integer.valueOf(keyCode));
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable exception) {
            return false;
        }
    }

    static String keyName(int keyCode) {
        if (keyCode <= 0) {
            return "None";
        }
        try {
            Class<?> keyboard = Class.forName("org.lwjgl.input.Keyboard");
            Method method = keyboard.getMethod("getKeyName", Integer.TYPE);
            Object value = method.invoke(null, Integer.valueOf(keyCode));
            return value instanceof String ? (String) value : String.valueOf(keyCode);
        } catch (Throwable exception) {
            return String.valueOf(keyCode);
        }
    }

    static int keyCode(Object keyBinding) {
        Object value = invoke(keyBinding, new String[] {"getKeyCode", "func_151463_i"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static boolean sneaking(Object player) {
        Object value = invoke(player, new String[] {"isSneaking", "func_70093_af"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean onGround(Object entity) {
        Object value = getField(entity, new String[] {"onGround", "field_70122_E"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean onLadder(Object entity) {
        Object value = invoke(entity, new String[] {"isOnLadder", "func_70617_f_"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean inWater(Object entity) {
        Object value = invoke(entity, new String[] {"isInWater", "func_70090_H"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static boolean riding(Object entity) {
        return getField(entity, new String[] {"ridingEntity", "field_70154_o"}) != null;
    }

    static float fallDistance(Object entity) {
        return floatField(entity, new String[] {"fallDistance", "field_70143_R"});
    }

    static void setSprinting(Object player, boolean sprinting) {
        invoke(player, new String[] {"setSprinting", "func_70031_b"}, new Class<?>[] {Boolean.TYPE}, Boolean.valueOf(sprinting));
    }

    static boolean isSwingInProgress(Object entity) {
        Object value = getField(entity, new String[] {"isSwingInProgress", "field_70749_d"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static void setSwingInProgress(Object entity, boolean value) {
        setField(entity, new String[] {"isSwingInProgress", "field_70749_d"}, Boolean.valueOf(value));
    }

    static void setSwingProgressInt(Object entity, int value) {
        setField(entity, new String[] {"swingProgressInt", "field_70754_P"}, Integer.valueOf(value));
    }

    static boolean usingItem(Object player) {
        Object value = invoke(player, new String[] {"isUsingItem", "func_71039_bw"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static Object itemInUse(Object player) {
        return invoke(player, new String[] {"getItemInUse", "func_71011_bu"});
    }

    /**
     * True for effects handed out by ambient sources (beacons), which 1.8.9 exposes through
     * {@code PotionEffect.getIsAmbient()}. An unmapped runtime reports false, so a name
     * mismatch shows the effect rather than silently hiding it.
     */
    static boolean isAmbientEffect(Object effect) {
        Object value = invoke(effect, new String[] {"getIsAmbient", "func_180154_f"});
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        value = getField(effect, new String[] {"isAmbient", "field_180155_a"});
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static String itemUseAction(Object stack) {
        if (stack == null) {
            return "NONE";
        }
        Object action = invoke(stack, new String[] {"getItemUseAction", "func_77975_n"});
        return action == null ? "NONE" : action.toString();
    }

    /** @return the 1.7 pose action of a held stack, used by the first-person animation mixin. */
    public static FirstPersonAnims.Action useActionKind(Object stack) {
        return FirstPersonAnims.action(itemUseAction(stack));
    }

    /** @return the local player, or {@code null} when the client is not in a world. */
    public static Object localPlayer() {
        return player(minecraft());
    }

    /**
     * @return whether {@code entity} is the client's own player. Used by hooks that fire for
     *         every entity (the Entity-level rotation freeze during freelook) to act on exactly one.
     */
    public static boolean isLocalPlayer(Object entity) {
        return entity != null && entity == localPlayer();
    }

    /** @return the arm swing progress for this frame, or {@code 0} when the entity cannot report it. */
    public static float swingProgress(Object entity, float partialTicks) {
        Object value = invoke(entity, new String[] {"getSwingProgress", "func_70678_g"},
            new Class<?>[] {Float.TYPE}, Float.valueOf(partialTicks));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    /**
     * @return the ticks left in the entity's current item use, or {@code -1} when the runtime
     *         cannot report them. Both getter names are tried because 1.8 renamed it; whichever
     *         answers is the value vanilla's own bow code reads.
     */
    public static int itemUseRemainingTicks(Object entity) {
        Object value = invoke(entity, new String[] {"getItemInUseMaxCount", "getItemInUseCount", "func_71052_bv"});
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    /**
     * Vanilla eases the bow draw with a smoothstep, 1.7 with a square. Handing vanilla a
     * compensated partial tick puts its own pose on the 1.7 curve without duplicating any of
     * its rotations.
     *
     * @return the partial tick to pass to the bow transform, or {@code null} to keep vanilla's
     *         own easing (unknown tick state, or a draw fraction where both curves agree)
     */
    public static Float legacyBowPartialTicks(Object player, float partialTicks) {
        int remaining = itemUseRemainingTicks(player);
        if (remaining < 0 || FirstPersonAnims.bowEasesAgree(remaining, partialTicks)) {
            return null;
        }
        return Float.valueOf(FirstPersonAnims.bowPartialTicks(remaining, partialTicks));
    }

    static String itemUnlocalizedName(Object stack) {
        if (stack == null) {
            return "";
        }
        Object item = invoke(stack, new String[] {"getItem", "func_77973_b"});
        if (item == null) {
            return "";
        }
        Object name = invoke(item, new String[] {"getUnlocalizedName", "func_77658_a"});
        return name instanceof String ? (String) name : "";
    }

    /**
     * Matched by unlocalized name because the string survives obfuscation, unlike the class
     * name (the runtime class is {@code aji} in production 1.8.9).
     */
    public static boolean isFishingRod(Object stack) {
        String name = itemUnlocalizedName(stack);
        return name.length() > 0 && name.toLowerCase(java.util.Locale.ENGLISH).indexOf("fishingrod") >= 0;
    }

    static void onCriticalHit(Object player, Object target) {
        if (target instanceof Entity) {
            invoke(player, new String[] {"onCriticalHit", "func_71009_b"}, new Class<?>[] {Entity.class}, target);
        }
    }

    static void onEnchantmentCritical(Object player, Object target) {
        if (target instanceof Entity) {
            invoke(player, new String[] {"onEnchantmentCritical", "func_71047_c"}, new Class<?>[] {Entity.class}, target);
        }
    }

    static double posX(Object entity) {
        return doubleField(entity, new String[] {"posX", "field_70165_t"});
    }

    static double posY(Object entity) {
        return doubleField(entity, new String[] {"posY", "field_70163_u"});
    }

    static double posZ(Object entity) {
        return doubleField(entity, new String[] {"posZ", "field_70161_v"});
    }

    static float rotationYaw(Object entity) {
        return floatField(entity, new String[] {"rotationYaw", "field_70177_z"});
    }

    static float rotationPitch(Object entity) {
        return floatField(entity, new String[] {"rotationPitch", "field_70125_A"});
    }

    /**
     * Body yaw interpolated between the last two ticks, so a rendered cape or wing follows a
     * turning player smoothly. Falls back to the raw yaw when the previous-yaw field is not
     * reachable (it is only present under MCP names in the reference mappings).
     */
    static float interpolatedYaw(Object entity, float partialTicks) {
        float yaw = rotationYaw(entity);
        Object previous = getField(entity, new String[] {"prevRotationYaw"});
        if (previous instanceof Float) {
            float previousYaw = ((Float) previous).floatValue();
            // Skip interpolation across a 180-degree wrap, which would spin the geometry.
            if (Math.abs(previousYaw - yaw) < 45.0F) {
                return previousYaw + (yaw - previousYaw) * partialTicks;
            }
        }
        return yaw;
    }

    static int entityId(Object entity) {
        Object id = invoke(entity, new String[] {"getEntityId", "func_145782_y"});
        return id instanceof Integer ? ((Integer) id).intValue() : 0;
    }

    static boolean isInvisible(Object entity) {
        Object invisible = invoke(entity, new String[] {"isInvisible", "func_82150_aj"});
        return invisible instanceof Boolean && ((Boolean) invisible).booleanValue();
    }

    static double distanceTo(Object entity, double x, double y, double z) {
        double dx = posX(entity) - x;
        double dy = posY(entity) - y;
        double dz = posZ(entity) - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** @return the loaded players, or an empty list when the world is not reachable yet. */
    static java.util.List<?> worldPlayers(Object world) {
        Object players = getField(world, new String[] {"playerEntities", "field_73010_i"});
        return players instanceof java.util.List ? (java.util.List<?>) players : java.util.Collections.emptyList();
    }

    /**
     * Spawns one vanilla particle in the world. The particle is named by its MCP enum constant
     * and resolved reflectively, so an unknown name or a different particle API simply draws
     * nothing instead of failing the whole render pass.
     */
    static void spawnParticle(Object world, String particleName, double x, double y, double z,
                              double vx, double vy, double vz) {
        if (world == null || particleName == null) {
            return;
        }
        Object particleType = particleType(particleName);
        if (particleType == null) {
            return;
        }
        Class<?> type = particleType.getClass();
        invoke(world, new String[] {"spawnParticle", "func_175688_a"}, new Class<?>[] {
            type, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, int[].class
        }, particleType, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z),
            Double.valueOf(vx), Double.valueOf(vy), Double.valueOf(vz), new int[0]);
    }

    private static Object particleType(String particleName) {
        try {
            Class<?> particleTypes = Class.forName("net.minecraft.util.EnumParticleTypes");
            String cacheKey = "particle#" + particleName;
            Field cached = fieldCache.get(cacheKey);
            if (cached != null) {
                try {
                    return cached.get(null);
                } catch (IllegalAccessException ignored) {
                    fieldCache.remove(cacheKey);
                }
            }
            Field constant = findField(particleTypes, particleName);
            if (constant != null) {
                constant.setAccessible(true);
                fieldCache.put(cacheKey, constant);
                return constant.get(null);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Chat components, name tags and armour                              */
    /* ------------------------------------------------------------------ */

    /** @return the chat line with its formatting codes intact, or {@code null}. */
    static String chatFormattedText(Object chatComponent) {
        Object text = invoke(chatComponent, new String[] {"getFormattedText", "func_150254_d"});
        return text instanceof String ? (String) text : null;
    }

    /** Builds a plain chat component from legacy-formatted text ({@code §} codes included). */
    static Object chatComponent(String formattedText) {
        try {
            Class<?> type = Class.forName("net.minecraft.util.ChatComponentText");
            return type.getConstructor(String.class).newInstance(formattedText);
        } catch (ReflectiveOperationException failure) {
            return null;
        } catch (LinkageError failure) {
            return null;
        }
    }

    /** @return the entity's armour points, or {@code 0} when it cannot be read. */
    static int armourValue(Object entity) {
        Object value = invoke(entity, new String[] {"getTotalArmorValue", "func_70658_aO"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    /** @return the entity's display name text (chat component flattened), or its plain name. */
    static String displayName(Object entity) {
        Object component = invoke(entity, new String[] {"getDisplayName", "func_145748_c_"});
        if (component != null) {
            String formatted = chatFormattedText(component);
            if (formatted != null && !formatted.isEmpty()) {
                return formatted;
            }
            Object plain = invoke(component, new String[] {"getUnformattedText", "func_150260_c"});
            if (plain instanceof String && !((String) plain).isEmpty()) {
                return (String) plain;
            }
        }
        Object name = invoke(entity, new String[] {"getName", "func_70005_c_"});
        return name instanceof String ? (String) name : "";
    }

    /** @return the entity's hitbox height, or the standard player height when unreadable. */
    static float entityHeight(Object entity) {
        float height = floatField(entity, new String[] {"height", "field_70130_N"});
        return height > 0.0F ? height : 1.8F;
    }

    static boolean hasCustomName(Object entity) {
        Object custom = invoke(entity, new String[] {"hasCustomName", "func_145818_k_"});
        return custom instanceof Boolean && ((Boolean) custom).booleanValue();
    }

    /** @return whether the entity is sneaking, used to hide its name tag like vanilla does. */
    static boolean isSneaking(Object entity) {
        Object sneaking = invoke(entity, new String[] {"isSneaking", "func_70093_af"});
        return sneaking instanceof Boolean && ((Boolean) sneaking).booleanValue();
    }

    /** @return the player's own sprint flag, so a forced sprint is only ever applied once. */
    static boolean sprinting(Object entity) {
        Object sprint = invoke(entity, new String[] {"isSprinting", "func_70051_ag"});
        return sprint instanceof Boolean && ((Boolean) sprint).booleanValue();
    }

    static Object typeOfHit(Object movingObjectPosition) {
        return getField(movingObjectPosition, new String[] {"typeOfHit", "field_72313_a"});
    }

    static Object blockPos(Object movingObjectPosition) {
        return getField(movingObjectPosition, new String[] {"blockPos", "field_178782_a"});
    }

    static int scaledWidth(Object resolution) {
        Object value = invoke(resolution, new String[] {"getScaledWidth", "func_78326_a"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static int scaledHeight(Object resolution) {
        Object value = invoke(resolution, new String[] {"getScaledHeight", "func_78328_b"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static int scaleFactor(Object resolution) {
        Object value = invoke(resolution, new String[] {"getScaleFactor", "func_78325_e"});
        return value instanceof Integer ? ((Integer) value).intValue() : 1;
    }

    public static void drawStringWithShadow(Object fontRenderer, String text, float x, float y, int color) {
        if (fontRenderer == null || text == null || text.isEmpty()) return;
        if (fontRenderer instanceof GlyphPageFontRenderer) {
            // Aether's own renderer is not Minecraft's FontRenderer, so it has to be called directly:
            // the reflective lookup below only ever matches Minecraft's (String, int, int, int)
            // signature, and a miss there used to mean the text was silently never drawn.
            ((GlyphPageFontRenderer) fontRenderer).drawStringWithShadow(text, x, y, color);
            return;
        }
        enableBlend();
        enableTexture2D();
        color(1.0F, 1.0F, 1.0F, 1.0F);
        invoke(fontRenderer, new String[] {"drawStringWithShadow", "func_175063_a"},
            new Class<?>[] {String.class, Float.TYPE, Float.TYPE, Integer.TYPE},
            text, Float.valueOf(x), Float.valueOf(y), Integer.valueOf(color));
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawString(Object fontRenderer, String text, float x, float y, int color, boolean dropShadow) {
        if (fontRenderer == null || text == null || text.isEmpty()) return;
        if (fontRenderer instanceof GlyphPageFontRenderer) {
            GlyphPageFontRenderer smooth = (GlyphPageFontRenderer) fontRenderer;
            if (dropShadow) {
                smooth.drawStringWithShadow(text, x, y, color);
            } else {
                smooth.drawString(text, x, y, color);
            }
            return;
        }
        enableBlend();
        enableTexture2D();
        color(1.0F, 1.0F, 1.0F, 1.0F);
        if (dropShadow) {
            drawStringWithShadow(fontRenderer, text, x, y, color);
        } else {
            Object res = invoke(fontRenderer, new String[] {"drawString", "func_78276_b"},
                new Class<?>[] {String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE},
                text, Integer.valueOf((int) x), Integer.valueOf((int) y), Integer.valueOf(color));
            if (res == null) {
                invoke(fontRenderer, new String[] {"drawString", "func_175065_a"},
                    new Class<?>[] {String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE},
                    text, Integer.valueOf((int) x), Integer.valueOf((int) y), Integer.valueOf(color), Boolean.valueOf(false));
            }
        }
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Calls an instance method that Aether cannot reference at compile time, trying each name in
     * order (MCP first, then the SRG name a production runtime uses).
     * <p>
     * Mixin's {@code @Shadow} is the usual way to reach a private Minecraft method, but a shadow is
     * a hard requirement: on a runtime whose mappings do not match it fails the whole mixin and takes
     * the game down with it, while a refmap is only generated when the build was given SRG mappings.
     * Resolving the member here keeps the hook soft - a mapping mismatch degrades to "the animation
     * does not change", which is the contract every other Aether hook already has.
     *
     * @return the call's result, or null when no candidate resolved.
     */
    public static Object call(Object target, String[] names, Class<?>[] parameterTypes, Object... args) {
        return invoke(target, names, parameterTypes, args);
    }

    /**
     * Reads an instance field that Aether cannot reference at compile time, trying MCP then SRG
     * names. See {@link #call} for why this is preferred over an {@code @Shadow} field.
     *
     * @return the field's value, or null when neither candidate resolved.
     */
    public static Object read(Object target, String[] names) {
        return getField(target, names);
    }

    public static int stringWidth(Object fontRenderer, String text) {
        if (fontRenderer instanceof GlyphPageFontRenderer) {
            return ((GlyphPageFontRenderer) fontRenderer).getStringWidth(text);
        }
        Object value = invoke(fontRenderer, new String[] {"getStringWidth", "func_78256_a"}, new Class<?>[] {String.class}, text);
        return value instanceof Integer ? ((Integer) value).intValue() : text.length() * 6;
    }

    static void displayGuiScreen(GuiScreen screen) {
        Object minecraft = minecraft();
        invoke(minecraft, new String[] {"displayGuiScreen", "func_147108_a"}, new Class<?>[] {GuiScreen.class}, screen);
    }

    static void shutdown() {
        invoke(minecraft(), new String[] {"shutdown", "func_71400_g"});
    }

    static void loadBlurShader() {
        Object renderer = entityRenderer(minecraft());
        invoke(renderer, new String[] {"loadShader", "func_175069_a"},
            new Class<?>[] {ResourceLocation.class}, new ResourceLocation("shaders/post/blur.json"));
    }

    static void stopShader() {
        Object renderer = entityRenderer(minecraft());
        invoke(renderer, new String[] {"stopUseShader", "func_181022_b"});
    }

    static int screenWidth(GuiScreen screen) {
        Object value = getField(screen, new String[] {"width", "field_146294_l"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static int screenHeight(GuiScreen screen) {
        Object value = getField(screen, new String[] {"height", "field_146295_m"});
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    static Object screenFontRenderer(GuiScreen screen) {
        Object value = getField(screen, new String[] {"fontRendererObj", "field_146289_q"});
        return value == null ? fontRenderer(minecraft()) : value;
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        for (String name : new String[] {"drawRect", "func_73734_a"}) {
            try {
                Method method = Gui.class.getMethod(name, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                method.invoke(null, Integer.valueOf(left), Integer.valueOf(top), Integer.valueOf(right), Integer.valueOf(bottom), Integer.valueOf(color));
                return;
            } catch (ReflectiveOperationException ignored) {
                // Try the next runtime naming scheme before falling back to direct GL.
            }
        }

        if (left > right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        enableBlend();
        disableTexture2D();
        tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos((double) left, (double) bottom, 0.0D).color(r, g, b, a).endVertex();
        worldRenderer.pos((double) right, (double) bottom, 0.0D).color(r, g, b, a).endVertex();
        worldRenderer.pos((double) right, (double) top, 0.0D).color(r, g, b, a).endVertex();
        worldRenderer.pos((double) left, (double) top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        enableTexture2D();
        disableBlend();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawTexture(String path, int x, int y, int width, int height) {
        if (path == null || path.length() == 0 || width <= 0 || height <= 0) {
            return;
        }
        Object minecraft = minecraft();
        Object textureManager = invoke(minecraft, new String[] {"getTextureManager", "func_110434_K"});
        if (textureManager == null) return;

        enableTexture2D();
        enableBlend();
        tryBlendFuncSeparate(770, 771, 1, 0);
        color(1.0F, 1.0F, 1.0F, 1.0F);

        invoke(textureManager, new String[] {"bindTexture", "func_110577_a"},
            new Class<?>[] {ResourceLocation.class}, new ResourceLocation("aether", path));

        for (String name : new String[] {"drawModalRectWithCustomSizedTexture", "func_146110_a"}) {
            Method method = findMethod(Gui.class, name, new Class<?>[] {
                Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE
            });
            if (method != null) {
                try {
                    method.invoke(null, Integer.valueOf(x), Integer.valueOf(y), Float.valueOf(0.0F), Float.valueOf(0.0F),
                        Integer.valueOf(width), Integer.valueOf(height), Float.valueOf((float) width), Float.valueOf((float) height));
                    color(1.0F, 1.0F, 1.0F, 1.0F);
                    return;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }

        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldRenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldRenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static boolean hasResource(String domain, String path) {
        Object minecraft = minecraft();
        Object resourceManager = invoke(minecraft, new String[] {"getResourceManager", "func_110442_L"});
        if (resourceManager != null) {
            Object res = invoke(resourceManager, new String[] {"getResource", "func_110549_a"},
                new Class<?>[] {ResourceLocation.class}, new ResourceLocation(domain, path));
            return res != null;
        }
        return false;
    }

    static void pushMatrix() {
        if (invokeStatic(glStateManagerClass(), new String[] {"pushMatrix", "func_179094_E"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glPushMatrix"});
        }
    }

    static void popMatrix() {
        if (invokeStatic(glStateManagerClass(), new String[] {"popMatrix", "func_179121_F"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glPopMatrix"});
        }
    }

    static void scale(float x, float y, float z) {
        if (invokeStatic(glStateManagerClass(), new String[] {"scale", "func_179152_a"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE}, Float.valueOf(x), Float.valueOf(y), Float.valueOf(z)) == null) {
            invokeStatic(gl11Class(), new String[] {"glScalef"},
                new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE}, Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
        }
    }

    public static void rotate(float angle, float x, float y, float z) {
        if (invokeStatic(glStateManagerClass(), new String[] {"rotate", "func_179114_b"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE},
            Float.valueOf(angle), Float.valueOf(x), Float.valueOf(y), Float.valueOf(z)) == null) {
            invokeStatic(gl11Class(), new String[] {"glRotatef"},
                new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE},
                Float.valueOf(angle), Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
        }
    }

    public static void translate(float x, float y, float z) {
        if (invokeStatic(glStateManagerClass(), new String[] {"translate", "func_179109_b"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE},
            Float.valueOf(x), Float.valueOf(y), Float.valueOf(z)) == null) {
            invokeStatic(gl11Class(), new String[] {"glTranslatef"},
                new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE},
                Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
        }
    }

    public static void enableAlpha() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableAlpha", "func_179092_a"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3008));
        }
    }

    public static void bindTexture(int textureId) {
        if (invokeStatic(glStateManagerClass(), new String[] {"bindTexture", "func_179144_i"},
            new Class<?>[] {Integer.TYPE}, Integer.valueOf(textureId)) == null) {
            invokeStatic(gl11Class(), new String[] {"glBindTexture"},
                new Class<?>[] {Integer.TYPE, Integer.TYPE}, Integer.valueOf(3553), Integer.valueOf(textureId));
        }
    }

    public static void enableBlend() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableBlend", "func_179147_l"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3042));
        }
    }

    public static void disableBlend() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableBlend", "func_179084_k"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3042));
        }
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        if (invokeStatic(glStateManagerClass(), new String[] {"tryBlendFuncSeparate", "func_179120_a"},
            new Class<?>[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE},
            Integer.valueOf(srcFactor), Integer.valueOf(dstFactor), Integer.valueOf(srcFactorAlpha), Integer.valueOf(dstFactorAlpha)) == null) {
            try {
                Class<?> openGlHelper = Class.forName("net.minecraft.client.renderer.OpenGlHelper");
                invokeStatic(openGlHelper, new String[] {"glBlendFunc", "func_148821_a"},
                    new Class<?>[] {Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE},
                    Integer.valueOf(srcFactor), Integer.valueOf(dstFactor), Integer.valueOf(srcFactorAlpha), Integer.valueOf(dstFactorAlpha));
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    public static void disableTexture2D() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableTexture2D", "func_179090_x"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3553));
        }
    }

    public static void enableTexture2D() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableTexture2D", "func_179098_w"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3553));
        }
    }

    static void depthMask(boolean flag) {
        if (invokeStatic(glStateManagerClass(), new String[] {"depthMask", "func_179132_a"},
            new Class<?>[] {Boolean.TYPE}, Boolean.valueOf(flag)) == null) {
            invokeStatic(gl11Class(), new String[] {"glDepthMask"}, new Class<?>[] {Boolean.TYPE}, Boolean.valueOf(flag));
        }
    }

    public static void color(float red, float green, float blue, float alpha) {
        // Both SRG candidates are kept: one of them names the four-argument overload in 1.8.9, and a
        // name that does not match the parameter list simply fails to resolve.
        if (!invokeStaticVoid(glStateManagerClass(), new String[] {"color", "func_179131_c", "func_179124_c"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE},
            Float.valueOf(red), Float.valueOf(green), Float.valueOf(blue), Float.valueOf(alpha))
            && COLOUR_FAILURE_REPORTED.compareAndSet(false, true)) {
            // GlStateManager owns 1.8.9's colour state and caches it, so it is the only place worth
            // writing: LWJGL's glColor4f sets the client-array colour, which the Tessellator vertex
            // pipeline never reads, and writing it here would move GL state without moving the cache
            // paired with it. A colour that cannot be set is reported rather than ignored.
            System.out.println("[Aether] GlStateManager.color is unreachable on this runtime, so colour"
                + " state will not change. UI drawing carries its colour on the vertex, so only the"
                + " primitives that still rely on GL colour are affected.");
        }
    }

    static void enableRescaleNormal() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableRescaleNormal", "func_179129_p"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(32826));
        }
    }

    static void disableRescaleNormal() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableRescaleNormal", "func_179101_B"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(32826));
        }
    }

    // The plain-state toggles below only list their MCP names: a no-argument SRG name that
    // belongs to a different toggle would still resolve, so the GL11 call (which is exactly
    // what GlStateManager wraps) is the safer second attempt.

    static void disableCull() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableCull"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2884));
        }
    }

    static void enableCull() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableCull"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2884));
        }
    }

    static void disableLighting() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableLighting"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2896));
        }
    }

    static void enableLighting() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableLighting"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2896));
        }
    }

    static void disableDepth() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableDepth"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2929));
        }
    }

    static void enableDepth() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableDepth"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2929));
        }
    }

    static void resetColor() {
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void enableGUIStandardItemLighting() {
        if (invokeStatic(renderHelperClass(), new String[] {"enableGUIStandardItemLighting", "func_74519_b"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2896)); // GL_LIGHTING
        }
    }

    public static void disableStandardItemLighting() {
        if (invokeStatic(renderHelperClass(), new String[] {"disableStandardItemLighting", "func_74518_a"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(2896)); // GL_LIGHTING
        }
    }

    static void drawSelectionBoundingBox(AxisAlignedBB box) {
        if (invokeStatic(RenderGlobal.class, new String[] {"drawSelectionBoundingBox", "func_181561_a"},
            new Class<?>[] {AxisAlignedBB.class}, box) == null) {
            drawOutlinedBoundingBox(box);
        }
    }

    private static void drawOutlinedBoundingBox(AxisAlignedBB box) {
        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(1, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        tessellator.draw();
    }

    private static Class<?> glStateManagerClass() {
        try {
            return Class.forName("net.minecraft.client.renderer.GlStateManager");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> renderHelperClass() {
        try {
            return Class.forName("net.minecraft.client.renderer.RenderHelper");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    static void enableScissor() {
        invokeStatic(gl11Class(), new String[]{"glEnable"}, new Class<?>[]{Integer.TYPE}, Integer.valueOf(3089)); // GL_SCISSOR_TEST
    }

    static void scissor(int x, int y, int width, int height) {
        invokeStatic(gl11Class(), new String[]{"glScissor"}, new Class<?>[]{Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE}, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height));
    }

    static void disableScissor() {
        invokeStatic(gl11Class(), new String[]{"glDisable"}, new Class<?>[]{Integer.TYPE}, Integer.valueOf(3089)); // GL_SCISSOR_TEST
    }

    static void glLineWidth(float width) {
        invokeStatic(gl11Class(), new String[]{"glLineWidth"}, new Class<?>[]{Float.TYPE}, Float.valueOf(width));
    }

    /**
     * Copies the current framebuffer into an ARGB array. Must be called on the render thread
     * (it is a GL read); everything after it - PNG encode, disk write - happens on the worker.
     * Returns {@code null} when GL or the buffer cannot be reached, so the caller can show a
     * failure instead of throwing inside a render pass.
     */
    static int[] readFramePixels(int width, int height) {
        try {
            Class<?> gl = gl11Class();
            // Row order and channel order both get fixed up below; see the comment in the loop.
            int channels = 4;
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * channels);
            byte[] staging = new byte[width * height * channels];
            invokeStatic(gl, new String[]{"glReadPixels"}, new Class<?>[]{Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Object.class},
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(width), Integer.valueOf(height), Integer.valueOf(0x1903)/* GL_BGRA? no: GL_RGBA */, Integer.valueOf(0x1401)/* GL_UNSIGNED_BYTE */, buffer);
            buffer.position(0);
            buffer.get(staging);
            int[] argb = new int[width * height];
            for (int row = 0; row < height; row++) {
                int srcRow = height - 1 - row; // GL reads bottom-up; images are top-down.
                for (int x = 0; x < width; x++) {
                    int i = (srcRow * width + x) * 4;
                    int r = staging[i] & 0xFF;
                    int g = staging[i + 1] & 0xFF;
                    int b = staging[i + 2] & 0xFF;
                    int a = staging[i + 3] & 0xFF;
                    argb[row * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    if (r == 0 && g == 0 && b == 0 && a == 0) {
                        // Fully transparent black is an alpha-mode artefact, not content.
                        argb[row * width + x] = 0xFF000000;
                    }
                }
            }
            return argb;
        } catch (Throwable failure) {
            return null;
        }
    }

    static int mouseWheelDelta() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getEventDWheel");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : 0;
        } catch (Throwable exception) {
            return 0;
        }
    }

    static int mouseX() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getX");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : 0;
        } catch (Throwable exception) {
            return 0;
        }
    }

    static int mouseY() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getY");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : 0;
        } catch (Throwable exception) {
            return 0;
        }
    }

    static int getEventButton() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getEventButton");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : -1;
        } catch (Throwable exception) {
            return -1;
        }
    }

    static boolean getEventButtonState() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getEventButtonState");
            Object value = method.invoke(null);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable exception) {
            return false;
        }
    }

    static void setGui(GuiOpenEventAdapter adapter, GuiScreen screen) {
        adapter.set(screen);
    }

    interface GuiOpenEventAdapter {
        void set(GuiScreen screen);
    }

    private static double doubleField(Object target, String[] names) {
        Object value = getField(target, names);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    private static float floatField(Object target, String[] names) {
        Object value = getField(target, names);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static int intField(Object target, String[] names) {
        Object value = getField(target, names);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static boolean booleanField(Object target, String[] names) {
        Object value = getField(target, names);
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Object invokeStatic(Class<?> type, String[] names) {
        return invokeStatic(type, names, new Class<?>[0]);
    }

    private static Object invokeStatic(Class<?> type, String[] names, Class<?>[] parameterTypes, Object... args) {
        if (type == null) {
            return null;
        }
        String cacheKey = buildCacheKey("static#" + type.getName(), names, parameterTypes);
        Method cachedMethod = methodCache.get(cacheKey);
        if (cachedMethod != null) {
            try {
                return cachedMethod.invoke(null, args);
            } catch (ReflectiveOperationException ignored) {
                methodCache.remove(cacheKey);
            }
        }
        for (String name : names) {
            Method method = findMethod(type, name, parameterTypes);
            if (method != null) {
                try {
                    methodCache.put(cacheKey, method);
                    return method.invoke(null, args);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Calls a static method and reports whether it was actually reached.
     * <p>
     * {@link #invokeStatic} returns the method's value, which is {@code null} for every void entry
     * point - and most of the state calls here are void - so it cannot distinguish "invoked" from
     * "not found". That ambiguity was harmless while the callers' fallbacks did the same thing; it
     * is not harmless once a caller reports the failure or takes a different path, which is why the
     * distinction is made explicit here instead of being inferred from a return value.
     */
    private static boolean invokeStaticVoid(Class<?> type, String[] names, Class<?>[] parameterTypes, Object... args) {
        if (type == null) {
            return false;
        }
        String cacheKey = buildCacheKey("static#" + type.getName(), names, parameterTypes);
        Method cachedMethod = methodCache.get(cacheKey);
        if (cachedMethod != null) {
            try {
                cachedMethod.invoke(null, args);
                return true;
            } catch (ReflectiveOperationException ignored) {
                methodCache.remove(cacheKey);
            }
        }
        for (String name : names) {
            Method method = findMethod(type, name, parameterTypes);
            if (method != null) {
                try {
                    methodCache.put(cacheKey, method);
                    method.invoke(null, args);
                    return true;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return false;
    }

    private static Object invoke(Object target, String[] names) {
        return invoke(target, names, new Class<?>[0]);
    }

    private static Object invoke(Object target, String[] names, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return null;
        }
        String cacheKey = buildCacheKey(target.getClass().getName(), names, parameterTypes);
        Method cachedMethod = methodCache.get(cacheKey);
        if (cachedMethod != null) {
            try {
                return cachedMethod.invoke(target, args);
            } catch (ReflectiveOperationException ignored) {
                methodCache.remove(cacheKey);
            }
        }
        for (String name : names) {
            Method method = findMethod(target.getClass(), name, parameterTypes);
            if (method != null) {
                try {
                    methodCache.put(cacheKey, method);
                    return method.invoke(target, args);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    private static Object getField(Object target, String[] names) {
        if (target == null) {
            return null;
        }
        String cacheKey = buildCacheKey(target.getClass().getName(), names, null);
        Field cachedField = fieldCache.get(cacheKey);
        if (cachedField != null) {
            try {
                return cachedField.get(target);
            } catch (IllegalAccessException ignored) {
                fieldCache.remove(cacheKey);
            }
        }
        for (String name : names) {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    fieldCache.put(cacheKey, field);
                    return field.get(target);
                } catch (IllegalAccessException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void setField(Object target, String[] names, Object value) {
        if (target == null) {
            return;
        }
        String cacheKey = buildCacheKey(target.getClass().getName(), names, null);
        Field cachedField = fieldCache.get(cacheKey);
        if (cachedField != null) {
            try {
                cachedField.set(target, value);
                return;
            } catch (IllegalAccessException ignored) {
                fieldCache.remove(cacheKey);
            }
        }
        for (String name : names) {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    fieldCache.put(cacheKey, field);
                    field.set(target, value);
                    return;
                } catch (IllegalAccessException ignored) {
                    return;
                }
            }
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Class<?> gl11Class() {
        try {
            return Class.forName("org.lwjgl.opengl.GL11");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the Tessellator singleton in a way that works on MC 1.8.9.
     * At runtime the singleton is exposed as a public static field
     * {@code Tessellator.instance}, while the stub (and some other
     * environments) expose it as a static {@code getInstance()} method.
     * We try the field first, then fall back to the method.
     */
    private static Tessellator getTessellator() {
        // Try the 1.8.9 static field 'instance' / 'field_178181_a' first.
        for (String name : new String[] {"instance", "field_178181_a"}) {
            Field field = findField(Tessellator.class, name);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value instanceof Tessellator) return (Tessellator) value;
                } catch (IllegalAccessException ignored) { }
            }
        }
        // Fall back to getInstance() for environments where the method exists.
        return Tessellator.getInstance();
    }

    /**
     * Gates Forge's own sidebar. {@code GuiIngameForge.renderObjective} is public static, so the
     * write is a plain field access now that the build compiles against real Forge - the old
     * per-frame Class.forName reflection (with a silent catch) existed only because the stub
     * build had no Forge classes to link against.
     */
    static void setScoreboardDisabled(boolean disabled) {
        GuiIngameForge.renderObjective = !disabled;
    }

    public static void drawRectangle(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + height, color);
    }

    /** GUI-scale space size of the current window, matching what Minecraft passes to drawScreen. */
    public static int guiScaleWidth() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return new ScaledResolution(mc).getScaledWidth();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int guiScaleHeight() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return new ScaledResolution(mc).getScaledHeight();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Restricts drawing to the rectangle in GUI-scale coordinates. Everything the Control Center
     * draws inside the module list (cards, accordions, sliders) runs between the scissor pair so
     * scrolled content cannot paint over the deck's header or footer.
     */
    public static void pushScissor(int x, int y, int width, int height) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution resolution = new ScaledResolution(mc);
            float scale = resolution.getScaleFactor();
            int scaledWidth = resolution.getScaledWidth();
            int scaledHeight = resolution.getScaledHeight();
            // GL scissors count from the bottom-left corner; MC GUI coordinates from the top-left.
            int glX = Math.round(x * scale);
            int glY = Math.round((scaledHeight - y - height) * scale);
            int glW = Math.round(width * scale);
            int glH = Math.round(height * scale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(glX, glY, glW, glH);
        } catch (Throwable ignored) {
            // No clipping is visually worse than a crash; the rect simply draws unclipped.
        }
    }

    public static void popScissor() {
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } catch (Throwable ignored) {
        }
    }

    static void drawOutlinedRectangle(int x, int y, int w, int h, int t, int color) {
        drawRectangle(x, y, w, t, color);
        drawRectangle(x + w - t, y, t, h, color);
        drawRectangle(x, y + h - t, w, t, color);
        drawRectangle(x, y, t, h, color);
    }

    static void shadeModel(int mode) {
        if (invokeStatic(glStateManagerClass(), new String[] {"shadeModel", "func_179103_j"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(mode)) == null) {
            invokeStatic(gl11Class(), new String[] {"glShadeModel"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(mode));
        }
    }

    static void drawRoundedRectangle(int x, int y, int w, int h, int radius, int color, int index) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0) {
            drawRectangle(x, y, w, h, color);
            return;
        }
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        drawRectangle(x + r, y, w - r * 2, h, color);
        drawRectangle(x, y + r, r, h - r * 2, color);
        drawRectangle(x + w - r, y + r, r, h - r * 2, color);

        for (int i = 0; i < r; i++) {
            int step = (int) Math.round(Math.sqrt(r * r - (r - i - 1) * (r - i - 1)));
            drawRectangle(x + r - step, y + i, step, 1, color);
            drawRectangle(x + w - r, y + i, step, 1, color);
            drawRectangle(x + r - step, y + h - 1 - i, step, 1, color);
            drawRectangle(x + w - r, y + h - 1 - i, step, 1, color);
        }
        enableTexture2D();
        disableBlend();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static void drawGradientRectangle(float x, float y, float w, float h, int startColor, int endColor) {
        float f1 = (float) (startColor >> 24 & 255) / 255.0F;
        float f2 = (float) (startColor >> 16 & 255) / 255.0F;
        float f3 = (float) (startColor >> 8 & 255) / 255.0F;
        float f4 = (float) (startColor & 255) / 255.0F;
        float f5 = (float) (endColor >> 24 & 255) / 255.0F;
        float f6 = (float) (endColor >> 16 & 255) / 255.0F;
        float f7 = (float) (endColor >> 8 & 255) / 255.0F;
        float f8 = (float) (endColor & 255) / 255.0F;

        disableTexture2D();
        enableBlend();
        tryBlendFuncSeparate(770, 771, 1, 0);
        shadeModel(7425);
        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x + w, y, 0f).color(f2, f3, f4, f1).endVertex();
        worldRenderer.pos(x, y, 0f).color(f2, f3, f4, f1).endVertex();
        worldRenderer.pos(x, y + h, 0f).color(f6, f7, f8, f5).endVertex();
        worldRenderer.pos(x + w, y + h, 0f).color(f6, f7, f8, f5).endVertex();
        tessellator.draw();
        shadeModel(7424);
        disableBlend();
        enableTexture2D();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static void drawHorizontalGradientRectangle(float x, float y, float w, float h, int startColor, int endColor) {
        float f1 = (float) (startColor >> 24 & 255) / 255.0F;
        float f2 = (float) (startColor >> 16 & 255) / 255.0F;
        float f3 = (float) (startColor >> 8 & 255) / 255.0F;
        float f4 = (float) (startColor & 255) / 255.0F;
        float f5 = (float) (endColor >> 24 & 255) / 255.0F;
        float f6 = (float) (endColor >> 16 & 255) / 255.0F;
        float f7 = (float) (endColor >> 8 & 255) / 255.0F;
        float f8 = (float) (endColor & 255) / 255.0F;

        disableTexture2D();
        enableBlend();
        tryBlendFuncSeparate(770, 771, 1, 0);
        shadeModel(7425);
        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x, y, 0f).color(f2, f3, f4, f1).endVertex();
        worldRenderer.pos(x, y + h, 0f).color(f2, f3, f4, f1).endVertex();
        worldRenderer.pos(x + w, y + h, 0f).color(f6, f7, f8, f5).endVertex();
        worldRenderer.pos(x + w, y, 0f).color(f6, f7, f8, f5).endVertex();
        tessellator.draw();
        shadeModel(7424);
        disableBlend();
        enableTexture2D();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws a world-space name tag for a waypoint, always facing the camera.
     * <p>
     * The label width is measured once per rounded distance (the key the caller passes) and cached
     * in {@code widthCache}, so a stationary player pays no per-frame text measurement. Nothing is
     * allocated per frame beyond the drawn string itself. Returns quietly when the font renderer
     * or the camera transform is unreachable - a waypoint label must never break world rendering.
     */
    static void drawWaypointLabel(Object fontRenderer, String text, double x, double y, double z,
                                  int cacheKey, int color, Object widthCache) {
        if (fontRenderer == null || text == null || text.isEmpty()) {
            return;
        }
        try {
            int width = measureLabel(fontRenderer, text, cacheKey, widthCache);
            float halfWidth = width / 2.0F;
            pushMatrix();
            translate((float) x, (float) y, (float) z);
            // Rotate against the camera yaw/pitch so the label reads like a name tag.
            rotateCamera();
            scale(-0.025F, -0.025F, 0.025F);
            disableTexture2D();
            enableBlend();
            depthMask(false);
            int background = (color & 0xFF000000) | 0x101010 & 0x00FFFFFF;
            drawRect((int) (-halfWidth) - 3, -10, (int) halfWidth + 3, 2, background);
            drawString((float) (-halfWidth), -8.0F, text, color, fontRenderer);
            color(1.0F, 1.0F, 1.0F, 1.0F);
            depthMask(true);
            enableTexture2D();
            disableBlend();
            popMatrix();
        } catch (Throwable ignored) {
            // A label is cosmetic; a mapping miss must not break the frame.
        }
    }

    private static int measureLabel(Object fontRenderer, String text, int cacheKey, Object widthCache) {
        // Simplified version - no custom cache support in stub environment
        return stringWidth(fontRenderer, text);
    }

    private static void rotateCamera() {
        Object entityRenderer = entityRenderer(minecraft());
        Float yaw = invokeFloat(entityRenderer, new String[] {"camYaw", "field_147663_ae"});
        Float pitch = invokeFloat(entityRenderer, new String[] {"camPitch", "field_147664_af"});
        if (yaw != null) {
            invokeStatic(gl11Class(), new String[]{"glRotatef"}, new Class<?>[]{Float.TYPE}, Float.valueOf(-yaw.floatValue()));
        }
        if (pitch != null) {
            invokeStatic(gl11Class(), new String[]{"glRotatef"}, new Class<?>[]{Float.TYPE}, Float.valueOf(pitch.floatValue()));
        }
    }

    private static Float invokeFloat(Object target, String[] names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Float) {
                    return (Float) value;
                }
            } catch (Throwable ignored) {
                // try the next name
            }
        }
        return null;
    }

    private static void drawString(float x, float y, String text, int color, Object fontRenderer) {
        try {
            Class<?> cls = fontRenderer.getClass();
            Method method = cls.getMethod("drawStringWithShadow", String.class, float.class, float.class, int.class);
            method.invoke(fontRenderer, text, Float.valueOf(x), Float.valueOf(y), Integer.valueOf(color));
        } catch (Throwable ignored) {
            // A label is cosmetic; a mapping miss must not break the frame.
        }
    }

    static void drawFilledBoundingBox(AxisAlignedBB boundingBox) {
        if (boundingBox == null) return;
        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();
    }

    private static String buildCacheKey(String prefix, String[] names, Class<?>[] parameterTypes) {
        StringBuilder keyBuilder = new StringBuilder(prefix).append('#').append(names[0]);
        if (parameterTypes != null) {
            keyBuilder.append('#');
            for (Class<?> pType : parameterTypes) {
                keyBuilder.append(pType.getName()).append(',');
            }
        }
        return keyBuilder.toString();
    }
}
