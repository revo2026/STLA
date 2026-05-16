package com.stla.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Instructor model matching public.instructors table.
 */
public class Instructor {

    private String id;
    private String profileId;
    private String title;
    private String[] expertiseTags;
    private Integer yearsExperience;
    private BigDecimal ratingAvg;
    private int ratingCount;
    private int totalStudents;
    private int totalCourses;
    private boolean isPublic;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Verification fields
    private String idFrontUrl;
    private String idBackUrl;
    private String experienceCertificateUrl;
    private String cvUrl;
    private String verificationStatus;   // PENDING | VERIFIED | REJECTED
    private String rejectionReason;
    private String instructorBio;

    private Profile profile;

    public Instructor() {
        this.verificationStatus = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String[] getExpertiseTags() { return expertiseTags; }
    public void setExpertiseTags(String[] expertiseTags) { this.expertiseTags = expertiseTags; }

    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }

    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getTotalCourses() { return totalCourses; }
    public void setTotalCourses(int totalCourses) { this.totalCourses = totalCourses; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    // --- Verification getters/setters ---

    public String getIdFrontUrl() { return idFrontUrl; }
    public void setIdFrontUrl(String idFrontUrl) { this.idFrontUrl = idFrontUrl; }

    public String getIdBackUrl() { return idBackUrl; }
    public void setIdBackUrl(String idBackUrl) { this.idBackUrl = idBackUrl; }

    public String getExperienceCertificateUrl() { return experienceCertificateUrl; }
    public void setExperienceCertificateUrl(String experienceCertificateUrl) { this.experienceCertificateUrl = experienceCertificateUrl; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getInstructorBio() { return instructorBio; }
    public void setInstructorBio(String instructorBio) { this.instructorBio = instructorBio; }

    /** Convenience: true only if verification_status == VERIFIED */
    public boolean isVerificationApproved() {
        return "VERIFIED".equalsIgnoreCase(verificationStatus);
    }
}
