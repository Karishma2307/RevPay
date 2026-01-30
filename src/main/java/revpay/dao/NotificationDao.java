package revpay.dao;

import java.util.List;
import revpay.model.Notification;

public interface NotificationDao {

    void create(long userId, String type, String title, String message);

    List<Notification> findByUserId(long userId);

    List<Notification> findUnreadByUserId(long userId);

    void markAsRead(long userId, long nId);

    void markAllAsRead(long userId);
}