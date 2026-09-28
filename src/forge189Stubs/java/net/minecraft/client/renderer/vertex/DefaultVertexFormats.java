package net.minecraft.client.renderer.vertex;

/**
 * Compile-time stand-in for Minecraft's DefaultVertexFormats.
 * <p>
 * Every constant is a distinct instance with the same name and type as the real field, so code
 * compiled against this stub links against Minecraft at runtime and the headless tests can assert
 * which layout a draw selected by comparing identities.
 */
public class DefaultVertexFormats {
    /** Position only - used for the untextured fills and the text decorations. */
    public static final VertexFormat POSITION = new VertexFormat();
    /** Position plus a per-vertex colour - used by the text decorations. */
    public static final VertexFormat POSITION_COLOR = new VertexFormat();
    /** Position plus texture coordinates - used by the blit and texture paths. */
    public static final VertexFormat POSITION_TEX = new VertexFormat();
    /**
     * Position, texture coordinates and a per-vertex colour. This is the layout the glyph quads
     * use, and it exists in Minecraft 1.8.9 (the entity renderers build their shadow batches with
     * it).
     */
    public static final VertexFormat POSITION_TEX_COLOR = new VertexFormat();
}
