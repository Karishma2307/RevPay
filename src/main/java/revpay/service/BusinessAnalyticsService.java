package revpay.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private TransactionDao transactionDao = new TransactionDaoImpl();
    private InvoiceDao invoiceDao = new InvoiceDaoImpl();
    private LoanDao loanDao = new LoanDaoImpl();

    public void showAnalytics(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Business Analytics");

        long userId = businessUser.getUserId();

        // Load data
        List<Transaction> txns = transactionDao.findByUserId(userId);
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

            if (isIn) {
                totalInflow += amount;
            }
            if (isOut) {
                totalOutflow += amount;
            }

            if ("INVOICE_PAYMENT".equals(type) && isIn) {
                invoicePaymentsIn += amount;
            } else if ("LOAN_DISBURSEMENT".equals(type) && isIn) {
                loanDisbursementIn += amount;
            } else if ("LOAN_REPAYMENT".equals(type) && isOut) {
                loanRepaymentOut += amount;
            } else if ("WALLET_TOPUP".equals(type) && isIn) {
                walletTopupIn += amount;
            } else if ("TRANSFER".equals(type)) {
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

        Map<String, Double> customerTotals = new HashMap<String, Double>(); // customer -> total paid

        for (Invoice inv : invoices) {
            String status = inv.getStatus() == null ? "" : inv.getStatus().toUpperCase();
            double amt = inv.getTotalAmount();
            if ("PAID".equals(status)) {
                paidCount++;
                paidTotal += amt;

                String key = buildCustomerKey(inv.getCustomerName(), inv.getCustomerEmail());
                Double cur = customerTotals.get(key);
                if (cur == null) cur = 0.0;
                customerTotals.put(key, cur + amt);

            } else if ("PENDING".equals(status)) {
                pendingCount++;
                pendingTotal += amt;
            } else if ("CANCELLED".equals(status)) {
                cancelledCount++;
            } else {
                // ignore others
            }
        }

        // 3) Loans analytics
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

        // ---- Print analytics ----

        // Section 1: Transaction Summary
        System.out.println("== Transaction Summary ==");
        System.out.printf("Total Inflow (wallet credits) : $%.2f%n", totalInflow);
        System.out.printf("Total Outflow (wallet debits) : $%.2f%n", totalOutflow);
        System.out.println();

        System.out.println("Breakdown by Type (In/Out where relevant):");
        System.out.printf("  Invoice Payments In   : $%.2f%n", invoicePaymentsIn);
        System.out.printf("  Loan Disbursements In : $%.2f%n", loanDisbursementIn);
        System.out.printf("  Loan Repayments Out   : $%.2f%n", loanRepaymentOut);
        System.out.printf("  Wallet Topups In      : $%.2f%n", walletTopupIn);
        System.out.printf("  Transfers In          : $%.2f%n", transfersIn);
        System.out.printf("  Transfers Out         : $%.2f%n", transfersOut);
        System.out.println();

        // Section 2: Invoice Analytics
        System.out.println("== Invoice Analytics ==");
        System.out.println("Total Invoices    : " + totalInvoices);
        System.out.println("Paid Invoices     : " + paidCount + " (Total: $" + format2(paidTotal) + ")");
        System.out.println("Pending Invoices  : " + pendingCount + " (Total: $" + format2(pendingTotal) + ")");
        System.out.println("Cancelled Invoices: " + cancelledCount);
        System.out.println();

        // Top customers by paid amount
        System.out.println("Top Customers (by PAID invoice amount):");
        if (customerTotals.isEmpty()) {
            System.out.println("  No paid invoices yet.");
        } else {
            List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(customerTotals.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                    // descending
                    return Double.compare(e2.getValue(), e1.getValue());
                }
            });

            int limit = Math.min(5, entries.size());
            for (int i = 0; i < limit; i++) {
                Map.Entry<String, Double> e = entries.get(i);
                System.out.printf("  %d) %s  -> $%.2f%n", (i + 1), e.getKey(), e.getValue().doubleValue());
            }
        }
        System.out.println();

        // Section 3: Loan Analytics
        System.out.println("== Loan Analytics ==");
        System.out.println("Total Loans        : " + loansTotal);
        System.out.println("  Pending          : " + loansPending);
        System.out.println("  Active           : " + loansActive);
        System.out.println("  Closed           : " + loansClosed);
        System.out.println("  Rejected         : " + loansRejected);
        System.out.printf("Total Requested    : $%.2f%n", totalLoanRequested);
        System.out.printf("Total Outstanding  : $%.2f%n", totalOutstanding);
        System.out.println();

        ConsoleUtil.pause(sc);
    }

    private String buildCustomerKey(String name, String email) {
        String n = (name == null || name.trim().length() == 0) ? "Unknown" : name.trim();
        String e = (email == null || email.trim().length() == 0) ? "-" : email.trim();
        return n + " <" + e + ">";
    }

    private String format2(double v) {
        return String.format("%.2f", v);
    }
}
