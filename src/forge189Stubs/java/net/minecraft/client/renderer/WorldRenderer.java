package net.minecraft.client.renderer;

import net.minecraft.client.renderer.vertex.VertexFormat;

public class WorldRenderer {
    public void begin(int mode, VertexFormat format) {}
    public WorldRenderer pos(double x, double y, double z) { return this; }
    public WorldRenderer color(float red, float green, float blue, float alpha) { return this; }
    public void endVertex() {}
}
