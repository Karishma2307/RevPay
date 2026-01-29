package revpay.service;

import java.util.List;

import revpay.dao.NotificationDao;
import revpay.dao.impl.NotificationDaoImpl;
import revpay.model.Notification;
import revpay.util.ConsoleUtil;

import java.util.Scanner;

public class NotificationService {

    private NotificationDao notificationDao = new NotificationDaoImpl();

    public void notifyUser(long userId, String type, String title, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        notificationDao.createNotification(n);
    }

    public void showNotifications(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Notifications");
        System.out.println("1. All Notifications");
        System.out.println("2. Unread Only");
        System.out.println("3. Mark all as read");
        System.out.println("4. Back");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        if ("1".equals(choice) || "2".equals(choice)) {
            boolean unreadOnly = "2".equals(choice);
            List<Notification> list = notificationDao.findByUserId(userId, unreadOnly);
            if (list.isEmpty()) {
                System.out.println("No notifications.");
            } else {
                int i = 1;
                for (Notification n : list) {
                    String status = n.isRead() ? "READ" : "UNREAD";
                    System.out.printf("[%d] [%s] [%s] %s - %s%n",
                            i++, status, n.getType(), n.getTitle(), n.getMessage());
                }
            }
            ConsoleUtil.pause(sc);
        } else if ("3".equals(choice)) {
            notificationDao.markAllRead(userId);
            System.out.println("[INFO] All notifications marked as read.");
            ConsoleUtil.pause(sc);
        } else {
            // back
        }
    }
}
