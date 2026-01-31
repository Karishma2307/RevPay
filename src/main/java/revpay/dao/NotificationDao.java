package revpay.dao;

import java.util.List;
import revpay.model.Notification;

public interface NotificationDao {

    void createNotification(Notification n);

    List<Notification> findByUserId(long userId, boolean unreadOnly);

    void markAllAsRead(long userId);
}
