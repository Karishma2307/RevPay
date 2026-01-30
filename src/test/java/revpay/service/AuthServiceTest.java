package revpay.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

import revpay.model.User;

class AuthServiceTest {

    // Add 30 ENTER presses so ConsoleUtil.pause(sc) never crashes
    private String extraEnters() {
        return "\n\n\n\n\n\n\n\n\n\n" +
               "\n\n\n\n\n\n\n\n\n\n" +
               "\n\n\n\n\n\n\n\n\n\n";
    }

    // Security Questions dummy inputs (change count if your service asks more/less)
    private String securitySetupInputs() {
        // Common pattern: choose question + answer repeated 3 times
        return "1\nans1\n2\nans2\n3\nans3\n";
    }

    // -----------------------------
    // 1) REGISTER PERSONAL SUCCESS
    // -----------------------------
    @Test
    void register_personal_success() {
        long t = System.currentTimeMillis();

        String username = "john_test_" + t;
        String email = "john" + t + "@test.com";
        String phone = "9" + String.valueOf(t).substring(3, 12); // ensures 10 digits starting with 9

        String input =
                "1\n" +                // PERSONAL
                "John Test\n" +
                username + "\n" +
                email + "\n" +
                phone + "\n" +
                "Password@1\n" +
                "Password@1\n" +
                "1234\n" +
                securitySetupInputs() +
                extraEnters();

        AuthService authService = new AuthService(new Scanner(input));
        User user = authService.register();

        assertNotNull(user, "Register returned null. Likely DB insert failed or duplicate data exists.");
        assertEquals("PERSONAL", user.getAccountType());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(phone, user.getPhone());
    }

    // -----------------------------
    // 2) REGISTER FAIL - INVALID CHOICE
    // -----------------------------
    @Test
    void register_fail_invalidChoice() {
        String input = "9\n" + extraEnters();

        AuthService authService = new AuthService(new Scanner(input));
        assertNull(authService.register());
    }

    // -----------------------------
    // 3) REGISTER FAIL - EMPTY NAME
    // -----------------------------
    @Test
    void register_fail_emptyName() {
        String input =
                "1\n" +
                "\n" +
                extraEnters();

        AuthService authService = new AuthService(new Scanner(input));
        assertNull(authService.register());
    }

    // -----------------------------
    // 4) REGISTER FAIL - USERNAME EMPTY
    // -----------------------------
    @Test
    void register_fail_emptyUsername() {
        String input =
                "1\n" +
                "John Test\n" +
                "\n" +
                extraEnters();

        AuthService authService = new AuthService(new Scanner(input));
        assertNull(authService.register());
    }

    // -----------------------------
    // 5) REGISTER FAIL - INVALID EMAIL
    // -----------------------------
    @Test
    void register_fail_invalidEmail() {
        String input =
                "1\n" +
                "John Test\n" +
                "john_test_unique_102\n" +
                "wrongEmail\n" +
                extraEnters();

        AuthService authService = new AuthService(new Scanner(input));
        assertNull(authService.register());
    }

   
}
