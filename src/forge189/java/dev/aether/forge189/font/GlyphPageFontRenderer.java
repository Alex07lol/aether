package dev.aether.forge189.font;

import dev.aether.forge189.Mc189Compat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.Font;
import java.util.Locale;

import org.lwjgl.opengl.GL11;

public final class GlyphPageFontRenderer {
    private float posX;
    private float posY;
    private final int[] colorCode = new int[32];
    private float red;
    private float blue;
    private float green;
    private float alpha;

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
        GlStateManager.color(this.red, this.green, this.blue, this.alpha);
        this.posX = x * 2.0f;
        this.posY = y * 2.0f;
        this.renderStringAtPos(text, dropShadow);
        return (int) (this.posX / 4.0f);
    }

    private void renderStringAtPos(String text, boolean shadow) {
        GlyphPage glyphPage = getCurrentGlyphPage();

        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();

        glyphPage.bindTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

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
                    GlStateManager.color((float) (j1 >> 16) / 255.0F, (float) (j1 >> 8 & 255) / 255.0F, (float) (j1 & 255) / 255.0F, this.alpha);
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
                    GlStateManager.color(this.red, this.green, this.blue, this.alpha);
                }

                ++i;
            } else {
                glyphPage = getCurrentGlyphPage();
                glyphPage.bindTexture();
                float f = glyphPage.drawChar(c0, posX, posY);
                doDraw(f, glyphPage);
            }
        }

        glyphPage.unbindTexture();
        GL11.glPopMatrix();
    }

    private void doDraw(float f, GlyphPage glyphPage) {
        if (this.strikethroughStyle) {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION);
            worldrenderer.pos((double) this.posX, (double) (this.posY + (float) (glyphPage.getMaxFontHeight() / 2)), 0.0D).endVertex();
            worldrenderer.pos((double) (this.posX + f), (double) (this.posY + (float) (glyphPage.getMaxFontHeight() / 2)), 0.0D).endVertex();
            worldrenderer.pos((double) (this.posX + f), (double) (this.posY + (float) (glyphPage.getMaxFontHeight() / 2) - 1.0F), 0.0D).endVertex();
            worldrenderer.pos((double) this.posX, (double) (this.posY + (float) (glyphPage.getMaxFontHeight() / 2) - 1.0F), 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        if (this.underlineStyle) {
            Tessellator tessellator1 = Tessellator.getInstance();
            WorldRenderer worldrenderer1 = tessellator1.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldrenderer1.begin(7, DefaultVertexFormats.POSITION);
            int l = this.underlineStyle ? -1 : 0;
            worldrenderer1.pos((double) (this.posX + (float) l), (double) (this.posY + (float) glyphPage.getMaxFontHeight()), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + f), (double) (this.posY + (float) glyphPage.getMaxFontHeight()), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + f), (double) (this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + (float) l), (double) (this.posY + (float) glyphPage.getMaxFontHeight() - 1.0F), 0.0D).endVertex();
            tessellator1.draw();
            GlStateManager.enableTexture2D();
        }

        this.posX += f;
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

    public int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        int size = text.length();
        boolean on = false;

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if (character == '§') {
                on = true;
            } else if (on && character >= '0' && character <= 'r') {
                int colorIndex = "0123456789abcdefklmnor".indexOf(character);
                if (colorIndex < 16) {
                    boldStyle = false;
                    italicStyle = false;
                } else if (colorIndex == 17) {
                    boldStyle = true;
                } else if (colorIndex == 20) {
                    italicStyle = true;
                } else if (colorIndex == 21) {
                    boldStyle = false;
                    italicStyle = false;
                }
                i++;
                on = false;
            } else {
                if (on) i--;
                character = text.charAt(i);
                GlyphPage currentPage = getCurrentGlyphPage();
                width += currentPage.getWidth(character) - 8;
            }
        }

        return width / 2;
    }
}
