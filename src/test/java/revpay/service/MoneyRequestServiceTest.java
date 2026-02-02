package revpay.service;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.MoneyRequestDao;
import revpay.dao.TransactionDao;
import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.model.MoneyRequest;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.HashUtil;

@ExtendWith(MockitoExtension.class)
class MoneyRequestServiceTest {

    private MoneyRequestService service;

   
    private MoneyRequestDao moneyRequestDao;
    private UserDao userDao;
    private WalletDao walletDao;
    private TransactionDao transactionDao;
    private NotificationService notificationService;
    private LowBalanceAlertService lowBalanceAlertService;

    private User requester;
    private User payer;

    private String extraEnters() {
        return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
    }

    @BeforeEach
    void setup() throws Exception {
        service = new MoneyRequestService();

        moneyRequestDao = mock(MoneyRequestDao.class);
        userDao = mock(UserDao.class);
        walletDao = mock(WalletDao.class);
        transactionDao = mock(TransactionDao.class);
        notificationService = mock(NotificationService.class);
        lowBalanceAlertService = mock(LowBalanceAlertService.class);

        
        inject(service, "moneyRequestDao", moneyRequestDao);
        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "transactionDao", transactionDao);
        inject(service, "notificationService", notificationService);
        inject(service, "lowBalanceAlertService", lowBalanceAlertService);

        requester = new User();
        requester.setUserId(1L);
        requester.setFullName("Requester");

        payer = new User();
        payer.setUserId(2L);
        payer.setFullName("Payer");
        payer.setTxnPinHash(HashUtil.hash("1111"));
    }

    // ------------------------------------------------
    // TEST 1: createRequest -> invalid choice
    // ------------------------------------------------
    @Test
    void createRequest_invalidChoice_shouldExitEarly() {
        String input = "9\n" + extraEnters();

        service.createRequest(new Scanner(input), requester);

        verifyNoInteractions(userDao, moneyRequestDao, notificationService);
    }

    // ------------------------------------------------
    // TEST 2: createRequest -> payer not found
    // ------------------------------------------------
    @Test
    void createRequest_userNotFound_shouldExitEarly() {
        when(userDao.findByEmail("no@test.com")).thenReturn(null);

        String input =
                "1\n" +
                "no@test.com\n" +
                extraEnters();

        service.createRequest(new Scanner(input), requester);

        verify(userDao).findByEmail("no@test.com");
        verifyNoInteractions(moneyRequestDao, notificationService);
    }

    // ------------------------------------------------
    // TEST 3: acceptRequest -> no incoming requests
    // ------------------------------------------------
    @Test
    void acceptRequest_noIncomingRequests_shouldExit() {
        when(moneyRequestDao.findIncoming(2L)).thenReturn(Collections.emptyList());

        service.acceptRequest(new Scanner(extraEnters()), payer);

        verify(moneyRequestDao).findIncoming(2L);
        verifyNoInteractions(walletDao, transactionDao, notificationService);
    }

    // ------------------------------------------------
    // TEST 4: declineRequest -> no incoming requests
    // ------------------------------------------------
    @Test
    void declineRequest_noIncomingRequests_shouldExit() {
        when(moneyRequestDao.findIncoming(2L)).thenReturn(Collections.emptyList());

        service.declineRequest(new Scanner(extraEnters()), payer);

        verify(moneyRequestDao).findIncoming(2L);
        verifyNoInteractions(notificationService);
    }

    // ------------------------------------------------
    // TEST 5: cancelRequest -> no outgoing requests
    // ------------------------------------------------
    @Test
    void cancelRequest_noOutgoingRequests_shouldExit() {
        when(moneyRequestDao.findOutgoing(1L)).thenReturn(Collections.emptyList());

        service.cancelRequest(new Scanner(extraEnters()), requester);

        verify(moneyRequestDao).findOutgoing(1L);
        verifyNoInteractions(notificationService);
    }

    
    private void inject(Object target, String field, Object mock) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, mock);
    }
}
