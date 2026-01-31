package revpay.service;

import java.util.List;
import java.util.Scanner;

import revpay.dao.NotificationDao;
import revpay.dao.impl.NotificationDaoImpl;
import revpay.model.Notification;
import revpay.util.ConsoleUtil;

public class NotificationService {

    private final NotificationDao notificationDao = new NotificationDaoImpl();

    public void notifyUser(long userId, String type, String title, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        notificationDao.createNotification(n);
    }

    public void showNotificationsMenu(Scanner sc, long userId) {
        showNotifications(sc, userId);
    }

    public void showNotifications(Scanner sc, long userId) {
        while (true) {
            ConsoleUtil.printHeader("Notifications");
            System.out.println("1. All Notifications");
            System.out.println("2. Unread Only");
            System.out.println("3. Mark all as read");
            System.out.println("4. Back");
            System.out.print("Choice: ");

            String choice = sc.nextLine();

            if ("4".equals(choice)) return;

            if ("1".equals(choice) || "2".equals(choice)) {
                boolean unreadOnly = "2".equals(choice);
                List<Notification> list = notificationDao.findByUserId(userId, unreadOnly);

                if (list == null || list.isEmpty()) {
                    System.out.println("No notifications.");
                    ConsoleUtil.pause(sc);
                    continue;
                }

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
                notificationDao.markAllAsRead(userId);
                System.out.println("Marked all as read.");
                ConsoleUtil.pause(sc);

            } else {
                System.out.println("Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }
}
