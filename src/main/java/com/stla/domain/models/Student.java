package com.stla.domain.models;

import java.time.LocalDateTime;

/**
 * Student model matching public.students table.
 */
public class Student {

    private String id;
    private String profileId;
    private String headline;
    private String[] interests;
    private String learningGoals;
    private String skillLevel;         // beginner, intermediate, advanced
    private String preferredLanguage;  // en, ar, fr, etc.
    private Integer dailyGoalMinutes;  // 15, 30, 45, 60, 90, 120
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient: joined profile data for convenience
    private Profile profile;

    public Student() {}

    public Student(String id, String profileId) {
        this.id = id;
        this.profileId = profileId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String[] getInterests() { return interests; }
    public void setInterests(String[] interests) { this.interests = interests; }

    public String getLearningGoals() { return learningGoals; }
    public void setLearningGoals(String learningGoals) { this.learningGoals = learningGoals; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public Integer getDailyGoalMinutes() { return dailyGoalMinutes; }
    public void setDailyGoalMinutes(Integer dailyGoalMinutes) { this.dailyGoalMinutes = dailyGoalMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
}
