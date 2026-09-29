package dev.aether.forge189.mixin;

/**
 * Freelook's rotation freeze used to live here, targeting {@code EntityPlayerSP.setAngles}.
 * That was a stub-era mistake: {@code setAngles} is declared on {@link net.minecraft.entity.Entity},
 * so an injection into the subclass never bound — on real Minecraft the hook simply did not exist.
 * The freeze now lives in {@link EntityMixin}, on the declaring class. This marker remains only so
 * the old class name is not silently reused for something else.
 */
final class EntityPlayerSPMixin {

    private EntityPlayerSPMixin() {
    }
}
