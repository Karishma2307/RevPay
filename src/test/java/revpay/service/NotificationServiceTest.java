package revpay.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import revpay.dao.NotificationDao;
import revpay.model.Notification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private NotificationService service;
    private NotificationDao notificationDao;

    @BeforeEach
    void setup() throws Exception {
        service = new NotificationService();
        notificationDao = mock(NotificationDao.class);

       
        boolean injected = tryInject(service, "notificationDao", notificationDao)
                        || tryInject(service, "dao", notificationDao);

        if (!injected) {
            fail("Could not inject NotificationDao. Field name not found: 'notificationDao' or 'dao'. " +
                 "Check your NotificationService field name.");
        }
    }

    // ---------------------------------------------------
    // TEST 1: notifyUser should create notification
    // ---------------------------------------------------
    @Test
    void notifyUser_shouldCreateNotification() {
        service.notifyUser(10L, "TRANSACTION", "Title", "Message");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDao, times(1)).createNotification(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals("TRANSACTION", saved.getType());
        assertEquals("Title", saved.getTitle());
        assertEquals("Message", saved.getMessage());
    }

    // ---------------------------------------------------
    // TEST 2: menu option 4 -> Back (exit)
    // ---------------------------------------------------
    @Test
    void showNotificationsMenu_back_shouldExit() {
        String input = "4\n";
        service.showNotificationsMenu(new Scanner(input), 10L);

        verifyNoInteractions(notificationDao);
    }

    // ---------------------------------------------------
    // TEST 3: option 3 -> markAllAsRead then back
    // ---------------------------------------------------
    @Test
    void showNotificationsMenu_markAllAsRead_shouldCallDao() {
        
        String input = "3\n\n4\n";
        service.showNotificationsMenu(new Scanner(input), 10L);

        verify(notificationDao, times(1)).markAllAsRead(10L);
    }

    // ---------------------------------------------------
    // TEST 4: option 1 -> all notifications
    // ---------------------------------------------------
    @Test
    void showNotificationsMenu_all_shouldQueryDao() {
        Notification n1 = new Notification();
        n1.setTitle("T1");
        n1.setType("TYPE1");
        n1.setMessage("M1");

        
        try {
            n1.getClass().getMethod("setRead", boolean.class).invoke(n1, false);
        } catch (Exception ignore) {
            
        }

        n1.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        when(notificationDao.findByUserId(10L, false)).thenReturn(Arrays.asList(n1));

        
        String input = "1\n\n4\n";
        service.showNotificationsMenu(new Scanner(input), 10L);

        verify(notificationDao, times(1)).findByUserId(10L, false);
    }

    // ---------------------------------------------------
    // TEST 5: option 2 -> unread only, empty list
    // ---------------------------------------------------
    @Test
    void showNotificationsMenu_unreadOnly_empty_shouldQueryDao() {
        when(notificationDao.findByUserId(10L, true)).thenReturn(Collections.emptyList());

        
        String input = "2\n\n4\n";
        service.showNotificationsMenu(new Scanner(input), 10L);

        verify(notificationDao, times(1)).findByUserId(10L, true);
    }

    
    private boolean tryInject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
