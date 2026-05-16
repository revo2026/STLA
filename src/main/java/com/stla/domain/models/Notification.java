package com.stla.domain.models;

import com.stla.domain.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * Notification model matching public.notifications table.
 */
public class Notification {

    private String id;
    private String recipientProfileId;
    private String actorProfileId;
    private NotificationType type;
    private String title;
    private String body;
    private String referenceTable;
    private String referenceId;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Notification() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientProfileId() { return recipientProfileId; }
    public void setRecipientProfileId(String recipientProfileId) { this.recipientProfileId = recipientProfileId; }

    public String getActorProfileId() { return actorProfileId; }
    public void setActorProfileId(String actorProfileId) { this.actorProfileId = actorProfileId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getReferenceTable() { return referenceTable; }
    public void setReferenceTable(String referenceTable) { this.referenceTable = referenceTable; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
