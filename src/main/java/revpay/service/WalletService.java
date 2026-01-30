package revpay.service;

import java.util.Scanner;

import revpay.dao.PaymentMethodDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.PaymentMethodDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.PaymentMethod;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class WalletService {

    private final WalletDao walletDao = new WalletDaoImpl();
    private final PaymentMethodDao paymentMethodDao = new PaymentMethodDaoImpl();

    // ✅ Step 5: low balance alerts
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    // Wallet: Add Money (from Card)
    public void addMoneyFromCard(Scanner sc, User user) {
        ConsoleUtil.printHeader("Wallet - Add Money (from Card)");

        PaymentMethod def = paymentMethodDao.findDefaultByUserId(user.getUserId());
        if (def == null) {
            System.out.println("[ERROR] No default payment method found. Add a card first and set it as default.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Default Payment Method: " + def.getProvider() + " ****" + def.getLast4()
                + " (Label: " + def.getLabel() + ")");

        System.out.print("Enter amount to add: ");
        String amtStr = sc.nextLine().trim();

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
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

        Wallet w = walletDao.getWalletByUserId(user.getUserId());
        if (w == null) {
            walletDao.createWalletForUser(user.getUserId());
            w = walletDao.getWalletByUserId(user.getUserId());
        }

        double newBalance = w.getBalance() + amount;
        walletDao.updateBalance(user.getUserId(), newBalance);

        System.out.println("[INFO] Wallet credited successfully. New Balance: " + newBalance);
        ConsoleUtil.pause(sc);
    }

    // Wallet: Withdraw (to Bank) - simulated
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
        String amtStr = sc.nextLine().trim();

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
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

        if (w.getBalance() < amount) {
            System.out.println("[ERROR] Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = w.getBalance() - amount;
        walletDao.updateBalance(user.getUserId(), newBalance);

        // ✅ Step 5: trigger low balance alert after balance decreases
        lowBalanceAlertService.checkAndNotify(user.getUserId());

        System.out.println("[INFO] Withdrawal successful (simulated). New Balance: " + newBalance);
        ConsoleUtil.pause(sc);
    }
}
