package dev.aether.forge189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
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

    static int hurtTime(Object entity) {
        return intField(entity, new String[] {"hurtTime", "field_70737_aN"});
    }

    static void setHurtTime(Object entity, int value) {
        setField(entity, new String[] {"hurtTime", "field_70737_aN"}, Integer.valueOf(value));
    }

    static int maxHurtTime(Object entity) {
        return intField(entity, new String[] {"maxHurtTime", "field_70738_aO"});
    }

    static void setMaxHurtTime(Object entity, int value) {
        setField(entity, new String[] {"maxHurtTime", "field_70738_aO"}, Integer.valueOf(value));
    }

    static float attackedAtYaw(Object entity) {
        return floatField(entity, new String[] {"attackedAtYaw", "field_70739_aP"});
    }

    static AxisAlignedBB getEntityBoundingBox(Object entity) {
        Object obj = invoke(entity, new String[] {"getEntityBoundingBox", "func_174813_aQ"});
        return obj instanceof AxisAlignedBB ? (AxisAlignedBB) obj : null;
    }

    static void setAttackedAtYaw(Object entity, float value) {
        setField(entity, new String[] {"attackedAtYaw", "field_70739_aP"}, Float.valueOf(value));
    }

    static float gammaSetting(Object gameSettings) {
        return floatField(gameSettings, new String[] {"gammaSetting", "field_74333_Y"});
    }

    static void setGammaSetting(Object gameSettings, float value) {
        setField(gameSettings, new String[] {"gammaSetting", "field_74333_Y"}, Float.valueOf(value));
    }

    static float fovSetting(Object gameSettings) {
        return floatField(gameSettings, new String[] {"fovSetting", "field_74334_X"});
    }

    static void setFovSetting(Object gameSettings, float value) {
        setField(gameSettings, new String[] {"fovSetting", "field_74334_X"}, Float.valueOf(value));
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

    static void setWorldTime(Object world, long time) {
        invoke(world, new String[] {"setWorldTime", "func_72877_b"}, new Class<?>[] {Long.TYPE}, Long.valueOf(time));
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

    static Object worldInfo(Object world) {
        return invoke(world, new String[] {"getWorldInfo", "func_72912_H"});
    }

    static void setWorldRain(Object world, boolean raining) {
        invoke(world, new String[] {"setRainStrength", "func_72894_k"}, new Class<?>[] {Float.TYPE}, Float.valueOf(raining ? 1.0F : 0.0F));
        invoke(world, new String[] {"setThunderStrength", "func_147442_i"}, new Class<?>[] {Float.TYPE}, Float.valueOf(raining ? 1.0F : 0.0F));
        Object info = worldInfo(world);
        invoke(info, new String[] {"setRaining", "func_76084_b"}, new Class<?>[] {Boolean.TYPE}, Boolean.valueOf(raining));
        invoke(info, new String[] {"setThundering", "func_76069_a"}, new Class<?>[] {Boolean.TYPE}, Boolean.valueOf(raining));
        invoke(info, new String[] {"setRainTime", "func_76080_g"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(0));
        invoke(info, new String[] {"setThunderTime", "func_76090_f"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(0));
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

    static int stringWidth(Object fontRenderer, String text) {
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

    static void drawRect(int left, int top, int right, int bottom, int color) {
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
        color(r, g, b, a);

        Tessellator tessellator = getTessellator();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos((double) left, (double) bottom, 0.0D).endVertex();
        worldRenderer.pos((double) right, (double) bottom, 0.0D).endVertex();
        worldRenderer.pos((double) right, (double) top, 0.0D).endVertex();
        worldRenderer.pos((double) left, (double) top, 0.0D).endVertex();
        tessellator.draw();

        enableTexture2D();
        disableBlend();
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static void drawTexture(String path, int x, int y, int width, int height) {
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
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldRenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
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

    public static void setAngles(Object entity, float yaw, float pitch) {
        invoke(entity, new String[] {"setAngles", "func_70082_c"},
            new Class<?>[] {Float.TYPE, Float.TYPE}, Float.valueOf(yaw), Float.valueOf(pitch));
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

    static void enableBlend() {
        if (invokeStatic(glStateManagerClass(), new String[] {"enableBlend", "func_179147_l"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glEnable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3042));
        }
    }

    static void disableBlend() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableBlend", "func_179084_k"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3042));
        }
    }

    static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
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

    static void disableTexture2D() {
        if (invokeStatic(glStateManagerClass(), new String[] {"disableTexture2D", "func_179090_x"}) == null) {
            invokeStatic(gl11Class(), new String[] {"glDisable"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(3553));
        }
    }

    static void enableTexture2D() {
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

    static void color(float red, float green, float blue, float alpha) {
        if (invokeStatic(glStateManagerClass(), new String[] {"color", "func_179131_c", "func_179124_c"},
            new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE},
            Float.valueOf(red), Float.valueOf(green), Float.valueOf(blue), Float.valueOf(alpha)) == null) {
            invokeStatic(gl11Class(), new String[] {"glColor4f"},
                new Class<?>[] {Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE},
                Float.valueOf(red), Float.valueOf(green), Float.valueOf(blue), Float.valueOf(alpha));
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

    static void setScoreboardDisabled(boolean disabled) {
        try {
            Class<?> guiIngameForge = Class.forName("net.minecraftforge.client.GuiIngameForge");
            Field field = guiIngameForge.getDeclaredField("renderObjective");
            field.setAccessible(true);
            field.setBoolean(null, !disabled);
        } catch (Throwable ignored) {
        }
    }

    static void drawRectangle(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + height, color);
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

    static void drawCircle(float x, float y, float r, int h, int j, int color) {
        enableBlend();
        disableTexture2D();

        float a = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        color(red, g, b, a);

        Class<?> gl = gl11Class();
        invokeStatic(gl, new String[] {"glBegin"}, new Class<?>[] {Integer.TYPE}, Integer.valueOf(6));
        invokeStatic(gl, new String[] {"glVertex2f"}, new Class<?>[] {Float.TYPE, Float.TYPE}, Float.valueOf(x), Float.valueOf(y));

        for (float var = h; var <= j; var++) {
            color(red, g, b, a);
            float vx = (float) (r * Math.cos(Math.PI * var / 180) + x);
            float vy = (float) (r * Math.sin(Math.PI * var / 180) + y);
            invokeStatic(gl, new String[] {"glVertex2f"}, new Class<?>[] {Float.TYPE, Float.TYPE}, Float.valueOf(vx), Float.valueOf(vy));
        }

        invokeStatic(gl, new String[] {"glEnd"});
        enableTexture2D();
        disableBlend();
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
