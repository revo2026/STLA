package com.stla.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet transaction record mapping to wallet_transactions table.
 * Tracks every financial movement: earnings, withdrawals, adjustments.
 */
public class WalletTransaction {
    private String id;
    private String walletId;
    private String type;           // earning, withdrawal, adjustment
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private String status;         // pending, completed, failed, reversed
    private String relatedCourseId;
    private String relatedWithdrawalId;
    private LocalDateTime createdAt;

    // Transient (admin views)
    private String courseName;
    private String instructorName;
    private String instructorEmail;

    public WalletTransaction() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String wid) { this.walletId = wid; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal b) { this.balanceBefore = b; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal b) { this.balanceAfter = b; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getRelatedCourseId() { return relatedCourseId; }
    public void setRelatedCourseId(String c) { this.relatedCourseId = c; }
    public String getRelatedWithdrawalId() { return relatedWithdrawalId; }
    public void setRelatedWithdrawalId(String w) { this.relatedWithdrawalId = w; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String n) { this.courseName = n; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public String getInstructorEmail() { return instructorEmail; }
    public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }
}
