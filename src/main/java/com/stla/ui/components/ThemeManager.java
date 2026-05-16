package com.stla.ui.components;

import com.stla.app.AppConfig;
import javafx.scene.Scene;

/**
 * Theme manager: light/dark mode switching.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private boolean darkMode = false;
    private Scene scene;

    private static final String DARK_CSS = "/com/stla/css/dark-theme.css";

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme();
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        applyTheme();
    }

    public boolean isDarkMode() { return darkMode; }

    private void applyTheme() {
        if (scene == null) return;
        String darkCssUrl = getClass().getResource(DARK_CSS) != null ?
            getClass().getResource(DARK_CSS).toExternalForm() : null;

        if (darkCssUrl == null) return;

        if (darkMode) {
            if (!scene.getStylesheets().contains(darkCssUrl)) {
                scene.getStylesheets().add(darkCssUrl);
            }
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            scene.getStylesheets().remove(darkCssUrl);
            scene.getRoot().getStyleClass().remove("dark-theme");
        }
    }

    /** Apply all base stylesheets to a scene */
    public void applyBaseStyles(Scene scene) {
        this.scene = scene;
        String[] sheets = {
            "/com/stla/css/app.css",
            "/com/stla/css/buttons.css",
            "/com/stla/css/cards.css",
            "/com/stla/css/forms.css",
            "/com/stla/css/tables.css",
            "/com/stla/css/dashboard.css",
            "/com/stla/css/animations.css",
            "/com/stla/css/charts.css",
            "/com/stla/css/player.css",
            "/com/stla/css/sidebar.css",
            "/com/stla/css/upload.css",
            "/com/stla/css/theme.css"
        };
        for (String sheet : sheets) {
            var url = getClass().getResource(sheet);
            if (url != null) scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
