package dev.aether.forge189;

import dev.aether.TestSupport;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.impl.cosmetics.CurrentCapeModule;
import dev.aether.module.impl.cosmetics.CurrentHaloModule;
import dev.aether.module.impl.cosmetics.CurrentHatModule;
import dev.aether.module.impl.cosmetics.CurrentWingsModule;

import java.util.HashSet;
import java.util.Set;

/**
 * Guards the renderer abstraction introduced by the cosmetics overhaul: one renderer per
 * wearer-local slot, each bound to its own type and gating module, and the shared context
 * that carries frame time, settings and colour maths without any Minecraft classes.
 */
public final class CosmeticRendererRegistryTest {

    public static void main(String[] args) {
        CosmeticRenderer[] renderers = {
            new CapeCosmeticRenderer(),
            new WingsCosmeticRenderer(),
            new HaloCosmeticRenderer(),
            new HatCosmeticRenderer()
        };

        Set<CosmeticType> types = new HashSet<CosmeticType>();
        Set<String> modules = new HashSet<String>();
        for (CosmeticRenderer renderer : renderers) {
            TestSupport.assertTrue(renderer.type() != null, "Every renderer claims a type.");
            TestSupport.assertTrue(renderer.moduleId() != null && !renderer.moduleId().isEmpty(),
                "Every renderer names its gating module.");
            TestSupport.assertTrue(types.add(renderer.type()),
                "No two renderers claim the same type: " + renderer.type());
            TestSupport.assertTrue(modules.add(renderer.moduleId()),
                "No two renderers share a gating module: " + renderer.moduleId());
        }

        TestSupport.assertEquals(CosmeticType.STATIC_CAPE, new CapeCosmeticRenderer().type(),
            "The cape renderer owns the static cape slot.");
        TestSupport.assertEquals(CurrentCapeModule.ID, new CapeCosmeticRenderer().moduleId(),
            "The cape renderer is gated by the cape module.");
        TestSupport.assertEquals(CosmeticType.WINGS, new WingsCosmeticRenderer().type(),
            "The wings renderer owns the wings slot.");
        TestSupport.assertEquals(CurrentWingsModule.ID, new WingsCosmeticRenderer().moduleId(),
            "The wings renderer is gated by the wings module.");
        TestSupport.assertEquals(CosmeticType.HALO, new HaloCosmeticRenderer().type(),
            "The halo renderer owns the halo slot.");
        TestSupport.assertEquals(CurrentHaloModule.ID, new HaloCosmeticRenderer().moduleId(),
            "The halo renderer is gated by the halo module.");
        TestSupport.assertEquals(CosmeticType.HAT, new HatCosmeticRenderer().type(),
            "The hat renderer owns the hat slot.");
        TestSupport.assertEquals(CurrentHatModule.ID, new HatCosmeticRenderer().moduleId(),
            "The hat renderer is gated by the hat module.");

        // The context: frame values, delegated lookups, shared maths.
        final boolean[] enabled = {false};
        CosmeticRenderContext context = new CosmeticRenderContext(
            new CosmeticRenderContext.Settings() {
                public boolean enabled(String moduleId) {
                    return enabled[0];
                }

                public boolean settingBool(String moduleId, String key, boolean fallback) {
                    return fallback;
                }

                public int settingInt(String moduleId, String key, int fallback) {
                    return fallback + 1;
                }

                public String settingString(String moduleId, String key, String fallback) {
                    return key;
                }
            },
            new CosmeticRenderContext.AssetSource() {
                public CosmeticAsset effective(CosmeticType type) {
                    return null;
                }
            },
            new CosmeticRenderContext.TextureSource() {
                public Integer textureFor(CosmeticAsset asset) {
                    return null;
                }
            });

        context.beginFrame(12.5D, 0.42D);
        TestSupport.assertEquals(Double.valueOf(12.5D), Double.valueOf(context.seconds()),
            "beginFrame records the animation clock.");
        TestSupport.assertEquals(Double.valueOf(0.42D), Double.valueOf(context.speed()),
            "beginFrame records the wearer speed.");
        enabled[0] = true;
        TestSupport.assertTrue(context.enabled("any.module"), "enabled() delegates to settings.");
        TestSupport.assertEquals(Integer.valueOf(6), Integer.valueOf(context.settingInt("m", "k", 5)),
            "settingInt() delegates to settings.");
        TestSupport.assertEquals("k", context.settingString("m", "k", "x"),
            "settingString() delegates to settings.");

        TestSupport.assertEquals(Integer.valueOf(0x80FFFFFF),
            Integer.valueOf(CosmeticRenderContext.modulate(0xFFFFFFFF, 0.5D)),
            "modulate scales the alpha channel.");
        TestSupport.assertEquals(Integer.valueOf(0x00FFFFFF),
            Integer.valueOf(CosmeticRenderContext.modulate(0xFFFFFFFF, -1.0D)),
            "modulate clamps negative alpha to zero.");
        TestSupport.assertEquals(Integer.valueOf(0xFF808080),
            Integer.valueOf(CosmeticRenderContext.mix(0xFF000000, 0xFFFFFFFF, 0.5D)),
            "mix interpolates channels.");
        TestSupport.assertEquals(Integer.valueOf(3), Integer.valueOf(CosmeticRenderContext.clamp(9, 0, 3)),
            "clamp caps the upper bound.");
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(CosmeticRenderContext.clamp(-5, 0, 3)),
            "clamp caps the lower bound.");
    }
}
