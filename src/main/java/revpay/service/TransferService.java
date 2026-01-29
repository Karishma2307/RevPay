package revpay.service;

import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.UserDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;

public class TransferService {

    private UserDao userDao = new UserDaoImpl();
    private WalletDao walletDao = new WalletDaoImpl();
    private TransactionDao transactionDao = new TransactionDaoImpl();

    private NotificationService notificationService;

    public TransferService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void sendMoney(Scanner sc, User sender) {
        Wallet senderWallet = walletDao.findByUserId(sender.getUserId());
        if (senderWallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("------------- Send Money -------------");
        System.out.println("Send to by:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User recipient = null;
        if ("1".equals(choice)) {
            System.out.print("Recipient Email: ");
            String email = sc.nextLine();
            recipient = userDao.findByEmail(email);
        } else if ("2".equals(choice)) {
            System.out.print("Recipient Phone: ");
            String phone = sc.nextLine();
            recipient = userDao.findByPhone(phone);
        } else if ("3".equals(choice)) {
            System.out.print("Recipient Account ID: ");
            String accId = sc.nextLine();
            recipient = userDao.findByAccountId(accId);
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

        if (recipient.getUserId() == sender.getUserId()) {
            System.out.println("[ERROR] You cannot send money to yourself.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter amount (USD): ");
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

        if (senderWallet.getBalance() < amount) {
            System.out.println("[ERROR] Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Optional note: ");
        String note = sc.nextLine();

        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, sender.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        // receiver wallet
        Wallet recipientWallet = walletDao.findByUserId(recipient.getUserId());
        if (recipientWallet == null) {
            walletDao.createWalletForUser(recipient.getUserId());
            recipientWallet = walletDao.findByUserId(recipient.getUserId());
        }

        // update balances
        double newSenderBal = senderWallet.getBalance() - amount;
        double newRecipientBal = recipientWallet.getBalance() + amount;

        walletDao.updateBalance(senderWallet.getWalletId(), newSenderBal);
        walletDao.updateBalance(recipientWallet.getWalletId(), newRecipientBal);

        senderWallet.setBalance(newSenderBal);
        recipientWallet.setBalance(newRecipientBal);

        // create transaction record
        Transaction t = new Transaction();
        t.setFromUserId(sender.getUserId());
        t.setToUserId(recipient.getUserId());
        t.setAmount(amount);
        t.setCurrency("USD");
        t.setType("TRANSFER");
        t.setStatus("SUCCESS");
        t.setNote(note);
        long txnId = transactionDao.createTransaction(t);

        System.out.println("[INFO] Transaction Successful!");
        System.out.println("Transaction ID: " + txnId);
        System.out.println("Amount        : $" + amount);
        System.out.println("From          : " + sender.getAccountId());
        System.out.println("To            : " + recipient.getAccountId());
        System.out.println("Status        : COMPLETED");

        if (notificationService != null) {
            notificationService.notifyUser(sender.getUserId(), "TRANSACTION",
                    "Money Sent", "You sent $" + amount + " to " + recipient.getFullName());
            notificationService.notifyUser(recipient.getUserId(), "TRANSACTION",
                    "Money Received", "You received $" + amount + " from " + sender.getFullName());
        }

        ConsoleUtil.pause(sc);
    }
}
