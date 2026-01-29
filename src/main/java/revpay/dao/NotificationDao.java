package revpay.dao;

import java.util.List;
import revpay.model.Notification;

public interface NotificationDao {

    long createNotification(Notification notification);

    List<Notification> findByUserId(long userId, boolean unreadOnly);

    void markAllRead(long userId);
}