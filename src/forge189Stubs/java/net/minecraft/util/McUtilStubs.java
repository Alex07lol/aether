package net.minecraft.util;

public class McUtilStubs {}

class ResourceLocationStub {
    public ResourceLocationStub(String resourceName) {}
    public ResourceLocationStub(String domain, String path) {}
}

class AxisAlignedBBStub {
    public double minX, minY, minZ, maxX, maxY, maxZ;
    public AxisAlignedBBStub(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }
}
