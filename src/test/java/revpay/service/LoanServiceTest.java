package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
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
import revpay.model.User;
import revpay.model.Wallet;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private LoanService service;

    private LoanDao loanDao;
    private LoanRepaymentDao repaymentDao;
    private WalletDao walletDao;
    private TransactionDao transactionDao;
    private NotificationService notificationService;
    private LowBalanceAlertService lowBalanceAlertService;

    private User businessUser;

    
    private String manyEnters() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("\n");
        return sb.toString();
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
    }

    // --------------------------------------------------
    // TEST 1: applyForLoan → success
    // --------------------------------------------------
    @Test
    void applyForLoan_success() {

        when(loanDao.createLoan(any(Loan.class))).thenReturn(101L);

        String input =
                "5000\n" +                 
                "Business expansion\n" +    
                manyEnters();               

        service.applyForLoan(new Scanner(input), businessUser);

        verify(loanDao).createLoan(any(Loan.class));
        verify(notificationService).notifyUser(
                eq(10L),
                eq("LOAN"),
                eq("Loan Applied"),
                contains("Loan #101")
        );
    }

    // --------------------------------------------------
    // TEST 2: viewLoans → no loans
    // (viewLoans has ConsoleUtil.pause(sc) when empty)
    // --------------------------------------------------
    @Test
    void viewLoans_noLoans() {

        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.emptyList());

        service.viewLoans(new Scanner(manyEnters()), businessUser);

        verify(loanDao).findByBusinessUser(10L);
    }

    // --------------------------------------------------
    // TEST 3: makeRepayment → insufficient balance
    // (makeRepayment ends with ConsoleUtil.pause(sc))
    // --------------------------------------------------
    @Test
    void makeRepayment_insufficientBalance() {

        Loan loan = new Loan();
        loan.setLoanId(1L);
        loan.setBusinessUserId(10L);
        loan.setOutstandingAmount(5000);
        loan.setStatus("ACTIVE");

        Wallet wallet = new Wallet();
        wallet.setBalance(1000);

        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.singletonList(loan));
        when(loanDao.findById(1L)).thenReturn(loan);
        when(walletDao.getWalletByUserId(10L)).thenReturn(wallet);

        String input =
                "1\n" +      
                "3000\n" +   
                manyEnters(); 

        service.makeRepayment(new Scanner(input), businessUser);

        verify(walletDao, never()).updateBalance(anyLong(), anyDouble());
        verify(transactionDao, never()).createTransaction(anyLong(), anyLong(), anyDouble(), any(), any(), any());
    }

    // --------------------------------------------------
    // TEST 4: makeRepayment → full repayment (CLOSED)
    // --------------------------------------------------
    @Test
    void makeRepayment_fullRepayment_shouldCloseLoan() {

        Loan loan = new Loan();
        loan.setLoanId(2L);
        loan.setBusinessUserId(10L);
        loan.setOutstandingAmount(2000);
        loan.setStatus("ACTIVE");

        Wallet wallet = new Wallet();
        wallet.setBalance(3000);

        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.singletonList(loan));
        when(loanDao.findById(2L)).thenReturn(loan);
        when(walletDao.getWalletByUserId(10L)).thenReturn(wallet);

        String input =
                "2\n" +      
                "2000\n" +   
                manyEnters(); 
        service.makeRepayment(new Scanner(input), businessUser);

        verify(walletDao).updateBalance(10L, 1000.0);

        verify(transactionDao).createTransaction(
                eq(10L),
                eq(0L),
                eq(2000.0),
                eq("LOAN_REPAYMENT"),
                eq("SUCCESS"),
                contains("loan #2")
        );
    }

    // --------------------------------------------------
    // TEST 5: makeRepayment → partial repayment (ACTIVE)
    // --------------------------------------------------
    @Test
    void makeRepayment_partialRepayment_shouldRemainActive() {

        Loan loan = new Loan();
        loan.setLoanId(3L);
        loan.setBusinessUserId(10L);
        loan.setOutstandingAmount(5000);
        loan.setStatus("ACTIVE");

        Wallet wallet = new Wallet();
        wallet.setBalance(6000);

        when(loanDao.findByBusinessUser(10L)).thenReturn(Collections.singletonList(loan));
        when(loanDao.findById(3L)).thenReturn(loan);
        when(walletDao.getWalletByUserId(10L)).thenReturn(wallet);

        String input =
                "3\n" +      
                "2000\n" +   
                manyEnters(); 

        service.makeRepayment(new Scanner(input), businessUser);

        verify(walletDao).updateBalance(10L, 4000.0);

        verify(loanDao).updateLoan(argThat(l ->
                l.getOutstandingAmount() == 3000.0 &&
                "ACTIVE".equalsIgnoreCase(l.getStatus())
        ));
    }

    // --------------------------------------------------
    // Reflection injection helper
    // --------------------------------------------------
    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
