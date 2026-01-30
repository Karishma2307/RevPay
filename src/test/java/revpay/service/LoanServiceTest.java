package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.LoanDao;
import revpay.dao.LoanRepaymentDao;
import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.model.Loan;
import revpay.model.LoanRepayment;
import revpay.model.User;
import revpay.model.Wallet;

@ExtendWith(MockitoExtension.class)   // ✅ REQUIRED
class LoanServiceTest {

    private LoanService service;

    // Mockito mocks
    private LoanDao loanDao;
    private LoanRepaymentDao repaymentDao;
    private WalletDao walletDao;
    private TransactionDao transactionDao;
    private NotificationService notificationService;
    private LowBalanceAlertService lowBalanceAlertService;

    private User businessUser;

    private String extraEnters() {
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

    // -------------------------------------------------
    // 1) applyForLoan – invalid amount
    // -------------------------------------------------
    @Test
    void applyForLoan_invalidAmount_shouldNotCreateLoan() {
        String input = "abc\n" + extraEnters();

        service.applyForLoan(new Scanner(input), businessUser);

        verify(loanDao, never()).createLoan(any());
        verify(notificationService, never()).notifyUser(anyLong(), any(), any(), any());
    }

    // -------------------------------------------------
    // 2) applyForLoan – success
    // -------------------------------------------------
    @Test
    void applyForLoan_success_shouldCreateLoanAndNotify() {
        when(loanDao.createLoan(any(Loan.class))).thenReturn(101L);

        String input = "5000\nInventory\n" + extraEnters();

        service.applyForLoan(new Scanner(input), businessUser);

        verify(loanDao).createLoan(argThat(l ->
                l.getBusinessUserId() == 10L &&
                l.getAmount() == 5000.0 &&
                l.getOutstandingAmount() == 5000.0 &&
                "PENDING".equals(l.getStatus())
        ));

        verify(notificationService).notifyUser(
                eq(10L),
                eq("LOAN"),
                eq("Loan Applied"),
                contains("Loan #101")
        );
    }

    // -------------------------------------------------
    // 3) viewLoans – empty
    // -------------------------------------------------
    @Test
    void viewLoans_empty_shouldQueryDao() {
        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.emptyList());

        service.viewLoans(new Scanner(extraEnters()), businessUser);

        verify(loanDao).findByBusinessUser(10L);
    }

    // -------------------------------------------------
    // 4) makeRepayment – loan not found
    // -------------------------------------------------
    @Test
    void makeRepayment_loanNotFound_shouldExitEarly() {
        when(loanDao.findByBusinessUser(10L)).thenReturn(Arrays.asList(new Loan()));
        when(loanDao.findById(99L)).thenReturn(null);

        String input = "99\n" + extraEnters();

        service.makeRepayment(new Scanner(input), businessUser);

        verify(walletDao, never()).updateBalance(anyLong(), anyDouble());
        verify(repaymentDao, never()).createRepayment(any());
        verify(transactionDao, never()).createTransaction(anyLong(), anyLong(), anyDouble(), any(), any(), any(), any());
    }

    // -------------------------------------------------
    // 5) makeRepayment – success
    // -------------------------------------------------
    @Test
    void makeRepayment_success_shouldUpdateEverything() {
        when(loanDao.findByBusinessUser(10L)).thenReturn(Arrays.asList(new Loan()));

        Loan loan = new Loan();
        loan.setLoanId(5L);
        loan.setBusinessUserId(10L);
        loan.setOutstandingAmount(300.0);
        loan.setStatus("ACTIVE");

        when(loanDao.findById(5L)).thenReturn(loan);

        Wallet wallet = new Wallet();
        wallet.setBalance(1000.0);
        when(walletDao.getWalletByUserId(10L)).thenReturn(wallet);

        String input = "5\n100\n" + extraEnters();

        service.makeRepayment(new Scanner(input), businessUser);

        verify(walletDao).updateBalance(10L, 900.0);
        verify(lowBalanceAlertService).checkAndNotify(10L);

        verify(loanDao).updateLoan(argThat(l ->
                l.getOutstandingAmount() == 200.0 &&
                "ACTIVE".equals(l.getStatus())
        ));

        verify(repaymentDao).createRepayment(any(LoanRepayment.class));
        verify(transactionDao).createTransaction(
                eq(10L),
                eq(0L),
                eq(100.0),
                eq("LOAN_REPAYMENT"),
                eq("SUCCESS"),
                contains("loan #5"),
                eq("LOANPAY:5")
        );

        verify(notificationService).notifyUser(
                eq(10L),
                eq("LOAN"),
                eq("Loan Repayment"),
                contains("Outstanding")
        );
    }

    // -------------------------------------------------
    // Reflection helper
    // -------------------------------------------------
    private void inject(Object target, String field, Object mock) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, mock);
    }
}
