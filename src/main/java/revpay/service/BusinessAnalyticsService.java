package revpay.service;

import java.util.*;
import java.util.Scanner;

import revpay.dao.InvoiceDao;
import revpay.dao.LoanDao;
import revpay.dao.TransactionDao;
import revpay.dao.impl.InvoiceDaoImpl;
import revpay.dao.impl.LoanDaoImpl;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.model.Invoice;
import revpay.model.Loan;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.util.ConsoleUtil;

public class BusinessAnalyticsService {

    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final InvoiceDao invoiceDao = new InvoiceDaoImpl();
    private final LoanDao loanDao = new LoanDaoImpl();

    public void showAnalytics(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Business Analytics");

        long userId = businessUser.getUserId();

        // ✅ TransactionDao doesn't have findByUserId(), so use searchTransactions() with null filters
        List<Transaction> txns = transactionDao.searchTransactions(
                userId, null, null,
                null, null, null, null, null
        );

        List<Invoice> invoices = invoiceDao.findByBusinessUser(userId);
        List<Loan> loans = loanDao.findByBusinessUser(userId);

        // 1) Transaction summary
        double totalInflow = 0.0;
        double totalOutflow = 0.0;

        double invoicePaymentsIn = 0.0;
        double loanDisbursementIn = 0.0;
        double loanRepaymentOut = 0.0;
        double walletTopupIn = 0.0;
        double transfersIn = 0.0;
        double transfersOut = 0.0;

        for (Transaction t : txns) {
            Long fromId = t.getFromUserId();
            Long toId = t.getToUserId();
            double amount = t.getAmount();
            String type = t.getType() == null ? "" : t.getType().toUpperCase();

            boolean isIn = (toId != null && toId.longValue() == userId);
            boolean isOut = (fromId != null && fromId.longValue() == userId);

            if (isIn) totalInflow += amount;
            if (isOut) totalOutflow += amount;

            if ("INVOICE_PAYMENT".equals(type) && isIn) invoicePaymentsIn += amount;
            else if ("LOAN_DISBURSEMENT".equals(type) && isIn) loanDisbursementIn += amount;
            else if ("LOAN_REPAYMENT".equals(type) && isOut) loanRepaymentOut += amount;
            else if ("WALLET_TOPUP".equals(type) && isIn) walletTopupIn += amount;
            else if ("TRANSFER".equals(type)) {
                if (isIn) transfersIn += amount;
                if (isOut) transfersOut += amount;
            }
        }

        // 2) Invoice analytics
        int totalInvoices = invoices.size();
        int paidCount = 0;
        int pendingCount = 0;
        int cancelledCount = 0;
        double paidTotal = 0.0;
        double pendingTotal = 0.0;

        Map<String, Double> customerTotals = new HashMap<>(); // customer -> total paid

        for (Invoice inv : invoices) {
            String status = inv.getStatus() == null ? "" : inv.getStatus().toUpperCase();
            double amt = inv.getTotalAmount();

            if ("PAID".equals(status)) {
                paidCount++;
                paidTotal += amt;

                String key = buildCustomerKey(inv.getCustomerName(), inv.getCustomerEmail());
                customerTotals.put(key, customerTotals.getOrDefault(key, 0.0) + amt);

            } else if ("PENDING".equals(status)) {
                pendingCount++;
                pendingTotal += amt;

            } else if ("CANCELLED".equals(status)) {
                cancelledCount++;
            }
        }

        // 3) Loan analytics
        int loansTotal = loans.size();
        int loansPending = 0;
        int loansActive = 0;
        int loansClosed = 0;
        int loansRejected = 0;
        double totalLoanRequested = 0.0;
        double totalOutstanding = 0.0;

        for (Loan l : loans) {
            String status = l.getStatus() == null ? "" : l.getStatus().toUpperCase();
            totalLoanRequested += l.getAmount();
            totalOutstanding += l.getOutstandingAmount();

            if ("PENDING".equals(status)) loansPending++;
            else if ("ACTIVE".equals(status)) loansActive++;
            else if ("CLOSED".equals(status)) loansClosed++;
            else if ("REJECTED".equals(status)) loansRejected++;
        }

        System.out.println("== Transaction Summary ==");
        System.out.printf("Total Inflow  : $%.2f%n", totalInflow);
        System.out.printf("Total Outflow : $%.2f%n", totalOutflow);
        System.out.println();

        System.out.println("Breakdown:");
        System.out.printf("  Invoice Payments In   : $%.2f%n", invoicePaymentsIn);
        System.out.printf("  Loan Disbursements In : $%.2f%n", loanDisbursementIn);
        System.out.printf("  Loan Repayments Out   : $%.2f%n", loanRepaymentOut);
        System.out.printf("  Wallet Topups In      : $%.2f%n", walletTopupIn);
        System.out.printf("  Transfers In          : $%.2f%n", transfersIn);
        System.out.printf("  Transfers Out         : $%.2f%n", transfersOut);
        System.out.println();

        System.out.println("== Invoice Analytics ==");
        System.out.println("Total Invoices     : " + totalInvoices);
        System.out.println("Paid Invoices      : " + paidCount + " (Total $" + fmt(paidTotal) + ")");
        System.out.println("Pending Invoices   : " + pendingCount + " (Total $" + fmt(pendingTotal) + ")");
        System.out.println("Cancelled Invoices : " + cancelledCount);
        System.out.println();

        System.out.println("Top Customers (PAID):");
        if (customerTotals.isEmpty()) {
            System.out.println("  No paid invoices yet.");
        } else {
            List<Map.Entry<String, Double>> entries = new ArrayList<>(customerTotals.entrySet());
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            int limit = Math.min(5, entries.size());
            for (int i = 0; i < limit; i++) {
                Map.Entry<String, Double> e = entries.get(i);
                System.out.printf("  %d) %s -> $%.2f%n", i + 1, e.getKey(), e.getValue());
            }
        }

        System.out.println();
        System.out.println("== Loan Analytics ==");
        System.out.println("Total Loans     : " + loansTotal);
        System.out.println("  Pending       : " + loansPending);
        System.out.println("  Active        : " + loansActive);
        System.out.println("  Closed        : " + loansClosed);
        System.out.println("  Rejected      : " + loansRejected);
        System.out.printf("Requested Total : $%.2f%n", totalLoanRequested);
        System.out.printf("Outstanding     : $%.2f%n", totalOutstanding);

        ConsoleUtil.pause(sc);
    }

    private String buildCustomerKey(String name, String email) {
        String n = (name == null || name.trim().isEmpty()) ? "Unknown" : name.trim();
        String e = (email == null || email.trim().isEmpty()) ? "-" : email.trim();
        return n + " <" + e + ">";
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }
}
