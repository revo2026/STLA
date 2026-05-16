package com.stla.patterns.proxy;

import com.stla.core.session.SessionManager;
import com.stla.domain.enums.AppRole;

/**
 * Proxy Pattern: Access control for role-based screen navigation.
 */
public class AccessControlProxy {

    public boolean canAccess(String screen) {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) return false;

        AppRole role = session.getCurrentRole();
        return switch (role) {
            case STUDENT -> screen.startsWith("student");
            case INSTRUCTOR -> screen.startsWith("instructor");
            case ADMIN -> true; // Admins can access all screens
        };
    }

    public boolean isOwner(String resourceOwnerId) {
        String currentUserId = SessionManager.getInstance().getCurrentUserId();
        return currentUserId != null && currentUserId.equals(resourceOwnerId);
    }

    public boolean canManageCourse(String courseInstructorProfileId) {
        SessionManager session = SessionManager.getInstance();
        if (session.isAdmin()) return true;
        if (session.isInstructor()) {
            return session.getCurrentProfile().getId().equals(courseInstructorProfileId);
        }
        return false;
    }
}
