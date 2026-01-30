package revpay.service;

import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.UserDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;

public class TransferService {

    private final UserDao userDao = new UserDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();

    private final NotificationService notificationService = new NotificationService();

    // ✅ Step 5: low balance alerts
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void sendMoney(Scanner sc, User sender) {

        Wallet senderWallet = walletDao.getWalletByUserId(sender.getUserId());
        if (senderWallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        ConsoleUtil.printHeader("Send Money");

        System.out.println("Send to by:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User recipient = null;
        if ("1".equals(choice)) {
            System.out.print("Recipient Email: ");
            recipient = userDao.findByEmail(sc.nextLine());
        } else if ("2".equals(choice)) {
            System.out.print("Recipient Phone: ");
            recipient = userDao.findByPhone(sc.nextLine());
        } else if ("3".equals(choice)) {
            System.out.print("Recipient Account ID: ");
            recipient = userDao.findByAccountId(sc.nextLine());
        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (recipient == null) {
            System.out.println("[ERROR] Recipient not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Amount: ");
        double amount;
        try { amount = Double.parseDouble(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid amount."); ConsoleUtil.pause(sc); return; }

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (senderWallet.getBalance() < amount) {
            System.out.println("[ERROR] Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        // ✅ Txn PIN check
        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, sender.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet receiverWallet = walletDao.getWalletByUserId(recipient.getUserId());
        if (receiverWallet == null) {
            walletDao.createWalletForUser(recipient.getUserId());
            receiverWallet = walletDao.getWalletByUserId(recipient.getUserId());
        }

        System.out.print("Note (optional): ");
        String note = sc.nextLine();

        double newSenderBal = senderWallet.getBalance() - amount;
        double newReceiverBal = receiverWallet.getBalance() + amount;

        walletDao.updateBalance(sender.getUserId(), newSenderBal);
        walletDao.updateBalance(recipient.getUserId(), newReceiverBal);

        // ✅ Step 5: trigger low balance alert after sender balance decreases
        lowBalanceAlertService.checkAndNotify(sender.getUserId());

        transactionDao.createTransaction(
                sender.getUserId(),
                recipient.getUserId(),
                amount,
                "TRANSFER",
                "SUCCESS",
                note,
                "TX:" + System.currentTimeMillis()
        );

        // ✅ Type should be "TRANSACTION" to match prefs (not "TRANSFER")
        notificationService.notifyUser(
                recipient.getUserId(),
                "TRANSACTION",
                "Money Received",
                "You received $" + amount + " from " + sender.getFullName()
        );

        System.out.println("[INFO] Transfer successful.");
        ConsoleUtil.pause(sc);
    }
}
