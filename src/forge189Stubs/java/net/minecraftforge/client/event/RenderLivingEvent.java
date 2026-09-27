package net.minecraftforge.client.event;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Mirrors {@code net.minecraftforge.client.event.RenderLivingEvent} from 1.8.9:
 * <ul>
 *   <li>{@code RenderLivingEvent.Pre<T>} / {@code Post<T>} wrap the whole entity render pass, and</li>
 *   <li>{@code RenderLivingEvent.Specials.Pre} fires right before the vanilla name tag, so cancelling
 *       it suppresses the tag and lets a custom renderer draw its own.</li>
 * </ul>
 */
public class RenderLivingEvent<T extends EntityLivingBase> {
    public final RendererLivingEntity renderer;
    public final T entity;
    public final double x;
    public final double y;
    public final double z;

    private boolean canceled;

    public RenderLivingEvent(RendererLivingEntity renderer, T entity, double x, double y, double z) {
        this.renderer = renderer;
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public static class Pre<T extends EntityLivingBase> extends RenderLivingEvent<T> {
        public Pre(RendererLivingEntity renderer, T entity, double x, double y, double z) {
            super(renderer, entity, x, y, z);
        }
    }

    public static class Post<T extends EntityLivingBase> extends RenderLivingEvent<T> {
        public Post(RendererLivingEntity renderer, T entity, double x, double y, double z) {
            super(renderer, entity, x, y, z);
        }
    }

    /** The vanilla name tag pass; cancelling {@code Specials.Pre} hides the default tag. */
    public static class Specials {
        public final RendererLivingEntity renderer;
        public final EntityLivingBase entity;
        public final double x;
        public final double y;
        public final double z;

        private boolean canceled;

        public Specials(RendererLivingEntity renderer, EntityLivingBase entity, double x, double y, double z) {
            this.renderer = renderer;
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public boolean isCanceled() {
            return this.canceled;
        }

        public static class Pre extends Specials {
            public Pre(RendererLivingEntity renderer, EntityLivingBase entity, double x, double y, double z) {
                super(renderer, entity, x, y, z);
            }
        }

        public static class Post extends Specials {
            public Post(RendererLivingEntity renderer, EntityLivingBase entity, double x, double y, double z) {
                super(renderer, entity, x, y, z);
            }
        }
    }
}
