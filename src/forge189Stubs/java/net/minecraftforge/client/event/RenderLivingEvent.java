package net.minecraftforge.client.event;

import net.minecraft.entity.EntityLivingBase;

public class RenderLivingEvent {
    public static class Post<T extends EntityLivingBase> {
        public T entity;
        public double x, y, z;
    }
}
