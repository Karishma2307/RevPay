package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.model.User;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserDao userDao;
    @Mock private WalletDao walletDao;

    @Mock private PasswordRecoveryService recoveryService;
    @Mock private BusinessProfileService businessProfileService;

    private AuthService authService;

    // add many enters for ConsoleUtil.pause(sc)
    private String extraEnters() {
        return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
    }

    // IMPORTANT: adjust this to match your real setupSecurityQuestions flow
    private String securitySetupInputs() {
        return "1\nans1\n2\nans2\n3\nans3\n";
    }

    @BeforeEach
    void setup() throws Exception {
        authService = new AuthService(new Scanner(""));

        // Inject mocks into AuthService private fields
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);
    }

    // -----------------------------
    // 1) REGISTER PERSONAL SUCCESS
    // -----------------------------
    @Test
    void register_personal_success() throws Exception {
        String input =
                "1\n" +                      // PERSONAL
                "John Test\n" +
                "john123\n" +
                "john@test.com\n" +
                "9876543210\n" +
                "Password@1\n" +
                "Password@1\n" +
                "1234\n" +
                securitySetupInputs() +
                extraEnters();

        authService = new AuthService(new Scanner(input));
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);

        // username/email/phone/accountId not existing
        when(userDao.findByUsername("john123")).thenReturn(null);
        when(userDao.findByEmail("john@test.com")).thenReturn(null);
        when(userDao.findByPhone("9876543210")).thenReturn(null);
        when(userDao.findByAccountId(anyString())).thenReturn(null);

        // Create user returns newId
        when(userDao.createUser(any(User.class))).thenReturn(100L);

        User created = authService.register();

        assertNotNull(created);
        assertEquals("PERSONAL", created.getAccountType());
        assertEquals("john123", created.getUsername());
        assertEquals("john@test.com", created.getEmail());
        assertEquals("9876543210", created.getPhone());

        verify(userDao).createUser(any(User.class));
        verify(walletDao).createWalletForUser(100L);

        // BUSINESS profile should not be called for personal
        verify(businessProfileService, never()).collectAndSave(any(), anyLong());

        // Security questions setup must be called
        verify(recoveryService).setupSecurityQuestions(any(Scanner.class), eq(100L));
    }

    // -----------------------------
    // 2) REGISTER FAIL - INVALID CHOICE
    // -----------------------------
    @Test
    void register_fail_invalidChoice() throws Exception {
        String input = "9\n" + extraEnters();
        authService = new AuthService(new Scanner(input));
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);

        assertNull(authService.register());

        verifyNoInteractions(userDao, walletDao, recoveryService, businessProfileService);
    }

    // -----------------------------
    // 3) REGISTER FAIL - USERNAME TAKEN
    // -----------------------------
    @Test
    void register_fail_usernameTaken() throws Exception {
        String input =
                "1\n" +
                "John Test\n" +
                "john123\n" +
                extraEnters();

        authService = new AuthService(new Scanner(input));
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);

        when(userDao.findByUsername("john123")).thenReturn(new User());

        assertNull(authService.register());

        verify(userDao).findByUsername("john123");
        verify(userDao, never()).createUser(any());
        verify(walletDao, never()).createWalletForUser(anyLong());
    }

    // -----------------------------
    // 4) REGISTER FAIL - EMAIL EXISTS
    // -----------------------------
    @Test
    void register_fail_emailExists() throws Exception {
        String input =
                "1\n" +
                "John Test\n" +
                "john123\n" +
                "john@test.com\n" +
                extraEnters();

        authService = new AuthService(new Scanner(input));
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);

        when(userDao.findByUsername("john123")).thenReturn(null);
        when(userDao.findByEmail("john@test.com")).thenReturn(new User());

        assertNull(authService.register());

        verify(userDao).findByUsername("john123");
        verify(userDao).findByEmail("john@test.com");
        verify(userDao, never()).createUser(any());
    }

    // -----------------------------
    // 5) REGISTER FAIL - PASSWORD MISMATCH
    // -----------------------------
    @Test
    void register_fail_passwordMismatch() throws Exception {
        String input =
                "1\n" +
                "John Test\n" +
                "john123\n" +
                "john@test.com\n" +
                "9876543210\n" +
                "Password@1\n" +
                "Password@2\n" +
                extraEnters();

        authService = new AuthService(new Scanner(input));
        inject(authService, "userDao", userDao);
        inject(authService, "walletDao", walletDao);
        inject(authService, "recoveryService", recoveryService);
        inject(authService, "businessProfileService", businessProfileService);

        when(userDao.findByUsername(anyString())).thenReturn(null);
        when(userDao.findByEmail(anyString())).thenReturn(null);
        when(userDao.findByPhone(anyString())).thenReturn(null);

        assertNull(authService.register());

        verify(userDao, never()).createUser(any());
        verify(walletDao, never()).createWalletForUser(anyLong());
        verify(recoveryService, never()).setupSecurityQuestions(any(), anyLong());
    }

    // --------------------------------
    // Reflection helper to inject mocks
    // --------------------------------
    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
