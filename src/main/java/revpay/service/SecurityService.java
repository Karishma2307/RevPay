package revpay.service;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.UserDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;
import revpay.util.ValidationUtil;

public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final UserDao userDao = new UserDaoImpl();

    public void changePassword(Scanner sc, User user) {
        ConsoleUtil.printHeader("Change Password");

        if (user == null) {
            logger.warn("changePassword called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Change password started (userId={}, username='{}')", userId, safe(user.getUsername()));

        System.out.print("Current Password: ");
        String currentPw = sc.nextLine();

        if (!HashUtil.check(currentPw, user.getPasswordHash())) {
            logger.warn("Change password failed: current password mismatch (userId={})", userId);
            System.out.println("Current password incorrect.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Transaction PIN: ");
        String pin = sc.nextLine();

        if (!HashUtil.check(pin, user.getTxnPinHash())) {
            logger.warn("Change password failed: txn pin mismatch (userId={})", userId);
            System.out.println("Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("New Password: ");
        String newPw = sc.nextLine();

        System.out.print("Confirm New Password: ");
        String confirm = sc.nextLine();

        if (!newPw.equals(confirm)) {
            logger.warn("Change password failed: new passwords do not match (userId={})", userId);
            System.out.println("Passwords do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!ValidationUtil.isValidPassword(newPw)) {
            logger.warn("Change password failed: weak password (userId={})", userId);
            System.out.println("Weak password.");
            ConsoleUtil.pause(sc);
            return;
        }

        String newHash = HashUtil.hash(newPw);

        boolean ok;
        try {
            ok = userDao.updatePassword(userId, newHash);
        } catch (Exception e) {
            logger.error("Change password failed: DAO exception updating password (userId={})", userId, e);
            System.out.println("Failed to update password.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (ok) {
            user.setPasswordHash(newHash); // update in-memory user
            logger.info("Password changed successfully (userId={})", userId);
            System.out.println("Password changed successfully.");
        } else {
            logger.error("Change password failed: DAO returned false (userId={})", userId);
            System.out.println("Failed to update password.");
        }

        ConsoleUtil.pause(sc);
    }

    public void changeTxnPin(Scanner sc, User user) {
        ConsoleUtil.printHeader("Change Transaction PIN");

        if (user == null) {
            logger.warn("changeTxnPin called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Change txn pin started (userId={}, username='{}')", userId, safe(user.getUsername()));

        System.out.print("Login Password: ");
        String pw = sc.nextLine();

        if (!HashUtil.check(pw, user.getPasswordHash())) {
            logger.warn("Change txn pin failed: login password mismatch (userId={})", userId);
            System.out.println("Invalid login password.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Current Transaction PIN: ");
        String oldPin = sc.nextLine();

        if (!HashUtil.check(oldPin, user.getTxnPinHash())) {
            logger.warn("Change txn pin failed: current pin mismatch (userId={})", userId);
            System.out.println("Invalid current PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("New 4-digit PIN: ");
        String newPin = sc.nextLine();

        if (!ValidationUtil.isValidTxnPin(newPin)) {
            logger.warn("Change txn pin failed: invalid pin format (userId={})", userId);
            System.out.println("PIN must be exactly 4 digits.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Confirm New PIN: ");
        String confirm = sc.nextLine();

        if (!newPin.equals(confirm)) {
            logger.warn("Change txn pin failed: pins do not match (userId={})", userId);
            System.out.println("PINs do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        String newPinHash = HashUtil.hash(newPin);

        boolean ok;
        try {
            ok = userDao.updateTxnPin(userId, newPinHash);
        } catch (Exception e) {
            logger.error("Change txn pin failed: DAO exception updating pin (userId={})", userId, e);
            System.out.println("Failed to update PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (ok) {
            user.setTxnPinHash(newPinHash);
            logger.info("Transaction PIN changed successfully (userId={})", userId);
            System.out.println("Transaction PIN changed successfully.");
        } else {
            logger.error("Change txn pin failed: DAO returned false (userId={})", userId);
            System.out.println("Failed to update PIN.");
        }

        ConsoleUtil.pause(sc);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
