package dev.aether.forge189.font;

import dev.aether.forge189.Mc189Compat;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.Font;
import java.util.Locale;

import org.lwjgl.opengl.GL11;

/**
 * Draws text with a TrueType glyph atlas.
 * <p>
 * The whole string is rendered inside one 0.5-scaled matrix so the atlas can be sampled at
 * sub-pixel positions, exactly like Minecraft's own font renderer. Every quad this renderer emits -
 * glyphs and the underline/strikethrough decorations alike - carries its colour on the vertex, so a
 * text draw reads no colour state, one run of one colour is one draw call, and no caller can tint
 * text by leaving OpenGL in an unexpected state. Colour state itself is moved once per string, back
 * to white, so the UI primitives that follow a text draw start from the same predictable state
 * Minecraft leaves behind.
 */
public final class GlyphPageFontRenderer {
    private float posX;
    private float posY;
    private final int[] colorCode = new int[32];
    /** The colour the whole string was asked for; a reset style code returns to it. */
    private float red;
    private float blue;
    private float green;
    private float alpha;

    /**
     * The colour the glyphs currently being appended are drawn with. It differs from the string
     * colour only between a colour code and the next reset, and it is what every glyph vertex
     * carries, so an open batch never has to be re-read from OpenGL's colour state.
     */
    private float currentRed;
    private float currentGreen;
    private float currentBlue;

    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;

    private final GlyphPage regularGlyphPage;
    private final GlyphPage boldGlyphPage;
    private final GlyphPage italicGlyphPage;
    private final GlyphPage boldItalicGlyphPage;

    public GlyphPageFontRenderer(GlyphPage regularGlyphPage, GlyphPage boldGlyphPage, GlyphPage italicGlyphPage, GlyphPage boldItalicGlyphPage) {
        this.regularGlyphPage = regularGlyphPage;
        this.boldGlyphPage = boldGlyphPage;
        this.italicGlyphPage = italicGlyphPage;
        this.boldItalicGlyphPage = boldItalicGlyphPage;

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) {
                k += 85;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }
    }

    public static GlyphPageFontRenderer create(String fontName, int size, boolean bold, boolean italic, boolean boldItalic) {
        char[] chars = new char[256];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) i;
        }

        GlyphPage regularPage = new GlyphPage(new Font(fontName, Font.PLAIN, size), true, true);
        regularPage.generateGlyphPage(chars);
        regularPage.setupTexture();

        GlyphPage boldPage = bold ? new GlyphPage(new Font(fontName, Font.BOLD, size), true, true) : regularPage;
        if (bold) {
            boldPage.generateGlyphPage(chars);
            boldPage.setupTexture();
        }

        GlyphPage italicPage = italic ? new GlyphPage(new Font(fontName, Font.ITALIC, size), true, true) : regularPage;
        if (italic) {
            italicPage.generateGlyphPage(chars);
            italicPage.setupTexture();
        }

        GlyphPage boldItalicPage = boldItalic ? new GlyphPage(new Font(fontName, Font.BOLD | Font.ITALIC, size), true, true) : regularPage;
        if (boldItalic) {
            boldItalicPage.generateGlyphPage(chars);
            boldItalicPage.setupTexture();
        }

        return new GlyphPageFontRenderer(regularPage, boldPage, italicPage, boldItalicPage);
    }

    /**
     * @return true when every glyph page this renderer can reach owns an OpenGL texture, so a text
     *     draw is guaranteed to sample real glyph data rather than whatever was bound before.
     */
    public boolean isUsable() {
        return regularGlyphPage != null && regularGlyphPage.textureId() > 0;
    }

    /** @return the atlas behind the regular style, for the in-game font diagnostic. */
    public GlyphPage regularPage() {
        return regularGlyphPage;
    }

    /** @return how many glyph pages this renderer built, for the in-game font diagnostic. */
    public int pageCount() {
        int count = 1;
        if (boldGlyphPage != null && boldGlyphPage != regularGlyphPage) count++;
        if (italicGlyphPage != null && italicGlyphPage != regularGlyphPage) count++;
        if (boldItalicGlyphPage != null && boldItalicGlyphPage != regularGlyphPage) count++;
        return count;
    }

    public int drawString(String text, float x, float y, int color) {
        Mc189Compat.enableAlpha();
        this.resetStyles();
        return this.renderString(text, x, y, color, false);
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        Mc189Compat.enableAlpha();
        this.resetStyles();
        int i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
        return Math.max(i, this.renderString(text, x, y, color, false));
    }

    private int renderString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null || text.length() == 0) {
            return 0;
        }

        if ((color & -67108864) == 0) {
            color |= -16777216;
        }

        if (dropShadow) {
            color = (color & 16579836) >> 2 | color & -16777216;
        }

        this.red = (float) (color >> 16 & 255) / 255.0F;
        this.green = (float) (color >> 8 & 255) / 255.0F;
        this.blue = (float) (color & 255) / 255.0F;
        this.alpha = (float) (color >> 24 & 255) / 255.0F;
        this.currentRed = this.red;
        this.currentGreen = this.green;
        this.currentBlue = this.blue;
        this.posX = x * 2.0f;
        this.posY = y * 2.0f;
        this.renderStringAtPos(text, dropShadow);
        // posX advanced in the doubled (pre-scale) space, so halving it gives the end x on screen.
        return (int) (this.posX / 2.0f);
    }

    /**
     * Appends a whole string to the Tessellator pipeline.
     * <p>
     * Glyphs go into one quad batch per colour run, with the colour on every vertex, so a line of
     * text costs one draw call per colour instead of one per character. The batch is flushed
     * whenever the colour or the glyph page changes, because the vertices already appended carry
     * the layout and colour that were current when they were written, and a texture change would
     * make the whole batch sample the wrong page. The decorations are flushed separately because
     * they are untextured, and they take the running colour on their own vertices too.
     */
    private void renderStringAtPos(String text, boolean shadow) {
        if (!isUsable()) {
            // Without a texture the quads would sample whatever is bound, which shows up as random
            // coloured blocks. Nothing is drawn instead; the caller falls back to the MC font.
            return;
        }

        GlyphPage glyphPage = getCurrentGlyphPage();

        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        Mc189Compat.enableTexture2D();

        glyphPage.bindTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        boolean batching = false;

        for (int i = 0; i < text.length(); ++i) {
            char c0 = text.charAt(i);

            if (c0 == 167 && i + 1 < text.length()) {
                int i1 = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                if (i1 < 16) {
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;

                    if (i1 < 0) i1 = 15;
                    if (shadow) i1 += 16;

                    int j1 = this.colorCode[i1];
                    this.currentRed = (float) (j1 >> 16 & 255) / 255.0F;
                    this.currentGreen = (float) (j1 >> 8 & 255) / 255.0F;
                    this.currentBlue = (float) (j1 & 255) / 255.0F;
                } else if (i1 == 17) {
                    this.boldStyle = true;
                } else if (i1 == 18) {
                    this.strikethroughStyle = true;
                } else if (i1 == 19) {
                    this.underlineStyle = true;
                } else if (i1 == 20) {
                    this.italicStyle = true;
                } else {
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    this.currentRed = this.red;
                    this.currentGreen = this.green;
                    this.currentBlue = this.blue;
                }

                ++i;

                if (batching) {
                    tessellator.draw();
                    batching = false;
                }
            } else {
                GlyphPage nextPage = getCurrentGlyphPage();
                if (nextPage != glyphPage) {
                    if (batching) {
                        tessellator.draw();
                        batching = false;
                    }
                    glyphPage = nextPage;
                    glyphPage.bindTexture();
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                }

                if (!batching) {
                    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                    batching = true;
                }

                float advance = glyphPage.appendChar(worldRenderer, c0, posX, posY,
                    this.currentRed, this.currentGreen, this.currentBlue, this.alpha);

                if (this.strikethroughStyle || this.underlineStyle) {
                    if (batching) {
                        tessellator.draw();
                        batching = false;
                    }
                    drawDecorations(advance, glyphPage, tessellator);
                }

                this.posX += advance;
            }
        }

        if (batching) {
            tessellator.draw();
        }

        glyphPage.unbindTexture();
        GL11.glPopMatrix();
    }

    /**
     * Draws Minecraft's underline and strikethrough quads for the glyph just appended.
     * <p>
     * These keep Minecraft's geometry - an untextured quad under the glyph - but carry the running
     * colour on their own vertices, exactly like the glyphs do. That is what lets a decorated run be
     * coloured correctly even though nothing in the draw path reads or writes OpenGL's colour state.
     */
    private void drawDecorations(float advance, GlyphPage glyphPage, Tessellator tessellator) {
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        int halfHeight = glyphPage.getMaxFontHeight() / 2;

        if (this.strikethroughStyle) {
            Mc189Compat.disableTexture2D();
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos((double) this.posX, (double) (this.posY + (float) halfHeight), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) (this.posX + advance), (double) (this.posY + (float) halfHeight), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) (this.posX + advance), (double) (this.posY + (float) halfHeight - 1.0F), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) this.posX, (double) (this.posY + (float) halfHeight - 1.0F), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            tessellator.draw();
            Mc189Compat.enableTexture2D();
        }

        if (this.underlineStyle) {
            Mc189Compat.disableTexture2D();
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            int l = this.underlineStyle ? -1 : 0;
            worldRenderer.pos((double) (this.posX + (float) l), (double) (this.posY + (float) glyphPage.getMaxFontHeight()), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) (this.posX + advance), (double) (this.posY + (float) glyphPage.getMaxFontHeight()), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) (this.posX + advance), (double) (this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            worldRenderer.pos((double) (this.posX + (float) l), (double) (this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F), 0.0D).color(currentRed, currentGreen, currentBlue, alpha).endVertex();
            tessellator.draw();
            Mc189Compat.enableTexture2D();
        }
    }

    private GlyphPage getCurrentGlyphPage() {
        if (boldStyle && italicStyle) return boldItalicGlyphPage;
        else if (boldStyle) return boldGlyphPage;
        else if (italicStyle) return italicGlyphPage;
        else return regularGlyphPage;
    }

    private void resetStyles() {
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    public int getFontHeight() {
        return regularGlyphPage.getMaxFontHeight() / 2;
    }

    /**
     * Measures a string without touching the renderer's style state: measurement happens during
     * layout, and a measurement that left {@code §l}/{@code §o} behind would change the glyph page
     * of the next draw.
     */
    public int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        int size = text.length();
        boolean bold = this.boldStyle;
        boolean italic = this.italicStyle;

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if (character == '\u00a7' && i + 1 < size) {
                // Consume only the code character here. The loop increment then lands on the first
                // character the code applies to, which is how Minecraft measures too; consuming an
                // extra character made every string with a colour code measure one glyph narrow, so
                // centred text drifted and tooltips were sized for less text than they drew.
                int colorIndex = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;
                } else if (colorIndex == 20) {
                    italic = true;
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                }
                i++;
                continue;
            }

            GlyphPage currentPage = pageFor(bold, italic);
            if (currentPage == null) {
                return width / 2;
            }
            width += currentPage.getWidth(character) - 8;
        }

        return width / 2;
    }

    private GlyphPage pageFor(boolean bold, boolean italic) {
        if (bold && italic) return boldItalicGlyphPage;
        if (bold) return boldGlyphPage;
        if (italic) return italicGlyphPage;
        return regularGlyphPage;
    }
}
