package revpay.service;

import java.util.List;
import java.util.Scanner;

import revpay.dao.MoneyRequestDao;
import revpay.dao.TransactionDao;
import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.MoneyRequestDaoImpl;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.UserDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.MoneyRequest;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;

public class MoneyRequestService {

    private MoneyRequestDao moneyRequestDao = new MoneyRequestDaoImpl();
    private UserDao userDao = new UserDaoImpl();
    private WalletDao walletDao = new WalletDaoImpl();
    private TransactionDao transactionDao = new TransactionDaoImpl();
    private NotificationService notificationService;

    public MoneyRequestService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ================
    //  CREATE REQUEST
    // ================
    public void createRequest(Scanner sc, User requester) {
        ConsoleUtil.printHeader("Request Money");

        System.out.println("Request from (who should pay you):");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User target = null;
        if ("1".equals(choice)) {
            System.out.print("Payer Email: ");
            String email = sc.nextLine();
            target = userDao.findByEmail(email.trim());
        } else if ("2".equals(choice)) {
            System.out.print("Payer Phone: ");
            String phone = sc.nextLine();
            target = userDao.findByPhone(phone.trim());
        } else if ("3".equals(choice)) {
            System.out.print("Payer Account ID: ");
            String accId = sc.nextLine();
            target = userDao.findByAccountId(accId.trim());
        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (target == null) {
            System.out.println("[ERROR] User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (target.getUserId() == requester.getUserId()) {
            System.out.println("[ERROR] You cannot request money from yourself.");
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

        System.out.print("Optional note: ");
        String note = sc.nextLine();

        MoneyRequest req = new MoneyRequest();
        req.setFromUserId(requester.getUserId());
        req.setToUserId(target.getUserId());
        req.setAmount(amount);
        req.setCurrency("USD");
        req.setNote(note);

        long id = moneyRequestDao.createRequest(req);
        System.out.println("[INFO] Money request created with ID: " + id);
        System.out.println("Requested From: " + target.getFullName() + " (" + target.getAccountId() + ")");
        System.out.println("Amount        : $" + amount);

        if (notificationService != null) {
            notificationService.notifyUser(target.getUserId(), "REQUEST",
                    "Money Request",
                    "You have a money request of $" + amount + " from " + requester.getFullName() + ".");
        }

        ConsoleUtil.pause(sc);
    }

    // ===========================
    //  MANAGE INCOMING/OUTGOING
    // ===========================
    public void manageRequests(Scanner sc, User currentUser) {
        while (true) {
            ConsoleUtil.printHeader("Money Requests");
            System.out.println("1. View Incoming Requests");
            System.out.println("2. View Outgoing Requests");
            System.out.println("3. Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                handleIncoming(sc, currentUser);
            } else if ("2".equals(choice)) {
                handleOutgoing(sc, currentUser);
            } else if ("3".equals(choice)) {
                break;
            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private void handleIncoming(Scanner sc, User currentUser) {
        List<MoneyRequest> list = moneyRequestDao.findIncoming(currentUser.getUserId());
        ConsoleUtil.printHeader("Incoming Requests");
        if (list.isEmpty()) {
            System.out.println("No incoming money requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        int i = 1;
        for (MoneyRequest r : list) {
            User from = userDao.findByAccountId(findAccountId(r.getFromUserId()));
            String fromName = (from != null) ? from.getFullName() : ("User#" + r.getFromUserId());
            System.out.printf("[%d] ID=%d | From=%s | Amount=$%.2f | Status=%s | Note=%s%n",
                    i++, r.getRequestId(), fromName, r.getAmount(), r.getStatus(), r.getNote());
        }

        System.out.println("--------------------------------------------");
        System.out.print("Enter Request ID to manage (or 0 to back): ");
        String idStr = sc.nextLine();
        long reqId;
        try {
            reqId = Long.parseLong(idStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }
        if (reqId == 0) {
            return;
        }

        MoneyRequest req = moneyRequestDao.findById(reqId);
        if (req == null || req.getToUserId() != currentUser.getUserId()) {
            System.out.println("[ERROR] Request not found or not assigned to you.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("Selected Request ID: " + req.getRequestId() + " | Status: " + req.getStatus());
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            System.out.println("[INFO] Only PENDING requests can be accepted/declined.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("1. Accept");
        System.out.println("2. Decline");
        System.out.println("3. Back");
        System.out.print("Choice: ");
        String ch = sc.nextLine();

        if ("1".equals(ch)) {
            acceptRequest(sc, currentUser, req);
        } else if ("2".equals(ch)) {
            moneyRequestDao.updateStatus(req.getRequestId(), "DECLINED");
            System.out.println("[INFO] Request declined.");
            if (notificationService != null) {
                notificationService.notifyUser(req.getFromUserId(), "REQUEST",
                        "Request Declined",
                        "Your money request (ID " + req.getRequestId() + ") was declined.");
            }
            ConsoleUtil.pause(sc);
        } else {
            // back
        }
    }

    private void handleOutgoing(Scanner sc, User currentUser) {
        List<MoneyRequest> list = moneyRequestDao.findOutgoing(currentUser.getUserId());
        ConsoleUtil.printHeader("Outgoing Requests");
        if (list.isEmpty()) {
            System.out.println("No outgoing money requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        int i = 1;
        for (MoneyRequest r : list) {
            User to = userDao.findByAccountId(findAccountId(r.getToUserId()));
            String toName = (to != null) ? to.getFullName() : ("User#" + r.getToUserId());
            System.out.printf("[%d] ID=%d | To=%s | Amount=$%.2f | Status=%s | Note=%s%n",
                    i++, r.getRequestId(), toName, r.getAmount(), r.getStatus(), r.getNote());
        }

        System.out.println("--------------------------------------------");
        System.out.print("Enter Request ID to cancel (or 0 to back): ");
        String idStr = sc.nextLine();
        long reqId;
        try {
            reqId = Long.parseLong(idStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }
        if (reqId == 0) {
            return;
        }

        MoneyRequest req = moneyRequestDao.findById(reqId);
        if (req == null || req.getFromUserId() != currentUser.getUserId()) {
            System.out.println("[ERROR] Request not found or not created by you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            System.out.println("[INFO] Only PENDING requests can be cancelled.");
            ConsoleUtil.pause(sc);
            return;
        }

        moneyRequestDao.updateStatus(req.getRequestId(), "CANCELLED");
        System.out.println("[INFO] Request cancelled.");

        if (notificationService != null) {
            notificationService.notifyUser(req.getToUserId(), "REQUEST",
                    "Request Cancelled",
                    "Money request (ID " + req.getRequestId() + ") was cancelled by requester.");
        }

        ConsoleUtil.pause(sc);
    }

    // ================
    //   ACCEPT FLOW
    // ================
    private void acceptRequest(Scanner sc, User payer, MoneyRequest req) {
        Wallet payerWallet = walletDao.findByUserId(payer.getUserId());
        if (payerWallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payerWallet.getBalance() < req.getAmount()) {
            System.out.println("[ERROR] Insufficient balance to accept this request.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Transaction PIN to confirm payment: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, payer.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid PIN. Payment aborted.");
            ConsoleUtil.pause(sc);
            return;
        }

        // receiver = requester
        Wallet receiverWallet = walletDao.findByUserId(req.getFromUserId());
        if (receiverWallet == null) {
            walletDao.createWalletForUser(req.getFromUserId());
            receiverWallet = walletDao.findByUserId(req.getFromUserId());
        }

        double newPayerBal = payerWallet.getBalance() - req.getAmount();
        double newReceiverBal = receiverWallet.getBalance() + req.getAmount();

        walletDao.updateBalance(payerWallet.getWalletId(), newPayerBal);
        walletDao.updateBalance(receiverWallet.getWalletId(), newReceiverBal);

        payerWallet.setBalance(newPayerBal);
        receiverWallet.setBalance(newReceiverBal);

        // Record transaction
        Transaction t = new Transaction();
        t.setFromUserId(payer.getUserId());
        t.setToUserId(req.getFromUserId());
        t.setAmount(req.getAmount());
        t.setCurrency(req.getCurrency());
        t.setType("TRANSFER");
        t.setStatus("SUCCESS");
        t.setNote("Money Request #" + req.getRequestId());
        long txnId = transactionDao.createTransaction(t);

        moneyRequestDao.updateStatus(req.getRequestId(), "ACCEPTED");

        System.out.println("[INFO] Request accepted and payment sent.");
        System.out.println("Transaction ID: " + txnId);
        System.out.println("Amount        : $" + req.getAmount());

        if (notificationService != null) {
            notificationService.notifyUser(req.getFromUserId(), "REQUEST",
                    "Request Accepted",
                    "Your money request (ID " + req.getRequestId() + ") was accepted and paid.");
            notificationService.notifyUser(payer.getUserId(), "TRANSACTION",
                    "Payment Sent",
                    "You paid $" + req.getAmount() + " for money request ID " + req.getRequestId() + ".");
        }

        ConsoleUtil.pause(sc);
    }

    // Helper to get accountId from userId (for printing)
    private String findAccountId(long userId) {
        
        return "U-" + userId;
    }
}
