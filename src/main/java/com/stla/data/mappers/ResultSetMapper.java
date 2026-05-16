package com.stla.data.mappers;

import com.stla.domain.enums.*;
import com.stla.domain.models.*;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utility class to map JDBC ResultSet rows to domain model objects.
 */
public final class ResultSetMapper {

    private ResultSetMapper() {}

    public static Profile mapProfile(ResultSet rs) throws SQLException {
        Profile p = new Profile();
        p.setId(rs.getString("id"));
        p.setRole(AppRole.fromValue(rs.getString("role")));
        p.setFullName(rs.getString("full_name"));
        p.setEmail(rs.getString("email"));
        p.setPasswordHash(rs.getString("password_hash"));
        p.setPhone(rs.getString("phone"));
        p.setAvatarUrl(rs.getString("avatar_url"));
        p.setBio(rs.getString("bio"));
        p.setCountry(rs.getString("country"));
        p.setTimezone(rs.getString("timezone"));
        p.setActive(rs.getBoolean("is_active"));
        p.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
        p.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return p;
    }

    public static Student mapStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getString("id"));
        s.setProfileId(rs.getString("profile_id"));
        s.setHeadline(rs.getString("headline"));
        s.setInterests(toStringArray(rs.getArray("interests")));
        s.setLearningGoals(rs.getString("learning_goals"));
        // New columns — safe fallback if not yet migrated
        try { s.setSkillLevel(rs.getString("skill_level")); } catch (SQLException ignored) {}
        try { s.setPreferredLanguage(rs.getString("preferred_language")); } catch (SQLException ignored) {}
        try { s.setDailyGoalMinutes(rs.getObject("daily_goal_minutes") != null ? rs.getInt("daily_goal_minutes") : 30); } catch (SQLException ignored) { s.setDailyGoalMinutes(30); }
        s.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        s.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return s;
    }

    public static Instructor mapInstructor(ResultSet rs) throws SQLException {
        Instructor i = new Instructor();
        i.setId(rs.getString("id"));
        i.setProfileId(rs.getString("profile_id"));
        i.setTitle(rs.getString("title"));
        i.setExpertiseTags(toStringArray(rs.getArray("expertise_tags")));
        i.setYearsExperience(rs.getObject("years_experience") != null ? rs.getInt("years_experience") : null);
        i.setRatingAvg(rs.getBigDecimal("rating_avg"));
        i.setRatingCount(rs.getInt("rating_count"));
        i.setTotalStudents(rs.getInt("total_students"));
        i.setTotalCourses(rs.getInt("total_courses"));
        i.setPublic(rs.getBoolean("is_public"));
        i.setVerified(rs.getBoolean("is_verified"));
        i.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        i.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        // Verification fields — safe fallback if migration not applied
        try { i.setIdFrontUrl(rs.getString("id_front_url")); } catch (SQLException ignored) {}
        try { i.setIdBackUrl(rs.getString("id_back_url")); } catch (SQLException ignored) {}
        try { i.setExperienceCertificateUrl(rs.getString("experience_certificate_url")); } catch (SQLException ignored) {}
        try { i.setCvUrl(rs.getString("cv_url")); } catch (SQLException ignored) {}
        try { i.setVerificationStatus(rs.getString("verification_status")); } catch (SQLException ignored) { i.setVerificationStatus("PENDING"); }
        try { i.setRejectionReason(rs.getString("rejection_reason")); } catch (SQLException ignored) {}
        try { i.setInstructorBio(rs.getString("instructor_bio")); } catch (SQLException ignored) {}
        return i;
    }

    public static Admin mapAdmin(ResultSet rs) throws SQLException {
        Admin a = new Admin();
        a.setId(rs.getString("id"));
        a.setProfileId(rs.getString("profile_id"));
        a.setAdminLevel(rs.getInt("admin_level"));
        a.setPermissions(rs.getString("permissions"));
        a.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        a.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return a;
    }

    public static Category mapCategory(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        c.setDescription(rs.getString("description"));
        c.setIconName(rs.getString("icon_name"));
        c.setActive(rs.getBoolean("is_active"));
        c.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return c;
    }

    public static Course mapCourse(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getString("id"));
        c.setInstructorId(rs.getString("instructor_id"));
        c.setCategoryId(rs.getString("category_id"));
        c.setTitle(rs.getString("title"));
        c.setSlug(rs.getString("slug"));
        c.setSubtitle(rs.getString("subtitle"));
        c.setDescription(rs.getString("description"));
        c.setThumbnailUrl(rs.getString("thumbnail_url"));
        c.setIntroVideoUrl(rs.getString("intro_video_url"));
        c.setLanguage(rs.getString("language"));
        c.setLevel(CourseLevel.fromValue(rs.getString("level")));
        c.setPrice(rs.getBigDecimal("price"));
        c.setEstimatedHours(rs.getObject("estimated_hours") != null ? rs.getInt("estimated_hours") : null);
        c.setStatus(CourseStatus.fromValue(rs.getString("status")));
        c.setApprovalNote(rs.getString("approval_note"));
        c.setApprovedByAdminId(rs.getString("approved_by_admin_id"));
        c.setApprovedAt(toLocalDateTime(rs.getTimestamp("approved_at")));
        c.setPublishedAt(toLocalDateTime(rs.getTimestamp("published_at")));
        c.setFeatured(rs.getBoolean("is_featured"));
        c.setArchived(rs.getBoolean("is_archived"));
        c.setRatingAvg(rs.getBigDecimal("rating_avg"));
        c.setRatingCount(rs.getInt("rating_count"));
        try {
            int live = rs.getInt("live_enrollment_count");
            c.setEnrollmentCount(rs.wasNull() ? rs.getInt("enrollment_count") : live);
        } catch (SQLException e) {
            c.setEnrollmentCount(rs.getInt("enrollment_count"));
        }
        c.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        // New fields — safe fallback
        try { c.setRejectionReason(rs.getString("rejection_reason")); } catch (SQLException ignored) {}
        try { c.setWhatYouWillLearn(rs.getString("what_you_will_learn")); } catch (SQLException ignored) {}
        try { c.setRequirements(rs.getString("requirements")); } catch (SQLException ignored) {}
        try { c.setTargetAudience(rs.getString("target_audience")); } catch (SQLException ignored) {}
        try { c.setHasCertificate(rs.getBoolean("has_certificate")); } catch (SQLException ignored) {}
        try { c.setHasQuiz(rs.getBoolean("has_quiz")); } catch (SQLException ignored) {}
        try { c.setHasMentorSupport(rs.getBoolean("has_mentor_support")); } catch (SQLException ignored) {}
        try { c.setHasResources(rs.getBoolean("has_resources")); } catch (SQLException ignored) {}
        return c;
    }

    public static Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getString("id"));
        n.setRecipientProfileId(rs.getString("recipient_profile_id"));
        n.setActorProfileId(rs.getString("actor_profile_id"));
        n.setType(NotificationType.fromValue(rs.getString("type")));
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setReferenceTable(rs.getString("reference_table"));
        n.setReferenceId(rs.getString("reference_id"));
        n.setRead(rs.getBoolean("is_read"));
        n.setReadAt(toLocalDateTime(rs.getTimestamp("read_at")));
        n.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        n.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return n;
    }

    // --- Helpers ---

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static String[] toStringArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) return new String[0];
        Object[] objArr = (Object[]) sqlArray.getArray();
        String[] result = new String[objArr.length];
        for (int i = 0; i < objArr.length; i++) {
            result[i] = objArr[i] != null ? objArr[i].toString() : null;
        }
        return result;
    }
}
