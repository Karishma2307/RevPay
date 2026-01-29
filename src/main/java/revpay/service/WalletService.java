package revpay.service;

import java.util.Scanner;

import revpay.dao.WalletDao;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class WalletService {

    private WalletDao walletDao = new WalletDaoImpl();

    public Wallet getWallet(long userId) {
        return walletDao.findByUserId(userId);
    }

    public void addMoney(Scanner sc, Wallet wallet, NotificationService notificationService) {
        if (wallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("------------- Wallet: Add Money -------------");
        System.out.print("Enter amount to add: ");
        String amtStr = sc.nextLine();

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be positive.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = wallet.getBalance() + amount;
        walletDao.updateBalance(wallet.getWalletId(), newBalance);
        wallet.setBalance(newBalance);

        System.out.printf("[INFO] $%.2f added to wallet. New balance: $%.2f%n", amount, newBalance);

        if (notificationService != null) {
            notificationService.notifyUser(wallet.getUserId(), "TRANSACTION",
                    "Wallet Top-up", "You added $" + amount + " to your wallet.");
        }

        ConsoleUtil.pause(sc);
    }
}
