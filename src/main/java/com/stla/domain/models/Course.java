package com.stla.domain.models;

import com.stla.domain.enums.CourseLevel;
import com.stla.domain.enums.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Course model matching public.courses table.
 */
public class Course {

    private String id;
    private String instructorId;
    private String categoryId;
    private String title;
    private String slug;
    private String subtitle;
    private String description;
    private String thumbnailUrl;
    private String introVideoUrl;
    private String language;
    private CourseLevel level;
    private BigDecimal price;
    private Integer estimatedHours;
    private CourseStatus status;
    private String approvalNote;
    private String rejectionReason;
    private String whatYouWillLearn;
    private String requirements;
    private String targetAudience;
    private String approvedByAdminId;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;
    private boolean isFeatured;
    private boolean isArchived;
    private BigDecimal ratingAvg;
    private int ratingCount;
    private int enrollmentCount;
    private boolean hasCertificate;
    private boolean hasQuiz;
    private boolean hasMentorSupport;
    private boolean hasResources;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient: joined data
    private String instructorName;
    private String categoryName;

    public Course() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInstructorId() { return instructorId; }
    public void setInstructorId(String instructorId) { this.instructorId = instructorId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getIntroVideoUrl() { return introVideoUrl; }
    public void setIntroVideoUrl(String introVideoUrl) { this.introVideoUrl = introVideoUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public CourseLevel getLevel() { return level; }
    public void setLevel(CourseLevel level) { this.level = level; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Integer estimatedHours) { this.estimatedHours = estimatedHours; }

    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public String getApprovalNote() { return approvalNote; }
    public void setApprovalNote(String approvalNote) { this.approvalNote = approvalNote; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getWhatYouWillLearn() { return whatYouWillLearn; }
    public void setWhatYouWillLearn(String whatYouWillLearn) { this.whatYouWillLearn = whatYouWillLearn; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public int getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(int enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public boolean isHasCertificate() { return hasCertificate; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }

    public boolean isHasQuiz() { return hasQuiz; }
    public void setHasQuiz(boolean hasQuiz) { this.hasQuiz = hasQuiz; }

    public boolean isHasMentorSupport() { return hasMentorSupport; }
    public void setHasMentorSupport(boolean hasMentorSupport) { this.hasMentorSupport = hasMentorSupport; }

    public boolean isHasResources() { return hasResources; }
    public void setHasResources(boolean hasResources) { this.hasResources = hasResources; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
