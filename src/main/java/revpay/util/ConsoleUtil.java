package revpay.util;

import java.util.Scanner;

public class ConsoleUtil {

    public static void printHeader(String title) {
        System.out.println("========================================");
        System.out.println(" " + title);
        System.out.println("========================================");
    }

    public static void pause(Scanner sc) {
        System.out.println("Press Enter to continue...");
        sc.nextLine();
    }
}