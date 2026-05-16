package com.stla.domain.models;

import java.time.LocalDateTime;

public class IssuedCertificate {
    private String id;
    private String certificateId;
    private String studentId;
    private String courseId;
    private String enrollmentId;
    private String certificateNo;
    private String fileUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;

    // Transient
    private String courseName;

    public IssuedCertificate() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String cid) { this.certificateId = cid; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String sid) { this.studentId = sid; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String cid) { this.courseId = cid; }
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String eid) { this.enrollmentId = eid; }
    public String getCertificateNo() { return certificateNo; }
    public void setCertificateNo(String cn) { this.certificateNo = cn; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String f) { this.fileUrl = f; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime d) { this.issuedAt = d; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String n) { this.courseName = n; }
}
