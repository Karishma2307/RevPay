package revpay.service;

import java.util.Scanner;

import revpay.dao.UserDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;
import revpay.util.ValidationUtil;

public class SecurityService {

    private final UserDao userDao = new UserDaoImpl();

    
    public void changePassword(Scanner sc, User user) {
        ConsoleUtil.printHeader("Change Password");

        System.out.print("Current Password: ");
        String currentPw = sc.nextLine();

        if (!HashUtil.check(currentPw, user.getPasswordHash())) {
            System.out.println("[ERROR] Current password incorrect.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Transaction PIN: ");
        String pin = sc.nextLine();

        if (!HashUtil.check(pin, user.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("New Password: ");
        String newPw = sc.nextLine();

        System.out.print("Confirm New Password: ");
        String confirm = sc.nextLine();

        if (!newPw.equals(confirm)) {
            System.out.println("[ERROR] Passwords do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!ValidationUtil.isValidPassword(newPw)) {
            System.out.println("[ERROR] Weak password.");
            ConsoleUtil.pause(sc);
            return;
        }

        String newHash = HashUtil.hash(newPw);
        boolean ok = userDao.updatePassword(user.getUserId(), newHash);

        if (ok) {
            user.setPasswordHash(newHash); // update in-memory user
            System.out.println("[INFO] Password changed successfully.");
        } else {
            System.out.println("[ERROR] Failed to update password.");
        }

        ConsoleUtil.pause(sc);
    }

    public void changeTxnPin(Scanner sc, User user) {
        ConsoleUtil.printHeader("Change Transaction PIN");

        System.out.print("Login Password: ");
        String pw = sc.nextLine();

        if (!HashUtil.check(pw, user.getPasswordHash())) {
            System.out.println("[ERROR] Invalid login password.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Current Transaction PIN: ");
        String oldPin = sc.nextLine();

        if (!HashUtil.check(oldPin, user.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid current PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("New 4-digit PIN: ");
        String newPin = sc.nextLine();

        if (!ValidationUtil.isValidTxnPin(newPin)) {
            System.out.println("[ERROR] PIN must be exactly 4 digits.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Confirm New PIN: ");
        String confirm = sc.nextLine();

        if (!newPin.equals(confirm)) {
            System.out.println("[ERROR] PINs do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        String newPinHash = HashUtil.hash(newPin);
        boolean ok = userDao.updateTxnPin(user.getUserId(), newPinHash);

        if (ok) {
            user.setTxnPinHash(newPinHash);
            System.out.println("[INFO] Transaction PIN changed successfully.");
        } else {
            System.out.println("[ERROR] Failed to update PIN.");
        }

        ConsoleUtil.pause(sc);
    }
}
