package dev.aether.ui;

import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Uploads images as GL textures the menu can bind, owned by Minecraft's texture
 * manager under Aether-namespaced locations so cleanup stays with the game.
 */
public final class UiTextures {

    private UiTextures() {
    }

    /** A GL texture name plus the location Minecraft knows it by. */
    public static final class Handle {
        private final ResourceLocation location;
        private final int glId;

        Handle(ResourceLocation location, int glId) {
            this.location = location;
            this.glId = glId;
        }

        public int glId() {
            return glId;
        }
    }

    /** Uploads an image once per call site key; the caller caches the handle. */
    public static Handle upload(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("aether-ui", texture);
            return new Handle(location, texture.getGlTextureId());
        } catch (Throwable failed) {
            return null;
        }
    }
}
