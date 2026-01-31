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
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;
import revpay.util.HashUtil;

public class MoneyRequestService {

    private final MoneyRequestDao moneyRequestDao = new MoneyRequestDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void createRequest(Scanner sc, User requester) {

        ConsoleUtil.printHeader("Request Money");

        System.out.println("Request from (who should pay you):");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User payer = null;
        if ("1".equals(choice)) {
            System.out.print("Payer Email: ");
            payer = userDao.findByEmail(sc.nextLine());
        } else if ("2".equals(choice)) {
            System.out.print("Payer Phone: ");
            payer = userDao.findByPhone(sc.nextLine());
        } else if ("3".equals(choice)) {
            System.out.print("Payer Account ID: ");
            payer = userDao.findByAccountId(sc.nextLine());
        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payer == null) {
            System.out.println("[ERROR] User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payer.getUserId() == requester.getUserId()) {
            System.out.println("[ERROR] You cannot request money from yourself.");
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

        System.out.print("Note (optional): ");
        String note = sc.nextLine();

        MoneyRequest req = new MoneyRequest();
        req.setFromUserId(requester.getUserId());
        req.setToUserId(payer.getUserId());
        req.setAmount(amount);
        req.setStatus("PENDING");
        req.setNote(note);

        long id = moneyRequestDao.createRequest(req);

        notificationService.notifyUser(
                payer.getUserId(),
                "REQUEST",
                "Money Request",
                requester.getFullName() + " requested ₹" + amount
        );

        System.out.println("[INFO] Request created. ID: " + id);
        ConsoleUtil.pause(sc);
    }

    public void viewIncoming(Scanner sc, User user) {
        ConsoleUtil.printHeader("Incoming Requests");

        List<MoneyRequest> list = moneyRequestDao.findIncoming(user.getUserId());
        if (list == null || list.isEmpty()) {
            System.out.println("No incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        for (MoneyRequest r : list) {
            System.out.println("ID: " + r.getRequestId()
                    + " | From(UserId): " + r.getFromUserId()
                    + " | Amount: ₹" + r.getAmount()
                    + " | Status: " + r.getStatus());
        }

        ConsoleUtil.pause(sc);
    }

    public void viewOutgoing(Scanner sc, User user) {
        ConsoleUtil.printHeader("Outgoing Requests");

        List<MoneyRequest> list = moneyRequestDao.findOutgoing(user.getUserId());
        if (list == null || list.isEmpty()) {
            System.out.println("No outgoing requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        for (MoneyRequest r : list) {
            System.out.println("ID: " + r.getRequestId()
                    + " | To(UserId): " + r.getToUserId()
                    + " | Amount: ₹" + r.getAmount()
                    + " | Status: " + r.getStatus());
        }

        ConsoleUtil.pause(sc);
    }

    public void acceptRequest(Scanner sc, User payer) {

        ConsoleUtil.printHeader("Accept Money Request");

        List<MoneyRequest> incoming = moneyRequestDao.findIncoming(payer.getUserId());
        if (incoming == null || incoming.isEmpty()) {
            System.out.println("No incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        boolean anyPending = false;
        for (MoneyRequest r : incoming) {
            if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                anyPending = true;
                System.out.println("ID: " + r.getRequestId()
                        + " | From(UserId): " + r.getFromUserId()
                        + " | Amount: ₹" + r.getAmount()
                        + " | Note: " + (r.getNote() == null ? "" : r.getNote()));
            }
        }

        if (!anyPending) {
            System.out.println("No PENDING requests to accept.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Request ID to accept: ");
        long requestId;
        try { requestId = Long.parseLong(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid ID."); ConsoleUtil.pause(sc); return; }

        MoneyRequest req = moneyRequestDao.findById(requestId);
        if (req == null || req.getToUserId() != payer.getUserId()) {
            System.out.println("[ERROR] Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            System.out.println("[ERROR] Only PENDING requests can be accepted.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, payer.getTxnPinHash())) {
            System.out.println("[ERROR] Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet payerWallet = walletDao.getWalletByUserId(payer.getUserId());
        if (payerWallet == null || payerWallet.getBalance() < req.getAmount()) {
            System.out.println("[ERROR] Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet receiverWallet = walletDao.getWalletByUserId(req.getFromUserId());
        if (receiverWallet == null) {
            walletDao.createWalletForUser(req.getFromUserId());
            receiverWallet = walletDao.getWalletByUserId(req.getFromUserId());
        }

        walletDao.updateBalance(payer.getUserId(), payerWallet.getBalance() - req.getAmount());
        walletDao.updateBalance(req.getFromUserId(), receiverWallet.getBalance() + req.getAmount());

        try { lowBalanceAlertService.checkAndNotify(payer.getUserId()); } catch (Exception ignored) {}

        // ✅ FIXED: 6 params only
        transactionDao.createTransaction(
                payer.getUserId(),
                req.getFromUserId(),
                req.getAmount(),
                "REQUEST_PAYMENT",
                "SUCCESS",
                req.getNote()
        );

        moneyRequestDao.updateStatus(req.getRequestId(), "ACCEPTED");

        notificationService.notifyUser(
                req.getFromUserId(),
                "REQUEST",
                "Request Accepted",
                "Your request #" + req.getRequestId() + " was accepted. Amount ₹" + req.getAmount()
        );

        System.out.println("[INFO] Request accepted and payment completed.");
        ConsoleUtil.pause(sc);
    }

    public void declineRequest(Scanner sc, User payer) {

        ConsoleUtil.printHeader("Decline Request");

        List<MoneyRequest> list = moneyRequestDao.findIncoming(payer.getUserId());
        if (list == null || list.isEmpty()) {
            System.out.println("No incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        boolean anyPending = false;
        for (MoneyRequest r : list) {
            if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                anyPending = true;
                System.out.println("ID: " + r.getRequestId()
                        + " | From(UserId): " + r.getFromUserId()
                        + " | Amount: ₹" + r.getAmount());
            }
        }

        if (!anyPending) {
            System.out.println("No PENDING requests to decline.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Request ID to decline: ");
        long id;
        try { id = Long.parseLong(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid ID."); ConsoleUtil.pause(sc); return; }

        MoneyRequest req = moneyRequestDao.findById(id);
        if (req == null || req.getToUserId() != payer.getUserId()) {
            System.out.println("[ERROR] Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            System.out.println("[ERROR] Only PENDING requests can be declined.");
            ConsoleUtil.pause(sc);
            return;
        }

        moneyRequestDao.updateStatus(id, "DECLINED");

        notificationService.notifyUser(
                req.getFromUserId(),
                "REQUEST",
                "Request Declined",
                "Your request #" + id + " was declined."
        );

        System.out.println("[INFO] Request declined.");
        ConsoleUtil.pause(sc);
    }

    public void cancelRequest(Scanner sc, User user) {

        ConsoleUtil.printHeader("Cancel Outgoing Request");

        List<MoneyRequest> list = moneyRequestDao.findOutgoing(user.getUserId());
        if (list == null || list.isEmpty()) {
            System.out.println("No outgoing requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        boolean anyPending = false;
        for (MoneyRequest r : list) {
            if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                anyPending = true;
                System.out.println("ID: " + r.getRequestId()
                        + " | To(UserId): " + r.getToUserId()
                        + " | Amount: ₹" + r.getAmount());
            }
        }

        if (!anyPending) {
            System.out.println("No PENDING outgoing requests to cancel.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Request ID to cancel: ");
        long id;
        try { id = Long.parseLong(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid ID."); ConsoleUtil.pause(sc); return; }

        MoneyRequest req = moneyRequestDao.findById(id);
        if (req == null || req.getFromUserId() != user.getUserId()) {
            System.out.println("[ERROR] Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            System.out.println("[ERROR] Only PENDING requests can be cancelled.");
            ConsoleUtil.pause(sc);
            return;
        }

        moneyRequestDao.updateStatus(id, "CANCELLED");

        notificationService.notifyUser(
                req.getToUserId(),
                "REQUEST",
                "Request Cancelled",
                "Request #" + id + " was cancelled by requester."
        );

        System.out.println("[INFO] Request cancelled.");
        ConsoleUtil.pause(sc);
    }
}
