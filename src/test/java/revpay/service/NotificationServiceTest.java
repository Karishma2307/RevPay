package revpay.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.NotificationDao;
import revpay.dao.NotificationPrefDao;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private NotificationService service;

    private NotificationDao dao;
    private NotificationPrefDao prefDao;

    private String manyEnters() {
        // Safe buffer if ConsoleUtil.pause(sc) reads more than once
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) sb.append("\n");
        return sb.toString();
    }

    @BeforeEach
    void setup() throws Exception {
        service = new NotificationService();

        dao = mock(NotificationDao.class);
        prefDao = mock(NotificationPrefDao.class);

        inject(service, "dao", dao);
        inject(service, "prefDao", prefDao);
    }

    // ---------------------------------------------------------
    // TEST 1: notifyUser should always ensure prefs exist
    // ---------------------------------------------------------
    @Test
    void notifyUser_shouldEnsureExists() {
        when(prefDao.isEnabled(10L, "TRANSACTION")).thenReturn(false);

        service.notifyUser(10L, "TRANSACTION", "t", "m");

        verify(prefDao).ensureExists(10L);
    }

    // ---------------------------------------------------------
    // TEST 2: notifyUser when disabled -> should NOT create
    // ---------------------------------------------------------
    @Test
    void notifyUser_disabled_shouldNotCreate() {
        when(prefDao.isEnabled(10L, "REQUEST")).thenReturn(false);

        service.notifyUser(10L, "REQUEST", "title", "msg");

        verify(prefDao).ensureExists(10L);
        verify(prefDao).isEnabled(10L, "REQUEST");
        verifyNoInteractions(dao);
    }

    // ---------------------------------------------------------
    // TEST 3: notifyUser when enabled -> should create
    // ---------------------------------------------------------
    @Test
    void notifyUser_enabled_shouldCreate() {
        when(prefDao.isEnabled(10L, "LOAN")).thenReturn(true);

        service.notifyUser(10L, "LOAN", "Loan", "Applied");

        verify(prefDao).ensureExists(10L);
        verify(prefDao).isEnabled(10L, "LOAN");
        verify(dao).create(10L, "LOAN", "Loan", "Applied");
    }

    // ---------------------------------------------------------
    // TEST 4: showNotificationsMenu option 4 -> markAllAsRead then back
    // ---------------------------------------------------------
    @Test
    void showNotificationsMenu_markAllAsRead_thenBack() {
        // Option 4 calls ConsoleUtil.pause(sc) once.
        // Then menu loops again, we choose 6 to exit.
        String input =
                "4\n" +
                "\n" +     // pause after markAllAsRead
                "6\n";

        service.showNotificationsMenu(new Scanner(input), 10L);

        verify(prefDao).ensureExists(10L);
        verify(dao).markAllAsRead(10L);
    }

    // ---------------------------------------------------------
    // TEST 5: managePreferences option 6 -> updateThreshold then back
    // FIXED: add one Enter for pause immediately after threshold input
    // ---------------------------------------------------------
    @Test
    void managePreferences_setThreshold_thenBack() {
        String input =
                "6\n" +     // set threshold
                "200\n" +   // value
                "\n" +      // pause() consumes this
                "7\n";      // back (exit)

        service.managePreferences(new Scanner(input), 10L);

        verify(prefDao).ensureExists(10L);
        verify(prefDao).updateThreshold(10L, 200);
    }

    // ---------------------------------------------------------
    // Reflection injection helper
    // ---------------------------------------------------------
    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
