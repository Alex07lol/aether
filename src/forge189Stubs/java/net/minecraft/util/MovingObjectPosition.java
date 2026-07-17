package net.minecraft.util;

public class MovingObjectPosition {
    public MovingObjectType typeOfHit;
    public BlockPos blockPos;

    public enum MovingObjectType { MISS, BLOCK, ENTITY }
}
