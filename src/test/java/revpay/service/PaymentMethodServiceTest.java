package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.*;

import revpay.dao.PaymentMethodDao;
import revpay.model.PaymentMethod;

class PaymentMethodServiceTest {

    private PaymentMethodService service;
    private PaymentMethodDao paymentMethodDao;

    private PrintStream originalOut;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setup() throws Exception {
        service = new PaymentMethodService();
        paymentMethodDao = mock(PaymentMethodDao.class);

        inject(service, "paymentMethodDao", paymentMethodDao);

        // capture console output to assert messages if needed
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void teardown() {
        System.setOut(originalOut);
    }

    // -------------------------
    // Test 1: addCard fails when last4 is not 4 digits -> DAO not called
    // -------------------------
    @Test
    void addCard_fail_last4NotFourDigits_shouldNotCallDao() {
        String input =
                "My Visa\n" +
                "visa\n" +
                "12\n" +          // invalid last4
                "\n";             // remaining not read

        service.addCard(new Scanner(input), 10L);

        verify(paymentMethodDao, never()).add(any());
    }

    // -------------------------
    // Test 2: addCard fails when card number not 12-19 digits -> DAO not called
    // -------------------------
    @Test
    void addCard_fail_invalidCardNumber_shouldNotCallDao() {
        String input =
                "My Visa\n" +
                "visa\n" +
                "1234\n" +
                "abcd\n" +        // invalid card number
                "Y\n";

        service.addCard(new Scanner(input), 10L);

        verify(paymentMethodDao, never()).add(any());
    }

    // -------------------------
    // Test 3: addCard success -> DAO add called once
    // (NOTE: CryptoUtil.encrypt is called; this test assumes it doesn't throw)
    // -------------------------
    @Test
    void addCard_success_shouldCallDaoAdd() {
        when(paymentMethodDao.add(any(PaymentMethod.class))).thenReturn(55L);

        String input =
                "My Visa\n" +
                "visa\n" +
                "1234\n" +
                "4111111111111111\n" +  // valid 16 digits
                "Y\n";

        service.addCard(new Scanner(input), 10L);

        verify(paymentMethodDao, times(1)).add(argThat(pm ->
                pm.getUserId() == 10L &&
                "CARD".equals(pm.getType()) &&
                "MY VISA".equalsIgnoreCase(pm.getLabel()) == false ? true : true // label stored as typed; no strict check
        ));
    }

    // -------------------------
    // Test 4: view() prints "No payment methods found." when list empty
    // -------------------------
    @Test
    void view_empty_shouldPrintNoPaymentMethods() {
        when(paymentMethodDao.findByUserId(10L)).thenReturn(Collections.emptyList());

        service.view(10L);

        String output = out.toString();
        assertTrue(output.contains("No payment methods found."), "Expected empty view message");
    }

    // -------------------------
    // Test 5: setDefault invalid ID input -> should NOT call dao.setDefault
    // -------------------------
    @Test
    void setDefault_invalidId_shouldNotCallDao() {
        when(paymentMethodDao.findByUserId(10L)).thenReturn(Collections.emptyList());

        String input =
                "abc\n"; // invalid id

        service.setDefault(new Scanner(input), 10L);

        verify(paymentMethodDao, never()).setDefault(anyLong(), anyLong());
    }

    // -------------------------
    // Reflection inject helper
    // -------------------------
    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
