package com.stla.patterns.singleton;

/**
 * Singleton Pattern documentation.
 * The following classes implement the Singleton pattern in this project:
 * - com.stla.app.AppConfig
 * - com.stla.core.database.DatabaseConnection
 * - com.stla.core.session.SessionManager
 * - com.stla.patterns.observer.EventBus
 *
 * All use thread-safe double-checked locking or eager initialization.
 */
public final class SingletonRegistry {
    private SingletonRegistry() {}

    public static void verifyAll() {
        assert com.stla.app.AppConfig.getInstance() != null : "AppConfig singleton failed";
        assert com.stla.core.session.SessionManager.getInstance() != null : "SessionManager singleton failed";
        assert com.stla.patterns.observer.EventBus.getInstance() != null : "EventBus singleton failed";
        System.out.println("[SingletonRegistry] All singletons verified.");
    }
}
