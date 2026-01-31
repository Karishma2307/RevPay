package revpay.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

import revpay.dao.UserDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.DbUtil;
import revpay.util.HashUtil;

public class TransferService {

    private final UserDao userDao = new UserDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void sendMoney(Scanner sc, User sender) {

        ConsoleUtil.printHeader("Send Money");

        System.out.println("Send to:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User receiver = null;
        if ("1".equals(choice)) {
            System.out.print("Receiver Email: ");
            receiver = userDao.findByEmail(sc.nextLine());
        } else if ("2".equals(choice)) {
            System.out.print("Receiver Phone: ");
            receiver = userDao.findByPhone(sc.nextLine());
        } else if ("3".equals(choice)) {
            System.out.print("Receiver Account ID: ");
            receiver = userDao.findByAccountId(sc.nextLine());
        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (receiver == null) {
            System.out.println("[ERROR] User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (receiver.getUserId() == sender.getUserId()) {
            System.out.println("[ERROR] Cannot send to yourself.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Note (optional): ");
        String note = sc.nextLine();

      
        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, sender.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        
        try (Connection con = DbUtil.getConnection()) {

            con.setAutoCommit(false);

            boolean ok;
            try {
                ok = TransferTxHelper.performTransfer(
                        con,
                        sender.getUserId(),
                        receiver.getUserId(),
                        amount,
                        note
                );
            } catch (Exception ex) {
                
                try { con.rollback(); } catch (Exception ignore) {}
                System.out.println("[ERROR] Transfer failed while saving wallet/transaction.");
                System.out.println("[DEV] " + ex.getMessage());
                ConsoleUtil.pause(sc);
                return;
            }

            if (!ok) {
                con.rollback();
                System.out.println("[ERROR] Transfer failed (insufficient balance or wallet missing).");
                ConsoleUtil.pause(sc);
                return;
            }

           
            try {
                lowBalanceAlertService.checkAndNotify(sender.getUserId());
            } catch (Exception ignored) {
            }

            
            try {
                notificationService.notifyUser(
                        sender.getUserId(),
                        "TRANSFER",
                        "Money Sent",
                        "You sent ₹" + amount + " to " + receiver.getFullName()
                );

                notificationService.notifyUser(
                        receiver.getUserId(),
                        "TRANSFER",
                        "Money Received",
                        "You received ₹" + amount + " from " + sender.getFullName()
                );
            } catch (Exception e) {
                System.out.println("[WARN] Transfer saved but notification failed.");
                System.out.println("[DEV] " + e.getMessage());
            }

            con.commit();
            System.out.println("[INFO] Transfer successful.");
            ConsoleUtil.pause(sc);

        } catch (SQLException e) {
            System.out.println("[ERROR] Transfer failed due to DB login/connection issue.");
            System.out.println("[DEV] " + e.getMessage());
            ConsoleUtil.pause(sc);
        }
    }
}
