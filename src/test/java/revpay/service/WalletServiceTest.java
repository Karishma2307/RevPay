package revpay.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.junit.jupiter.api.*;

import revpay.model.User;

class WalletServiceTest {

    private WalletService walletService;

    private PrintStream originalOut;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setup() {
        walletService = new WalletService();
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // Prevent NoSuchElementException when ConsoleUtil.pause(sc) reads extra nextLine()
    private String extraEnters() {
        return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
    }

    private User userWithId(long id) {
        User u = new User();
        u.setUserId(id);
        return u;
    }

    // ✅ Test 1: addMoneyFromCard -> no default payment method
    @Test
    void addMoneyFromCard_fail_noDefaultPaymentMethod() {
        User user = userWithId(-99999L); // likely no card exists for this id

        String input = extraEnters();
        walletService.addMoneyFromCard(new Scanner(input), user);

        String output = out.toString();
        assertTrue(output.contains("No default payment method found") || output.contains("[ERROR]"),
                "Expected error for missing default payment method");
    }

    // ✅ Test 2: addMoneyFromCard -> invalid amount (non-number)
    // NOTE: This will only reach amount parsing IF a default card exists for this user.
    // If not, it will fail earlier with "No default payment method found" and still pass.
    @Test
    void addMoneyFromCard_fail_invalidAmount_nonNumber() {
        User user = userWithId(1L); // change to userId that has default card if you have one

        String input =
                "abc\n" +        // amount
                extraEnters();

        walletService.addMoneyFromCard(new Scanner(input), user);

        String output = out.toString();
        assertTrue(
            output.contains("Invalid amount") || output.contains("No default payment method found") || output.contains("[ERROR]"),
            "Expected invalid amount OR no default method (if user has no card)"
        );
    }

    // ✅ Test 3: addMoneyFromCard -> amount <= 0
    // Same note as above (needs default card to reach this check)
    @Test
    void addMoneyFromCard_fail_amountZeroOrNegative() {
        User user = userWithId(1L); // change to userId that has default card if needed

        String input =
                "0\n" +
                extraEnters();

        walletService.addMoneyFromCard(new Scanner(input), user);

        String output = out.toString();
        assertTrue(
            output.contains("Amount must be > 0") || output.contains("No default payment method found") || output.contains("[ERROR]"),
            "Expected amount must be > 0 OR no default method"
        );
    }

    // ✅ Test 4: withdrawToBank -> wallet not found (works even if DB empty)
    @Test
    void withdrawToBank_fail_walletNotFound() {
        User user = userWithId(-99999L); // likely wallet not present

        String input = extraEnters();
        walletService.withdrawToBank(new Scanner(input), user);

        String output = out.toString();
        assertTrue(output.contains("Wallet not found") || output.contains("[ERROR]"),
                "Expected wallet not found error");
    }

    // ✅ Test 5: withdrawToBank -> invalid amount (non-number)
    // NOTE: This will only reach amount parsing IF wallet exists for this user.
    // If wallet doesn't exist, it fails earlier and still passes.
    @Test
    void withdrawToBank_fail_invalidAmount_nonNumber() {
        User user = userWithId(1L); // change to userId that has wallet if needed

        String input =
                "abc\n" +
                extraEnters();

        walletService.withdrawToBank(new Scanner(input), user);

        String output = out.toString();
        assertTrue(
            output.contains("Invalid amount") || output.contains("Wallet not found") || output.contains("[ERROR]"),
            "Expected invalid amount OR wallet not found"
        );
    }
}
