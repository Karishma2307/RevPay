package revpay.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.TransactionDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.util.ConsoleUtil;

public class TransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);

    private final TransactionDao dao = new TransactionDaoImpl();

    public void showHistory(User user, Scanner sc) {
        showFilteredHistory(sc, user);
    }

    public void showFilteredHistory(Scanner sc, User user) {

        ConsoleUtil.printHeader("Transaction History");

        if (user == null) {
            logger.warn("showFilteredHistory called with null user");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Transaction history search started (userId={}, username='{}')", userId, safe(user.getUsername()));

        System.out.print("Type (TRANSFER/DEPOSIT/WITHDRAW or blank): ");
        String type = emptyToNull(sc.nextLine());

        System.out.print("Status (SUCCESS/FAILED/PENDING or blank): ");
        String status = emptyToNull(sc.nextLine());

        LocalDate from = ConsoleUtil.readOptionalDate(sc, "From Date (yyyy-MM-dd) or blank: ");
        LocalDate to = ConsoleUtil.readOptionalDate(sc, "To Date (yyyy-MM-dd) or blank: ");

        Date fromDate = (from == null) ? null : Date.valueOf(from);
        Date toDate = (to == null) ? null : Date.valueOf(to);

        System.out.print("Min Amount or blank: ");
        Double min = parseDoubleOrNull(sc.nextLine(), userId, "minAmount");

        System.out.print("Max Amount or blank: ");
        Double max = parseDoubleOrNull(sc.nextLine(), userId, "maxAmount");

        System.out.print("Keyword in note or blank: ");
        String keyword = emptyToNull(sc.nextLine());

        logger.debug("Transaction history filters (userId={}, type={}, status={}, fromDate={}, toDate={}, min={}, max={}, keyword={})",
                userId,
                safe(type),
                safe(status),
                (fromDate == null ? "-" : fromDate),
                (toDate == null ? "-" : toDate),
                (min == null ? "-" : fmt(min)),
                (max == null ? "-" : fmt(max)),
                safe(keyword));

        List<Transaction> txs;
        try {
            txs = dao.searchTransactions(
                    userId,
                    type,
                    status,
                    fromDate,
                    toDate,
                    min,
                    max,
                    keyword
            );
        } catch (Exception e) {
            logger.error("Transaction history search failed: DAO error (userId={})", userId, e);
            System.out.println("Unable to load transaction history right now.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (txs == null || txs.isEmpty()) {
            logger.info("No transactions found (userId={})", userId);
            System.out.println("No transactions found.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Transactions found (userId={}, count={})", userId, txs.size());

        for (Transaction t : txs) {
            System.out.println("----------------------------------------");
            System.out.println("ID: " + t.getTransactionId());
            System.out.println("From: " + t.getFromUserId() + "  To: " + t.getToUserId());
            System.out.println("Amount: ₹" + t.getAmount());
            System.out.println("Type: " + t.getType() + " | Status: " + t.getStatus());
            System.out.println("Note: " + t.getNote());
            System.out.println("Date: " + t.getCreatedAt());
        }

        ConsoleUtil.pause(sc);
    }

    private String emptyToNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }

    private Double parseDoubleOrNull(String s, long userId, String fieldName) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            logger.warn("Invalid number for {} ignored (userId={}, input='{}')", fieldName, userId, safe(s));
            System.out.println("Invalid number, ignoring.");
            return null;
        }
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
