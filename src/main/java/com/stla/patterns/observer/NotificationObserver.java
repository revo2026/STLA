package com.stla.patterns.observer;

import com.stla.services.NotificationService;

import java.util.EnumSet;
import java.util.Set;

/**
 * Observer that persists notifications when AppEvents are published.
 */
public class NotificationObserver implements EventListener {

    private static final Set<AppEvent.EventType> HANDLED = EnumSet.complementOf(
            EnumSet.of(AppEvent.EventType.COURSE_PROGRESS_UPDATED));

    private final NotificationService notificationService = new NotificationService();

    public static void register() {
        NotificationObserver observer = new NotificationObserver();
        EventBus bus = EventBus.getInstance();
        for (AppEvent.EventType type : HANDLED) {
            bus.subscribe(type, observer);
        }
    }

    @Override
    public void onEvent(AppEvent event) {
        try {
            notificationService.handleAppEvent(event);
        } catch (Exception e) {
            System.err.println("[NotificationObserver] " + e.getMessage());
        }
    }
}
