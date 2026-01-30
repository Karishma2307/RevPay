package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import revpay.dao.LoanDao;
import revpay.dao.LoanRepaymentDao;
import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.model.Loan;
import revpay.model.LoanRepayment;
import revpay.model.User;
import revpay.model.Wallet;

class LoanServiceTest {

    private LoanService service;

    // Mocks
    private LoanDao loanDao;
    private LoanRepaymentDao repaymentDao;
    private WalletDao walletDao;
    private TransactionDao transactionDao;
    private NotificationService notificationService;
    private LowBalanceAlertService lowBalanceAlertService;

    private User businessUser;

    private String extraEnters() {
        // avoids NoSuchElementException from ConsoleUtil.pause(sc)
        return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
    }

    @BeforeEach
    void setup() throws Exception {
        service = new LoanService();

        loanDao = mock(LoanDao.class);
        repaymentDao = mock(LoanRepaymentDao.class);
        walletDao = mock(WalletDao.class);
        transactionDao = mock(TransactionDao.class);
        notificationService = mock(NotificationService.class);
        lowBalanceAlertService = mock(LowBalanceAlertService.class);

        // Inject mocks into LoanService private final fields
        inject(service, "loanDao", loanDao);
        inject(service, "repaymentDao", repaymentDao);
        inject(service, "walletDao", walletDao);
        inject(service, "transactionDao", transactionDao);
        inject(service, "notificationService", notificationService);
        inject(service, "lowBalanceAlertService", lowBalanceAlertService);

        businessUser = new User();
        businessUser.setUserId(10L);
        businessUser.setFullName("Biz User");
    }

    // -------------------------
    // TEST 1: applyForLoan invalid amount -> should not create loan
    // -------------------------
    @Test
    void applyForLoan_invalidAmount_shouldNotCreateLoan() {
        String input =
                "abc\n" +          // invalid amount
                extraEnters();

        service.applyForLoan(new Scanner(input), businessUser);

        verify(loanDao, never()).createLoan(any());
        verify(notificationService, never()).notifyUser(anyLong(), any(), any(), any());
    }

    // -------------------------
    // TEST 2: applyForLoan success -> creates loan and sends notification
    // -------------------------
    @Test
    void applyForLoan_success_shouldCreateLoan_andNotify() {
        when(loanDao.createLoan(any(Loan.class))).thenReturn(101L);

        String input =
                "5000\n" +         // amount
                "Inventory\n" +    // purpose
                extraEnters();

        service.applyForLoan(new Scanner(input), businessUser);

        verify(loanDao, times(1)).createLoan(argThat(l ->
                l.getBusinessUserId() == 10L &&
                l.getAmount() == 5000.0 &&
                "Inventory".equals(l.getPurpose()) &&
                "PENDING".equals(l.getStatus()) &&
                l.getOutstandingAmount() == 5000.0
        ));

        verify(notificationService).notifyUser(eq(10L), eq("LOAN"), eq("Loan Applied"), contains("Loan #101"));
    }

    // -------------------------
    // TEST 3: viewLoans empty -> prints "No loans found." and pauses
    // -------------------------
    @Test
    void viewLoans_empty_shouldCallFindByBusinessUser() {
        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.emptyList());

        service.viewLoans(new Scanner(extraEnters()), businessUser);

        verify(loanDao).findByBusinessUser(10L);
    }

    // -------------------------
    // TEST 4: makeRepayment loan not found for user -> exits
    // -------------------------
    @Test
    void makeRepayment_loanNotFoundForUser_shouldExitNoUpdates() {
        // viewLoans() calls findByBusinessUser first
        when(loanDao.findByBusinessUser(10L)).thenReturn(Arrays.asList(new Loan()));

        // user enters loanId
        String input =
                "999\n" +   // loan id
                extraEnters();

        when(loanDao.findById(999L)).thenReturn(null);

        service.makeRepayment(new Scanner(input), businessUser);

        verify(loanDao).findById(999L);
        verify(walletDao, never()).updateBalance(anyLong(), anyDouble());
        verify(loanDao, never()).updateLoan(any());
        verify(repaymentDao, never()).createRepayment(any(LoanRepayment.class));
        verify(transactionDao, never()).createTransaction(anyLong(), anyLong(), anyDouble(), any(), any(), any(), any());
    }

    // -------------------------
    // TEST 5: makeRepayment success -> wallet deducted, loan updated, repayment + transaction created
    // -------------------------
    @Test
    void makeRepayment_success_shouldUpdateWalletLoanAndCreateRecords() {
        // viewLoans() first
        when(loanDao.findByBusinessUser(10L)).thenReturn(Arrays.asList(new Loan()));

        Loan loan = new Loan();
        loan.setLoanId(5L);
        loan.setBusinessUserId(10L);
        loan.setAmount(1000.0);
        loan.setOutstandingAmount(300.0);
        loan.setStatus("ACTIVE");
        loan.setPurpose("Test");

        when(loanDao.findById(5L)).thenReturn(loan);

        Wallet wallet = new Wallet();
        wallet.setUserId(10L);
        wallet.setBalance(1000.0);
        when(walletDao.getWalletByUserId(10L)).thenReturn(wallet);

        String input =
                "5\n" +     // loan id
                "100\n" +   // repay amount
                extraEnters();

        service.makeRepayment(new Scanner(input), businessUser);

        // wallet updated: 1000 - 100 = 900
        verify(walletDao).updateBalance(10L, 900.0);

        // low balance alert called
        verify(lowBalanceAlertService).checkAndNotify(10L);

        // loan outstanding reduced: 300 - 100 = 200 (ACTIVE)
        verify(loanDao).updateLoan(argThat(l ->
                l.getLoanId() == 5L &&
                l.getOutstandingAmount() == 200.0 &&
                "ACTIVE".equalsIgnoreCase(l.getStatus())
        ));

        // repayment record created
        verify(repaymentDao).createRepayment(argThat(r ->
                r.getLoanId() == 5L &&
                r.getAmount() == 100.0 &&
                "SUCCESS".equalsIgnoreCase(r.getStatus())
        ));

        // transaction created
        verify(transactionDao).createTransaction(
                eq(10L),
                eq(0L),
                eq(100.0),
                eq("LOAN_REPAYMENT"),
                eq("SUCCESS"),
                contains("loan #5"),
                eq("LOANPAY:5")
        );

        // notification created
        verify(notificationService).notifyUser(eq(10L), eq("LOAN"), eq("Loan Repayment"), contains("Outstanding"));
    }

    // -------------------------
    // Reflection inject helper
    // -------------------------
    private void inject(Object target, String fieldName, Object mock) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, mock);
    }
}
