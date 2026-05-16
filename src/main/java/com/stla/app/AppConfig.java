package com.stla.app;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Singleton application configuration loaded from .env file.
 * Provides centralized access to all environment-based settings.
 */
public final class AppConfig {

    private static volatile AppConfig instance;
    private final Dotenv dotenv;

    // Database
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final String dbUrl;

    // Supabase API
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final String supabaseServiceRoleKey;

    // Application
    private final String appTitle;
    private final int appWidth;
    private final int appHeight;
    private final int platformCommissionPercent;

    private AppConfig() {
        this.dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        System.out.println("[AppConfig] .env loaded. DB_HOST=" + dotenv.get("SUPABASE_DB_HOST"));

        // Database configuration
        this.dbHost = getEnv("SUPABASE_DB_HOST", "localhost");
        this.dbPort = Integer.parseInt(getEnv("SUPABASE_DB_PORT", "5432"));
        this.dbName = getEnv("SUPABASE_DB_NAME", "postgres");
        this.dbUser = getEnv("SUPABASE_DB_USER", "postgres");
        this.dbPassword = getEnv("SUPABASE_DB_PASSWORD", "");
        this.dbUrl = String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=require&prepareThreshold=0",
                dbHost, dbPort, dbName
        );

        // Supabase API
        this.supabaseUrl = getEnv("SUPABASE_URL", "");
        this.supabaseAnonKey = getEnv("SUPABASE_ANON_KEY", "");
        this.supabaseServiceRoleKey = getEnv("SUPABASE_SERVICE_ROLE_KEY", "");

        // Application settings
        this.appTitle = getEnv("APP_TITLE", "STLA - Student Learning App");
        this.appWidth = Integer.parseInt(getEnv("APP_WIDTH", "1400"));
        this.appHeight = Integer.parseInt(getEnv("APP_HEIGHT", "900"));
        this.platformCommissionPercent = Integer.parseInt(getEnv("PLATFORM_COMMISSION_PERCENT", "20"));
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    private String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    // --- Getters ---

    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }

    public String getSupabaseUrl() { return supabaseUrl; }
    public String getSupabaseAnonKey() { return supabaseAnonKey; }
    public String getSupabaseServiceRoleKey() { return supabaseServiceRoleKey; }

    public String getAppTitle() { return appTitle; }
    public int getAppWidth() { return appWidth; }
    public int getAppHeight() { return appHeight; }
    public int getPlatformCommissionPercent() { return platformCommissionPercent; }
}
