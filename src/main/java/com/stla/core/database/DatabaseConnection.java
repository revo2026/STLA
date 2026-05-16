package com.stla.core.database;

import com.stla.app.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton database connection pool using HikariCP.
 * Connects to Supabase PostgreSQL with SSL.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        AppConfig config = AppConfig.getInstance();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setUsername(config.getDbUser());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        // Connection pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(300_000);       // 5 minutes
        hikariConfig.setConnectionTimeout(20_000);   // 20 seconds
        hikariConfig.setMaxLifetime(1_200_000);      // 20 minutes
        hikariConfig.setLeakDetectionThreshold(60_000);

        // PostgreSQL-specific optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        hikariConfig.setPoolName("STLA-HikariPool");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Get a connection from the pool.
     * Always use try-with-resources to ensure connections are returned.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Test the database connection.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Shutdown the connection pool gracefully.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
