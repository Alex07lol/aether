package dev.aether.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EventBus {
    private final Map<Class<?>, List<Subscription<?>>> subscriptions = new IdentityHashMap<Class<?>, List<Subscription<?>>>();

    public synchronized <T extends Event> Subscription<T> subscribe(Class<T> eventType, EventHandler<T> handler) {
        return subscribe(eventType, EventPriority.NORMAL, handler);
    }

    public synchronized <T extends Event> Subscription<T> subscribe(Class<T> eventType, EventPriority priority, EventHandler<T> handler) {
        if (eventType == null || priority == null || handler == null) {
            throw new IllegalArgumentException("Event type, priority, and handler are required.");
        }
        Subscription<T> subscription = new Subscription<T>(eventType, priority, handler);
        List<Subscription<?>> list = subscriptions.get(eventType);
        if (list == null) {
            list = new ArrayList<Subscription<?>>();
            subscriptions.put(eventType, list);
        }
        list.add(subscription);
        Collections.sort(list, new Comparator<Subscription<?>>() {
            public int compare(Subscription<?> left, Subscription<?> right) {
                return right.priority().weight() - left.priority().weight();
            }
        });
        return subscription;
    }

    public void publish(Event event) {
        List<Subscription<?>> snapshot;
        synchronized (this) {
            List<Subscription<?>> list = subscriptions.get(event.getClass());
            if (list == null || list.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<Subscription<?>>(list);
        }
        for (Subscription<?> subscription : snapshot) {
            dispatch(subscription, event);
        }
    }

    public synchronized void unsubscribe(Subscription<?> subscription) {
        List<Subscription<?>> list = subscriptions.get(subscription.eventType());
        if (list != null) {
            list.remove(subscription);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void dispatch(Subscription<?> subscription, Event event) {
        Subscription<T> typed = (Subscription<T>) subscription;
        typed.handler().handle((T) event);
    }
}

