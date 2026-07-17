package dev.aether.event;

public enum EventPriority {
    LOW(0),
    NORMAL(50),
    HIGH(100);

    private final int weight;

    EventPriority(int weight) {
        this.weight = weight;
    }

    int weight() {
        return weight;
    }
}

