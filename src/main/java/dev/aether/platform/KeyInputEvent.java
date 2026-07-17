package dev.aether.platform;

import dev.aether.event.Event;

public final class KeyInputEvent implements Event {
    private final String action;
    private final int keyCode;

    public KeyInputEvent(String action, int keyCode) {
        this.action = action;
        this.keyCode = keyCode;
    }

    public String action() {
        return action;
    }

    public int keyCode() {
        return keyCode;
    }
}

