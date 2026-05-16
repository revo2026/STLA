package com.stla.domain.interfaces;

import com.stla.domain.models.Notification;
import java.util.List;

public interface NotificationRepository {
    List<Notification> findByRecipient(String profileId);
    List<Notification> findRecentByRecipient(String profileId, int limit);
    List<Notification> findUnreadByRecipient(String profileId);
    int countUnread(String profileId);
    void save(Notification notification);
    void markAsRead(String id);
    void markAllAsRead(String profileId);
}
