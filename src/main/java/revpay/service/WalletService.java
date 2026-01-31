package revpay.service;

import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class WalletService {

    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao txDao = new TransactionDaoImpl();

   
    public void addMoneyFromCard(Scanner sc, User user) {
        ConsoleUtil.printHeader("Wallet - Add Money (from Card)");

        Wallet w = walletDao.getWalletByUserId(user.getUserId());
        if (w == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Current Balance: " + w.getBalance());
        System.out.print("Enter topup amount: ");
        double amount = readAmount(sc);

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be greater than 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = w.getBalance() + amount;

        try {
         
            walletDao.updateBalance(user.getUserId(), newBalance);

         
            boolean saved = txDao.createTransaction(
                    user.getUserId(),
                    user.getUserId(),
                    amount,
                    "DEPOSIT",
                    "SUCCESS",
                    "Wallet topup from card"
            );

            if (!saved) {
                System.out.println("[WARN] Wallet updated but transaction history not saved.");
            }

            System.out.println("[INFO] Topup successful. New Balance: " + newBalance);

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to topup wallet.");
            System.out.println("[DEV] " + e.getMessage());
        }

        ConsoleUtil.pause(sc);
    }

    
    public void withdrawToBank(Scanner sc, User user) {
        ConsoleUtil.printHeader("Wallet - Withdraw (to Bank)");

        Wallet w = walletDao.getWalletByUserId(user.getUserId());
        if (w == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Current Balance: " + w.getBalance());
        System.out.print("Enter withdraw amount: ");
        double amount = readAmount(sc);

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be greater than 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount > w.getBalance()) {
            System.out.println("[ERROR] Insufficient balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = w.getBalance() - amount;

        try {
           
            walletDao.updateBalance(user.getUserId(), newBalance);

          
            boolean saved = txDao.createTransaction(
                    user.getUserId(),
                    user.getUserId(),
                    amount,
                    "WITHDRAW",
                    "SUCCESS",
                    "Wallet withdrawal to bank (simulated)"
            );

            if (!saved) {
                System.out.println("[WARN] Wallet updated but transaction history not saved.");
            }

            System.out.println("[INFO] Withdrawal successful (simulated). New Balance: " + newBalance);

        } catch (Exception e) {
            System.out.println("[ERROR] Withdrawal failed.");
            System.out.println("[DEV] " + e.getMessage());
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
}
