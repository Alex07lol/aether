package dev.aether.platform;

import dev.aether.event.Event;

public final class ClientTickEvent implements Event {
    private final long tickTimeMillis;

    public ClientTickEvent(long tickTimeMillis) {
        this.tickTimeMillis = tickTimeMillis;
    }

    public long tickTimeMillis() {
        return tickTimeMillis;
    }
}

