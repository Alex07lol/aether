package net.minecraft.entity;

import net.minecraft.util.AxisAlignedBB;

public class Entity {
    public int hurtTime;
    public int maxHurtTime;
    public float attackedAtYaw;
    public double posX, posY, posZ;
    public double lastTickPosX, lastTickPosY, lastTickPosZ;
    public boolean onGround;
    public float fallDistance;
    public Entity ridingEntity;

    public boolean isSneaking() { return false; }
    public boolean isOnLadder() { return false; }
    public boolean isInWater() { return false; }
    public AxisAlignedBB getEntityBoundingBox() { return null; }
}
