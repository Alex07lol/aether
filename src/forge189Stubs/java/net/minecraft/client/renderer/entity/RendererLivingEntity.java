package net.minecraft.client.renderer.entity;

import net.minecraft.entity.EntityLivingBase;

public class RendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
    protected RendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }
}
