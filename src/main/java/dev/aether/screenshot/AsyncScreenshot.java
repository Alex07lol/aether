package dev.aether.screenshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The state of an in-flight asynchronous screenshot, shared between the render thread and the
 * encode worker.
 * <p>
 * The render thread copies pixels into a plain {@code int[]} and stops touching them; from then on
 * only the worker reads the buffer and only the state fields below are written. That split is the
 * whole safety argument: OpenGL stays on the render thread, disk I/O and PNG encoding move off it,
 * and the two sides meet only at the handover.
 * <p>
 * The capture sequence is deliberately two-phase, because the framebuffer can only be read
 * synchronously on the render thread:
 * <ol>
 *   <li>{@link #begin(int[])} - render thread: reserve the slot and hand over the pixel copy.</li>
 *   <li>{@link #complete(String)} / {@link #fail(String)} - worker: report the encode result.</li>
 * </ol>
 */
public final class AsyncScreenshot {
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicReference<String> error = new AtomicReference<String>();
    private final AtomicReference<String> savedFile = new AtomicReference<String>();
    private final AtomicInteger pendingEncodes = new AtomicInteger(0);

    /** Reserves the pipeline for a new capture. @return false when one is already running. */
    public boolean begin() {
        if (!this.inFlight.compareAndSet(false, true)) {
            return false;
        }
        this.error.set(null);
        this.savedFile.set(null);
        this.pendingEncodes.incrementAndGet();
        return true;
    }

    /** Worker: the PNG is on disk. {@code fileName} is what the Screenshots page will list. */
    public void complete(String fileName) {
        this.savedFile.set(fileName);
        finish();
    }

    /** Worker: encoding failed; {@code message} is shown as a notification, not thrown. */
    public void fail(String message) {
        this.error.set(message == null ? "screenshot failed" : message);
        finish();
    }

    private void finish() {
        this.pendingEncodes.decrementAndGet();
        this.inFlight.set(false);
    }

    /** @return true from the first read after {@link #begin} until the worker reports back. */
    public boolean isInFlight() {
        return this.inFlight.get();
    }

    /** @return the failure message of the most recent capture, or {@code null} when it succeeded. */
    public String lastError() {
        return this.error.get();
    }

    /** @return the file name of the most recent capture, or {@code null} while pending or failed. */
    public String lastSavedFile() {
        return this.savedFile.get();
    }

    /** Encodes waiting at the moment of the call; a gauge for tests, not for gameplay. */
    public int pendingEncodes() {
        return this.pendingEncodes.get();
    }

    /** Drops the finished-capture state; the next notification poll starts fresh. */
    public void consume() {
        this.error.set(null);
        this.savedFile.set(null);
    }

    /**
     * Waits for a worker run to report, for tests. Returns false on timeout; a real client never
     * blocks on this.
     */
    public boolean awaitCompletion(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (this.isInFlight()) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            try {
                Thread.sleep(5L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /** @return true when any of {@code names} is the file the last capture produced. */
    public boolean isAmong(List<String> names) {
        String saved = this.savedFile.get();
        return saved != null && names != null && names.contains(saved);
    }
}
