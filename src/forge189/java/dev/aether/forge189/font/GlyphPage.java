package dev.aether.forge189.font;

import dev.aether.forge189.Mc189Compat;
import dev.aether.graphics.TextureIdResolver;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.GL11;

/**
 * One glyph atlas for one font style.
 * <p>
 * The atlas is rasterised with AWT into an ARGB image, uploaded as a Minecraft
 * {@code DynamicTexture}, and drawn char by char as textured quads.
 * <p>
 * The upload is deliberately defensive. Aether reaches Minecraft through reflection, so the
 * texture-id lookup cannot be compiled into a direct call that ForgeGradle reobfuscates - and a
 * lookup that silently yields 0 makes OpenGL sample its default 1x1 image, which Minecraft's
 * enabled {@code GL_ALPHA_TEST} discards: panels keep drawing, every glyph vanishes. So the id is
 * resolved through {@link TextureIdResolver} (development name, SRG name, then a structural scan),
 * and when that still fails the atlas is uploaded into an Aether-owned GL texture instead of ever
 * binding 0. Whatever happens is reported once, with the reason.
 */
public final class GlyphPage {
    private static final AtomicBoolean DIAGNOSTIC_LOGGED = new AtomicBoolean(false);
    /** Floor for the atlas edge, so a font with unusable metrics cannot ask for a 0x0 image. */
    private static final int MIN_ATLAS = 64;

    private int imgSize;
    private int maxFontHeight = -1;
    private final Font font;
    private final boolean antiAliasing;
    private final boolean fractionalMetrics;
    private final HashMap<Character, Glyph> glyphCharacterMap = new HashMap<Character, Glyph>();

    private BufferedImage bufferedImage;
    private DynamicTexture loadedTexture;

    /** OpenGL name actually used for drawing; never 0 once {@link #setupTexture()} succeeded. */
    private int textureId;
    /** How the texture name was obtained, for the in-game font diagnostic. */
    private String textureSource = "not created";
    /** True when Aether uploaded the atlas itself because the Minecraft id was unresolvable. */
    private boolean textureOwned;

    public GlyphPage(Font font, boolean antiAliasing, boolean fractionalMetrics) {
        this.font = font;
        this.antiAliasing = antiAliasing;
        this.fractionalMetrics = fractionalMetrics;
    }

    public void generateGlyphPage(char[] chars) {
        if (chars == null || chars.length == 0) {
            return;
        }

        double maxWidth = -1;
        double maxHeight = -1;

        AffineTransform affineTransform = new AffineTransform();
        FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, antiAliasing, fractionalMetrics);

        for (char ch : chars) {
            Rectangle2D bounds = font.getStringBounds(Character.toString(ch), fontRenderContext);

            if (maxWidth < bounds.getWidth()) maxWidth = bounds.getWidth();
            if (maxHeight < bounds.getHeight()) maxHeight = bounds.getHeight();
        }

        maxWidth += 2;
        maxHeight += 2;

        imgSize = (int) Math.ceil(Math.max(
                Math.ceil(Math.sqrt(maxWidth * maxWidth * chars.length) / maxWidth),
                Math.ceil(Math.sqrt(maxHeight * maxHeight * chars.length) / maxHeight))
                * Math.max(maxWidth, maxHeight)) + 1;

        // A font that reports no usable metrics makes the packing formula collapse to 0, and
        // BufferedImage then rejects the atlas with an exception that takes the screen down.
        if (imgSize < MIN_ATLAS) {
            imgSize = MIN_ATLAS;
        }

        bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();

        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, imgSize, imgSize);

        g.setColor(Color.white);

        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAliasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasing ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        FontMetrics fontMetrics = g.getFontMetrics();

        int currentCharHeight = 0;
        int posX = 0;
        int posY = 1;

        for (char ch : chars) {
            Glyph glyph = new Glyph();

            Rectangle2D bounds = fontMetrics.getStringBounds(Character.toString(ch), g);

            glyph.width = bounds.getBounds().width + 8;
            glyph.height = bounds.getBounds().height;

            if (posX + glyph.width >= imgSize) {
                posX = 0;
                posY += currentCharHeight;
                currentCharHeight = 0;
            }

            glyph.x = posX;
            glyph.y = posY;

            if (glyph.height > maxFontHeight) maxFontHeight = glyph.height;

            if (glyph.height > currentCharHeight) currentCharHeight = glyph.height;

            g.drawString(Character.toString(ch), posX + 2, posY + fontMetrics.getAscent());

            posX += glyph.width;

            glyphCharacterMap.put(ch, glyph);
        }

        g.dispose();

        // Blank metrics (some headless font configurations) would leave the row height at -1 and
        // every layout that reads it would draw off-row. The point size is the honest fallback.
        if (maxFontHeight <= 0) {
            maxFontHeight = (int) Math.ceil(font.getSize2D());
        }
    }

    /**
     * Uploads the atlas and records the OpenGL name the draw path must bind.
     * <p>
     * Must run on the render thread with the OpenGL context current.
     */
    public void setupTexture() {
        textureId = 0;
        textureOwned = false;

        loadedTexture = new DynamicTexture(bufferedImage);
        TextureIdResolver.Resolution resolution = TextureIdResolver.resolve(loadedTexture);
        if (resolution.resolved()) {
            textureId = resolution.textureId();
            textureSource = resolution.describe();
        } else {
            textureId = uploadOwnTexture();
            textureOwned = textureId > 0;
            textureSource = textureOwned
                ? "aether upload; Minecraft's DynamicTexture id " + resolution.describe()
                : "unavailable: " + resolution.failure();
        }

        if (DIAGNOSTIC_LOGGED.compareAndSet(false, true)) {
            System.out.println("[Aether] Custom font atlas " + imgSize + "x" + imgSize
                + ", glyphs " + glyphCharacterMap.size() + ", " + textureSource);
            if (textureId <= 0) {
                System.out.println("[Aether] The custom font has no OpenGL texture in this"
                    + " environment, so the UI continues on the Minecraft font.");
            }
        }
    }

    /**
     * Uploads the atlas into a texture Aether owns, for the case where Minecraft's own texture id
     * cannot be read on the running mappings.
     * <p>
     * This is the same upload Minecraft's {@code TextureUtil} performs - the ARGB pixels are
     * re-ordered into the byte order OpenGL expects for {@code GL_RGBA} - with the one extra step
     * Minecraft gets from {@code TextureUtil} and Aether would otherwise miss: a non-mipmap
     * {@code GL_TEXTURE_MIN_FILTER}. Leaving the default minification filter makes the texture
     * incomplete without mipmaps, and an incomplete texture samples as black.
     *
     * @return the new texture name, or 0 when no OpenGL context is available.
     */
    private int uploadOwnTexture() {
        try {
            int id = GL11.glGenTextures();
            if (id <= 0) {
                return 0;
            }
            int[] pixels = new int[imgSize * imgSize];
            bufferedImage.getRGB(0, 0, imgSize, imgSize, pixels, 0, imgSize);
            for (int i = 0; i < pixels.length; i++) {
                int argb = pixels[i];
                int a = argb >> 24 & 0xFF;
                int r = argb >> 16 & 0xFF;
                int g = argb >> 8 & 0xFF;
                int b = argb & 0xFF;
                pixels[i] = a << 24 | b << 16 | g << 8 | r;
            }
            IntBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
            buffer.put(pixels);
            buffer.flip();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, imgSize, imgSize, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            // The bind above bypassed GlStateManager, so tell it what is bound now. Otherwise its
            // cache would report a stale texture and skip the next real bind.
            Mc189Compat.bindTexture(id);
            return id;
        } catch (Throwable unavailable) {
            return 0;
        }
    }

    /**
     * Binds the atlas for drawing.
     * <p>
     * A missing texture is reported once and nothing is bound: binding 0 would point the sampler at
     * OpenGL's default 1x1 image, which alpha-testing then rejects, and the symptom (cards draw,
     * text does not) is far harder to diagnose than a log line.
     */
    public void bindTexture() {
        if (textureId <= 0) {
            return;
        }
        Mc189Compat.bindTexture(textureId);
    }

    /**
     * Restores the colour state the rest of the UI expects.
     * <p>
     * Deliberately does not bind texture 0 - the glyph page stays bound, exactly as Minecraft's own
     * font renderer leaves its atlas bound.
     */
    public void unbindTexture() {
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Appends one glyph quad to an open quad batch on the shared Tessellator pipeline.
     * <p>
     * The colour travels on the vertex rather than being read from OpenGL's colour state, which is
     * what lets a caller batch a whole run of glyphs into a single draw call and what keeps the
     * vertices correct even if the colour state is ever out of step with what the renderer intends.
     *
     * @param worldRenderer a renderer the caller has already begun a {@code POSITION_TEX_COLOR}
     *     quad batch on.
     * @return the advance in atlas pixels; 0 for a character this page has no glyph for.
     */
    public float appendChar(WorldRenderer worldRenderer, char ch, float x, float y,
                            float red, float green, float blue, float alpha) {
        Glyph glyph = glyphCharacterMap.get(ch);

        if (glyph == null) return 0.0F;

        float pageX = glyph.x / (float) imgSize;
        float pageY = glyph.y / (float) imgSize;

        float pageWidth = glyph.width / (float) imgSize;
        float pageHeight = glyph.height / (float) imgSize;

        float width = glyph.width;
        float height = glyph.height;

        worldRenderer.pos(x, y, 0.0D).tex(pageX, pageY).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x, y + height, 0.0D).tex(pageX, pageY + pageHeight).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).tex(pageX + pageWidth, pageY + pageHeight).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).tex(pageX + pageWidth, pageY).color(red, green, blue, alpha).endVertex();

        return width - 8;
    }

    /**
     * Draws a single glyph, beginning and flushing the batch itself. Used by the font diagnostic,
     * which draws one magnified glyph outside any string.
     *
     * @param argb the colour to draw with, alpha included.
     * @return the advance in atlas pixels.
     */
    public float drawChar(char ch, float x, float y, int argb) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float advance = appendChar(worldRenderer, ch, x, y,
            (argb >> 16 & 0xFF) / 255.0F,
            (argb >> 8 & 0xFF) / 255.0F,
            (argb & 0xFF) / 255.0F,
            (argb >>> 24 & 0xFF) / 255.0F);
        tessellator.draw();
        return advance;
    }

    public float getWidth(char ch) {
        Glyph glyph = glyphCharacterMap.get(ch);
        return glyph != null ? glyph.width : 0;
    }

    /**
     * @param ch the character to look up.
     * @return {x, y, width, height} of the glyph inside the atlas, or null when the atlas has no
     *     glyph for it. Exposed because the texture coordinates a glyph quad uses are derived from
     *     this rect, so the in-game diagnostic and the tests can prove they stay inside the atlas.
     */
    public int[] glyphBounds(char ch) {
        Glyph glyph = glyphCharacterMap.get(ch);
        return glyph == null ? null : new int[] {glyph.x, glyph.y, glyph.width, glyph.height};
    }

    public int getMaxFontHeight() {
        return maxFontHeight;
    }

    /** @return the atlas edge length in pixels. */
    public int atlasSize() {
        return imgSize;
    }

    /** @return the OpenGL texture name in use, or 0 when the atlas could not be uploaded. */
    public int textureId() {
        return textureId;
    }

    /** @return true when Aether uploaded the atlas itself instead of reusing Minecraft's texture. */
    public boolean textureIsOwned() {
        return textureOwned;
    }

    /** @return a one-line description of where the texture came from, for diagnostics. */
    public String textureSource() {
        return textureSource;
    }

    /** @return the number of glyphs in the atlas. */
    public int glyphCount() {
        return glyphCharacterMap.size();
    }

    static class Glyph {
        private int x;
        private int y;
        private int width;
        private int height;
    }
}
