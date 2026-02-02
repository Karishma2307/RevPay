package revpay.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.UserDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.DbUtil;
import revpay.util.HashUtil;

public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final UserDao userDao = new UserDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void sendMoney(Scanner sc, User sender) {

        ConsoleUtil.printHeader("Send Money");

        if (sender == null) {
            logger.warn("sendMoney called with null sender");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long senderId = sender.getUserId();
        logger.info("Transfer flow started (senderId={}, username='{}')", senderId, safe(sender.getUsername()));

        System.out.println("Send to:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User receiver = null;

        try {
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
                logger.warn("Transfer failed: invalid receiver lookup choice='{}' (senderId={})", choice, senderId);
                System.out.println("Invalid choice.");
                ConsoleUtil.pause(sc);
                return;
            }
        } catch (Exception e) {
            logger.error("Transfer failed: DAO error while finding receiver (senderId={}, choice='{}')", senderId, choice, e);
            System.out.println("Unable to find receiver right now.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (receiver == null) {
            logger.warn("Transfer failed: receiver not found (senderId={}, choice='{}')", senderId, choice);
            System.out.println("User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (receiver.getUserId() == senderId) {
            logger.warn("Transfer blocked: sender tried to send to self (senderId={})", senderId);
            System.out.println("Cannot send to yourself.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Transfer failed: invalid amount input (senderId={})", senderId);
            System.out.println("Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount <= 0) {
            logger.warn("Transfer failed: amount <= 0 (senderId={}, amount={})", senderId, fmt(amount));
            System.out.println("Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Note (optional): ");
        String note = sc.nextLine();

        // PIN validation (do NOT log PIN)
        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, sender.getTxnPinHash())) {
            logger.warn("Transfer failed: invalid txn pin (senderId={}, receiverId={})", senderId, receiver.getUserId());
            System.out.println("Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        try (Connection con = DbUtil.getConnection()) {

            con.setAutoCommit(false);

            boolean ok;
            try {
                ok = TransferTxHelper.performTransfer(
                        con,
                        senderId,
                        receiver.getUserId(),
                        amount,
                        note
                );
            } catch (Exception ex) {
                try { con.rollback(); } catch (Exception rb) {
                    logger.error("Transfer rollback failed (senderId={}, receiverId={})", senderId, receiver.getUserId(), rb);
                }

                logger.error("Transfer failed while saving wallet/transaction (senderId={}, receiverId={}, amount={})",
                        senderId, receiver.getUserId(), fmt(amount), ex);

                System.out.println("Transfer failed while saving wallet/transaction.");
                ConsoleUtil.pause(sc);
                return;
            }

            if (!ok) {
                try { con.rollback(); } catch (Exception rb) {
                    logger.error("Transfer rollback failed (senderId={}, receiverId={})", senderId, receiver.getUserId(), rb);
                }

                logger.warn("Transfer failed: helper returned false (senderId={}, receiverId={}, amount={})",
                        senderId, receiver.getUserId(), fmt(amount));

                System.out.println("Transfer failed (insufficient balance or wallet missing).");
                ConsoleUtil.pause(sc);
                return;
            }

            
            try {
                lowBalanceAlertService.checkAndNotify(senderId);
            } catch (Exception e) {
                logger.error("Low balance check failed after transfer (senderId={})", senderId, e);
            }

            try {
                notificationService.notifyUser(
                        senderId,
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
                logger.error("Transfer saved but notification failed (senderId={}, receiverId={}, amount={})",
                        senderId, receiver.getUserId(), fmt(amount), e);
                System.out.println("Transfer saved but notification failed.");
            }

            con.commit();

            logger.info("Transfer successful (senderId={}, receiverId={}, amount={})",
                    senderId, receiver.getUserId(), fmt(amount));

            System.out.println("Transfer successful.");
            ConsoleUtil.pause(sc);

        } catch (SQLException e) {
            logger.error("Transfer failed due to DB connection/SQL error (senderId={}, receiverId={}, amount={})",
                    senderId, receiver.getUserId(), fmt(amount), e);

            System.out.println("Transfer failed due to DB login/connection issue.");
            ConsoleUtil.pause(sc);
        }
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
