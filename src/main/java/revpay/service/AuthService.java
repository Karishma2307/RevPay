package revpay.service;

import java.util.Random;
import java.util.Scanner;

import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;
import revpay.util.ValidationUtil;

public class AuthService {

    private UserDao userDao = new UserDaoImpl();
    private WalletDao walletDao = new WalletDaoImpl();

    private PasswordRecoveryService recoveryService = new PasswordRecoveryService();
    private BusinessProfileService businessProfileService = new BusinessProfileService();

    private Scanner sc;

    public AuthService(Scanner sc) {
        this.sc = sc;
    }

    // =======================
    // REGISTRATION
    // =======================
    public User register() {
        ConsoleUtil.printHeader("Registration");

        System.out.println("Select account type:");
        System.out.println("1. Personal Account");
        System.out.println("2. Business Account");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        String accountType;
        if ("1".equals(choice)) accountType = "PERSONAL";
        else if ("2".equals(choice)) accountType = "BUSINESS";
        else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return null;
        }

        User user = new User();
        user.setAccountType(accountType);

        // Full name
        System.out.print("Full Name : ");
        String fullName = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(fullName)) {
            System.out.println("[ERROR] Full name cannot be empty.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setFullName(fullName.trim());

        // Username (unique)
        System.out.print("Username  : ");
        String username = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(username)) {
            System.out.println("[ERROR] Username cannot be empty.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByUsername(username.trim()) != null) {
            System.out.println("[ERROR] Username already taken.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setUsername(username.trim());

        // Email (unique)
        System.out.print("Email     : ");
        String email = sc.nextLine();
        if (!ValidationUtil.isValidEmail(email)) {
            System.out.println("[ERROR] Invalid email format.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByEmail(email.trim()) != null) {
            System.out.println("[ERROR] Email already registered.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setEmail(email.trim());

        // Phone (unique)
        System.out.print("Phone     : ");
        String phone = sc.nextLine();
        if (!ValidationUtil.isValidPhone(phone)) {
            System.out.println("[ERROR] Phone must be exactly 10 digits.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByPhone(phone.trim()) != null) {
            System.out.println("[ERROR] Phone already registered.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setPhone(phone.trim());

        // Password
        System.out.print("Password  : ");
        String pw1 = sc.nextLine();
        System.out.print("Confirm Password: ");
        String pw2 = sc.nextLine();

        if (!pw1.equals(pw2)) {
            System.out.println("[ERROR] Passwords do not match.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (!ValidationUtil.isValidPassword(pw1)) {
            System.out.println("[ERROR] Weak password. Must contain uppercase, lowercase, digit, special char and min 8 chars.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setPasswordHash(HashUtil.hash(pw1));

        // Transaction PIN
        System.out.print("Create 4-digit Transaction PIN: ");
        String pin = sc.nextLine();
        if (!ValidationUtil.isValidTxnPin(pin)) {
            System.out.println("[ERROR] PIN must be exactly 4 digits (0-9).");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setTxnPinHash(HashUtil.hash(pin));

        // Generate ACCOUNT_ID
        String prefix = "PERSONAL".equals(accountType) ? "P-" : "B-";
        String accountId = null;
        int attempts = 0;
        do {
            long temp = System.currentTimeMillis() % 1000000;
            accountId = prefix + (100000 + temp);
            attempts++;
        } while (userDao.findByAccountId(accountId) != null && attempts < 10);

        user.setAccountId(accountId);

        long newId = userDao.createUser(user);
        if (newId <= 0) {
            System.out.println("[ERROR] Failed to create user.");
            ConsoleUtil.pause(sc);
            return null;
        }

        // Create wallet row
        walletDao.createWalletForUser(newId);

        // If BUSINESS -> insert business profile details
        if ("BUSINESS".equals(accountType)) {
            boolean ok = businessProfileService.collectAndSave(sc, newId);
            if (!ok) {
                System.out.println("[WARN] Business profile was not saved properly. (But account is created)");
            }
        }

        // Setup Security Questions
        recoveryService.setupSecurityQuestions(sc, newId);

        System.out.println("[INFO] " + accountType + " account created successfully.");
        System.out.println("[INFO] Your Account ID: " + user.getAccountId());
        ConsoleUtil.pause(sc);
        return user;
    }

    // =======================
    // LOGIN
    // =======================
    public User login() {
        ConsoleUtil.printHeader("Login");

        System.out.println("Login using:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User user = null;

        if ("1".equals(choice)) {
            System.out.print("Email   : ");
            String email = sc.nextLine();
            if (!ValidationUtil.isValidEmail(email)) {
                System.out.println("[ERROR] Invalid email format.");
                ConsoleUtil.pause(sc);
                return null;
            }
            user = userDao.findByEmail(email.trim());

        } else if ("2".equals(choice)) {
            System.out.print("Phone   : ");
            String phone = sc.nextLine();
            if (!ValidationUtil.isValidPhone(phone)) {
                System.out.println("[ERROR] Phone must be exactly 10 digits.");
                ConsoleUtil.pause(sc);
                return null;
            }
            user = userDao.findByPhone(phone.trim());

        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return null;
        }

        if (user == null) {
            System.out.println("[ERROR] User not found.");
            ConsoleUtil.pause(sc);
            return null;
        }

        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            System.out.println("[ERROR] Account locked due to failed attempts.");
            ConsoleUtil.pause(sc);
            return null;
        }

        System.out.print("Password: ");
        String pw = sc.nextLine();

        if (!HashUtil.check(pw, user.getPasswordHash())) {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            userDao.updateFailedAttempts(user.getUserId(), newAttempts);

            if (newAttempts >= 3) {
                userDao.updateStatus(user.getUserId(), "LOCKED");
                System.out.println("[ERROR] Too many failed attempts. Account locked.");
            } else {
                System.out.println("[ERROR] Incorrect password. Attempts: " + newAttempts);
            }
            ConsoleUtil.pause(sc);
            return null;
        }

        // Reset attempts
        userDao.updateFailedAttempts(user.getUserId(), 0);

        // Simulated 2FA
        String code = String.format("%06d", new Random().nextInt(1000000));
        System.out.println("[2FA] Security code (simulated): " + code);
        System.out.print("Enter security code: ");
        String entered = sc.nextLine();
        if (!code.equals(entered)) {
            System.out.println("[ERROR] Wrong security code.");
            ConsoleUtil.pause(sc);
            return null;
        }

        System.out.println("[INFO] Login successful.");
        ConsoleUtil.pause(sc);
        return user;
    }

    // =======================
    // FORGOT PASSWORD
    // =======================
    public void forgotPassword() {
        recoveryService.forgotPassword(sc);
    }
}