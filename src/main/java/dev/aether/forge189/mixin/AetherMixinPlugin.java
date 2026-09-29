package dev.aether.forge189.mixin;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

/**
 * The bridge that makes Mixin start at all on Forge 1.8.9.
 * <p>
 * Forge has no native Mixin support in this era, and in a {@code runClient} dev
 * environment there is no jar manifest for FML to scan, so the launch chain is:
 * <ol>
 *   <li>dev: {@code -Dfml.coreMods.load=...AetherMixinPlugin} (CoreModManager reads it),
 *       production: the {@code FMLCorePlugin} manifest attribute on the mod jar;</li>
 *   <li>this constructor runs during coremod loading, before any tweaker injects,
 *       and starts Mixin plus registers {@code mixins.aether.json} explicitly;</li>
 *   <li>the production jar additionally cascades {@code MixinTweaker} via its
 *       {@code TweakClass} attribute for launchers that skip the manifest coremod
 *       scan; MixinBootstrap is idempotent, so both entry points coexist.</li>
 * </ol>
 * This is also the reason the Mixin runtime (0.7.x, ASM 5) is embedded in the jar:
 * Forge 1.8.9 ships none, and its launch classpath pins ASM 5, which rules out
 * Mixin 0.8.x entirely.
 */
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.Name("aether-mixin")
public final class AetherMixinPlugin implements IFMLLoadingPlugin {

    public AetherMixinPlugin() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.aether.json");
        // The production environment is SRG-named, so tell Mixin which refmap table
        // applies. In a ForgeGradle dev run the classes are MCP-named and the injected
        // members match their source names directly - remapping there would only
        // translate good names into SRG names that do not exist.
        if (!isDeobfuscatedEnvironment()) {
            MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
        }
    }

    private static boolean isDeobfuscatedEnvironment() {
        Object flag = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // Nothing to react to: the config is already registered and the environment set.
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
