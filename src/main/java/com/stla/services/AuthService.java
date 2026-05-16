package com.stla.services;

import com.stla.core.session.SessionManager;
import com.stla.data.repositories.*;
import com.stla.domain.enums.AppRole;
import com.stla.domain.models.*;

import java.util.Optional;

/**
 * Authentication service. Validates credentials against the profiles table
 * and initializes the session with role-specific data.
 */
public class AuthService {

    private final ProfileRepositoryImpl profileRepo = new ProfileRepositoryImpl();
    private final StudentRepositoryImpl studentRepo = new StudentRepositoryImpl();
    private final InstructorRepositoryImpl instructorRepo = new InstructorRepositoryImpl();
    private final AdminRepositoryImpl adminRepo = new AdminRepositoryImpl();

    /**
     * Authenticate user by email and password.
     * The database stores bcrypt hashes created by pgcrypto's crypt().
     * We compare using a SQL query with crypt() for compatibility.
     */
    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return AuthResult.failure("Email and password are required.");
        }

        Optional<Profile> profileOpt = profileRepo.findByEmail(email.trim().toLowerCase());
        if (profileOpt.isEmpty()) {
            return AuthResult.failure("Invalid email or password.");
        }

        Profile profile = profileOpt.get();

        if (!profile.isActive()) {
            return AuthResult.failure("Your account has been deactivated. Contact support.");
        }

        // Verify password: compare with stored hash
        // The DB stores pgcrypto crypt() hashes. We do a SQL-level check.
        if (!verifyPassword(email, password)) {
            return AuthResult.failure("Invalid email or password.");
        }

        // Update last login
        profileRepo.updateLastLogin(profile.getId());

        // Initialize session based on role
        SessionManager session = SessionManager.getInstance();
        switch (profile.getRole()) {
            case STUDENT -> {
                Optional<Student> student = studentRepo.findByProfileId(profile.getId());
                if (student.isEmpty()) return AuthResult.failure("Student record not found.");
                session.loginAsStudent(profile, student.get());
            }
            case INSTRUCTOR -> {
                Optional<Instructor> instructor = instructorRepo.findByProfileId(profile.getId());
                if (instructor.isEmpty()) return AuthResult.failure("Instructor record not found.");
                session.loginAsInstructor(profile, instructor.get());
            }
            case ADMIN -> {
                Optional<Admin> admin = adminRepo.findByProfileId(profile.getId());
                if (admin.isEmpty()) return AuthResult.failure("Admin record not found.");
                session.loginAsAdmin(profile, admin.get());
            }
        }

        return AuthResult.success(profile);
    }

    /**
     * Verify password using SQL-level crypt() comparison for pgcrypto compatibility.
     */
    private boolean verifyPassword(String email, String password) {
        String sql = "SELECT id FROM profiles WHERE email = ? AND password_hash = crypt(?, password_hash)";
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.err.println("Password verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Register a new user with all profile and role-specific fields.
     * Uses a DB transaction — if role record insert fails, the profile insert is rolled back.
     * Admin registration is explicitly blocked.
     */
    public AuthResult register(String fullName, String email, String password, AppRole role,
                               String phone, String country, String bio, String avatarUrl, String timezone,
                               String headline, String interests, String learningGoals,
                               String skillLevel, String preferredLanguage, String dailyGoalMinutes,
                               String title, String expertise, String yearsExp,
                               String instructorBio, String idFrontUrl, String idBackUrl,
                               String certUrl, String cvUrl) {

        // --- Input validation ---
        if (fullName == null || fullName.isBlank()) return AuthResult.failure("Full name is required.");
        if (email == null || email.isBlank()) return AuthResult.failure("Email is required.");
        if (password == null || password.length() < 8) return AuthResult.failure("Password must be at least 8 characters.");
        if (role == AppRole.ADMIN) return AuthResult.failure("Admin accounts can only be created by the system administrator.");

        String cleanEmail = email.trim().toLowerCase();

        // Check uniqueness
        if (profileRepo.findByEmail(cleanEmail).isPresent()) {
            return AuthResult.failure("An account with this email already exists.");
        }

        java.sql.Connection conn = null;
        try {
            conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // BEGIN TRANSACTION

            // 1. Hash password via pgcrypto
            String hashedPassword = hashPassword(conn, password);

            // 2. Insert into profiles
            String profileId = java.util.UUID.randomUUID().toString();
            String profileSql = """
                INSERT INTO profiles (id, role, full_name, email, password_hash, phone, avatar_url, bio, country, timezone, is_active)
                VALUES (?::uuid, ?::app_role, ?, ?, ?, ?, ?, ?, ?, ?, true)
                """;
            try (var ps = conn.prepareStatement(profileSql)) {
                ps.setString(1, profileId);
                ps.setString(2, role.getValue());
                ps.setString(3, fullName.trim());
                ps.setString(4, cleanEmail);
                ps.setString(5, hashedPassword);
                ps.setString(6, phone == null || phone.isBlank() ? null : phone.trim());
                ps.setString(7, avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl.trim());
                ps.setString(8, bio == null || bio.isBlank() ? null : bio.trim());
                ps.setString(9, country == null || country.isBlank() ? null : country.trim());
                ps.setString(10, timezone == null || timezone.isBlank() ? null : timezone.trim());
                ps.executeUpdate();
            }

            // 3. Insert role-specific record using Factory pattern
            String roleRecordId = java.util.UUID.randomUUID().toString();
            switch (role) {
                case STUDENT -> {
                    String studentSql = "INSERT INTO students (id, profile_id, headline, interests, learning_goals, skill_level, preferred_language, daily_goal_minutes) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?)";
                    try (var ps = conn.prepareStatement(studentSql)) {
                        ps.setString(1, roleRecordId);
                        ps.setString(2, profileId);
                        ps.setString(3, headline == null || headline.isBlank() ? null : headline.trim());
                        String[] interestArr = (interests == null || interests.isBlank()) ? new String[0] : interests.split(",\\s*");
                        ps.setArray(4, conn.createArrayOf("text", interestArr));
                        ps.setString(5, learningGoals == null || learningGoals.isBlank() ? null : learningGoals.trim());
                        ps.setString(6, skillLevel == null || skillLevel.isBlank() ? "beginner" : skillLevel.trim());
                        ps.setString(7, preferredLanguage == null || preferredLanguage.isBlank() ? "en" : preferredLanguage.trim());
                        Integer goalMins = 30;
                        if (dailyGoalMinutes != null && !dailyGoalMinutes.isBlank()) {
                            try { goalMins = Integer.parseInt(dailyGoalMinutes.trim()); } catch (NumberFormatException ignored) {}
                        }
                        ps.setInt(8, goalMins);
                        ps.executeUpdate();
                    }
                }
                case INSTRUCTOR -> {
                    System.out.println("[AuthService] Saving instructor with doc URLs:");
                    System.out.println("[AuthService]   id_front_url = " + idFrontUrl);
                    System.out.println("[AuthService]   id_back_url  = " + idBackUrl);
                    System.out.println("[AuthService]   cert_url     = " + certUrl);
                    System.out.println("[AuthService]   cv_url       = " + cvUrl);
                    String instrSql = "INSERT INTO instructors (id, profile_id, title, expertise_tags, years_experience, instructor_bio, id_front_url, id_back_url, experience_certificate_url, cv_url, verification_status) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
                    try (var ps = conn.prepareStatement(instrSql)) {
                        ps.setString(1, roleRecordId);
                        ps.setString(2, profileId);
                        ps.setString(3, title == null || title.isBlank() ? null : title.trim());
                        String[] expTags = (expertise == null || expertise.isBlank()) ? new String[0] : expertise.split(",\\s*");
                        ps.setArray(4, conn.createArrayOf("text", expTags));
                        Integer years = null;
                        if (yearsExp != null && !yearsExp.isBlank()) {
                            try { years = Integer.parseInt(yearsExp.trim()); } catch (NumberFormatException ignored) {}
                        }
                        if (years != null) ps.setInt(5, years);
                        else ps.setNull(5, java.sql.Types.INTEGER);
                        ps.setString(6, instructorBio == null || instructorBio.isBlank() ? null : instructorBio.trim());
                        ps.setString(7, idFrontUrl);
                        ps.setString(8, idBackUrl);
                        ps.setString(9, certUrl);
                        ps.setString(10, cvUrl);
                        ps.executeUpdate();
                    }
                }
                default -> { /* Admin blocked above */ }
            }

            conn.commit(); // COMMIT TRANSACTION

            // Build profile model for session
            Profile profile = new Profile(profileId, role, fullName.trim(), cleanEmail);
            profile.setPasswordHash(hashedPassword);
            profile.setPhone(phone);
            profile.setAvatarUrl(avatarUrl);
            profile.setBio(bio);
            profile.setCountry(country);
            profile.setTimezone(timezone);
            profile.setActive(true);

            // Initialize session so navigateToDashboard() works
            SessionManager session = SessionManager.getInstance();
            switch (role) {
                case STUDENT -> {
                    Student student = com.stla.patterns.factory.UserFactory.createStudent(profile);
                    student.setId(roleRecordId);
                    student.setHeadline(headline);
                    student.setInterests(interests == null ? new String[0] : interests.split(",\\s*"));
                    student.setLearningGoals(learningGoals);
                    student.setSkillLevel(skillLevel);
                    student.setPreferredLanguage(preferredLanguage);
                    try { student.setDailyGoalMinutes(Integer.parseInt(dailyGoalMinutes)); } catch (Exception ignored) { student.setDailyGoalMinutes(30); }
                    session.loginAsStudent(profile, student);
                }
                case INSTRUCTOR -> {
                    Instructor instructor = com.stla.patterns.factory.UserFactory.createInstructor(profile);
                    instructor.setId(roleRecordId);
                    instructor.setTitle(title);
                    instructor.setExpertiseTags(expertise == null ? new String[0] : expertise.split(",\\s*"));
                    Integer years = null;
                    try { years = Integer.parseInt(yearsExp); } catch (Exception ignored) {}
                    instructor.setYearsExperience(years);
                    instructor.setInstructorBio(instructorBio);
                    instructor.setIdFrontUrl(idFrontUrl);
                    instructor.setIdBackUrl(idBackUrl);
                    instructor.setExperienceCertificateUrl(certUrl);
                    instructor.setCvUrl(cvUrl);
                    instructor.setVerificationStatus("PENDING");
                    session.loginAsInstructor(profile, instructor);
                }
                default -> {}
            }

            com.stla.patterns.observer.EventBus bus = com.stla.patterns.observer.EventBus.getInstance();
            bus.publish(new com.stla.patterns.observer.AppEvent(
                com.stla.patterns.observer.AppEvent.EventType.USER_REGISTERED,
                profileId, profileId, "New " + role.getValue() + " registered: " + fullName));
            if (role == AppRole.INSTRUCTOR) {
                bus.publish(new com.stla.patterns.observer.AppEvent(
                    com.stla.patterns.observer.AppEvent.EventType.INSTRUCTOR_VERIFICATION_SUBMITTED,
                    profileId, roleRecordId,
                    "Instructor " + fullName + " submitted verification documents for review."));
            }

            return AuthResult.success(profile);

        } catch (Exception e) {
            // ROLLBACK on any failure
            if (conn != null) {
                try { conn.rollback(); } catch (Exception rb) { System.err.println("Rollback error: " + rb.getMessage()); }
            }
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return AuthResult.failure("Registration failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Hash password using pgcrypto crypt() + gen_salt('bf') within the given connection/transaction.
     */
    private String hashPassword(java.sql.Connection conn, String password) throws java.sql.SQLException {
        String sql = "SELECT crypt(?, gen_salt('bf')) AS hash";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, password);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("hash");
            }
        }
        throw new java.sql.SQLException("Failed to hash password via pgcrypto.");
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }

    /**
     * Result wrapper for authentication operations.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final Profile profile;

        private AuthResult(boolean success, String message, Profile profile) {
            this.success = success;
            this.message = message;
            this.profile = profile;
        }

        public static AuthResult success(Profile profile) {
            return new AuthResult(true, "Login successful", profile);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Profile getProfile() { return profile; }
    }
}
