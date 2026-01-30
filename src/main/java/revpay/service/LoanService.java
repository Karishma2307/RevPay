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
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class LoanService {

    private final LoanDao loanDao = new LoanDaoImpl();
    private final LoanRepaymentDao repaymentDao = new LoanRepaymentDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final NotificationService notificationService = new NotificationService();

    // ✅ Step 5: low balance alerts
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    // Wrappers for BusinessMenu
    public void applyLoan(Scanner sc, User businessUser) {
        applyForLoan(sc, businessUser);
    }

    public void repayLoan(Scanner sc, User businessUser) {
        makeRepayment(sc, businessUser);
    }

    // ===== Apply
    public void applyForLoan(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Apply for Business Loan");

        System.out.print("Loan Amount: ");
        double amount;
        try { amount = Double.parseDouble(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid amount."); ConsoleUtil.pause(sc); return; }

        if (amount <= 0) {
            System.out.println("[ERROR] Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Purpose: ");
        String purpose = sc.nextLine();

        Loan loan = new Loan();
        loan.setBusinessUserId(businessUser.getUserId());
        loan.setAmount(amount);
        loan.setPurpose(purpose);
        loan.setStatus("PENDING");
        loan.setOutstandingAmount(amount);

        long loanId = loanDao.createLoan(loan);

        notificationService.notifyUser(businessUser.getUserId(), "LOAN", "Loan Applied",
                "Loan #" + loanId + " applied for $" + amount);

        System.out.println("[INFO] Loan application created. Loan ID: " + loanId + " (Status: PENDING)");
        ConsoleUtil.pause(sc);
    }

    // ===== View
    public void viewLoans(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("My Loans");
        List<Loan> loans = loanDao.findByBusinessUser(businessUser.getUserId());

        if (loans == null || loans.isEmpty()) {
            System.out.println("No loans found.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.println("ID | Amount | Outstanding | Status | Purpose");
        System.out.println("---------------------------------------------");
        for (Loan l : loans) {
            System.out.printf("%d | %.2f | %.2f | %s | %s%n",
                    l.getLoanId(), l.getAmount(), l.getOutstandingAmount(), l.getStatus(), l.getPurpose());
        }
        // do NOT pause here because repayment flow calls viewLoans then continues
    }

    // ===== Repay
    public void makeRepayment(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Loan Repayment");

        viewLoans(sc, businessUser);
        System.out.println();
        System.out.print("Enter Loan ID to repay: ");
        long loanId;
        try { loanId = Long.parseLong(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid Loan ID."); ConsoleUtil.pause(sc); return; }

        Loan loan = loanDao.findById(loanId);
        if (loan == null || loan.getBusinessUserId() != businessUser.getUserId()) {
            System.out.println("[ERROR] Loan not found for this business user.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(loan.getStatus()) && !"PENDING".equalsIgnoreCase(loan.getStatus())) {
            System.out.println("[ERROR] Only ACTIVE/PENDING loans can be repaid in this demo.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Repayment Amount: ");
        double pay;
        try { pay = Double.parseDouble(sc.nextLine()); }
        catch (Exception e) { System.out.println("[ERROR] Invalid amount."); ConsoleUtil.pause(sc); return; }

        if (pay <= 0) {
            System.out.println("[ERROR] Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet = walletDao.getWalletByUserId(businessUser.getUserId());
        if (wallet == null) {
            System.out.println("[ERROR] Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (wallet.getBalance() < pay) {
            System.out.println("[ERROR] Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newWalletBal = wallet.getBalance() - pay;
        walletDao.updateBalance(businessUser.getUserId(), newWalletBal);

        // ✅ Step 5: low balance alert after deduction
        lowBalanceAlertService.checkAndNotify(businessUser.getUserId());

        double newOutstanding = Math.max(0, loan.getOutstandingAmount() - pay);
        loan.setOutstandingAmount(newOutstanding);

        if (newOutstanding == 0) loan.setStatus("CLOSED");
        else loan.setStatus("ACTIVE");

        loanDao.updateLoan(loan);

        LoanRepayment r = new LoanRepayment();
        r.setLoanId(loanId);
        r.setAmount(pay);
        r.setStatus("SUCCESS");
        repaymentDao.createRepayment(r);

        transactionDao.createTransaction(
                businessUser.getUserId(),
                0L,
                pay,
                "LOAN_REPAYMENT",
                "SUCCESS",
                "Loan repayment for loan #" + loanId,
                "LOANPAY:" + loanId
        );

        notificationService.notifyUser(businessUser.getUserId(), "LOAN", "Loan Repayment",
                "Paid $" + pay + " for Loan #" + loanId + ". Outstanding: $" + newOutstanding);

        System.out.println("[INFO] Repayment successful. New wallet balance: " + newWalletBal);
        System.out.println("[INFO] Outstanding amount: " + newOutstanding + " | Status: " + loan.getStatus());
        ConsoleUtil.pause(sc);
    }
}
