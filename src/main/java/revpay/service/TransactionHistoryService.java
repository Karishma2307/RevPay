package revpay.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.util.ConsoleUtil;

public class TransactionHistoryService {

    private final TransactionDao dao = new TransactionDaoImpl();

    public void showHistory(User user, Scanner sc) {
        showFilteredHistory(sc, user);
    }

    public void showFilteredHistory(Scanner sc, User user) {

        ConsoleUtil.printHeader("Transaction History");

        System.out.print("Type (TRANSFER/DEPOSIT/WITHDRAW or blank): ");
        String type = emptyToNull(sc.nextLine());

        System.out.print("Status (SUCCESS/FAILED/PENDING or blank): ");
        String status = emptyToNull(sc.nextLine());

        LocalDate from = ConsoleUtil.readOptionalDate(sc, "From Date (yyyy-MM-dd) or blank: ");
        LocalDate to = ConsoleUtil.readOptionalDate(sc, "To Date (yyyy-MM-dd) or blank: ");

        Date fromDate = (from == null) ? null : Date.valueOf(from);
        Date toDate = (to == null) ? null : Date.valueOf(to);

        System.out.print("Min Amount or blank: ");
        Double min = parseDoubleOrNull(sc.nextLine());

        System.out.print("Max Amount or blank: ");
        Double max = parseDoubleOrNull(sc.nextLine());

        System.out.print("Keyword in note or blank: ");
        String keyword = emptyToNull(sc.nextLine());

        List<Transaction> txs = dao.searchTransactions(
                user.getUserId(),
                type,
                status,
                fromDate,
                toDate,
                min,
                max,
                keyword
        );

        if (txs == null || txs.isEmpty()) {
            System.out.println("No transactions found.");
            ConsoleUtil.pause(sc);
            return;
        }

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

    private Double parseDoubleOrNull(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            System.out.println("[WARN] Invalid number, ignoring.");
            return null;
        }
    }
}
