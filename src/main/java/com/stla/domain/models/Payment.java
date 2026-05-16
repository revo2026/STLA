package com.stla.domain.models;

import com.stla.domain.enums.PaymentStatus;
import com.stla.domain.enums.PaymentMethodType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private String id;
    private String studentId;
    private String courseId;
    private String enrollmentId;
    private String paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String gatewayProvider;
    private String gatewayTransactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String paymentReference;
    private String walletProvider;
    private BigDecimal commissionAmount;
    private BigDecimal instructorShare;
    private BigDecimal adminShare;

    // Transient
    private String studentName;
    private String courseName;
    private String methodType;

    public Payment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String id) { this.paymentMethodId = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getGatewayProvider() { return gatewayProvider; }
    public void setGatewayProvider(String gp) { this.gatewayProvider = gp; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String id) { this.gatewayTransactionId = id; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String n) { this.studentName = n; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String n) { this.courseName = n; }
    public String getMethodType() { return methodType; }
    public void setMethodType(String m) { this.methodType = m; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getWalletProvider() { return walletProvider; }
    public void setWalletProvider(String walletProvider) { this.walletProvider = walletProvider; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getInstructorShare() { return instructorShare; }
    public void setInstructorShare(BigDecimal instructorShare) { this.instructorShare = instructorShare; }
    public BigDecimal getAdminShare() { return adminShare; }
    public void setAdminShare(BigDecimal adminShare) { this.adminShare = adminShare; }
}
