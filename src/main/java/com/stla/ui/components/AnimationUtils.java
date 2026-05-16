package com.stla.ui.components;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Reusable animation utilities for smooth screen transitions and UI effects.
 */
public class AnimationUtils {

    /** Fade-in a node from 0 to 1 opacity */
    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.play();
    }

    /** Fade-in with slide-up effect */
    public static void fadeInUp(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(20);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    /** Slide in from left */
    public static void slideInLeft(Node node, double durationMs) {
        node.setTranslateX(-40);
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromX(-40); tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Scale bounce effect for stats/KPI */
    public static void scaleBounce(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
        st.setFromX(0.8); st.setFromY(0.8);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    /** Stagger animation for a list of nodes */
    public static void staggerFadeIn(javafx.scene.layout.Pane parent, double delayMs) {
        for (int i = 0; i < parent.getChildren().size(); i++) {
            Node child = parent.getChildren().get(i);
            child.setOpacity(0);
            child.setTranslateY(15);
            FadeTransition ft = new FadeTransition(Duration.millis(350), child);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * delayMs));
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), child);
            tt.setFromY(15); tt.setToY(0);
            tt.setDelay(Duration.millis(i * delayMs));
            tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        }
    }

    /** Pulse animation (for notification bell) */
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.2); st.setToY(1.2);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    /** Shake animation (for errors) */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(80), node);
        tt.setFromX(0); tt.setToX(8);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /** Success checkmark bounce */
    public static void successBounce(Node node) {
        node.setScaleX(0); node.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(500), node);
        st.setFromX(0); st.setFromY(0);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }
}
