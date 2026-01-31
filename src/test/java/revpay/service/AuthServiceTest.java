package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.UserDao;
import revpay.dao.WalletDao;
import revpay.model.User;
import revpay.util.HashUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private UserDao userDao;
    private WalletDao walletDao;

    private PasswordRecoveryService recoveryService;
    private BusinessProfileService businessProfileService;

    @BeforeEach
    void setup() {
        userDao = mock(UserDao.class);
        walletDao = mock(WalletDao.class);
        recoveryService = mock(PasswordRecoveryService.class);
        businessProfileService = mock(BusinessProfileService.class);
    }

    // ---------------------------------------------------------
    // TEST 1: register() invalid account type choice -> returns null
    // ---------------------------------------------------------
    @Test
    void register_invalidChoice_shouldReturnNull() throws Exception {
        Scanner sc = new Scanner("9\n\n"); // invalid choice + pause enter
        AuthService service = new AuthService(sc);

        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "recoveryService", recoveryService);
        inject(service, "businessProfileService", businessProfileService);

        User result = service.register();

        assertNull(result);
        verifyNoInteractions(userDao);
        verifyNoInteractions(walletDao);
    }

    // ---------------------------------------------------------
    // TEST 2: register() username already exists -> returns null
    // ---------------------------------------------------------
    @Test
    void register_usernameTaken_shouldReturnNull() throws Exception {
        String input =
                "1\n" +                 // Personal
                "Karishma Shaik\n" +     // full name
                "karishma\n" +           // username
                "\n";                    // pause

        Scanner sc = new Scanner(input);
        AuthService service = new AuthService(sc);

        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "recoveryService", recoveryService);
        inject(service, "businessProfileService", businessProfileService);

       
        when(userDao.findByUsername("karishma")).thenReturn(new User());

        User result = service.register();

        assertNull(result);
        verify(userDao).findByUsername("karishma");
        verify(userDao, never()).createUser(any());
        verifyNoInteractions(walletDao);
    }

    
    @Test
    void register_successPersonal_shouldCreateUserWalletAndSecurityQ() throws Exception {
        String input =
                "1\n" +                 // Personal
                "Karishma Shaik\n" +     // full name
                "karishma\n" +           // username
                "k@revpay.com\n" +        // email
                "9876543210\n" +          // phone
                "Abcd@1234\n" +           // password
                "Abcd@1234\n" +           // confirm
                "1234\n" +                // txn pin
                "\n";                     // final pause enter

        Scanner sc = new Scanner(input);
        AuthService service = new AuthService(sc);

        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "recoveryService", recoveryService);
        inject(service, "businessProfileService", businessProfileService);

        
        when(userDao.findByUsername("karishma")).thenReturn(null);
        when(userDao.findByEmail("k@revpay.com")).thenReturn(null);
        when(userDao.findByPhone("9876543210")).thenReturn(null);
        when(userDao.findByAccountId(anyString())).thenReturn(null);

        
        when(userDao.createUser(any(User.class))).thenReturn(101L);

        User result = service.register();

        assertNotNull(result);
        assertEquals("PERSONAL", result.getAccountType());
        assertEquals("karishma", result.getUsername());
        assertEquals("k@revpay.com", result.getEmail());
        assertEquals("9876543210", result.getPhone());
        assertNotNull(result.getAccountId());

        
        verify(userDao).createUser(any(User.class));

     
        verify(walletDao).createWalletForUser(101L);

       
        verify(recoveryService).setupSecurityQuestions(any(Scanner.class), eq(101L));

     
        verifyNoInteractions(businessProfileService);
    }

    
    @Test
    void login_wrongPassword_shouldLockAfter3Attempts() throws Exception {
       
        String input =
                "1\n" +              
                "k@revpay.com\n" +    
                "Wrong@123\n" +     
                "\n";                

        Scanner sc = new Scanner(input);
        AuthService service = new AuthService(sc);

        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "recoveryService", recoveryService);
        inject(service, "businessProfileService", businessProfileService);

        User dbUser = new User();
        dbUser.setUserId(50L);
        dbUser.setUsername("karishma");
        dbUser.setEmail("k@revpay.com");
        dbUser.setStatus("ACTIVE");

     
        dbUser.setFailedLoginAttempts(2);

        
        dbUser.setPasswordHash(HashUtil.hash("Correct@123"));

        when(userDao.findByEmail("k@revpay.com")).thenReturn(dbUser);

        User result = service.login();

        assertNull(result);
        verify(userDao).updateFailedAttempts(50L, 3);
        verify(userDao).updateStatus(50L, "LOCKED");

      
        verify(userDao, never()).updateFailedAttempts(50L, 0);
    }

    
    @Test
    void forgotPassword_shouldDelegateToRecoveryService() throws Exception {
        Scanner sc = new Scanner("\n");
        AuthService service = new AuthService(sc);

        inject(service, "userDao", userDao);
        inject(service, "walletDao", walletDao);
        inject(service, "recoveryService", recoveryService);
        inject(service, "businessProfileService", businessProfileService);

        service.forgotPassword();

        verify(recoveryService).forgotPassword(any(Scanner.class));
    }

    
    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
