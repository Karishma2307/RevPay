package revpay.service;

import java.util.Random;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.UserDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;
import revpay.util.ValidationUtil;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private UserDao userDao = new UserDaoImpl();
    private WalletDao walletDao = new WalletDaoImpl();

    private PasswordRecoveryService recoveryService = new PasswordRecoveryService();
    private BusinessProfileService businessProfileService = new BusinessProfileService();

    private Scanner sc;

    public AuthService(Scanner sc) {
        this.sc = sc;
    }

    
    public User register() {
        ConsoleUtil.printHeader("Registration");
        logger.info("Registration flow started");

        System.out.println("Select account type:");
        System.out.println("1. Personal Account");
        System.out.println("2. Business Account");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        String accountType;
        if ("1".equals(choice)) accountType = "PERSONAL";
        else if ("2".equals(choice)) accountType = "BUSINESS";
        else {
            logger.warn("Registration failed: invalid account type choice='{}'", choice);
            System.out.println("Invalid choice.");
            ConsoleUtil.pause(sc);
            return null;
        }

        User user = new User();
        user.setAccountType(accountType);

       
        System.out.print("Full Name : ");
        String fullName = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(fullName)) {
            logger.warn("Registration failed: empty full name (accountType={})", accountType);
            System.out.println("Full name cannot be empty.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setFullName(fullName.trim());

       
        System.out.print("Username  : ");
        String username = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(username)) {
            logger.warn("Registration failed: empty username (accountType={}, fullName='{}')",
                    accountType, user.getFullName());
            System.out.println("Username cannot be empty.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByUsername(username.trim()) != null) {
            logger.warn("Registration failed: username already taken username='{}'", username.trim());
            System.out.println("Username already taken.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setUsername(username.trim());

       
        System.out.print("Email     : ");
        String email = sc.nextLine();
        if (!ValidationUtil.isValidEmail(email)) {
            logger.warn("Registration failed: invalid email format email='{}'", email);
            System.out.println("Invalid email format.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByEmail(email.trim()) != null) {
            logger.warn("Registration failed: email already registered email='{}'", email.trim());
            System.out.println("Email already registered.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setEmail(email.trim());

       
        System.out.print("Phone     : ");
        String phone = sc.nextLine();
        if (!ValidationUtil.isValidPhone(phone)) {
            logger.warn("Registration failed: invalid phone format phone='{}'", phone);
            System.out.println("Phone must be exactly 10 digits.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (userDao.findByPhone(phone.trim()) != null) {
            logger.warn("Registration failed: phone already registered phone='{}'", phone.trim());
            System.out.println("Phone already registered.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setPhone(phone.trim());

        
        System.out.print("Password  : ");
        String pw1 = sc.nextLine();
        System.out.print("Confirm Password: ");
        String pw2 = sc.nextLine();

        if (!pw1.equals(pw2)) {
            logger.warn("Registration failed: passwords do not match (username='{}', email='{}')",
                    user.getUsername(), user.getEmail());
            System.out.println("Passwords do not match.");
            ConsoleUtil.pause(sc);
            return null;
        }
        if (!ValidationUtil.isValidPassword(pw1)) {
            logger.warn("Registration failed: weak password (username='{}', email='{}')",
                    user.getUsername(), user.getEmail());
            System.out.println("Weak password. Must contain uppercase, lowercase, digit, special char and min 8 chars.");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setPasswordHash(HashUtil.hash(pw1));

        
        System.out.print("Create 4-digit Transaction PIN: ");
        String pin = sc.nextLine();
        if (!ValidationUtil.isValidTxnPin(pin)) {
            logger.warn("Registration failed: invalid txn pin format (username='{}', email='{}')",
                    user.getUsername(), user.getEmail());
            System.out.println("PIN must be exactly 4 digits (0-9).");
            ConsoleUtil.pause(sc);
            return null;
        }
        user.setTxnPinHash(HashUtil.hash(pin));

        
        String prefix = "PERSONAL".equals(accountType) ? "P-" : "B-";
        String accountId = null;
        int attempts = 0;
        do {
            long temp = System.currentTimeMillis() % 1000000;
            accountId = prefix + (100000 + temp);
            attempts++;
        } while (userDao.findByAccountId(accountId) != null && attempts < 10);

        user.setAccountId(accountId);

        long newId;
        try {
            newId = userDao.createUser(user);
        } catch (Exception e) {
            logger.error("Registration failed: exception while creating user (username='{}', email='{}')",
                    user.getUsername(), user.getEmail(), e);
            System.out.println("Failed to create user.");
            ConsoleUtil.pause(sc);
            return null;
        }

        if (newId <= 0) {
            logger.error("Registration failed: createUser returned non-positive id (username='{}', email='{}', returnedId={})",
                    user.getUsername(), user.getEmail(), newId);
            System.out.println("Failed to create user.");
            ConsoleUtil.pause(sc);
            return null;
        }

        // Create wallet row
        try {
            walletDao.createWalletForUser(newId);
            logger.info("Wallet created for new userId={}", newId);
        } catch (Exception e) {
            logger.error("Registration warning: user created but wallet creation failed userId={}", newId, e);
           
        }

       
        if ("BUSINESS".equals(accountType)) {
            boolean ok = businessProfileService.collectAndSave(sc, newId);
            if (!ok) {
                logger.warn("Business profile not saved properly (userId={})", newId);
                System.out.println("Business profile was not saved properly. (But account is created)");
            } else {
                logger.info("Business profile saved (userId={})", newId);
            }
        }

        
        try {
            recoveryService.setupSecurityQuestions(sc, newId);
            logger.info("Security questions setup completed (userId={})", newId);
        } catch (Exception e) {
            logger.error("Security questions setup failed (userId={})", newId, e);
            
        }

        logger.info("{} account created successfully (userId={}, username='{}', accountId='{}')",
                accountType, newId, user.getUsername(), user.getAccountId());

        System.out.println("  " + accountType + " account created successfully.");
        System.out.println("Your Account ID: " + user.getAccountId());
        ConsoleUtil.pause(sc);
        return user;
    }

    
    public User login() {
        ConsoleUtil.printHeader("Login");
        logger.info("Login flow started");

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
                logger.warn("Login failed: invalid email format email='{}'", email);
                System.out.println("Invalid email format.");
                ConsoleUtil.pause(sc);
                return null;
            }
            user = userDao.findByEmail(email.trim());

        } else if ("2".equals(choice)) {
            System.out.print("Phone   : ");
            String phone = sc.nextLine();
            if (!ValidationUtil.isValidPhone(phone)) {
                logger.warn("Login failed: invalid phone format phone='{}'", phone);
                System.out.println("Phone must be exactly 10 digits.");
                ConsoleUtil.pause(sc);
                return null;
            }
            user = userDao.findByPhone(phone.trim());

        } else {
            logger.warn("Login failed: invalid login choice='{}'", choice);
            System.out.println("Invalid choice.");
            ConsoleUtil.pause(sc);
            return null;
        }

        if (user == null) {
            logger.warn("Login failed: user not found (loginChoice='{}')", choice);
            System.out.println("User not found.");
            ConsoleUtil.pause(sc);
            return null;
        }

        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            logger.warn("Login blocked: account locked (userId={}, username='{}')",
                    user.getUserId(), user.getUsername());
            System.out.println("Account locked due to failed attempts.");
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
                logger.warn("Account locked due to failed login attempts (userId={}, username='{}', attempts={})",
                        user.getUserId(), user.getUsername(), newAttempts);
                System.out.println("Too many failed attempts. Account locked.");
            } else {
                logger.warn("Incorrect password (userId={}, username='{}', attempts={})",
                        user.getUserId(), user.getUsername(), newAttempts);
                System.out.println("Incorrect password. Attempts: " + newAttempts);
            }
            ConsoleUtil.pause(sc);
            return null;
        }

      
        userDao.updateFailedAttempts(user.getUserId(), 0);

      
        String code = String.format("%06d", new Random().nextInt(1000000));
        System.out.println("Security code (simulated): " + code);
        System.out.print("Enter security code: ");
        String entered = sc.nextLine();
        if (!code.equals(entered)) {
            logger.warn("Login failed: wrong 2FA code (userId={}, username='{}')",
                    user.getUserId(), user.getUsername());
            System.out.println("Wrong security code.");
            ConsoleUtil.pause(sc);
            return null;
        }

        logger.info("Login successful (userId={}, username='{}')", user.getUserId(), user.getUsername());
        System.out.println("Login successful.");
        ConsoleUtil.pause(sc);
        return user;
    }

    
    public void forgotPassword() {
        logger.info("Forgot password flow started");
        recoveryService.forgotPassword(sc);
    }
}