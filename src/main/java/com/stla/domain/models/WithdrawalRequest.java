package com.stla.domain.models;

import com.stla.domain.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalRequest {
    private String id;
    private String instructorId;
    private String walletId;
    private BigDecimal amount;
    private String method;
    private String methodDetails;
    private WithdrawalStatus status;
    private String reviewedByAdminId;
    private String reviewNote;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    // Transient joined data
    private String instructorName;
    private String instructorEmail;
    private java.math.BigDecimal walletAvailableBalance;
    private java.math.BigDecimal walletPendingBalance;
    private int completedWithdrawalsCount;

    public WithdrawalRequest() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInstructorId() { return instructorId; }
    public void setInstructorId(String iid) { this.instructorId = iid; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String wid) { this.walletId = wid; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public String getMethod() { return method; }
    public void setMethod(String m) { this.method = m; }
    public String getMethodDetails() { return methodDetails; }
    public void setMethodDetails(String d) { this.methodDetails = d; }
    public WithdrawalStatus getStatus() { return status; }
    public void setStatus(WithdrawalStatus s) { this.status = s; }
    public String getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(String rid) { this.reviewedByAdminId = rid; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String n) { this.reviewNote = n; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime r) { this.requestedAt = r; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime r) { this.reviewedAt = r; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime c) { this.completedAt = c; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String n) { this.instructorName = n; }
    public String getInstructorEmail() { return instructorEmail; }
    public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }
    public java.math.BigDecimal getWalletAvailableBalance() { return walletAvailableBalance; }
    public void setWalletAvailableBalance(java.math.BigDecimal walletAvailableBalance) { this.walletAvailableBalance = walletAvailableBalance; }
    public java.math.BigDecimal getWalletPendingBalance() { return walletPendingBalance; }
    public void setWalletPendingBalance(java.math.BigDecimal walletPendingBalance) { this.walletPendingBalance = walletPendingBalance; }
    public int getCompletedWithdrawalsCount() { return completedWithdrawalsCount; }
    public void setCompletedWithdrawalsCount(int completedWithdrawalsCount) { this.completedWithdrawalsCount = completedWithdrawalsCount; }
}
