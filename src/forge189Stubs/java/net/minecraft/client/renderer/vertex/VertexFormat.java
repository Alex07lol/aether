package net.minecraft.client.renderer.vertex;

/**
 * Compile-time stand-in for Minecraft's VertexFormat.
 * <p>
 * The adapter never inspects a format's elements, so the stub only has to be a distinct object per
 * format: the headless tests compare against {@link DefaultVertexFormats}' constants by identity to
 * prove which vertex layout a draw used.
 */
public class VertexFormat {
}
