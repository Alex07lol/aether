package dev.aether.event;

public final class Subscription<T extends Event> {
    private final Class<T> eventType;
    private final EventPriority priority;
    private final EventHandler<T> handler;

    Subscription(Class<T> eventType, EventPriority priority, EventHandler<T> handler) {
        this.eventType = eventType;
        this.priority = priority;
        this.handler = handler;
    }

    public Class<T> eventType() {
        return eventType;
    }

    public EventPriority priority() {
        return priority;
    }

    EventHandler<T> handler() {
        return handler;
    }
}

