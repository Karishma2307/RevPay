package revpay.service;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    private final LoanDao loanDao = new LoanDaoImpl();
    private final LoanRepaymentDao repaymentDao = new LoanRepaymentDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final LowBalanceAlertService lowBalanceAlertService = new LowBalanceAlertService();

    public void applyLoan(Scanner sc, User businessUser) {
        applyForLoan(sc, businessUser);
    }

    public void repayLoan(Scanner sc, User businessUser) {
        makeRepayment(sc, businessUser);
    }

    public void applyForLoan(Scanner sc, User businessUser) {

        ConsoleUtil.printHeader("Apply for Business Loan");

        if (businessUser == null) {
            logger.warn("applyForLoan called with null businessUser");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = businessUser.getUserId();
        logger.info("Loan application started (businessUserId={}, username='{}')", userId, safe(businessUser.getUsername()));

        System.out.print("Loan Amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Loan application failed: invalid amount input (businessUserId={})", userId);
            System.out.println("Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (amount <= 0) {
            logger.warn("Loan application failed: amount <= 0 (businessUserId={}, amount={})", userId, fmt(amount));
            System.out.println("Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Purpose: ");
        String purpose = sc.nextLine();

        Loan loan = new Loan();
        loan.setBusinessUserId(userId);
        loan.setAmount(amount);
        loan.setPurpose(purpose);
        loan.setStatus("PENDING");
        loan.setOutstandingAmount(amount);

        long loanId;
        try {
            loanId = loanDao.createLoan(loan);
        } catch (Exception e) {
            logger.error("Loan application failed: DAO error while creating loan (businessUserId={}, amount={})",
                    userId, fmt(amount), e);
            System.out.println("Failed to apply for loan.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Loan created (loanId={}, businessUserId={}, amount={}, status=PENDING)", loanId, userId, fmt(amount));

        try {
            notificationService.notifyUser(
                    userId,
                    "LOAN",
                    "Loan Applied",
                    "Loan #" + loanId + " applied for ₹" + amount
            );
        } catch (Exception e) {
            logger.error("Failed to send loan applied notification (loanId={}, businessUserId={})", loanId, userId, e);
        }

        System.out.println("Loan application created. Loan ID: " + loanId + " (Status: PENDING)");
        ConsoleUtil.pause(sc);
    }

    public void viewLoans(Scanner sc, User businessUser) {

        ConsoleUtil.printHeader("My Loans");

        if (businessUser == null) {
            logger.warn("viewLoans called with null businessUser");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = businessUser.getUserId();

        List<Loan> loans;
        try {
            loans = loanDao.findByBusinessUser(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch loans list (businessUserId={})", userId, e);
            System.out.println("Unable to load loans right now.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (loans == null || loans.isEmpty()) {
            logger.info("No loans found (businessUserId={})", userId);
            System.out.println("No loans found.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.debug("Loans fetched (businessUserId={}, count={})", userId, loans.size());

        System.out.println("ID | Amount | Outstanding | Status | Purpose");
        System.out.println("---------------------------------------------");

        for (Loan l : loans) {
            System.out.printf("%d | %.2f | %.2f | %s | %s%n",
                    l.getLoanId(),
                    l.getAmount(),
                    l.getOutstandingAmount(),
                    l.getStatus(),
                    l.getPurpose());
        }
    }

    public void makeRepayment(Scanner sc, User businessUser) {

        ConsoleUtil.printHeader("Loan Repayment");

        if (businessUser == null) {
            logger.warn("makeRepayment called with null businessUser");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = businessUser.getUserId();
        logger.info("Loan repayment started (businessUserId={}, username='{}')", userId, safe(businessUser.getUsername()));

        viewLoans(sc, businessUser);
        System.out.println();

        System.out.print("Enter Loan ID to repay: ");
        long loanId;
        try {
            loanId = Long.parseLong(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Loan repayment failed: invalid loanId input (businessUserId={})", userId);
            System.out.println("Invalid Loan ID.");
            ConsoleUtil.pause(sc);
            return;
        }

        Loan loan;
        try {
            loan = loanDao.findById(loanId);
        } catch (Exception e) {
            logger.error("Loan repayment failed: DAO error fetching loan (loanId={}, businessUserId={})",
                    loanId, userId, e);
            System.out.println("Unable to fetch loan.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (loan == null || loan.getBusinessUserId() != userId) {
            logger.warn("Loan repayment failed: loan not found for user (loanId={}, businessUserId={})", loanId, userId);
            System.out.println("Loan not found for this business user.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(loan.getStatus()) && !"PENDING".equalsIgnoreCase(loan.getStatus())) {
            logger.warn("Loan repayment blocked: invalid loan status (loanId={}, businessUserId={}, status={})",
                    loanId, userId, loan.getStatus());
            System.out.println("Only ACTIVE/PENDING loans can be repaid in this demo.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Repayment Amount: ");
        double pay;
        try {
            pay = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            logger.warn("Loan repayment failed: invalid amount input (loanId={}, businessUserId={})", loanId, userId);
            System.out.println("Invalid amount.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (pay <= 0) {
            logger.warn("Loan repayment failed: amount <= 0 (loanId={}, businessUserId={}, amount={})",
                    loanId, userId, fmt(pay));
            System.out.println("Amount must be > 0.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet;
        try {
            wallet = walletDao.getWalletByUserId(userId);
        } catch (Exception e) {
            logger.error("Loan repayment failed: DAO error fetching wallet (businessUserId={})", userId, e);
            System.out.println("Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (wallet == null) {
            logger.error("Loan repayment failed: wallet is null (businessUserId={})", userId);
            System.out.println("Wallet not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (wallet.getBalance() < pay) {
            logger.warn("Loan repayment failed: insufficient balance (businessUserId={}, balance={}, pay={})",
                    userId, fmt(wallet.getBalance()), fmt(pay));
            System.out.println("Insufficient wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newWalletBal = wallet.getBalance() - pay;

        try {
            walletDao.updateBalance(userId, newWalletBal);
        } catch (Exception e) {
            logger.error("Loan repayment failed: wallet update failed (businessUserId={}, newBalance={})",
                    userId, fmt(newWalletBal), e);
            System.out.println("Failed to update wallet balance.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            lowBalanceAlertService.checkAndNotify(userId);
        } catch (Exception e) {
            logger.error("Low balance check failed after repayment (businessUserId={})", userId, e);
        }

        double newOutstanding = Math.max(0, loan.getOutstandingAmount() - pay);
        loan.setOutstandingAmount(newOutstanding);
        loan.setStatus(newOutstanding == 0 ? "CLOSED" : "ACTIVE");

        try {
            loanDao.updateLoan(loan);
        } catch (Exception e) {
            logger.error("Loan repayment failed: loan update failed (loanId={}, businessUserId={}, newOutstanding={}, status={})",
                    loanId, userId, fmt(newOutstanding), loan.getStatus(), e);
            System.out.println("Failed to update loan status.");
            ConsoleUtil.pause(sc);
            return;
        }

        LoanRepayment r = new LoanRepayment();
        r.setLoanId(loanId);
        r.setAmount(pay);
        r.setStatus("SUCCESS");

        try {
            repaymentDao.createRepayment(r);
        } catch (Exception e) {
            logger.error("Loan repayment failed: repayment record creation failed (loanId={}, businessUserId={})",
                    loanId, userId, e);
            System.out.println("Failed to save repayment record.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            transactionDao.createTransaction(
                    userId,
                    0L,
                    pay,
                    "LOAN_REPAYMENT",
                    "SUCCESS",
                    "Loan repayment for loan #" + loanId
            );
        } catch (Exception e) {
            logger.error("Loan repayment failed: transaction creation failed (loanId={}, businessUserId={})",
                    loanId, userId, e);
            System.out.println("Failed to save repayment transaction.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            notificationService.notifyUser(
                    userId,
                    "LOAN",
                    "Loan Repayment",
                    "Paid ₹" + pay + " for Loan #" + loanId + ". Outstanding: ₹" + newOutstanding
            );
        } catch (Exception e) {
            logger.error("Failed to send loan repayment notification (loanId={}, businessUserId={})",
                    loanId, userId, e);
        }

        logger.info("Loan repayment successful (loanId={}, businessUserId={}, pay={}, newWalletBal={}, newOutstanding={}, loanStatus={})",
                loanId, userId, fmt(pay), fmt(newWalletBal), fmt(newOutstanding), loan.getStatus());

        System.out.println("Repayment successful. New wallet balance: " + newWalletBal);
        System.out.println("Outstanding amount: " + newOutstanding + " | Status: " + loan.getStatus());
        ConsoleUtil.pause(sc);
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
