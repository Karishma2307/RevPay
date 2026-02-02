package revpay.service;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.NotificationDao;
import revpay.dao.impl.NotificationDaoImpl;
import revpay.model.Notification;
import revpay.util.ConsoleUtil;

public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDao notificationDao = new NotificationDaoImpl();

    public void notifyUser(long userId, String type, String title, String message) {

        
        logger.info("Notify user requested (userId={}, type='{}', title='{}')",
                userId, safe(type), safe(title));

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);

        try {
            notificationDao.createNotification(n);
            logger.debug("Notification stored (userId={}, type='{}', title='{}')",
                    userId, safe(type), safe(title));
        } catch (Exception e) {
            logger.error("Failed to create notification (userId={}, type='{}', title='{}')",
                    userId, safe(type), safe(title), e);
        }
    }

    public void showNotificationsMenu(Scanner sc, long userId) {
        showNotifications(sc, userId);
    }

    public void showNotifications(Scanner sc, long userId) {
        logger.info("Notifications menu opened (userId={})", userId);

        while (true) {
            ConsoleUtil.printHeader("Notifications");
            System.out.println("1. All Notifications");
            System.out.println("2. Unread Only");
            System.out.println("3. Mark all as read");
            System.out.println("4. Back");
            System.out.print("Choice: ");

            String choice = sc.nextLine();

            if ("4".equals(choice)) {
                logger.info("Notifications menu closed (userId={})", userId);
                return;
            }

            if ("1".equals(choice) || "2".equals(choice)) {
                boolean unreadOnly = "2".equals(choice);

                List<Notification> list;
                try {
                    list = notificationDao.findByUserId(userId, unreadOnly);
                } catch (Exception e) {
                    logger.error("Failed to fetch notifications (userId={}, unreadOnly={})",
                            userId, unreadOnly, e);
                    System.out.println("Unable to load notifications right now.");
                    ConsoleUtil.pause(sc);
                    continue;
                }

                if (list == null || list.isEmpty()) {
                    logger.info("No notifications found (userId={}, unreadOnly={})", userId, unreadOnly);
                    System.out.println("No notifications.");
                    ConsoleUtil.pause(sc);
                    continue;
                }

                logger.debug("Notifications fetched (userId={}, unreadOnly={}, count={})",
                        userId, unreadOnly, list.size());

                for (Notification n : list) {
                    System.out.println("--------------------------------");
                    System.out.println("Title: " + n.getTitle());
                    System.out.println("Type : " + n.getType());
                    System.out.println("Msg  : " + n.getMessage());
                    System.out.println("Read : " + (n.isRead() ? "YES" : "NO"));
                    System.out.println("Date : " + n.getCreatedAt());
                }
                ConsoleUtil.pause(sc);

            } else if ("3".equals(choice)) {
                try {
                    notificationDao.markAllAsRead(userId);
                    logger.info("Marked all notifications as read (userId={})", userId);
                    System.out.println("Marked all as read.");
                } catch (Exception e) {
                    logger.error("Failed to mark notifications as read (userId={})", userId, e);
                    System.out.println("Failed to mark all as read.");
                }
                ConsoleUtil.pause(sc);

            } else {
                logger.warn("Invalid notifications menu option '{}' (userId={})", choice, userId);
                System.out.println("Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
