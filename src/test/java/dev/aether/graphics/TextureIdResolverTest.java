package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Covers the texture-id lookup that broke custom-font rendering in production.
 * <p>
 * The lookup exists because a development 1.8.9 client names the method {@code getGlTextureId()}
 * while a production client runs SRG names, where the same method is {@code func_110552_b()}. The
 * fixtures below are exactly those two environments, plus the shapes that must fail loudly rather
 * than quietly hand back texture 0 (binding 0 makes Minecraft's alpha test discard every glyph).
 */
public final class TextureIdResolverTest {
    private TextureIdResolverTest() {
    }

    public static void main(String[] args) {
        TestSupport.assertEquals("getGlTextureId", TextureIdResolver.METHOD_NAMES[0],
            "the development (MCP) name must be tried first");
        TestSupport.assertEquals("func_110552_b", TextureIdResolver.METHOD_NAMES[1],
            "the production (SRG) name must be tried second");

        developmentRuntime();
        productionRuntime();
        zeroThenSrg();
        structuralScan();
        unrelatedFuncNameIsRejected();
        missingGetterFailsLoudly();
        nullTextureFailsLoudly();
    }

    /** A dev client: the MCP name exists and the texture has a real OpenGL name. */
    private static void developmentRuntime() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new DevelopmentTexture(7));
        TestSupport.assertTrue(resolution.resolved(), "an MCP-named getter must resolve: " + resolution.describe());
        TestSupport.assertEquals(Integer.valueOf(7), Integer.valueOf(resolution.textureId()),
            "the resolved id must be the getter's value");
        TestSupport.assertEquals("getGlTextureId", resolution.resolvedBy(), "the MCP name is reported as the strategy");
        TestSupport.assertTrue(resolution.declaringClass().contains("DevelopmentTexture"),
            "the diagnostic must name the declaring class, got " + resolution.declaringClass());
        TestSupport.assertEquals(null, resolution.failure(), "a resolved lookup has no failure text");
    }

    /** A production client: only the SRG name exists, and it is inherited from a base class. */
    private static void productionRuntime() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new ObfuscatedTexture());
        TestSupport.assertTrue(resolution.resolved(),
            "a texture whose only getter is the SRG name must still resolve: " + resolution.describe());
        TestSupport.assertEquals(Integer.valueOf(41), Integer.valueOf(resolution.textureId()),
            "the inherited SRG getter supplies the id");
        TestSupport.assertEquals("func_110552_b", resolution.resolvedBy(), "the SRG name is reported as the strategy");
        TestSupport.assertTrue(resolution.declaringClass().contains("ObfuscatedTextureBase"),
            "the declaring class is the base class that actually holds the getter, got " + resolution.declaringClass());
    }

    /** A getter that answers 0 is not a texture name, so the next strategy has to be tried. */
    private static void zeroThenSrg() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new ZeroThenSrgTexture());
        TestSupport.assertTrue(resolution.resolved(), "a zero from the MCP name must not end the search");
        TestSupport.assertEquals(Integer.valueOf(58), Integer.valueOf(resolution.textureId()),
            "the SRG getter is used once the MCP one reports 0");
        TestSupport.assertEquals("func_110552_b", resolution.resolvedBy(), "the working strategy is reported");
    }

    /** Under an unknown SRG revision the shaped scan still finds the only no-argument func_ getter. */
    private static void structuralScan() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new UnknownSrgTexture());
        TestSupport.assertTrue(resolution.resolved(), "a renamed SRG getter must still be found: " + resolution.describe());
        TestSupport.assertEquals(Integer.valueOf(23), Integer.valueOf(resolution.textureId()),
            "the scanned getter supplies the id");
        TestSupport.assertEquals("scan:func_999001_a", resolution.resolvedBy(), "the scan reports the method it used");
    }

    /** A {@code func_}-prefixed name without the numeric infix is not an SRG name and must be ignored. */
    private static void unrelatedFuncNameIsRejected() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new DecoyFuncTexture());
        TestSupport.assertTrue(!resolution.resolved(),
            "a method named func_shouldNotMatch must not be mistaken for a getter");
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(resolution.textureId()),
            "an unresolved lookup reports texture 0 so the caller can log it");
    }

    /** No getter at all: the failure must name every strategy that was tried. */
    private static void missingGetterFailsLoudly() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(new BareTexture());
        TestSupport.assertTrue(!resolution.resolved(), "a texture without a getter cannot resolve");
        String failure = resolution.failure();
        TestSupport.assertTrue(failure != null && failure.length() > 0, "the failure must carry a reason");
        TestSupport.assertTrue(failure.contains("getGlTextureId"), "the MCP attempt is listed: " + failure);
        TestSupport.assertTrue(failure.contains("func_110552_b"), "the SRG attempt is listed: " + failure);
        TestSupport.assertTrue(failure.contains("scan"), "the structural attempt is listed: " + failure);
        TestSupport.assertTrue(resolution.describe().contains("unresolved"), "describe() reports the failure");
    }

    private static void nullTextureFailsLoudly() {
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(null);
        TestSupport.assertTrue(!resolution.resolved(), "a null texture cannot resolve");
        TestSupport.assertTrue(resolution.failure().contains("no texture instance"), "the reason names the null");
    }

    /* ── fixtures: one per runtime shape ─────────────────────────────────── */

    private static final class DevelopmentTexture {
        private final int id;

        DevelopmentTexture(int id) {
            this.id = id;
        }

        int getGlTextureId() {
            return id;
        }
    }

    private static class ObfuscatedTextureBase {
        private int glTextureId = -1;

        int func_110552_b() {
            if (glTextureId == -1) {
                glTextureId = 41;
            }
            return glTextureId;
        }
    }

    private static final class ObfuscatedTexture extends ObfuscatedTextureBase {
    }

    private static final class ZeroThenSrgTexture {
        int getGlTextureId() {
            return 0;
        }

        int func_110552_b() {
            return 58;
        }
    }

    private static final class UnknownSrgTexture {
        int func_999001_a() {
            return 23;
        }
    }

    private static final class DecoyFuncTexture {
        int func_shouldNotMatch() {
            return 99;
        }
    }

    private static final class BareTexture {
    }
}
