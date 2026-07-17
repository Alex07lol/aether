package dev.aether.forge189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Mc189Compat {
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
        } catch (ReflectiveOperationException exception) {
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
        } catch (ReflectiveOperationException exception) {
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

    static void drawStringWithShadow(Object fontRenderer, String text, float x, float y, int color) {
        invoke(fontRenderer, new String[] {"drawStringWithShadow", "func_175063_a"},
            new Class<?>[] {String.class, Float.TYPE, Float.TYPE, Integer.TYPE},
            text, Float.valueOf(x), Float.valueOf(y), Integer.valueOf(color));
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
                // Try the next runtime naming scheme.
            }
        }
    }

    static void drawTexture(String path, int x, int y, int width, int height) {
        if (path == null || path.length() == 0 || width <= 0 || height <= 0) {
            return;
        }
        Object minecraft = minecraft();
        Object textureManager = invoke(minecraft, new String[] {"getTextureManager", "func_110434_K"});
        invoke(textureManager, new String[] {"bindTexture", "func_110577_a"},
            new Class<?>[] {ResourceLocation.class}, new ResourceLocation("aether", path));
        invokeStatic(Gui.class, new String[] {"drawModalRectWithCustomSizedTexture", "func_146110_a"},
            new Class<?>[] {Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE, Integer.TYPE, Float.TYPE, Float.TYPE},
            Integer.valueOf(x), Integer.valueOf(y), Float.valueOf(0.0F), Float.valueOf(0.0F),
            Integer.valueOf(width), Integer.valueOf(height), Float.valueOf(width), Float.valueOf(height));
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
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    static int mouseX() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getX");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : 0;
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    static int mouseY() {
        try {
            Class<?> mouse = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouse.getMethod("getY");
            Object value = method.invoke(null);
            return value instanceof Integer ? ((Integer) value).intValue() : 0;
        } catch (ReflectiveOperationException exception) {
            return 0;
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
            try {
                Method method = type.getMethod(name, parameterTypes);
                method.setAccessible(true);
                methodCache.put(cacheKey, method);
                return method.invoke(null, args);
            } catch (ReflectiveOperationException ignored) {
                // Try the next runtime naming scheme.
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
            try {
                Method method = target.getClass().getMethod(name, parameterTypes);
                method.setAccessible(true);
                methodCache.put(cacheKey, method);
                return method.invoke(target, args);
            } catch (ReflectiveOperationException ignored) {
                // Try the next runtime naming scheme.
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
