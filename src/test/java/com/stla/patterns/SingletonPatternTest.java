package com.stla.patterns;

import com.stla.app.AppConfig;
import com.stla.patterns.observer.EventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Singleton Pattern.
 */
@DisplayName("Singleton Pattern")
class SingletonPatternTest {

    @Test
    @DisplayName("EventBus should return same instance")
    void eventBusSingleton() {
        assertSame(EventBus.getInstance(), EventBus.getInstance());
    }

    @Test
    @DisplayName("AppConfig should return same instance")
    void appConfigSingleton() {
        assertSame(AppConfig.getInstance(), AppConfig.getInstance());
    }

    @Test
    @DisplayName("AppConfig should have app title")
    void appConfigHasTitle() {
        String title = AppConfig.getInstance().getAppTitle();
        assertNotNull(title);
        assertFalse(title.isEmpty());
    }

    @Test
    @DisplayName("AppConfig should have positive dimensions")
    void appConfigHasDimensions() {
        assertTrue(AppConfig.getInstance().getAppWidth() > 0);
        assertTrue(AppConfig.getInstance().getAppHeight() > 0);
    }
}
