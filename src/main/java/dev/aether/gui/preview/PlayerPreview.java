package dev.aether.gui.preview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import dev.aether.AetherClient;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * The large rotating player preview of the cosmetics screen, ported from Leaf
 * Client's {@code CosmeticSettings} preview (GPLv3, see docs/GUI_REBUILD.md): the
 * local player rendered through the render manager at Leaf's position (entity feet at
 * design (1300, 800), 200 units tall), turning with the mouse the way
 * {@code GuiInventory.drawEntityOnScreen} turns it - yaw from the horizontal offset
 * from screen centre, pitch from the clamped vertical offset - with the equipped
 * cosmetics drawn from Aether's own data model in the same entity space.
 * <p>
 * Every piece of GL state the entity render touches is restored afterwards (standard
 * item lighting, rescale normal, lightmap texture unit, blend), and the render is
 * clipped to the preview region, so nothing leaks into the rest of the GUI.
 */
public final class PlayerPreview {

    private static final int CENTER_X = 1300;
    private static final int FEET_Y = 800;
    private static final int SCALE = 200;
    private static final float PITCH_LIMIT = 30.0F;

    private final AetherClient client;

    /** Mouse offset from the screen centre in design units (Leaf's raw inputs). */
    private double lookX;
    private double lookY;
    /** Vertical tilt in degrees, clamped. */
    private float pitch = 8.0F;

    /** Uploaded textures for imported cape PNGs, keyed by asset id. */
    private final Map<String, ResourceLocation> textureCache = new HashMap<String, ResourceLocation>();

    public PlayerPreview(AetherClient client) {
        this.client = client;
    }

    /** Feeds the preview the mouse offset from the screen centre (Leaf's model). */
    public void setMouseLook(double dxFromCenter, double clampedDyFromCenter) {
        this.lookX = dxFromCenter;
        this.lookY = clampedDyFromCenter;
    }

    public void render() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        int centerX = dev.aether.gui.GuiScale.x(CENTER_X);
        int feetY = dev.aether.gui.GuiScale.y(FEET_Y);
        int zoomGui = dev.aether.gui.GuiScale.w(SCALE);

        // Clip generously around the standing figure so a tall animation cannot paint
        // over the list, while the rectangle itself stays invisible (Leaf has no frame).
        int clipX = centerX - zoomGui;
        int clipY = feetY - (int) (2.4D * zoomGui);
        int clipW = zoomGui * 2;
        int clipH = (int) (2.4D * zoomGui) + dev.aether.gui.GuiScale.h(20);
        Mc189Compat.pushScissor(Math.max(0, clipX), Math.max(0, clipY), clipW, clipH);
        try {
            if (player == null) {
                dev.aether.gui.AetherFont.drawCentered(dev.aether.gui.AetherFont.Size.SMALL, "Preview available in game",
                    centerX - dev.aether.gui.GuiScale.w(200), feetY - dev.aether.gui.GuiScale.h(180),
                    dev.aether.gui.GuiScale.w(400), AetherUi.TEXT_DISABLED);
                return;
            }
            drawPlayer(centerX, feetY, zoomGui, player);
            drawCosmetics(centerX, feetY, zoomGui);
        } finally {
            Mc189Compat.popScissor();
        }
    }

    /**
     * The entity render, following the state sequence of the vanilla
     * {@code GuiInventory.drawEntityOnScreen}: depth on, matrix pushed, the yaw and
     * pitch derived from the mouse exactly as vanilla derives them from its
     * {@code mouseDX}/{@code mouseDY} arguments, then the full teardown.
     */
    private void drawPlayer(int centerX, int feetY, int zoomGui, EntityPlayer player) {
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableBlend();

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, feetY, 120.0F);
        GL11.glScalef(-zoomGui, zoomGui, zoomGui);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);

        float yaw = (float) (Math.atan(lookX / 40.0D) * 40.0D);
        float bodyPitch = -(float) (Math.atan(lookY / 40.0D) * 20.0D);

        float yawOffset = player.renderYawOffset;
        float bodyYaw = player.rotationYaw;
        float oldPitch = player.rotationPitch;
        float prevHeadYaw = player.prevRotationYawHead;
        float headYaw = player.rotationYawHead;
        player.renderYawOffset = yaw;
        player.rotationYaw = yaw;
        player.rotationPitch = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, bodyPitch));
        player.rotationYawHead = yaw;
        player.prevRotationYawHead = yaw;

        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.doRenderEntity(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        renderManager.setRenderShadow(true);

        player.renderYawOffset = yawOffset;
        player.rotationYaw = bodyYaw;
        player.rotationPitch = oldPitch;
        player.prevRotationYawHead = prevHeadYaw;
        player.rotationYawHead = headYaw;

        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws the equipped cosmetics in entity-local space (feet at the origin, one unit
     * = one block, y up) from the same data the in-world renderer consumes. The
     * transform mirrors the entity's, so both turn together with the mouse.
     */
    private void drawCosmetics(int centerX, int feetY, int zoomGui) {
        CosmeticAsset cape = client.cosmetics().effective(CosmeticType.STATIC_CAPE);
        if (cape == null) {
            cape = client.cosmetics().effective(CosmeticType.ANIMATED_CAPE);
        }
        GlStateManager.pushMatrix();
        try {
            GL11.glTranslatef(centerX, feetY, 120.0F);
            GL11.glScalef(-zoomGui, zoomGui, zoomGui);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef((float) (Math.atan(lookX / 40.0D) * 40.0D), 0.0F, -1.0F, 0.0F);

            Mc189Compat.disableLighting();
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
            Mc189Compat.disableCull();

            if (cape != null) {
                drawCape(cape);
            }
            drawWings(client.cosmetics().effective(CosmeticType.WINGS));
            drawHalo(client.cosmetics().effective(CosmeticType.HALO));
            drawHat(client.cosmetics().effective(CosmeticType.HAT));
        } finally {
            Mc189Compat.enableCull();
            Mc189Compat.enableLighting();
            Mc189Compat.enableTexture2D();
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private void drawCape(CosmeticAsset cape) {
        double shoulder = 1.44D;
        double length = 0.80D;
        double halfWidth = 0.31D;
        double z = -0.20D;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        Integer texture = cape.localFile() == null ? null : capeTexture(cape);
        if (texture != null) {
            Mc189Compat.bindTexture(texture.intValue());
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            renderer.pos(-halfWidth, shoulder, z).tex(0.0D, 0.0D).endVertex();
            renderer.pos(halfWidth, shoulder, z).tex(1.0D, 0.0D).endVertex();
            renderer.pos(halfWidth, shoulder - length, z - 0.06D).tex(1.0D, 1.0D).endVertex();
            renderer.pos(-halfWidth, shoulder - length, z - 0.06D).tex(0.0D, 1.0D).endVertex();
            tessellator.draw();
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(renderer, -halfWidth, shoulder, z, AetherUi.withAlpha(cape.primaryColor(), 0xE6));
        vertex(renderer, halfWidth, shoulder, z, AetherUi.withAlpha(cape.primaryColor(), 0xE6));
        vertex(renderer, halfWidth, shoulder - length, z - 0.06D, AetherUi.withAlpha(cape.secondaryColor(), 0xE6));
        vertex(renderer, -halfWidth, shoulder - length, z - 0.06D, AetherUi.withAlpha(cape.secondaryColor(), 0xE6));
        tessellator.draw();
    }

    private void drawWings(CosmeticAsset wings) {
        if (wings == null) {
            return;
        }
        int color = AetherUi.withAlpha(wings.primaryColor(), 0xD9);
        int edge = AetherUi.withAlpha(wings.secondaryColor(), 0xD9);
        double shoulderY = 1.38D;
        double z = -0.12D;
        double spread = 0.55D;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            double rootX = side * 0.14D;
            double tipX = side * (0.14D + spread);
            vertex(renderer, rootX, shoulderY, z, color);
            vertex(renderer, tipX, shoulderY - 0.10D, z - 0.18D, edge);
            vertex(renderer, tipX, shoulderY - 0.32D, z - 0.22D, edge);
            vertex(renderer, rootX, shoulderY - 0.28D, z, color);
        }
        tessellator.draw();
    }

    private void drawHalo(CosmeticAsset halo) {
        if (halo == null) {
            return;
        }
        int color = AetherUi.withAlpha(halo.primaryColor(), 0xE6);
        double height = 2.05D;
        double radius = 0.32D;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0D * i) / segments;
            double a1 = (Math.PI * 2.0D * (i + 1)) / segments;
            vertex(renderer, Math.cos(a0) * radius, height, Math.sin(a0) * radius, color);
            vertex(renderer, Math.cos(a1) * radius, height, Math.sin(a1) * radius, color);
            vertex(renderer, Math.cos(a1) * radius, height + 0.05D, Math.sin(a1) * radius, color);
            vertex(renderer, Math.cos(a0) * radius, height + 0.05D, Math.sin(a0) * radius, color);
        }
        tessellator.draw();
    }

    private void drawHat(CosmeticAsset hat) {
        if (hat == null) {
            return;
        }
        int color = AetherUi.withAlpha(hat.primaryColor(), 0xE6);
        int brim = AetherUi.withAlpha(hat.secondaryColor(), 0xE6);
        double headTop = 1.78D;
        double half = 0.18D;
        double brimZ = 0.26D;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(renderer, -half, headTop, half, color);
        vertex(renderer, half, headTop, half, color);
        vertex(renderer, half, headTop + 0.14D, half, color);
        vertex(renderer, -half, headTop + 0.14D, half, color);
        vertex(renderer, -half, headTop + 0.02D, half, brim);
        vertex(renderer, half, headTop + 0.02D, half, brim);
        vertex(renderer, half, headTop + 0.06D, brimZ, brim);
        vertex(renderer, -half, headTop + 0.06D, brimZ, brim);
        tessellator.draw();
    }

    private static void vertex(WorldRenderer renderer, double x, double y, double z, int argb) {
        float a = (argb >> 24 & 0xFF) / 255.0F;
        float r = (argb >> 16 & 0xFF) / 255.0F;
        float g = (argb >> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        renderer.pos(x, y, z).color(r, g, b, a).endVertex();
    }

    /** Uploads an imported cape PNG once and returns its GL texture name, or null. */
    private Integer capeTexture(CosmeticAsset cape) {
        ResourceLocation cached = textureCache.get(cape.id());
        if (cached != null) {
            return Integer.valueOf(Minecraft.getMinecraft().getTextureManager().getTexture(cached).getGlTextureId());
        }
        try {
            BufferedImage image = ImageIO.read(cape.localFile().toFile());
            if (image == null) {
                return null;
            }
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("aether-preview-" + cape.id(), texture);
            textureCache.put(cape.id(), location);
            return Integer.valueOf(texture.getGlTextureId());
        } catch (IOException failed) {
            return null;
        }
    }

    /**
     * A GL texture uploaded from an image, owned by the texture manager under an
     * Aether-namespaced location. Shared by the preview and the colour chart's
     * generated palette.
     */
    public static final class TextureHandle {
        private final ResourceLocation location;
        private final int glId;

        TextureHandle(ResourceLocation location, int glId) {
            this.location = location;
            this.glId = glId;
        }

        public int glId() {
            return glId;
        }
    }

    /** Uploads an image as a texture the GUI can bind, once per caller-managed key. */
    public static TextureHandle uploadTexture(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("aether-gui", texture);
            return new TextureHandle(location, texture.getGlTextureId());
        } catch (Throwable failed) {
            return null;
        }
    }

    public void dispose() {
        textureCache.clear();
    }
}
