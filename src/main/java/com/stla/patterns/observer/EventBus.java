package com.stla.patterns.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observer Pattern: Central event bus for publishing and subscribing to application events.
 */
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<AppEvent.EventType, List<EventListener>> listeners = new HashMap<>();

    private EventBus() {}
    public static EventBus getInstance() { return INSTANCE; }

    public void subscribe(AppEvent.EventType type, EventListener listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public void unsubscribe(AppEvent.EventType type, EventListener listener) {
        List<EventListener> subs = listeners.get(type);
        if (subs != null) subs.remove(listener);
    }

    public void publish(AppEvent event) {
        List<EventListener> subs = listeners.get(event.type());
        if (subs != null) subs.forEach(l -> l.onEvent(event));
    }
}
