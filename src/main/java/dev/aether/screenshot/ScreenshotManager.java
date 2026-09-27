package dev.aether.screenshot;

import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.ModuleRegistry;
import dev.aether.module.setting.Setting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ties the async screenshot pipeline to Aether: the registry provides the toggle, the store picks
 * the file, a single worker thread does the encoding, and notifications carry the result back to
 * whatever UI asked for the screenshot.
 * <p>
 * The caller-facing contract is {@link #capture()} and it must only be called from the render
 * thread: it is the one that reads the framebuffer, which is an OpenGL operation. Everything after
 * the pixel copy happens on the worker, so a slow encode can never stall a frame.
 */
public final class ScreenshotManager {
    private final ModuleRegistry modules;
    private final ScreenshotStore store;
    private final AsyncScreenshot state = new AsyncScreenshot();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /** Single worker: PNG encodes queue behind each other instead of racing the disk. */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Aether-Screenshot-Encoder");
            thread.setDaemon(true);
            return thread;
        }
    });

    public ScreenshotManager(ModuleRegistry modules, ScreenshotStore store) {
        this.modules = modules;
        this.store = store;
    }

    public ScreenshotStore store() {
        return this.store;
    }

    public AsyncScreenshot state() {
        return this.state;
    }

    /** @return true when the screenshot module is enabled. */
    public boolean enabled() {
        return enabled(this.modules);
    }

    /** @return the directory screenshots are written to. */
    public Path directory() {
        return this.store.directory();
    }

    /**
     * Takes a screenshot from {@code argb} pixels already copied off the GPU.
     * <p>
     * The caller must have copied the pixels on the render thread; this method is safe to call
     * from anywhere, but the framebuffer read in the adapter is not.
     *
     * @return false when a capture is already running, so spamming the key never queues a storm.
     */
    public boolean encode(int[] argb, int width, int height) {
        if (!this.state.begin()) {
            return false;
        }
        final Path target = this.store.newScreenshotFile();
        final String fileName = target.getFileName().toString();
        final int[] pixels = argb;
        final AsyncScreenshot sharedState = this.state;
        this.worker.execute(new Runnable() {
            public void run() {
                try {
                    ScreenshotStore.writePng(pixels, width, height, target);
                    sharedState.complete(fileName);
                } catch (IOException failed) {
                    sharedState.fail(failed.getMessage());
                } catch (RuntimeException failed) {
                    sharedState.fail(failed.getMessage());
                }
            }
        });
        return true;
    }

    /** @return true when the registered screenshot module is enabled, false when it is not. */
    public static boolean enabled(ModuleRegistry modules) {
        return stateOf(modules) == ModuleState.ENABLED;
    }

    private static ClientModule module(ModuleRegistry modules) {
        try {
            return modules.get("performance.screenshot");
        } catch (IllegalArgumentException unknownModule) {
            return null;
        }
    }

    private static ModuleState stateOf(ModuleRegistry modules) {
        ClientModule module = module(modules);
        return module == null ? ModuleState.DISABLED : module.state();
    }

    /** @return the toast/notification behaviour chosen for the screenshot module. */
    public String notificationMode() {
        ClientModule module = module(this.modules);
        if (module == null) {
            return "notification";
        }
        for (Setting<?> setting : module.settings()) {
            if ("notification".equals(setting.id()) && setting.value() instanceof String) {
                String value = (String) setting.value();
                return "silent".equalsIgnoreCase(value) ? "silent" : "notification";
            }
        }
        return "notification";
    }

    /** Stops accepting new captures and lets the running one finish; safe to call twice. */
    public void shutdown() {
        if (this.shutdown.compareAndSet(false, true)) {
            this.worker.shutdown();
        }
    }
}
