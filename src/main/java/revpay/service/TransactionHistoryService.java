package revpay.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.ExportUtil;

public class TransactionHistoryService {

    private TransactionDao dao = new TransactionDaoImpl();

    public void showFilteredHistory(Scanner sc, User user) {
        ConsoleUtil.printHeader("Transaction History - Filter/Search/Export");

        System.out.print("Filter TYPE (SEND/RECEIVE/TOPUP/WITHDRAW/INVOICE/LOAN) or blank: ");
        String type = blankToNull(sc.nextLine());

        System.out.print("Filter STATUS (SUCCESS/PENDING/FAILED) or blank: ");
        String status = blankToNull(sc.nextLine());

        Date from = null;
        Date to = null;

        System.out.print("From Date (yyyy-MM-dd) or blank: ");
        String fromStr = sc.nextLine().trim();
        if (fromStr.length() > 0) from = parseDate(fromStr);

        System.out.print("To Date (yyyy-MM-dd) or blank: ");
        String toStr = sc.nextLine().trim();
        if (toStr.length() > 0) to = parseDate(toStr);

        System.out.print("Min Amount or blank: ");
        Double min = parseDoubleOrNull(sc.nextLine());

        System.out.print("Max Amount or blank: ");
        Double max = parseDoubleOrNull(sc.nextLine());

        System.out.print("Search keyword in NOTE/TYPE/STATUS or blank: ");
        String keyword = blankToNull(sc.nextLine());

        List<Transaction> txs = dao.searchTransactions(user.getUserId(), type, status, from, to, min, max, keyword);

        ConsoleUtil.printHeader("Results");
        if (txs == null || txs.isEmpty()) {
            System.out.println("No transactions found.");
            ConsoleUtil.pause(sc);
            return;
        }

        for (Transaction t : txs) {
            System.out.println("----------------------------------------");
            System.out.println("TXN_ID   : " + t.getTransactionId());
            System.out.println("TYPE     : " + t.getType());
            System.out.println("STATUS   : " + t.getStatus());
            System.out.println("AMOUNT   : $" + String.format("%.2f", t.getAmount()));
            System.out.println("FROM     : " + t.getFromUserId());
            System.out.println("TO       : " + t.getToUserId());
            System.out.println("NOTE     : " + safe(t.getNote()));
            System.out.println("DATE     : " + safe(t.getCreatedAt()));
        }

        System.out.println("----------------------------------------");
        System.out.print("Export these results to CSV? (y/n): ");
        String ex = sc.nextLine().trim();

        if ("y".equalsIgnoreCase(ex)) {
            String file = ExportUtil.exportTransactionsToCsv(txs, "revpay_transactions_" + user.getUserId());
            if (file != null) System.out.println("[INFO] Exported to: " + file);
            else System.out.println("[ERROR] Export failed.");
        }

        ConsoleUtil.pause(sc);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.length() == 0 ? null : s;
    }

    private Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s.trim());
        } catch (Exception e) {
            System.out.println("[WARN] Invalid date format, ignoring.");
            return null;
        }
    }

    private Double parseDoubleOrNull(String s) {
        try {
            s = s.trim();
            if (s.length() == 0) return null;
            return Double.valueOf(Double.parseDouble(s));
        } catch (Exception e) {
            System.out.println("[WARN] Invalid number, ignoring.");
            return null;
        }
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}