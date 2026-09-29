package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.forge189.font.AetherFontManager;
import dev.aether.forge189.font.GlyphPage;
import dev.aether.forge189.font.GlyphPageFontRenderer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

/**
 * The minimal glyph-render test: one black background, one custom-font string, one magnified
 * textured glyph, and the raw glyph atlas next to a readout of where the atlas texture came from.
 * <p>
 * A whole-UI bug hunt cannot tell these three failures apart, and they need completely different
 * fixes:
 * <ul>
 *   <li>the atlas is empty - the AWT rasterisation is wrong;</li>
 *   <li>the atlas has glyphs but the GL texture is missing or bound as 0 - Minecraft's alpha test
 *       then discards every fragment, so panels draw and text does not;</li>
 *   <li>the atlas and texture are fine but the layout offsets are wrong.</li>
 * </ul>
 * The atlas preview shows the first case, the readout shows the second, and the string shows the
 * third. It is reachable with F9 from the Control Center and is intentionally tiny: it is a
 * diagnostic, not a feature.
 */
public final class AetherFontDiagScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final int PREVIEW_SIZE = 96;
    private static final float GLYPH_SCALE = 6.0F;

    private final AetherClient client;
    private final GuiScreen parent;

    public AetherFontDiagScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render();
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void render() {
        AetherUi.syncTheme();
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        if (width <= 0 || height <= 0) {
            return;
        }

        // 1. A black background, so anything that fails to draw is genuinely missing rather than
        //    indistinguishable from a dark theme.
        Mc189Compat.drawRect(0, 0, width, height, 0xFF000000);

        AetherFontManager fonts = AetherFontManager.instance();
        GlyphPageFontRenderer smooth = fonts.uiFont();
        Object vanilla = Mc189Compat.screenFontRenderer(this);

        int y = 16;
        AetherUi.text(vanilla, "Aether font diagnostic (F9) - ESC returns", 12, y, 0xFFFFD166);
        y += 18;

        if (smooth == null) {
            AetherUi.text(vanilla, "Custom font unavailable - the UI is drawing with the Minecraft font.", 12, y, 0xFFFF7A6B);
            y += 14;
            AetherUi.text(vanilla, fonts.diagnostic(), 12, y, 0xFFFF7A6B);
            return;
        }

        // 2. One custom-font string. If the texture binding is broken this is the line that
        //    disappears while every rectangle on the screen keeps drawing.
        smooth.drawString("Smooth font: AETHER 0123456789 abcdefghijklmnopqrstuvwxyz", 12, y, 0xFFFFFFFF);
        y += 20;
        smooth.drawString("With shadow and colour codes: \u00a7cR\u00a7ae\u00a79d \u00a7lBold\u00a7r \u00a7oItalic", 12, y, 0xFFF2F2F5);
        y += 20;
        smooth.drawStringWithShadow("Shadowed variant", 12, y, 0xFF9B8CFF);
        y += 22;

        GlyphPage page = smooth.regularPage();
        int[] bounds = page == null ? null : page.glyphBounds('A');

        // 3. One glyph, magnified, drawn straight from the atlas.
        if (bounds != null) {
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            Mc189Compat.enableTexture2D();
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            page.bindTexture();
            GL11.glPushMatrix();
            GL11.glScaled(GLYPH_SCALE, GLYPH_SCALE, GLYPH_SCALE);
            page.drawChar('A', 12.0F / GLYPH_SCALE, y / GLYPH_SCALE, 0xFFFFFFFF);
            GL11.glPopMatrix();
            AetherUi.text(vanilla, "one glyph, x" + (int) GLYPH_SCALE, 12 + (int) (bounds[2] * GLYPH_SCALE) + 8, y + 8, 0xFFA3A4B3);
        }

        // 4. The whole atlas, so an empty or wrongly uploaded atlas is obvious.
        if (page != null) {
            int previewX = width - PREVIEW_SIZE - 12;
            drawAtlasPreview(previewX, y);
            AetherUi.text(vanilla, "raw atlas", previewX, y + PREVIEW_SIZE + 2, 0xFFA3A4B3);
            y += PREVIEW_SIZE + 16;
        } else {
            y += 16;
        }

        // 5. The readout: this is what tells a texture problem from a rasterisation problem.
        y += 8;
        AetherUi.text(vanilla, "custom font: " + fonts.diagnostic(), 12, y, 0xFF7BD88F);
        y += 12;
        if (page != null) {
            AetherUi.text(vanilla, "atlas " + page.atlasSize() + "x" + page.atlasSize()
                + "  glyphs " + page.glyphCount()
                + "  texture " + page.textureId()
                + (page.textureIsOwned() ? " (aether upload)" : " (minecraft dynamic texture)"), 12, y, 0xFF7BD88F);
            y += 12;
            AetherUi.text(vanilla, "source: " + page.textureSource(), 12, y, 0xFFA3A4B3);
            y += 12;
            if (bounds != null) {
                AetherUi.text(vanilla, "'A' atlas rect x=" + bounds[0] + " y=" + bounds[1]
                    + " w=" + bounds[2] + " h=" + bounds[3]
                    + "  u=" + format(bounds[0] / (float) page.atlasSize())
                    + " v=" + format(bounds[1] / (float) page.atlasSize()), 12, y, 0xFFA3A4B3);
                y += 12;
            }
        }
        if (client != null) {
            AetherUi.text(vanilla, "theme: " + client.theme().name(), 12, y, 0xFFA3A4B3);
        }
    }

    /** Draws the atlas 1:1 so an empty rasterisation cannot hide behind a scaled preview. */
    private void drawAtlasPreview(int x, int y) {
        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        Mc189Compat.enableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        atlasPreviewVertex(worldRenderer, x, y, 0.0F, 0.0F);
        atlasPreviewVertex(worldRenderer, x + PREVIEW_SIZE, y, 1.0F, 0.0F);
        atlasPreviewVertex(worldRenderer, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 1.0F, 1.0F);
        atlasPreviewVertex(worldRenderer, x, y + PREVIEW_SIZE, 0.0F, 1.0F);
        tessellator.draw();
    }

    private static void atlasPreviewVertex(WorldRenderer worldRenderer, float x, float y, float u, float v) {
        worldRenderer.pos((double) x, (double) y, 0.0D).tex((double) u, (double) v)
            .color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.4f", Float.valueOf(value));
    }
}
