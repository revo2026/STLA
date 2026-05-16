package com.stla.patterns;

import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;
import com.stla.patterns.observer.EventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Observer Pattern — EventBus.
 */
@DisplayName("Observer Pattern: EventBus")
class ObserverPatternTest {

    @Test
    @DisplayName("EventBus should be a singleton")
    void shouldBeSingleton() {
        EventBus bus1 = EventBus.getInstance();
        EventBus bus2 = EventBus.getInstance();
        assertSame(bus1, bus2);
    }

    @Test
    @DisplayName("Subscriber should receive published event")
    void shouldReceiveEvent() {
        AtomicReference<AppEvent> received = new AtomicReference<>();
        EventListener listener = received::set;

        EventBus.getInstance().subscribe(AppEvent.EventType.ENROLLMENT_CREATED, listener);

        AppEvent event = new AppEvent(
            AppEvent.EventType.ENROLLMENT_CREATED, "actor1", "target1", "test message"
        );
        EventBus.getInstance().publish(event);

        assertNotNull(received.get());
        assertEquals("actor1", received.get().actorProfileId());
        assertEquals("target1", received.get().targetId());
        assertEquals("test message", received.get().message());

        // Cleanup
        EventBus.getInstance().unsubscribe(AppEvent.EventType.ENROLLMENT_CREATED, listener);
    }

    @Test
    @DisplayName("Unsubscribed listener should not receive events")
    void shouldNotReceiveAfterUnsubscribe() {
        AtomicInteger count = new AtomicInteger(0);
        EventListener listener = e -> count.incrementAndGet();

        EventBus.getInstance().subscribe(AppEvent.EventType.PAYMENT_COMPLETED, listener);
        EventBus.getInstance().unsubscribe(AppEvent.EventType.PAYMENT_COMPLETED, listener);

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.PAYMENT_COMPLETED, "a", "b", "test"
        ));

        assertEquals(0, count.get());
    }

    @Test
    @DisplayName("Multiple subscribers should all receive events")
    void shouldNotifyMultipleSubscribers() {
        AtomicInteger count = new AtomicInteger(0);
        EventListener l1 = e -> count.incrementAndGet();
        EventListener l2 = e -> count.incrementAndGet();

        EventBus.getInstance().subscribe(AppEvent.EventType.COURSE_APPROVED, l1);
        EventBus.getInstance().subscribe(AppEvent.EventType.COURSE_APPROVED, l2);

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.COURSE_APPROVED, "admin", "course1", "approved"
        ));

        assertEquals(2, count.get());

        EventBus.getInstance().unsubscribe(AppEvent.EventType.COURSE_APPROVED, l1);
        EventBus.getInstance().unsubscribe(AppEvent.EventType.COURSE_APPROVED, l2);
    }

    @Test
    @DisplayName("Event types should be independent")
    void eventTypesShouldBeIndependent() {
        AtomicInteger count = new AtomicInteger(0);
        EventListener listener = e -> count.incrementAndGet();

        EventBus.getInstance().subscribe(AppEvent.EventType.CERTIFICATE_ISSUED, listener);

        // Publish different event type — should NOT trigger
        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.WITHDRAWAL_REQUESTED, "a", "b", "test"
        ));

        assertEquals(0, count.get());

        EventBus.getInstance().unsubscribe(AppEvent.EventType.CERTIFICATE_ISSUED, listener);
    }
}
