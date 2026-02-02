package revpay.service;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao txDao = new TransactionDaoImpl();

    public void addMoneyFromCard(Scanner sc, User user) {
        ConsoleUtil.printHeader("Wallet - Add Money (from Card)");

        if (user == null) {
            logger.warn("addMoneyFromCard called with null user");
            System.out.println("[ERROR] Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Wallet topup started (userId={}, username='{}')", userId, safe(user.getUsername()));

        Wallet w;
        try {
            w = walletDao.getWalletByUserId(userId);
        } catch (Exception e) {
            logger.error("Failed to load wallet (userId={})", userId, e);
            System.out.println("Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (w == null) {
            logger.warn("Wallet not found (userId={})", userId);
            System.out.println("Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Current Balance: " + w.getBalance());
        System.out.print("Enter topup amount: ");
        double amount = readAmount(sc);

        if (amount <= 0) {
            logger.warn("Topup failed: invalid amount (userId={}, amount={})", userId, fmt(amount));
            System.out.println("Amount must be greater than 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = w.getBalance() + amount;

        try {
            walletDao.updateBalance(userId, newBalance);
            logger.info("Wallet balance updated for topup (userId={}, amount={}, newBalance={})",
                    userId, fmt(amount), fmt(newBalance));

            boolean saved = txDao.createTransaction(
                    userId,
                    userId,
                    amount,
                    "DEPOSIT",
                    "SUCCESS",
                    "Wallet topup from card"
            );

            if (!saved) {
                logger.warn("Topup saved wallet but transaction history not saved (userId={}, amount={})",
                        userId, fmt(amount));
                System.out.println("Wallet updated but transaction history not saved.");
            } else {
                logger.debug("Topup transaction saved (userId={}, amount={})", userId, fmt(amount));
            }

            System.out.println("Topup successful. New Balance: " + newBalance);

        } catch (Exception e) {
            logger.error("Topup failed due to exception (userId={}, amount={})", userId, fmt(amount), e);
            System.out.println("Failed to topup wallet.");
        }

        ConsoleUtil.pause(sc);
    }

    public void withdrawToBank(Scanner sc, User user) {
        ConsoleUtil.printHeader("Wallet - Withdraw (to Bank)");

        if (user == null) {
            logger.warn("withdrawToBank called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Wallet withdraw started (userId={}, username='{}')", userId, safe(user.getUsername()));

        Wallet w;
        try {
            w = walletDao.getWalletByUserId(userId);
        } catch (Exception e) {
            logger.error("Failed to load wallet (userId={})", userId, e);
            System.out.println("Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (w == null) {
            logger.warn("Wallet not found (userId={})", userId);
            System.out.println(" Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Current Balance: " + w.getBalance());
        System.out.print("Enter withdraw amount: ");
        double amount = readAmount(sc);

        if (amount <= 0) {
            logger.warn("Withdraw failed: invalid amount (userId={}, amount={})", userId, fmt(amount));
            System.out.println("Amount must be greater than 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount > w.getBalance()) {
            logger.warn("Withdraw failed: insufficient balance (userId={}, balance={}, amount={})",
                    userId, fmt(w.getBalance()), fmt(amount));
            System.out.println("Insufficient balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = w.getBalance() - amount;

        try {
            walletDao.updateBalance(userId, newBalance);
            logger.info("Wallet balance updated for withdraw (userId={}, amount={}, newBalance={})",
                    userId, fmt(amount), fmt(newBalance));

            boolean saved = txDao.createTransaction(
                    userId,
                    userId,
                    amount,
                    "WITHDRAW",
                    "SUCCESS",
                    "Wallet withdrawal to bank (simulated)"
            );

            if (!saved) {
                logger.warn("Withdraw saved wallet but transaction history not saved (userId={}, amount={})",
                        userId, fmt(amount));
                System.out.println("Wallet updated but transaction history not saved.");
            } else {
                logger.debug("Withdraw transaction saved (userId={}, amount={})", userId, fmt(amount));
            }

            System.out.println("Withdrawal successful (simulated). New Balance: " + newBalance);

        } catch (Exception e) {
            logger.error("Withdrawal failed due to exception (userId={}, amount={})", userId, fmt(amount), e);
            System.out.println("Withdrawal failed.");
        }

        ConsoleUtil.pause(sc);
    }

    private double readAmount(Scanner sc) {
        try {
            String s = sc.nextLine();
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
