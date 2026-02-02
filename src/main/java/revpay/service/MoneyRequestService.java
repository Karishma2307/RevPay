package revpay.service;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(MoneyRequestService.class);

    private final MoneyRequestDao moneyRequestDao = new MoneyRequestDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void createRequest(Scanner sc, User requester) {

        ConsoleUtil.printHeader("Request Money");

        if (requester == null) {
            logger.warn("createRequest called with null requester");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long requesterId = requester.getUserId();
        logger.info("Create money request started (requesterId={}, username='{}')", requesterId, safe(requester.getUsername()));

        System.out.println("Request from (who should pay you):");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.println("3. Account ID");
        System.out.print("Choice: ");
        String choice = sc.nextLine();

        User payer = null;
        try {
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
                logger.warn("Create request failed: invalid payer lookup choice='{}' (requesterId={})", choice, requesterId);
                System.out.println("[ERROR] Invalid choice.");
                ConsoleUtil.pause(sc);
                return;
            }
        } catch (Exception e) {
            logger.error("Create request failed: DAO error while finding payer (requesterId={}, choice='{}')", requesterId, choice, e);
            System.out.println("Unable to find payer right now.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payer == null) {
            logger.warn("Create request failed: payer not found (requesterId={}, choice='{}')", requesterId, choice);
            System.out.println("User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payer.getUserId() == requesterId) {
            logger.warn("Create request blocked: requester tried to request from self (userId={})", requesterId);
            System.out.println("You cannot request money from yourself.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Create request failed: invalid amount input (requesterId={})", requesterId);
            System.out.println("Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount <= 0) {
            logger.warn("Create request failed: amount <= 0 (requesterId={}, amount={})", requesterId, fmt(amount));
            System.out.println("Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Note (optional): ");
        String note = sc.nextLine();

        MoneyRequest req = new MoneyRequest();
        req.setFromUserId(requesterId);
        req.setToUserId(payer.getUserId());
        req.setAmount(amount);
        req.setStatus("PENDING");
        req.setNote(note);

        long id;
        try {
            id = moneyRequestDao.createRequest(req);
        } catch (Exception e) {
            logger.error("Create request failed: DAO error while creating request (requesterId={}, payerId={}, amount={})",
                    requesterId, payer.getUserId(), fmt(amount), e);
            System.out.println("Failed to create request.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Money request created (requestId={}, requesterId={}, payerId={}, amount={}, status=PENDING)",
                id, requesterId, payer.getUserId(), fmt(amount));

        try {
            notificationService.notifyUser(
                    payer.getUserId(),
                    "REQUEST",
                    "Money Request",
                    requester.getFullName() + " requested ₹" + amount
            );
        } catch (Exception e) {
            logger.error("Failed to notify payer about money request (requestId={}, payerId={})", id, payer.getUserId(), e);
        }

        System.out.println("Request created. ID: " + id);
        ConsoleUtil.pause(sc);
    }

    public void viewIncoming(Scanner sc, User user) {
        ConsoleUtil.printHeader("Incoming Requests");

        if (user == null) {
            logger.warn("viewIncoming called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();

        List<MoneyRequest> list;
        try {
            list = moneyRequestDao.findIncoming(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch incoming requests (userId={})", userId, e);
            System.out.println("Unable to load incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (list == null || list.isEmpty()) {
            logger.info("No incoming requests (userId={})", userId);
            System.out.println("No incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.debug("Incoming requests fetched (userId={}, count={})", userId, list.size());

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

        if (user == null) {
            logger.warn("viewOutgoing called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();

        List<MoneyRequest> list;
        try {
            list = moneyRequestDao.findOutgoing(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch outgoing requests (userId={})", userId, e);
            System.out.println("Unable to load outgoing requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (list == null || list.isEmpty()) {
            logger.info("No outgoing requests (userId={})", userId);
            System.out.println("No outgoing requests.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.debug("Outgoing requests fetched (userId={}, count={})", userId, list.size());

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

        if (payer == null) {
            logger.warn("acceptRequest called with null payer");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long payerId = payer.getUserId();
        logger.info("Accept request flow started (payerId={}, username='{}')", payerId, safe(payer.getUsername()));

        List<MoneyRequest> incoming;
        try {
            incoming = moneyRequestDao.findIncoming(payerId);
        } catch (Exception e) {
            logger.error("Failed to fetch incoming requests for accept (payerId={})", payerId, e);
            System.out.println("Unable to load incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

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
        try {
            requestId = Long.parseLong(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Accept request failed: invalid requestId input (payerId={})", payerId);
            System.out.println("Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        MoneyRequest req;
        try {
            req = moneyRequestDao.findById(requestId);
        } catch (Exception e) {
            logger.error("Accept request failed: DAO error fetching request (requestId={}, payerId={})",
                    requestId, payerId, e);
            System.out.println("Unable to fetch request.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (req == null || req.getToUserId() != payerId) {
            logger.warn("Accept request failed: request not found for payer (requestId={}, payerId={})",
                    requestId, payerId);
            System.out.println("Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            logger.warn("Accept request blocked: not PENDING (requestId={}, payerId={}, status={})",
                    requestId, payerId, req.getStatus());
            System.out.println("Only PENDING requests can be accepted.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter Transaction PIN: ");
        String pin = sc.nextLine();
        if (!HashUtil.check(pin, payer.getTxnPinHash())) {
            logger.warn("Accept request failed: invalid txn pin (payerId={}, requestId={})", payerId, requestId);
            System.out.println("Invalid Transaction PIN.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet payerWallet;
        try {
            payerWallet = walletDao.getWalletByUserId(payerId);
        } catch (Exception e) {
            logger.error("Accept request failed: DAO error fetching payer wallet (payerId={}, requestId={})",
                    payerId, requestId, e);
            System.out.println(" Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (payerWallet == null || payerWallet.getBalance() < req.getAmount()) {
            logger.warn("Accept request failed: insufficient balance (payerId={}, requestId={}, balance={}, amount={})",
                    payerId, requestId,
                    payerWallet == null ? "null" : fmt(payerWallet.getBalance()),
                    fmt(req.getAmount()));
            System.out.println("Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet receiverWallet;
        try {
            receiverWallet = walletDao.getWalletByUserId(req.getFromUserId());
            if (receiverWallet == null) {
                walletDao.createWalletForUser(req.getFromUserId());
                receiverWallet = walletDao.getWalletByUserId(req.getFromUserId());
            }
        } catch (Exception e) {
            logger.error("Accept request failed: receiver wallet fetch/create error (requestId={}, payerId={}, receiverId={})",
                    requestId, payerId, req.getFromUserId(), e);
            System.out.println("Receiver wallet not available.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (receiverWallet == null) {
            logger.error("Accept request failed: receiver wallet is null after create attempt (requestId={}, receiverId={})",
                    requestId, req.getFromUserId());
            System.out.println("Receiver wallet not available.");
            ConsoleUtil.pause(sc);
            return;
        }

        // Update balances
        double newPayerBal = payerWallet.getBalance() - req.getAmount();
        double newReceiverBal = receiverWallet.getBalance() + req.getAmount();

        try {
            walletDao.updateBalance(payerId, newPayerBal);
            walletDao.updateBalance(req.getFromUserId(), newReceiverBal);
        } catch (Exception e) {
            logger.error("Accept request failed: wallet balance update error (requestId={}, payerId={}, receiverId={})",
                    requestId, payerId, req.getFromUserId(), e);
            System.out.println("Failed to transfer amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            lowBalanceAlertService.checkAndNotify(payerId);
        } catch (Exception e) {
            logger.error("Low balance check failed after request accept (payerId={})", payerId, e);
        }

        
        try {
            transactionDao.createTransaction(
                    payerId,
                    req.getFromUserId(),
                    req.getAmount(),
                    "REQUEST_PAYMENT",
                    "SUCCESS",
                    req.getNote()
            );
        } catch (Exception e) {
            logger.error("Accept request failed: transaction creation error (requestId={}, payerId={}, receiverId={})",
                    requestId, payerId, req.getFromUserId(), e);
            System.out.println("Failed to record transaction.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            moneyRequestDao.updateStatus(req.getRequestId(), "ACCEPTED");
        } catch (Exception e) {
            logger.error("Accept request failed: status update error (requestId={}, payerId={})", requestId, payerId, e);
            System.out.println("Failed to update request status.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            notificationService.notifyUser(
                    req.getFromUserId(),
                    "REQUEST",
                    "Request Accepted",
                    "Your request #" + req.getRequestId() + " was accepted. Amount ₹" + req.getAmount()
            );
        } catch (Exception e) {
            logger.error("Failed to notify requester about acceptance (requestId={}, requesterId={})",
                    requestId, req.getFromUserId(), e);
        }

        logger.info("Request accepted and paid (requestId={}, payerId={}, requesterId={}, amount={})",
                requestId, payerId, req.getFromUserId(), fmt(req.getAmount()));

        System.out.println("Request accepted and payment completed.");
        ConsoleUtil.pause(sc);
    }

    public void declineRequest(Scanner sc, User payer) {

        ConsoleUtil.printHeader("Decline Request");

        if (payer == null) {
            logger.warn("declineRequest called with null payer");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long payerId = payer.getUserId();
        logger.info("Decline request flow started (payerId={}, username='{}')", payerId, safe(payer.getUsername()));

        List<MoneyRequest> list;
        try {
            list = moneyRequestDao.findIncoming(payerId);
        } catch (Exception e) {
            logger.error("Failed to fetch incoming requests for decline (payerId={})", payerId, e);
            System.out.println("Unable to load incoming requests.");
            ConsoleUtil.pause(sc);
            return;
        }

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
        try {
            id = Long.parseLong(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Decline request failed: invalid id input (payerId={})", payerId);
            System.out.println("Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        MoneyRequest req;
        try {
            req = moneyRequestDao.findById(id);
        } catch (Exception e) {
            logger.error("Decline request failed: DAO error fetching request (requestId={}, payerId={})", id, payerId, e);
            System.out.println("Unable to fetch request.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (req == null || req.getToUserId() != payerId) {
            logger.warn("Decline request failed: request not found for payer (requestId={}, payerId={})", id, payerId);
            System.out.println("Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            logger.warn("Decline request blocked: not PENDING (requestId={}, payerId={}, status={})", id, payerId, req.getStatus());
            System.out.println("Only PENDING requests can be declined.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            moneyRequestDao.updateStatus(id, "DECLINED");
        } catch (Exception e) {
            logger.error("Decline request failed: status update error (requestId={}, payerId={})", id, payerId, e);
            System.out.println("Failed to decline request.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            notificationService.notifyUser(
                    req.getFromUserId(),
                    "REQUEST",
                    "Request Declined",
                    "Your request #" + id + " was declined."
            );
        } catch (Exception e) {
            logger.error("Failed to notify requester about decline (requestId={}, requesterId={})", id, req.getFromUserId(), e);
        }

        logger.info("Request declined (requestId={}, payerId={}, requesterId={})", id, payerId, req.getFromUserId());

        System.out.println("Request declined.");
        ConsoleUtil.pause(sc);
    }

    public void cancelRequest(Scanner sc, User user) {

        ConsoleUtil.printHeader("Cancel Outgoing Request");

        if (user == null) {
            logger.warn("cancelRequest called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Cancel outgoing request flow started (userId={}, username='{}')", userId, safe(user.getUsername()));

        List<MoneyRequest> list;
        try {
            list = moneyRequestDao.findOutgoing(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch outgoing requests for cancel (userId={})", userId, e);
            System.out.println("Unable to load outgoing requests.");
            ConsoleUtil.pause(sc);
            return;
        }

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
        try {
            id = Long.parseLong(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Cancel request failed: invalid id input (userId={})", userId);
            System.out.println("Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        MoneyRequest req;
        try {
            req = moneyRequestDao.findById(id);
        } catch (Exception e) {
            logger.error("Cancel request failed: DAO error fetching request (requestId={}, userId={})", id, userId, e);
            System.out.println("Unable to fetch request.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (req == null || req.getFromUserId() != userId) {
            logger.warn("Cancel request failed: request not found for user (requestId={}, userId={})", id, userId);
            System.out.println("Request not found for you.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            logger.warn("Cancel request blocked: not PENDING (requestId={}, userId={}, status={})", id, userId, req.getStatus());
            System.out.println("Only PENDING requests can be cancelled.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            moneyRequestDao.updateStatus(id, "CANCELLED");
        } catch (Exception e) {
            logger.error("Cancel request failed: status update error (requestId={}, userId={})", id, userId, e);
            System.out.println("Failed to cancel request.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            notificationService.notifyUser(
                    req.getToUserId(),
                    "REQUEST",
                    "Request Cancelled",
                    "Request #" + id + " was cancelled by requester."
            );
        } catch (Exception e) {
            logger.error("Failed to notify payer about cancellation (requestId={}, payerId={})", id, req.getToUserId(), e);
        }

        logger.info("Outgoing request cancelled (requestId={}, requesterId={}, payerId={})", id, userId, req.getToUserId());

        System.out.println("Request cancelled.");
        ConsoleUtil.pause(sc);
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
