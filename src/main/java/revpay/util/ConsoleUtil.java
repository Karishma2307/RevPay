package revpay.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleUtil {

    public static void printHeader(String title) {
        System.out.println("\n========== " + title + " ==========\n");
    }

    public static void pause(Scanner sc) {
        System.out.print("Press Enter to continue...");
        sc.nextLine();
    }

   
    public static int readIntInRange(Scanner sc, String prompt, int min, int max) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) {
                System.out.print(prompt);
            }
            String input = sc.nextLine();

            try {
                int val = Integer.parseInt(input.trim());
                if (val < min || val > max) {
                    System.out.println("Enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return val;
            } catch (Exception e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

   
    public static LocalDate readOptionalDate(Scanner sc, String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine();
            if (input == null || input.trim().isEmpty()) return null;

            try {
                return LocalDate.parse(input.trim(), fmt);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-MM-dd or blank.");
            }
        }
    }
}
