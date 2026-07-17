package dev.aether.event;

import dev.aether.TestSupport;

public final class EventBusTest {
    public static void main(String[] args) {
        EventBus bus = new EventBus();
        final StringBuilder calls = new StringBuilder();

        bus.subscribe(SampleEvent.class, EventPriority.LOW, new EventHandler<SampleEvent>() {
            public void handle(SampleEvent event) {
                calls.append("low");
            }
        });
        bus.subscribe(SampleEvent.class, EventPriority.HIGH, new EventHandler<SampleEvent>() {
            public void handle(SampleEvent event) {
                calls.append("high:");
            }
        });

        bus.publish(new SampleEvent());
        TestSupport.assertEquals("high:low", calls.toString(), "Handlers should run by priority.");
    }

    private static final class SampleEvent implements Event {
    }
}

