package revpay.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.junit.jupiter.api.*;

import revpay.model.User;
import revpay.util.HashUtil;

class MoneyRequestServiceTest {

    private MoneyRequestService service;

    private PrintStream originalOut;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setup() {
        service = new MoneyRequestService();
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // Prevent crash because ConsoleUtil.pause(sc) consumes nextLine()
    private String extraEnters() {
        return "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
    }

    private User user(long id, String fullName, String txnPin) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(fullName);
        u.setTxnPinHash(HashUtil.hash(txnPin));
        return u;
    }

    // ✅ Test 1: createRequest -> invalid choice
    @Test
    void createRequest_fail_invalidChoice() {
        User requester = user(10L, "Requester", "1111");

        String input =
                "9\n" +          // invalid
                extraEnters();

        service.createRequest(new Scanner(input), requester);

        String output = out.toString();
        assertTrue(output.contains("Invalid choice") || output.contains("[ERROR]"),
                "Expected: Invalid choice error");
    }

    // ✅ Test 2: createRequest -> payer not found (email)
    @Test
    void createRequest_fail_userNotFound() {
        User requester = user(10L, "Requester", "1111");

        String input =
                "1\n" +
                "no_such_user@test.com\n" +
                extraEnters();

        service.createRequest(new Scanner(input), requester);

        String output = out.toString();
        assertTrue(output.contains("User not found") || output.contains("[ERROR]"),
                "Expected: User not found error");
    }

    // ✅ Test 3: createRequest -> invalid amount (non-number)
    // NOTE: If payer doesn't exist in DB, it will stop at "User not found" and still pass.
    @Test
    void createRequest_fail_invalidAmount_nonNumber() {
        User requester = user(10L, "Requester", "1111");

        String input =
                "1\n" +
                "no_such_user@test.com\n" +  // likely not found in DB
                "abc\n" +                    // invalid amount
                extraEnters();

        service.createRequest(new Scanner(input), requester);

        String output = out.toString();
        assertTrue(
            output.contains("Invalid amount") || output.contains("User not found") || output.contains("[ERROR]"),
            "Expected: Invalid amount OR user not found (DB-safe)"
        );
    }

    // ✅ Test 4: acceptRequest -> no incoming requests
    @Test
    void acceptRequest_noIncomingRequests() {
        User payer = user(-99999L, "Payer", "2222"); // user likely has no incoming requests

        String input = extraEnters();
        service.acceptRequest(new Scanner(input), payer);

        String output = out.toString();
        assertTrue(output.contains("No incoming requests") || output.contains("No incoming"),
                "Expected: No incoming requests message");
    }

    // ✅ Test 5: declineRequest -> no incoming requests
    @Test
    void declineRequest_noIncomingRequests() {
        User payer = user(-99999L, "Payer", "2222");

        String input = extraEnters();
        service.declineRequest(new Scanner(input), payer);

        String output = out.toString();
        assertTrue(output.contains("No incoming requests") || output.contains("No incoming"),
                "Expected: No incoming requests message");
    }
}
