package com.stla.domain.models;

import java.time.LocalDateTime;

public class ActivityLog {
    private String id;
    private String adminId;
    private String actorProfileId;
    private String action;
    private String targetTable;
    private String targetId;
    private String details;
    private LocalDateTime createdAt;

    // Transient
    private String actorName;

    public ActivityLog() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String aid) { this.adminId = aid; }
    public String getActorProfileId() { return actorProfileId; }
    public void setActorProfileId(String apid) { this.actorProfileId = apid; }
    public String getAction() { return action; }
    public void setAction(String a) { this.action = a; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String tt) { this.targetTable = tt; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String tid) { this.targetId = tid; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getActorName() { return actorName; }
    public void setActorName(String n) { this.actorName = n; }
}
