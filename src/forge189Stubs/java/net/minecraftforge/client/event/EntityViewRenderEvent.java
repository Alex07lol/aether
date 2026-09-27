package net.minecraftforge.client.event;

public class EntityViewRenderEvent {
    public static class CameraSetup {
        public float yaw;
        public float pitch;
        public float roll;
    }

    /**
     * Mirrors {@code net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity}: FML
     * fires it from {@code EntityRenderer.setupFog} and it is {@code @Cancelable}, so a
     * handler can zero the density to clear the fog.
     */
    public static class FogDensity extends EntityViewRenderEvent {
        public float density;

        public FogDensity(Object renderer, Object entity, Object block, double renderPartialTicks, float density) {
            this.density = density;
        }

        public void setCanceled(boolean canceled) {
        }

        public boolean isCanceled() {
            return false;
        }
    }
}
