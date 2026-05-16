package com.stla.core.session;

import com.stla.domain.enums.AppRole;
import com.stla.domain.models.Profile;
import com.stla.domain.models.Student;
import com.stla.domain.models.Instructor;
import com.stla.domain.models.Admin;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton session manager that holds the currently logged-in user state.
 * Supports profile change listeners for reactive UI updates.
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    private Profile currentProfile;
    private Student currentStudent;
    private Instructor currentInstructor;
    private Admin currentAdmin;
    private boolean loggedIn;

    // Profile change listeners — UI components subscribe here
    private final List<Runnable> profileChangeListeners = new ArrayList<>();

    private SessionManager() {
        this.loggedIn = false;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    // --- Login / Logout ---

    public void loginAsStudent(Profile profile, Student student) {
        this.currentProfile = profile;
        this.currentStudent = student;
        this.currentInstructor = null;
        this.currentAdmin = null;
        this.loggedIn = true;
    }

    public void loginAsInstructor(Profile profile, Instructor instructor) {
        this.currentProfile = profile;
        this.currentStudent = null;
        this.currentInstructor = instructor;
        this.currentAdmin = null;
        this.loggedIn = true;
    }

    public void loginAsAdmin(Profile profile, Admin admin) {
        this.currentProfile = profile;
        this.currentStudent = null;
        this.currentInstructor = null;
        this.currentAdmin = admin;
        this.loggedIn = true;
    }

    public void logout() {
        this.currentProfile = null;
        this.currentStudent = null;
        this.currentInstructor = null;
        this.currentAdmin = null;
        this.loggedIn = false;
        profileChangeListeners.clear();
    }

    // --- Accessors ---

    public boolean isLoggedIn() { return loggedIn; }

    public Profile getCurrentProfile() { return currentProfile; }

    public AppRole getCurrentRole() {
        return currentProfile != null ? currentProfile.getRole() : null;
    }

    public Student getCurrentStudent() { return currentStudent; }
    public Instructor getCurrentInstructor() { return currentInstructor; }
    public Admin getCurrentAdmin() { return currentAdmin; }

    public String getCurrentUserId() {
        return currentProfile != null ? currentProfile.getId() : null;
    }

    public String getCurrentUserName() {
        return currentProfile != null ? currentProfile.getFullName() : "Guest";
    }

    public String getCurrentUserEmail() {
        return currentProfile != null ? currentProfile.getEmail() : "";
    }

    public String getCurrentUserAvatar() {
        return currentProfile != null ? currentProfile.getAvatarUrl() : null;
    }

    public boolean isStudent() {
        return loggedIn && currentProfile != null && currentProfile.getRole() == AppRole.STUDENT;
    }

    public boolean isInstructor() {
        return loggedIn && currentProfile != null && currentProfile.getRole() == AppRole.INSTRUCTOR;
    }

    public boolean isAdmin() {
        return loggedIn && currentProfile != null && currentProfile.getRole() == AppRole.ADMIN;
    }

    // --- Profile Change Listeners ---

    /** Register a listener that will be called on the FX thread when profile changes. */
    public void addProfileChangeListener(Runnable listener) {
        if (listener != null && !profileChangeListeners.contains(listener)) {
            profileChangeListeners.add(listener);
        }
    }

    /** Remove a listener. */
    public void removeProfileChangeListener(Runnable listener) {
        profileChangeListeners.remove(listener);
    }

    /** Notify all listeners on the FX Application Thread. */
    public void notifyProfileChanged() {
        for (Runnable listener : new ArrayList<>(profileChangeListeners)) {
            try {
                if (Platform.isFxApplicationThread()) {
                    listener.run();
                } else {
                    Platform.runLater(listener);
                }
            } catch (Exception e) {
                System.err.println("[SessionManager] Listener error: " + e.getMessage());
            }
        }
    }
}
