package com.stla.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InstructorWallet {
    private String id;
    private String instructorId;
    private String currency;
    private BigDecimal pendingBalance;
    private BigDecimal availableBalance;
    private BigDecimal totalEarned;
    private BigDecimal totalWithdrawn;
    private LocalDateTime lastSettlementAt;
    private LocalDateTime createdAt;

    public InstructorWallet() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInstructorId() { return instructorId; }
    public void setInstructorId(String iid) { this.instructorId = iid; }
    public String getCurrency() { return currency; }
    public void setCurrency(String c) { this.currency = c; }
    public BigDecimal getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(BigDecimal p) { this.pendingBalance = p; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal a) { this.availableBalance = a; }
    public BigDecimal getTotalEarned() { return totalEarned; }
    public void setTotalEarned(BigDecimal t) { this.totalEarned = t; }
    public BigDecimal getTotalWithdrawn() { return totalWithdrawn; }
    public void setTotalWithdrawn(BigDecimal t) { this.totalWithdrawn = t; }
    public LocalDateTime getLastSettlementAt() { return lastSettlementAt; }
    public void setLastSettlementAt(LocalDateTime l) { this.lastSettlementAt = l; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
