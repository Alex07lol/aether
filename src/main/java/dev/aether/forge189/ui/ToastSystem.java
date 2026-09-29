package dev.aether.forge189.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ToastSystem {
    private static final int MAX_TOASTS = 3;
    private final List<AetherToast> toasts = new ArrayList<>();

    public void push(String text, int accent) {
        if (toasts.size() >= MAX_TOASTS) toasts.remove(0);
        toasts.add(new AetherToast(text, accent));
    }

    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<AetherToast> it = toasts.iterator();
        while (it.hasNext()) {
            AetherToast t = it.next();
            t.tick(now);
            if (!t.alive(now)) it.remove();
        }
    }

    public List<AetherToast> snapshot() {
        return new ArrayList<>(toasts);
    }
}