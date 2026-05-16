package com.stla.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminWallet {
    private String id;
    private String currency;
    private BigDecimal totalRevenue;
    private BigDecimal totalCommissions;
    private BigDecimal pendingInstructorPayouts;
    private BigDecimal paidInstructorPayouts;
    private BigDecimal availableBalance;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal v) { this.totalRevenue = v; }
    public BigDecimal getTotalCommissions() { return totalCommissions; }
    public void setTotalCommissions(BigDecimal v) { this.totalCommissions = v; }
    public BigDecimal getPendingInstructorPayouts() { return pendingInstructorPayouts; }
    public void setPendingInstructorPayouts(BigDecimal v) { this.pendingInstructorPayouts = v; }
    public BigDecimal getPaidInstructorPayouts() { return paidInstructorPayouts; }
    public void setPaidInstructorPayouts(BigDecimal v) { this.paidInstructorPayouts = v; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal v) { this.availableBalance = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
