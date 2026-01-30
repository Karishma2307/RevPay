package revpay.service;

import java.util.List;
import java.util.Scanner;

import revpay.dao.NotificationDao;
import revpay.dao.NotificationPrefDao;
import revpay.dao.impl.NotificationDaoImpl;
import revpay.dao.impl.NotificationPrefDaoImpl;
import revpay.model.Notification;
import revpay.util.ConsoleUtil;

public class NotificationService {

    private final NotificationDao dao = new NotificationDaoImpl();
    private final NotificationPrefDao prefDao = new NotificationPrefDaoImpl();

    // ✅ Updated: respects notification preferences
    public void notifyUser(long userId, String type, String title, String message) {

        // ensure pref row exists
        prefDao.ensureExists(userId);

        // if disabled, do nothing
        if (!prefDao.isEnabled(userId, type)) return;

        dao.create(userId, type, title, message);
    }

    public void showNotificationsMenu(Scanner sc, long userId) {
        // ensure prefs exists for this user
        prefDao.ensureExists(userId);

        while (true) {
            ConsoleUtil.printHeader("Notifications");
            System.out.println("1. View All");
            System.out.println("2. View Unread");
            System.out.println("3. Mark One As Read");
            System.out.println("4. Mark All As Read");
            System.out.println("5. Preferences (Enable/Disable + Threshold)"); // ✅ Step 5
            System.out.println("6. Back");
            System.out.print("Choice: ");
            String c = sc.nextLine();

            if ("1".equals(c)) {
                showList(sc, dao.findByUserId(userId));

            } else if ("2".equals(c)) {
                showList(sc, dao.findUnreadByUserId(userId));

            } else if ("3".equals(c)) {
                System.out.print("Enter Notification ID (N_ID): ");
                String s = sc.nextLine();
                if (!s.matches("\\d+")) {
                    System.out.println("[ERROR] Invalid ID.");
                } else {
                    dao.markAsRead(userId, Long.parseLong(s));
                    System.out.println("[INFO] Marked as read.");
                }
                ConsoleUtil.pause(sc);

            } else if ("4".equals(c)) {
                dao.markAllAsRead(userId);
                System.out.println("[INFO] All marked as read.");
                ConsoleUtil.pause(sc);

            } else if ("5".equals(c)) {
                managePreferences(sc, userId);

            } else {
                break;
            }
        }
    }

    // ✅ Step 5: Notification Preferences Menu
    public void managePreferences(Scanner sc, long userId) {
        prefDao.ensureExists(userId);

        while (true) {
            ConsoleUtil.printHeader("Notification Preferences");
            System.out.println("Toggle categories:");
            System.out.println("1. TRANSACTION");
            System.out.println("2. REQUEST");
            System.out.println("3. INVOICE");
            System.out.println("4. LOAN");
            System.out.println("5. ALERT (Low Balance)");
            System.out.println("6. Set Low Balance Threshold");
            System.out.println("7. Back");
            System.out.print("Choice: ");

            String c = sc.nextLine();

            if ("7".equals(c)) return;

            if ("6".equals(c)) {
                System.out.print("Enter threshold amount: ");
                String thStr = sc.nextLine().trim();
                if (!thStr.matches("\\d+")) {
                    System.out.println("[ERROR] Threshold must be a number.");
                    ConsoleUtil.pause(sc);
                    continue;
                }
                int th = Integer.parseInt(thStr);
                prefDao.updateThreshold(userId, th);
                System.out.println("[INFO] Threshold updated to: " + th);
                ConsoleUtil.pause(sc);
                continue;
            }

            String type = null;
            if ("1".equals(c)) type = "TRANSACTION";
            else if ("2".equals(c)) type = "REQUEST";
            else if ("3".equals(c)) type = "INVOICE";
            else if ("4".equals(c)) type = "LOAN";
            else if ("5".equals(c)) type = "ALERT";

            if (type == null) {
                System.out.println("[ERROR] Invalid choice.");
                ConsoleUtil.pause(sc);
                continue;
            }

            boolean current = prefDao.isEnabled(userId, type);
            prefDao.updatePref(userId, type, !current);

            System.out.println("[INFO] " + type + " notifications are now: " + (!current ? "ON" : "OFF"));
            ConsoleUtil.pause(sc);
        }
    }

    private void showList(Scanner sc, List<Notification> list) {
        ConsoleUtil.printHeader("Notification List");
        if (list == null || list.isEmpty()) {
            System.out.println("No notifications.");
            ConsoleUtil.pause(sc);
            return;
        }

        for (Notification n : list) {
            System.out.println("----------------------------------------");
            System.out.println("N_ID   : " + n.getNotificationId() + (n.isRead() ? " (READ)" : " (UNREAD)"));
            System.out.println("TYPE   : " + n.getType());
            System.out.println("TITLE  : " + safe(n.getTitle()));
            System.out.println("MSG    : " + safe(n.getMessage()));
            System.out.println("DATE   : " + safe(n.getCreatedAt()));
        }
        System.out.println("----------------------------------------");
        ConsoleUtil.pause(sc);
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
