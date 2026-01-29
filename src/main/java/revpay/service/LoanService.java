package revpay.service;

import java.util.List;
import java.util.Scanner;

import revpay.dao.LoanDao;
import revpay.dao.LoanRepaymentDao;
import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.LoanDaoImpl;
import revpay.dao.impl.LoanRepaymentDaoImpl;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Loan;
import revpay.model.LoanRepayment;
import revpay.model.Transaction;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class LoanService {

    private LoanDao loanDao = new LoanDaoImpl();
    private LoanRepaymentDao repaymentDao = new LoanRepaymentDaoImpl();
    private WalletDao walletDao = new WalletDaoImpl();
    private TransactionDao transactionDao = new TransactionDaoImpl();
    private NotificationService notificationService;

    public LoanService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void manageLoans(Scanner sc, User businessUser) {
        while (true) {
            ConsoleUtil.printHeader("Business Loans");
            System.out.println("1. Apply for Loan");
            System.out.println("2. View My Loans");
            System.out.println("3. Disburse/Activate Approved Loan (simulate approval)");
            System.out.println("4. Make Repayment");
            System.out.println("5. Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                applyForLoan(sc, businessUser);
            } else if ("2".equals(choice)) {
                viewLoans(sc, businessUser);
            } else if ("3".equals(choice)) {
                activateLoan(sc, businessUser);
            } else if ("4".equals(choice)) {
                repayLoan(sc, businessUser);
            } else if ("5".equals(choice)) {
                break;
            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private void applyForLoan(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Apply for Business Loan");

        System.out.print("Loan amount (USD): ");
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

        System.out.print("Interest rate (% per year, e.g. 10): ");
        String rateStr = sc.nextLine();
        double rate;
        try {
            rate = Double.parseDouble(rateStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid rate.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Term (months): ");
        String termStr = sc.nextLine();
        int term;
        try {
            term = Integer.parseInt(termStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid term.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Purpose / Business use: ");
        String purpose = sc.nextLine();

        Loan loan = new Loan();
        loan.setBusinessUserId(businessUser.getUserId());
        loan.setAmount(amount);
        loan.setInterestRate(rate);
        loan.setTermMonths(term);
        loan.setPurpose(purpose);
        loan.setStatus("PENDING");
        loan.setApprovedAmount(0.0);
        loan.setOutstandingAmount(0.0);

        long id = loanDao.createLoan(loan);
        if (id <= 0) {
            System.out.println("[ERROR] Failed to create loan application.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("[INFO] Loan application submitted.");
        System.out.println("Loan ID   : " + id);
        System.out.printf("Amount    : $%.2f%n", amount);
        System.out.printf("Interest  : %.2f%%%n", rate);
        System.out.println("Term      : " + term + " months");
        System.out.println("Status    : PENDING");

        if (notificationService != null) {
            notificationService.notifyUser(businessUser.getUserId(), "LOAN",
                    "Loan Application Submitted",
                    "Loan #" + id + " submitted for amount $" + amount + ".");
        }

        ConsoleUtil.pause(sc);
    }

    private void viewLoans(Scanner sc, User businessUser) {
        List<Loan> list = loanDao.findByBusinessUser(businessUser.getUserId());
        ConsoleUtil.printHeader("My Loans");
        if (list.isEmpty()) {
            System.out.println("No loans found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("ID   | Amount   | Status    | Rate   | Term | Outstanding");
        System.out.println("--------------------------------------------------------------");
        for (Loan l : list) {
            System.out.printf("%-4d | $%-7.2f | %-9s | %-6.2f | %-4d | $%-10.2f%n",
                    l.getLoanId(),
                    l.getAmount(),
                    l.getStatus(),
                    l.getInterestRate(),
                    l.getTermMonths(),
                    l.getOutstandingAmount());
        }

        System.out.println("--------------------------------------------------------------");
        System.out.print("Enter Loan ID to view details (or 0 to back): ");
        String idStr = sc.nextLine();
        long loanId;
        try {
            loanId = Long.parseLong(idStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (loanId == 0) {
            return;
        }

        Loan loan = loanDao.findById(loanId);
        if (loan == null || loan.getBusinessUserId() != businessUser.getUserId()) {
            System.out.println("[ERROR] Loan not found for this business user.");
            ConsoleUtil.pause(sc);
            return;
        }

        showLoanDetails(sc, loan);
    }

    private void showLoanDetails(Scanner sc, Loan loan) {
        ConsoleUtil.printHeader("Loan #" + loan.getLoanId());
        System.out.printf("Amount requested : $%.2f%n", loan.getAmount());
        System.out.printf("Interest rate    : %.2f%%%n", loan.getInterestRate());
        System.out.println("Term (months)    : " + loan.getTermMonths());
        System.out.println("Purpose          : " + loan.getPurpose());
        System.out.println("Status           : " + loan.getStatus());
        System.out.printf("Approved amount  : $%.2f%n", loan.getApprovedAmount());
        System.out.printf("Outstanding      : $%.2f%n", loan.getOutstandingAmount());
        System.out.println("------------------------------------------------");

        List<LoanRepayment> reps = repaymentDao.findByLoanId(loan.getLoanId());
        if (reps.isEmpty()) {
            System.out.println("No repayments yet.");
        } else {
            System.out.println("Repayments:");
            System.out.println("ID   | Amount   | Paid At");
            System.out.println("-------------------------------");
            for (LoanRepayment r : reps) {
                String dateStr = (r.getPaidAt() == null) ? "" : r.getPaidAt().toString();
                System.out.printf("%-4d | $%-7.2f | %s%n",
                        r.getRepaymentId(),
                        r.getAmount(),
                        dateStr);
            }
        }

        ConsoleUtil.pause(sc);
    }

    private void activateLoan(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Activate / Disburse Loan");
        System.out.println("This simulates that the bank approved the loan and funds are transferred to your wallet.");

        System.out.print("Enter Loan ID to activate: ");
        String idStr = sc.nextLine();
        long loanId;
        try {
            loanId = Long.parseLong(idStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        Loan loan = loanDao.findById(loanId);
        if (loan == null || loan.getBusinessUserId() != businessUser.getUserId()) {
            System.out.println("[ERROR] Loan not found for this business user.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(loan.getStatus())) {
            System.out.println("[INFO] Only PENDING loans can be activated.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet = walletDao.findByUserId(businessUser.getUserId());
        if (wallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        double approved = loan.getAmount(); // simple: approve full amount
        double totalToRepay = approved + (approved * (loan.getInterestRate() / 100.0)); // principal + simple interest

        double newBalance = wallet.getBalance() + approved;
        walletDao.updateBalance(wallet.getWalletId(), newBalance);
        wallet.setBalance(newBalance);

        loan.setStatus("ACTIVE");
        loan.setApprovedAmount(approved);
        loan.setOutstandingAmount(totalToRepay);
        loanDao.updateLoan(loan);

        Transaction t = new Transaction();
        t.setFromUserId(null); // bank/external
        t.setToUserId(businessUser.getUserId());
        t.setAmount(approved);
        t.setCurrency("USD");
        t.setType("LOAN_DISBURSEMENT");
        t.setStatus("SUCCESS");
        t.setNote("Loan #" + loan.getLoanId());
        long txnId = transactionDao.createTransaction(t);

        System.out.println("[INFO] Loan activated and funds credited to wallet.");
        System.out.println("Loan ID        : " + loan.getLoanId());
        System.out.printf("Approved amount: $%.2f%n", approved);
        System.out.printf("Total to repay : $%.2f%n", totalToRepay);
        System.out.println("Transaction ID : " + txnId);
        System.out.printf("New wallet bal : $%.2f%n", newBalance);

        if (notificationService != null) {
            notificationService.notifyUser(businessUser.getUserId(), "LOAN",
                    "Loan Activated",
                    "Loan #" + loan.getLoanId() + " activated. Funds $" + approved + " credited to wallet.");
        }

        ConsoleUtil.pause(sc);
    }

    private void repayLoan(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Loan Repayment");
        System.out.print("Enter Loan ID to repay: ");
        String idStr = sc.nextLine();
        long loanId;
        try {
            loanId = Long.parseLong(idStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        Loan loan = loanDao.findById(loanId);
        if (loan == null || loan.getBusinessUserId() != businessUser.getUserId()) {
            System.out.println("[ERROR] Loan not found for this business user.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(loan.getStatus())) {
            System.out.println("[INFO] Only ACTIVE loans can be repaid.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet = walletDao.findByUserId(businessUser.getUserId());
        if (wallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.printf("Outstanding amount: $%.2f%n", loan.getOutstandingAmount());
        System.out.printf("Wallet balance     : $%.2f%n", wallet.getBalance());
        System.out.print("Repayment amount   : ");
        String amtStr = sc.nextLine();
        double pay;
        try {
            pay = Double.parseDouble(amtStr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (pay <= 0) {
            System.out.println("[ERROR] Amount must be positive.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (pay > wallet.getBalance()) {
            System.out.println("[ERROR] Not enough wallet balance to make this repayment.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (pay > loan.getOutstandingAmount()) {
            pay = loan.getOutstandingAmount(); // cap to outstanding
        }

        double newWalletBal = wallet.getBalance() - pay;
        walletDao.updateBalance(wallet.getWalletId(), newWalletBal);
        wallet.setBalance(newWalletBal);

        double newOutstanding = loan.getOutstandingAmount() - pay;
        loan.setOutstandingAmount(newOutstanding);
        if (newOutstanding <= 0.005) {
            loan.setOutstandingAmount(0.0);
            loan.setStatus("CLOSED");
        }
        loanDao.updateLoan(loan);

        LoanRepayment rep = new LoanRepayment();
        rep.setLoanId(loan.getLoanId());
        rep.setAmount(pay);
        repaymentDao.createRepayment(rep);

        Transaction t = new Transaction();
        t.setFromUserId(businessUser.getUserId());
        t.setToUserId(null); // bank/external
        t.setAmount(pay);
        t.setCurrency("USD");
        t.setType("LOAN_REPAYMENT");
        t.setStatus("SUCCESS");
        t.setNote("Loan #" + loan.getLoanId());
        long txnId = transactionDao.createTransaction(t);

        System.out.println("[INFO] Repayment successful.");
        System.out.println("Loan ID        : " + loan.getLoanId());
        System.out.printf("Paid           : $%.2f%n", pay);
        System.out.printf("New outstanding: $%.2f%n", loan.getOutstandingAmount());
        System.out.printf("New wallet bal : $%.2f%n", newWalletBal);
        System.out.println("Transaction ID : " + txnId);

        if (notificationService != null) {
            notificationService.notifyUser(businessUser.getUserId(), "LOAN",
                    "Loan Repayment",
                    "You repaid $" + pay + " for Loan #" + loan.getLoanId() + ".");
        }

        ConsoleUtil.pause(sc);
    }
}
