package dev.aether.graphics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the OpenGL texture name owned by a Minecraft texture object.
 * <p>
 * Aether's glyph pages are backed by {@code net.minecraft.client.renderer.texture.DynamicTexture},
 * and the draw path has to bind that texture itself. Asking for the id is the one place where the
 * adapter cannot be written with a normal Java call, because Aether reaches Minecraft through
 * reflection: a reflective call site cannot be reobfuscated by ForgeGradle the way a direct call
 * can.
 * <p>
 * On a <em>development</em> runtime the method is named {@code getGlTextureId()} (the MCP name).
 * On a <em>production</em> 1.8.9 runtime Forge renames methods to their SRG names while leaving
 * class names intact, so the very same method is {@code func_110552_b()}. A lookup that only knows
 * the MCP name therefore throws {@link NoSuchMethodException} in the exact environment the user
 * runs, which is why a hard-coded lookup must never be allowed to fall back to texture 0: binding
 * texture 0 makes OpenGL sample the default 1x1 image, and Minecraft's enabled
 * {@code GL_ALPHA_TEST} then discards every glyph - rectangles keep rendering, text disappears.
 * <p>
 * The resolution strategy is therefore:
 * <ol>
 *   <li>walk the real class hierarchy (the declaring class is never assumed by name),</li>
 *   <li>try the development name, then the SRG name,</li>
 *   <li>fall back to the only no-argument {@code func_*} method on the hierarchy, which is how the
 *       id getter looks under any SRG revision,</li>
 *   <li>accept a result only when it is a positive OpenGL texture name, and</li>
 *   <li>report exactly what was tried when nothing resolves, so callers can log a real diagnostic
 *       instead of silently binding 0.</li>
 * </ol>
 * The class has no Minecraft or LWJGL dependency, so the strategy is covered by unit tests.
 */
public final class TextureIdResolver {

    /** Development name first, then the SRG name a production 1.8.9 client runs. */
    public static final String[] METHOD_NAMES = {"getGlTextureId", "func_110552_b"};

    /** Prefix used by every SRG method name; the structural pass matches only this shape. */
    private static final String SRG_PREFIX = "func_";

    private TextureIdResolver() {
    }

    /** Outcome of one resolution attempt. */
    public static final class Resolution {
        private final int textureId;
        private final String resolvedBy;
        private final String declaringClass;
        private final String failure;

        Resolution(int textureId, String resolvedBy, String declaringClass, String failure) {
            this.textureId = textureId;
            this.resolvedBy = resolvedBy;
            this.declaringClass = declaringClass;
            this.failure = failure;
        }

        /** @return true when a positive OpenGL texture name was found. */
        public boolean resolved() {
            return textureId > 0;
        }

        /** @return the OpenGL texture name, or 0 when nothing resolved. */
        public int textureId() {
            return textureId;
        }

        /** @return how the id was found: the method name, or {@code scan:<method>} for the scan. */
        public String resolvedBy() {
            return resolvedBy;
        }

        /** @return the class that actually declares the method, e.g. {@code ...AbstractTexture}. */
        public String declaringClass() {
            return declaringClass;
        }

        /** @return every attempt that failed, for a one-line warning; empty when resolved. */
        public String failure() {
            return failure;
        }

        /** @return a single human-readable line for logs and the in-game font diagnostic. */
        public String describe() {
            if (resolved()) {
                return "texture " + textureId + " via " + resolvedBy + "() on " + declaringClass;
            }
            return "unresolved (" + failure + ")";
        }

        @Override
        public String toString() {
            return describe();
        }
    }

    /**
     * Resolves the texture name of {@code texture}.
     *
     * @param texture the Minecraft texture object, e.g. a {@code DynamicTexture}; may be null.
     * @return a resolution that is either usable ({@link Resolution#resolved()}) or carries the
     *     reason it failed.
     */
    public static Resolution resolve(Object texture) {
        if (texture == null) {
            return new Resolution(0, null, null, "no texture instance was created");
        }
        Class<?> type = texture.getClass();
        List<String> attempts = new ArrayList<String>();

        for (String name : METHOD_NAMES) {
            Method method = findNoArgIntMethod(type, name, false);
            if (method == null) {
                attempts.add(name + "(): not present");
                continue;
            }
            Integer id = invokeInt(method, texture);
            if (id == null) {
                attempts.add(name + "(): threw");
                continue;
            }
            if (id.intValue() > 0) {
                return new Resolution(id.intValue(), name, method.getDeclaringClass().getName(), null);
            }
            attempts.add(name + "(): returned " + id + ", not a texture name");
        }

        // Structural pass: under any SRG revision the id getter is the only no-argument func_ method
        // on the texture hierarchy that returns int, so the id is still found when both names above
        // are wrong for the running mappings.
        Method scanned = findNoArgIntMethod(type, null, true);
        if (scanned != null) {
            Integer id = invokeInt(scanned, texture);
            if (id != null && id.intValue() > 0) {
                return new Resolution(id.intValue(), "scan:" + scanned.getName(),
                    scanned.getDeclaringClass().getName(), null);
            }
            attempts.add("scan of " + scanned.getName() + "(): returned " + id);
        } else {
            attempts.add("scan: no no-argument func_* method returning int on " + type.getName());
        }

        return new Resolution(0, null, null, join(attempts));
    }

    /**
     * Walks the hierarchy looking for a no-argument method that returns {@code int}.
     *
     * @param type the class to start from.
     * @param name the exact method name to look for, or null to accept any SRG-shaped name.
     * @param allowScan true to accept a {@code func_*} name instead of an exact one.
     * @return the method, or null when the hierarchy has no match.
     */
    static Method findNoArgIntMethod(Class<?> type, String name, boolean allowScan) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : declaredMethods(current)) {
                if (method.getParameterTypes().length != 0) {
                    continue;
                }
                if (method.getReturnType() != Integer.TYPE) {
                    continue;
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (name != null) {
                    if (!name.equals(method.getName())) {
                        continue;
                    }
                } else if (!allowScan || !looksLikeSrgName(method.getName())) {
                    continue;
                }
                if (makeAccessible(method)) {
                    return method;
                }
            }
        }
        return null;
    }

    /** @return true for {@code func_110552_b}-shaped names, which is every SRG method name. */
    private static boolean looksLikeSrgName(String name) {
        if (name == null || !name.startsWith(SRG_PREFIX)) {
            return false;
        }
        int index = SRG_PREFIX.length();
        int digits = 0;
        while (index < name.length() && Character.isDigit(name.charAt(index))) {
            index++;
            digits++;
        }
        return digits > 0 && index < name.length() && name.charAt(index) == '_';
    }

    private static Method[] declaredMethods(Class<?> type) {
        try {
            return type.getDeclaredMethods();
        } catch (Throwable unavailable) {
            // A closed or exotic class loader must degrade to "no match", never to an exception.
            return new Method[0];
        }
    }

    private static boolean makeAccessible(Method method) {
        try {
            method.setAccessible(true);
            return true;
        } catch (Throwable refused) {
            return false;
        }
    }

    private static Integer invokeInt(Method method, Object target) {
        try {
            Object value = method.invoke(target);
            return value instanceof Integer ? (Integer) value : null;
        } catch (IllegalAccessException refused) {
            return null;
        } catch (InvocationTargetException threw) {
            return null;
        } catch (Throwable refused) {
            // Linkage errors from a mapping mismatch must not take the UI down.
            return null;
        }
    }

    private static String join(List<String> attempts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < attempts.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(attempts.get(i));
        }
        return builder.toString();
    }
}
