package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import java.io.File;
import java.io.IOException;

@Mod(
    modid = ForgeAetherConstants.MOD_ID,
    name = ForgeAetherConstants.MOD_NAME,
    version = ForgeAetherConstants.VERSION,
    acceptedMinecraftVersions = "[1.8.9]",
    clientSideOnly = true
)
public final class AetherForgeMod {
    private AetherClient client;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File aetherDir = new File(event.getModConfigurationDirectory(), ForgeAetherConstants.MOD_ID);
        client = new AetherClient(new File(aetherDir, "client.json").toPath());
        try {
            client.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Aether configuration.", exception);
        }
        installShutdownSave();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ForgeKeyBindings keyBindings = new ForgeKeyBindings();
        keyBindings.register();

        // Every screen and the HUD render their colours from the shared AetherUi token hub,
        // so it needs the client before the first frame is drawn.
        AetherUi.bind(client);

        // The renderer needs the bridge to read live combat/toggle state (combo counter,
        // toggled sprint and sneak), so the bridge has to exist first.
        ForgeClientEventBridge clientEventBridge = new ForgeClientEventBridge(client, keyBindings);
        ForgeHudRenderer hudRenderer = new ForgeHudRenderer(client, clientEventBridge);
        ForgeHudEventBridge hudEventBridge = new ForgeHudEventBridge(client, hudRenderer);
        hudEventBridge.bind(clientEventBridge);
        MinecraftForge.EVENT_BUS.register(hudEventBridge);
        MinecraftForge.EVENT_BUS.register(new ForgeGuiEventBridge(client));
        MinecraftForge.EVENT_BUS.register(clientEventBridge);
        FMLCommonHandler.instance().bus().register(clientEventBridge);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        saveQuietly();
    }

    private void installShutdownSave() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                saveQuietly();
            }
        }, "Aether Config Save"));
    }

    private void saveQuietly() {
        if (client == null) {
            return;
        }
        try {
            client.save();
        } catch (IOException ignored) {
            // Forge will continue shutdown even if local config persistence fails.
        }
    }
}
