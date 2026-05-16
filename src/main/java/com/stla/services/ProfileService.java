package com.stla.services;

import com.stla.core.session.SessionManager;
import com.stla.data.repositories.InstructorRepositoryImpl;
import com.stla.data.repositories.ProfileRepositoryImpl;
import com.stla.data.repositories.StudentRepositoryImpl;
import com.stla.domain.enums.AppRole;
import com.stla.domain.models.Instructor;
import com.stla.domain.models.Profile;
import com.stla.domain.models.Student;

import java.io.File;
import java.util.Optional;

/**
 * Service coordinating profile updates: validation → storage upload → DB update → session refresh.
 */
public class ProfileService {

    private final ProfileRepositoryImpl profileRepo = new ProfileRepositoryImpl();
    private final StudentRepositoryImpl studentRepo = new StudentRepositoryImpl();
    private final InstructorRepositoryImpl instructorRepo = new InstructorRepositoryImpl();
    private final SupabaseStorageService storageService = SupabaseStorageService.getInstance();

    /**
     * Update profile common fields (name, phone, bio, country) + student-specific fields.
     */
    public Result updateStudentProfile(Profile profile, Student student) {
        try {
            if (profile.getFullName() == null || profile.getFullName().isBlank())
                return Result.failure("Full name is required.");

            profileRepo.update(profile);
            studentRepo.update(student);

            // Refresh session
            SessionManager session = SessionManager.getInstance();
            session.getCurrentProfile().setFullName(profile.getFullName());
            session.getCurrentProfile().setPhone(profile.getPhone());
            session.getCurrentProfile().setBio(profile.getBio());
            session.getCurrentProfile().setCountry(profile.getCountry());
            session.getCurrentProfile().setAvatarUrl(profile.getAvatarUrl());
            if (session.getCurrentStudent() != null) {
                session.getCurrentStudent().setHeadline(student.getHeadline());
                session.getCurrentStudent().setInterests(student.getInterests());
                session.getCurrentStudent().setLearningGoals(student.getLearningGoals());
                session.getCurrentStudent().setSkillLevel(student.getSkillLevel());
                session.getCurrentStudent().setPreferredLanguage(student.getPreferredLanguage());
                session.getCurrentStudent().setDailyGoalMinutes(student.getDailyGoalMinutes());
            }
            session.notifyProfileChanged();

            return Result.success("Profile updated successfully.");
        } catch (Exception e) {
            System.err.println("Error updating student profile: " + e.getMessage());
            return Result.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * Update profile common fields + instructor-specific fields.
     */
    public Result updateInstructorProfile(Profile profile, Instructor instructor) {
        try {
            if (profile.getFullName() == null || profile.getFullName().isBlank())
                return Result.failure("Full name is required.");

            profileRepo.update(profile);
            instructorRepo.update(instructor);

            SessionManager session = SessionManager.getInstance();
            session.getCurrentProfile().setFullName(profile.getFullName());
            session.getCurrentProfile().setPhone(profile.getPhone());
            session.getCurrentProfile().setBio(profile.getBio());
            session.getCurrentProfile().setCountry(profile.getCountry());
            session.getCurrentProfile().setAvatarUrl(profile.getAvatarUrl());
            if (session.getCurrentInstructor() != null) {
                session.getCurrentInstructor().setTitle(instructor.getTitle());
                session.getCurrentInstructor().setExpertiseTags(instructor.getExpertiseTags());
                session.getCurrentInstructor().setYearsExperience(instructor.getYearsExperience());
            }
            session.notifyProfileChanged();

            return Result.success("Profile updated successfully.");
        } catch (Exception e) {
            System.err.println("Error updating instructor profile: " + e.getMessage());
            return Result.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * Update admin profile (profiles table only).
     */
    public Result updateAdminProfile(Profile profile) {
        try {
            if (profile.getFullName() == null || profile.getFullName().isBlank()) {
                return Result.failure("Full name is required.");
            }
            profileRepo.update(profile);

            SessionManager session = SessionManager.getInstance();
            session.getCurrentProfile().setFullName(profile.getFullName());
            session.getCurrentProfile().setPhone(profile.getPhone());
            session.getCurrentProfile().setBio(profile.getBio());
            session.getCurrentProfile().setCountry(profile.getCountry());
            session.getCurrentProfile().setAvatarUrl(profile.getAvatarUrl());
            session.notifyProfileChanged();

            return Result.success("Profile updated successfully.");
        } catch (Exception e) {
            System.err.println("Error updating admin profile: " + e.getMessage());
            return Result.failure("Update failed: " + e.getMessage());
        }
    }

    /**
     * Upload avatar, delete old one, update DB, refresh session.
     * @return new avatar URL or null on failure
     */
    public String updateAvatar(File imageFile, AppRole role) {
        try {
            SessionManager session = SessionManager.getInstance();
            String profileId = session.getCurrentUserId();
            String oldUrl = session.getCurrentUserAvatar();

            // Upload to correct bucket
            String newUrl;
            if (role == AppRole.INSTRUCTOR) {
                newUrl = storageService.uploadInstructorAvatar(imageFile, profileId);
            } else {
                newUrl = storageService.uploadStudentAvatar(imageFile, profileId);
            }

            if (newUrl != null) {
                // Delete old file (best-effort)
                storageService.deleteOldFile(oldUrl);

                // Update DB
                profileRepo.updateAvatarUrl(profileId, newUrl);

                // Update session
                session.getCurrentProfile().setAvatarUrl(newUrl);
                session.notifyProfileChanged();
            }
            return newUrl;
        } catch (Exception e) {
            System.err.println("Error updating avatar: " + e.getMessage());
            return null;
        }
    }

    /**
     * Re-fetch profile from DB and update session.
     */
    public void refreshCurrentProfile() {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) return;
        String profileId = session.getCurrentUserId();
        Optional<Profile> opt = profileRepo.findById(profileId);
        opt.ifPresent(p -> {
            Profile current = session.getCurrentProfile();
            current.setFullName(p.getFullName());
            current.setPhone(p.getPhone());
            current.setBio(p.getBio());
            current.setCountry(p.getCountry());
            current.setAvatarUrl(p.getAvatarUrl());
            session.notifyProfileChanged();
        });
    }

    // --- Simple result wrapper ---
    public record Result(boolean success, String message) {
        public static Result success(String msg) { return new Result(true, msg); }
        public static Result failure(String msg) { return new Result(false, msg); }
    }
}
