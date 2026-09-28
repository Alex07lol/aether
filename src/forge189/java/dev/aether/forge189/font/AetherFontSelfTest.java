package dev.aether.forge189.font;

import dev.aether.forge189.Mc189Compat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.Font;

/**
 * Headless verification of the custom font pipeline.
 * <p>
 * It runs against the bundled 1.8.9, LWJGL and Tessellator stubs, so it cannot see a pixel - but the
 * stubs record the vertices the renderer emits, which means the parts of the pipeline that decided
 * the production bug are all observable here: the atlas is rasterised, every glyph rect and derived
 * texture coordinate stays inside the atlas, measurement agrees with the advance the draw path uses,
 * style state does not leak out of a measurement, a missing OpenGL texture degrades instead of
 * drawing garbage, and - now that the glyphs go through the vertex pipeline rather than immediate
 * mode - every quad is emitted with the right layout, geometry, texture coordinates and colour, one
 * draw call per colour run, decorations included, so no part of a text draw reads or leaves colour
 * state.
 * <p>
 * The stub OpenGL hands out texture names, so the atlas reaches the self-upload path: the same code
 * production takes when Minecraft's texture id cannot be read.
 */
public final class AetherFontSelfTest {
    private static int checks;

    private AetherFontSelfTest() {
    }

    public static void main(String[] args) {
        GlyphPageFontRenderer medium = GlyphPageFontRenderer.create("SansSerif", 18, true, true, true);

        Check.assertNotNull(medium, "the renderer is built");
        Check.assertEquals(4, medium.pageCount(), "regular, bold, italic and bold-italic pages exist");

        GlyphPage page = medium.regularPage();
        Check.assertNotNull(page, "the regular page exists");
        Check.assertTrue(page.atlasSize() > 0, "the atlas has a real size, got " + page.atlasSize());
        Check.assertEquals(256, page.glyphCount(), "one glyph per character in the basic 256-char page");
        Check.assertTrue(page.getMaxFontHeight() > 0, "the atlas measures a font height");

        packingContract(page);
        measurementContract(medium, page);
        styleStateDoesNotLeak(medium);
        largerSizeGetsALargerAtlas(medium, page);
        textureIsAvailable(page);
        singleGlyphQuadContract(medium, page);
        wholeStringIsOneDraw(medium);
        colourCodeFlushesTheBatch(medium);
        decorationsCarryTheirOwnColour(medium);
        textIgnoresColourStateAndLeavesItWhite(medium);
        aMissingTextureDrawsNothing(medium);
        managerProvidesTheUiFont();

        System.out.println("AetherFontSelfTest passed (" + checks + " checks).");
    }

    /** Every glyph rect, and every texture coordinate derived from it, has to be inside the atlas. */
    private static void packingContract(GlyphPage page) {
        int size = page.atlasSize();
        int packed = 0;
        for (int i = 0; i < 256; i++) {
            int[] bounds = page.glyphBounds((char) i);
            Check.assertNotNull(bounds, "every character in the page has a glyph rect");
            int x = bounds[0];
            int y = bounds[1];
            int w = bounds[2];
            int h = bounds[3];
            Check.assertTrue(x >= 0 && y >= 0, "glyph rect starts inside the atlas");
            Check.assertTrue(x + w <= size, "glyph " + i + " stays inside the atlas horizontally (x=" + x + " w=" + w + " size=" + size + ")");
            Check.assertTrue(y + h <= size, "glyph " + i + " stays inside the atlas vertically (y=" + y + " h=" + h + " size=" + size + ")");
            float u = x / (float) size;
            float v = y / (float) size;
            Check.assertTrue(u >= 0.0F && u <= 1.0F, "the derived u coordinate is in range, got " + u);
            Check.assertTrue(v >= 0.0F && v <= 1.0F, "the derived v coordinate is in range, got " + v);
            Check.assertTrue(u + w / (float) size <= 1.0F, "the glyph quad does not sample past the right edge");
            Check.assertTrue(v + h / (float) size <= 1.0F, "the glyph quad does not sample past the bottom edge");
            if (i >= 33 && i <= 126) {
                // Only the printable ASCII range is guaranteed an advance: the upper half of the
                // page is Latin-1, where several code points (0x85, 0xA0, ...) are unmapped and
                // legitimately carry a zero advance.
                Check.assertTrue(w > 8, "printable glyph " + i + " has a positive advance, got " + w);
            }
            packed++;
        }
        Check.assertEquals(256, packed, "the packing check walked every glyph");
    }

    /** Measurement must equal the advance the draw path adds, or text overlaps or drifts. */
    private static void measurementContract(GlyphPageFontRenderer renderer, GlyphPage page) {
        String text = "AETHER";
        int expected = 0;
        for (int i = 0; i < text.length(); i++) {
            expected += (int) page.getWidth(text.charAt(i)) - 8;
        }
        expected /= 2;
        Check.assertEquals(Integer.valueOf(expected), Integer.valueOf(renderer.getStringWidth(text)),
            "getStringWidth matches the per-glyph advance the draw path uses");
        Check.assertTrue(renderer.getStringWidth(text) > 0, "a non-empty string measures wider than zero");
        Check.assertEquals(Integer.valueOf(0), Integer.valueOf(renderer.getStringWidth("")),
            "an empty string measures zero");
        Check.assertEquals(Integer.valueOf(0), Integer.valueOf(renderer.getStringWidth(null)),
            "a null string measures zero instead of throwing");
        Check.assertEquals(Integer.valueOf(renderer.getStringWidth("Red")),
            Integer.valueOf(renderer.getStringWidth("\u00a7cRed")),
            "a colour code adds no width");
        Check.assertTrue(renderer.getStringWidth("AETHER") < renderer.getStringWidth("AETHER AETHER"),
            "measurement grows with the text");
        Check.assertTrue(renderer.getFontHeight() > 0, "the font reports a row height");
    }

    /** Measuring a style-coded string must not change what the next measurement reports. */
    private static void styleStateDoesNotLeak(GlyphPageFontRenderer renderer) {
        int plainBefore = renderer.getStringWidth("Width");
        renderer.getStringWidth("\u00a7lBold\u00a7oItalic\u00a7r");
        int plainAfter = renderer.getStringWidth("Width");
        Check.assertEquals(Integer.valueOf(plainBefore), Integer.valueOf(plainAfter),
            "a measurement leaves the renderer's style state alone");
    }

    /** The atlas size follows the requested point size, which is what makes the four sizes real. */
    private static void largerSizeGetsALargerAtlas(GlyphPageFontRenderer medium, GlyphPage mediumPage) {
        GlyphPageFontRenderer title = GlyphPageFontRenderer.create("SansSerif", 32, true, true, true);
        Check.assertTrue(title.regularPage().atlasSize() > mediumPage.atlasSize(),
            "a 32pt atlas is larger than an 18pt one (" + title.regularPage().atlasSize() + " vs " + mediumPage.atlasSize() + ")");
        Check.assertTrue(title.getFontHeight() >= medium.getFontHeight(),
            "the larger size reports at least as tall a row");
    }

    /**
     * The atlas has to reach OpenGL. The stub's DynamicTexture cannot supply an id (which is exactly
     * what a production runtime without the SRG name does), so this asserts the fallback that the
     * production path depends on: Aether uploaded the atlas itself and got a real texture name.
     */
    private static void textureIsAvailable(GlyphPage page) {
        Check.assertTrue(page.textureId() > 0, "the atlas owns an OpenGL texture, got " + page.textureId());
        Check.assertTrue(page.textureIsOwned(), "the atlas was uploaded by Aether, not read back from Minecraft");
        Check.assertTrue(page.textureSource().contains("aether upload"),
            "the diagnostic names the upload path, got: " + page.textureSource());
    }

    /** One glyph has to produce one textured, coloured quad with the atlas rect's coordinates. */
    private static void singleGlyphQuadContract(GlyphPageFontRenderer renderer, GlyphPage page) {
        int[] bounds = page.glyphBounds('A');
        Check.assertNotNull(bounds, "the glyph rect for 'A' is known");

        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        renderer.drawString("A", 10.0F, 20.0F, 0xFF3366CC);

        Check.assertEquals(Integer.valueOf(1), Integer.valueOf(Tessellator.recordedDraws()),
            "a one-glyph string is a single draw call");
        WorldRenderer.Batch batch = WorldRenderer.lastBatch();
        Check.assertNotNull(batch, "the glyph batch was recorded");
        Check.assertEquals(Integer.valueOf(7), Integer.valueOf(batch.mode), "the glyph quad is drawn as quads");
        Check.assertTrue(batch.format == DefaultVertexFormats.POSITION_TEX_COLOR,
            "the quad carries position, texture coordinates and colour per vertex");
        Check.assertEquals(Integer.valueOf(4), Integer.valueOf(batch.vertexCount), "one quad is four vertices");

        // The renderer works in a doubled, 0.5-scaled space, so screen (10,20) is (20,40) here.
        float x0 = 20.0F;
        float y0 = 40.0F;
        float x1 = x0 + bounds[2];
        float y1 = y0 + bounds[3];
        int size = page.atlasSize();
        // Float arithmetic, so compare with a tolerance rather than exactly: the renderer adds the
        // per-glyph fractions after scaling them, and the two orders differ in the last bit.
        Check.assertTrue(close(batch.x(0), x0), "the quad starts at the pen position, got " + batch.x(0));
        Check.assertTrue(close(batch.y(0), y0), "the quad starts at the pen position, got " + batch.y(0));
        Check.assertTrue(close(batch.x(2), x1), "the quad ends one glyph width later, got " + batch.x(2));
        Check.assertTrue(close(batch.y(1), y1), "the quad ends one glyph height lower, got " + batch.y(1));

        Check.assertTrue(close(batch.u(0), bounds[0] / (float) size),
            "the u coordinate is the glyph rect's left edge, got " + batch.u(0));
        Check.assertTrue(close(batch.u(2), (bounds[0] + bounds[2]) / (float) size),
            "the u coordinate reaches the glyph rect's right edge, got " + batch.u(2));
        Check.assertTrue(close(batch.v(0), bounds[1] / (float) size),
            "the v coordinate is the glyph rect's top edge, got " + batch.v(0));
        Check.assertTrue(close(batch.v(1), (bounds[1] + bounds[3]) / (float) size),
            "the v coordinate reaches the glyph rect's bottom edge, got " + batch.v(1));

        for (int vertex = 0; vertex < 4; vertex++) {
            Check.assertTrue(batch.hasTexCoords(vertex), "vertex " + vertex + " carries texture coordinates");
            Check.assertTrue(batch.hasColor(vertex), "vertex " + vertex + " carries a colour");
        }
        // 0xFF3366CC: the colour has to reach the vertices, not just some GL state.
        Check.assertTrue(close(batch.red(0), 0x33 / 255.0F), "the vertex red channel is the requested colour");
        Check.assertTrue(close(batch.green(0), 0x66 / 255.0F), "the vertex green channel is the requested colour");
        Check.assertTrue(close(batch.blue(0), 0xCC / 255.0F), "the vertex blue channel is the requested colour");
        Check.assertTrue(close(batch.alpha(0), 1.0F), "the vertex alpha channel is the requested colour");
    }

    /** The whole point of the vertex pipeline: a string costs one draw, not one per character. */
    private static void wholeStringIsOneDraw(GlyphPageFontRenderer renderer) {
        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        renderer.drawString("Batching a whole line", 0.0F, 0.0F, 0xFFFFFFFF);
        Check.assertEquals(Integer.valueOf(1), Integer.valueOf(Tessellator.recordedDraws()),
            "a single-colour string is one draw call regardless of length");
        Check.assertEquals(Integer.valueOf("Batching a whole line".length() * 4),
            Integer.valueOf(WorldRenderer.lastBatch().vertexCount),
            "every character contributed one quad to the same batch");
    }

    /** A colour code ends the batch, and the new colour travels on the new vertices. */
    private static void colourCodeFlushesTheBatch(GlyphPageFontRenderer renderer) {
        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        renderer.drawString("A\u00a7cB", 0.0F, 0.0F, 0xFFFFFFFF);
        Check.assertEquals(Integer.valueOf(2), Integer.valueOf(Tessellator.recordedDraws()),
            "a colour change flushes the open batch and starts a new one");
        WorldRenderer.Batch second = WorldRenderer.lastBatch();
        Check.assertEquals(Integer.valueOf(4), Integer.valueOf(second.vertexCount),
            "only the character after the code is in the second batch");
        // Minecraft's chat code 'c' is #FF5555.
        Check.assertTrue(close(second.red(0), 1.0F), "the code's red channel is applied, got " + second.red(0));
        Check.assertTrue(close(second.green(0), 85 / 255.0F), "the code's green channel is applied, got " + second.green(0));
        Check.assertTrue(close(second.blue(0), 85 / 255.0F), "the code's blue channel is applied, got " + second.blue(0));
    }

    /**
     * The underline and strikethrough quads must carry the run's colour, like the glyphs do, so the
     * decoration of a coloured run cannot come out in a stale colour left by whatever drew before.
     */
    private static void decorationsCarryTheirOwnColour(GlyphPageFontRenderer renderer) {
        // Minecraft's style codes: 'm' is strikethrough, 'n' is underline, and both apply to the
        // characters that follow them.
        Check.assertEquals(Integer.valueOf(2), Integer.valueOf(decoratedBatchCount(renderer, "\u00a7mA", 0xFF3366CC)),
            "a strikethrough glyph flushes the glyph batch and draws one decoration quad");
        WorldRenderer.Batch strike = WorldRenderer.lastBatch();
        Check.assertTrue(strike.format == DefaultVertexFormats.POSITION_COLOR,
            "the strikethrough quad carries colour on the vertex instead of reading GL colour state");
        Check.assertEquals(Integer.valueOf(4), Integer.valueOf(strike.vertexCount),
            "the strikethrough decoration is one quad");
        assertRunColour(strike, "strikethrough");

        Check.assertEquals(Integer.valueOf(2), Integer.valueOf(decoratedBatchCount(renderer, "\u00a7nA", 0xFF3366CC)),
            "an underlined glyph flushes the glyph batch and draws one decoration quad");
        WorldRenderer.Batch underline = WorldRenderer.lastBatch();
        Check.assertTrue(underline.format == DefaultVertexFormats.POSITION_COLOR,
            "the underline quad carries colour on the vertex instead of reading GL colour state");
        assertRunColour(underline, "underline");

        // The decoration sits under the glyph, so it has to be as wide as the advance and nowhere
        // else - a decoration that ignored the pen position would underline the whole line.
        Check.assertTrue(underline.x(1) > underline.x(0) && close(underline.y(0), underline.y(1)),
            "the underline is a horizontal bar at the pen position, got x0=" + underline.x(0)
                + " x1=" + underline.x(1) + " y0=" + underline.y(0) + " y1=" + underline.y(1));
    }

    /**
     * Text must take its colour from the call and leave the state white. This is the property that
     * makes a text draw safe to place between two UI primitives: it neither reads a colour left by
     * the previous draw nor leaves one for the next.
     */
    private static void textIgnoresColourStateAndLeavesItWhite(GlyphPageFontRenderer renderer) {
        GlStateManager.resetLastColor();
        Mc189Compat.color(0.5F, 0.25F, 1.0F, 1.0F);
        float[] seeded = GlStateManager.lastColor();
        Check.assertTrue(close(seeded[0], 0.5F) && close(seeded[1], 0.25F) && close(seeded[2], 1.0F),
            "the recorder follows colour state, so this test can see it");

        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        renderer.drawString("A", 0.0F, 0.0F, 0xFFFFFFFF);
        WorldRenderer.Batch glyph = WorldRenderer.lastBatch();
        Check.assertNotNull(glyph, "the glyph batch was recorded");
        Check.assertTrue(close(glyph.red(0), 1.0F) && close(glyph.green(0), 1.0F)
            && close(glyph.blue(0), 1.0F) && close(glyph.alpha(0), 1.0F),
            "white text stays white while the colour state is purple, got " + glyph.red(0) + ","
                + glyph.green(0) + "," + glyph.blue(0));

        float[] after = GlStateManager.lastColor();
        Check.assertTrue(close(after[0], 1.0F) && close(after[1], 1.0F) && close(after[2], 1.0F)
            && close(after[3], 1.0F),
            "a finished text draw leaves the colour state at white, got " + after[0] + "," + after[1]
                + "," + after[2] + "," + after[3]);
    }

    private static int decoratedBatchCount(GlyphPageFontRenderer renderer, String text, int color) {
        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        renderer.drawString(text, 0.0F, 0.0F, color);
        return Tessellator.recordedDraws();
    }

    private static void assertRunColour(WorldRenderer.Batch batch, String what) {
        for (int vertex = 0; vertex < batch.vertexCount; vertex++) {
            Check.assertTrue(batch.hasColor(vertex), "the " + what + " vertex " + vertex + " carries a colour");
            Check.assertTrue(!batch.hasTexCoords(vertex), "the " + what + " is untextured, like Minecraft's");
        }
        Check.assertTrue(close(batch.red(0), 0x33 / 255.0F) && close(batch.green(0), 0x66 / 255.0F)
            && close(batch.blue(0), 0xCC / 255.0F) && close(batch.alpha(0), 1.0F),
            "the " + what + " carries the run colour, got " + batch.red(0) + "," + batch.green(0)
                + "," + batch.blue(0) + "," + batch.alpha(0));
    }

    /**
     * An atlas that never reached OpenGL must draw nothing at all. Emitting quads anyway would sample
     * whatever texture happened to be bound, which is the "panels draw, text does not" failure in a
     * different disguise.
     */
    private static void aMissingTextureDrawsNothing(GlyphPageFontRenderer usable) {
        GlyphPage bare = new GlyphPage(new Font("SansSerif", Font.PLAIN, 18), true, true);
        char[] chars = new char[256];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) i;
        }
        bare.generateGlyphPage(chars);
        Check.assertEquals(Integer.valueOf(256), Integer.valueOf(bare.glyphCount()),
            "the bare page still rasterised its glyphs");
        Check.assertEquals(Integer.valueOf(0), Integer.valueOf(bare.textureId()),
            "a page whose setupTexture was never called has no texture");
        Check.assertTrue(bare.textureSource().equals("not created"), "the diagnostic says so");

        GlyphPageFontRenderer renderer = new GlyphPageFontRenderer(bare, bare, bare, bare);
        Check.assertTrue(!renderer.isUsable(), "the renderer reports itself unusable");
        bare.bindTexture();
        bare.unbindTexture();
        Tessellator.setRecording(true);
        Tessellator.resetRecorder();
        Check.assertEquals(Integer.valueOf(0), Integer.valueOf(renderer.drawString("must not throw", 0.0F, 0.0F, 0xFFFFFFFF)),
            "drawing without a texture is a documented no-op");
        renderer.drawStringWithShadow("must not throw either", 0.0F, 0.0F, 0xFFFFFFFF);
        Check.assertEquals(Integer.valueOf(0), Integer.valueOf(Tessellator.recordedDraws()),
            "no vertices are emitted for a font with no texture");
        Check.assertTrue(renderer.getStringWidth("still measurable") > 0,
            "layout keeps working while the texture is missing, so the UI can still be laid out");

        Check.assertTrue(usable.isUsable(), "the usable renderer is unaffected by the bare page");
    }

    /** The manager has to hand the UI a font it can actually draw with, and answer consistently. */
    private static void managerProvidesTheUiFont() {
        AetherFontManager manager = AetherFontManager.instance();
        Check.assertNotNull(manager, "the manager is a singleton");
        Check.assertTrue(!manager.hasFailed(), "the font built without a failure: " + manager.diagnostic());
        GlyphPageFontRenderer first = manager.uiFont();
        Check.assertNotNull(first, "the UI font is handed out");
        Check.assertTrue(first.isUsable(), "the UI font can draw");
        Check.assertTrue(first == manager.uiFont(), "the same instance is reused between frames");
        Check.assertTrue(manager.diagnostic().length() > 0, "the manager explains itself");
        Check.assertTrue(manager.diagnostic().equals(manager.diagnostic()),
            "the diagnostic is stable between frames");
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.0005F;
    }

    private static final class Check {
        private Check() {
        }

        static void assertTrue(boolean condition, String message) {
            checks++;
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(int expected, int actual, String message) {
            checks++;
            if (expected != actual) {
                throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            checks++;
            if (expected == null ? actual != null : !expected.equals(actual)) {
                throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
            }
        }

        static void assertNotNull(Object value, String message) {
            checks++;
            if (value == null) {
                throw new AssertionError(message);
            }
        }
    }
}
