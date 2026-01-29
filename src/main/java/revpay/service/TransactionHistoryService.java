package revpay.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import revpay.dao.TransactionDao;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.util.ConsoleUtil;

public class TransactionHistoryService {

    private TransactionDao transactionDao = new TransactionDaoImpl();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public void showHistory(Scanner sc, User user) {
        while (true) {
            ConsoleUtil.printHeader("Transaction History");
            System.out.println("1. View All Transactions");
            System.out.println("2. Filter by Type (TRANSFER / WALLET_TOPUP / WALLET_WITHDRAW)");
            System.out.println("3. Filter by Direction (Sent / Received)");
            System.out.println("4. Filter by Amount Range");
            System.out.println("5. Filter by Date Range (yyyy-MM-dd)");
            System.out.println("6. Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine();

            // load all transactions once each time we open history
            List<Transaction> all = transactionDao.findByUserId(user.getUserId());

            if ("1".equals(choice)) {
                printTransactions(all, user);
                ConsoleUtil.pause(sc);

            } else if ("2".equals(choice)) {
                filterByType(sc, all, user);

            } else if ("3".equals(choice)) {
                filterByDirection(sc, all, user);

            } else if ("4".equals(choice)) {
                filterByAmount(sc, all, user);

            } else if ("5".equals(choice)) {
                filterByDate(sc, all, user);

            } else if ("6".equals(choice)) {
                break;

            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private void filterByType(Scanner sc, List<Transaction> all, User user) {
        System.out.println("Filter by Type:");
        System.out.println("1. TRANSFER");
        System.out.println("2. WALLET_TOPUP");
        System.out.println("3. WALLET_WITHDRAW");
        System.out.print("Choice: ");
        String ch = sc.nextLine();

        String type = null;
        if ("1".equals(ch)) type = "TRANSFER";
        else if ("2".equals(ch)) type = "WALLET_TOPUP";
        else if ("3".equals(ch)) type = "WALLET_WITHDRAW";
        else {
            System.out.println("[ERROR] Invalid type option.");
            ConsoleUtil.pause(sc);
            return;
        }

        List<Transaction> filtered = new ArrayList<Transaction>();
        for (Transaction t : all) {
            if (t.getType() != null && t.getType().equalsIgnoreCase(type)) {
                filtered.add(t);
            }
        }

        ConsoleUtil.printHeader("Transactions - Type: " + type);
        printTransactions(filtered, user);
        ConsoleUtil.pause(sc);
    }

    private void filterByDirection(Scanner sc, List<Transaction> all, User user) {
        System.out.println("Direction:");
        System.out.println("1. Sent");
        System.out.println("2. Received");
        System.out.print("Choice: ");
        String ch = sc.nextLine();

        boolean sent;
        if ("1".equals(ch)) {
            sent = true;
        } else if ("2".equals(ch)) {
            sent = false;
        } else {
            System.out.println("[ERROR] Invalid direction option.");
            ConsoleUtil.pause(sc);
            return;
        }

        List<Transaction> filtered = new ArrayList<Transaction>();
        long userId = user.getUserId();

        for (Transaction t : all) {
            boolean isSent = (t.getFromUserId() != null && t.getFromUserId().longValue() == userId);
            boolean isReceived = (t.getToUserId() != null && t.getToUserId().longValue() == userId);

            if (sent && isSent) {
                filtered.add(t);
            } else if (!sent && isReceived) {
                filtered.add(t);
            }
        }

        ConsoleUtil.printHeader("Transactions - " + (sent ? "Sent" : "Received"));
        printTransactions(filtered, user);
        ConsoleUtil.pause(sc);
    }

    private void filterByAmount(Scanner sc, List<Transaction> all, User user) {
        System.out.print("Minimum amount (or blank for 0): ");
        String minStr = sc.nextLine();
        System.out.print("Maximum amount (or blank for no limit): ");
        String maxStr = sc.nextLine();

        double min = 0.0;
        double max = Double.MAX_VALUE;

        try {
            if (minStr != null && minStr.trim().length() > 0) {
                min = Double.parseDouble(minStr.trim());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid minimum amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            if (maxStr != null && maxStr.trim().length() > 0) {
                max = Double.parseDouble(maxStr.trim());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid maximum amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        List<Transaction> filtered = new ArrayList<Transaction>();
        for (Transaction t : all) {
            double amt = t.getAmount();
            if (amt >= min && amt <= max) {
                filtered.add(t);
            }
        }

        ConsoleUtil.printHeader("Transactions - Amount Range");
        printTransactions(filtered, user);
        ConsoleUtil.pause(sc);
    }

    private void filterByDate(Scanner sc, List<Transaction> all, User user) {
        System.out.print("Start date (yyyy-MM-dd): ");
        String startStr = sc.nextLine();
        System.out.print("End date   (yyyy-MM-dd): ");
        String endStr = sc.nextLine();

        Date start = null;
        Date end = null;

        try {
            if (startStr != null && startStr.trim().length() > 0) {
                start = DATE_FORMAT.parse(startStr.trim());
            }
            if (endStr != null && endStr.trim().length() > 0) {
                end = DATE_FORMAT.parse(endStr.trim());
            }
        } catch (ParseException e) {
            System.out.println("[ERROR] Invalid date format. Use yyyy-MM-dd.");
            ConsoleUtil.pause(sc);
            return;
        }

        List<Transaction> filtered = new ArrayList<Transaction>();
        for (Transaction t : all) {
            Date d = t.getCreatedAt();
            if (d == null) continue;

            boolean ok = true;
            if (start != null && d.before(start)) {
                ok = false;
            }
            if (end != null && d.after(end)) {
                ok = false;
            }
            if (ok) {
                filtered.add(t);
            }
        }

        ConsoleUtil.printHeader("Transactions - Date Range");
        printTransactions(filtered, user);
        ConsoleUtil.pause(sc);
    }

    private void printTransactions(List<Transaction> list, User user) {
        if (list == null || list.isEmpty()) {
            System.out.println("No transactions found for given filters.");
            return;
        }

        long userId = user.getUserId();
        System.out.println("ID       | Date       | Type           | Direction | Amount   | Note");
        System.out.println("--------------------------------------------------------------------------");

        for (Transaction t : list) {
            String direction;
            if (t.getFromUserId() != null && t.getFromUserId().longValue() == userId) {
                direction = "SENT";
            } else if (t.getToUserId() != null && t.getToUserId().longValue() == userId) {
                direction = "RECEIVED";
            } else {
                direction = "-";
            }

            String dateStr = "";
            if (t.getCreatedAt() != null) {
                dateStr = DATE_FORMAT.format(t.getCreatedAt());
            }

            System.out.printf("%-8d | %-10s | %-13s | %-9s | $%-7.2f | %s%n",
                    t.getTransactionId(),
                    dateStr,
                    t.getType(),
                    direction,
                    t.getAmount(),
                    t.getNote() == null ? "" : t.getNote());
        }
    }
}
