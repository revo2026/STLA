package com.stla.core.navigation;

import com.stla.patterns.facade.EnrollmentFacade;

import java.util.function.Consumer;

/** Holds navigation state between student checkout screens. */
public final class StudentNavigationContext {
    private static String selectedCourseId;
    private static EnrollmentFacade.PurchaseResult lastPurchaseResult;
    private static Consumer<String> openCourseDetails;
    private static Consumer<String> openCheckout;
    private static Consumer<String> openCoursePlayer;
    private static Runnable stopVideoPlayback;

    private StudentNavigationContext() {}

    public static void registerVideoStopHandler(Runnable handler) { stopVideoPlayback = handler; }

    public static void unregisterVideoStopHandler(Runnable handler) {
        if (stopVideoPlayback == handler) stopVideoPlayback = null;
    }

    /** Stops any active course video (call before leaving the learning screen). */
    public static void stopActiveVideo() {
        if (stopVideoPlayback != null) stopVideoPlayback.run();
    }

    public static void setSelectedCourseId(String courseId) { selectedCourseId = courseId; }
    public static String getSelectedCourseId() { return selectedCourseId; }

    public static void setLastPurchaseResult(EnrollmentFacade.PurchaseResult result) { lastPurchaseResult = result; }
    public static EnrollmentFacade.PurchaseResult getLastPurchaseResult() { return lastPurchaseResult; }

    public static void setOpenCourseDetails(Consumer<String> handler) { openCourseDetails = handler; }
    public static void setOpenCheckout(Consumer<String> handler) { openCheckout = handler; }
    public static void setOpenCoursePlayer(Consumer<String> handler) { openCoursePlayer = handler; }

    public static void goToCourseDetails(String courseId) {
        if (courseId == null || courseId.isBlank()) return;
        setSelectedCourseId(courseId);
        if (openCourseDetails != null) openCourseDetails.accept(courseId);
    }

    public static void goToCheckout(String courseId) {
        if (courseId == null || courseId.isBlank()) return;
        setSelectedCourseId(courseId);
        if (openCheckout != null) openCheckout.accept(courseId);
    }

    public static void goToCoursePlayer(String courseId) {
        if (courseId == null || courseId.isBlank()) return;
        setSelectedCourseId(courseId);
        stopActiveVideo();
        if (openCoursePlayer != null) {
            openCoursePlayer.accept(courseId);
        } else if (openCourseDetails != null) {
            openCourseDetails.accept(courseId);
        }
    }

    public static void clear() {
        selectedCourseId = null;
        lastPurchaseResult = null;
    }
}
