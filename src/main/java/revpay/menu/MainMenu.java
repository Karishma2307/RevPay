package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.service.AuthService;
import revpay.util.ConsoleUtil;

public class MainMenu {

    private Scanner sc;
    private AuthService authService;

    public MainMenu(Scanner sc) {
        this.sc = sc;
        this.authService = new AuthService(sc);
    }

    public void show() {
        while (true) {
            ConsoleUtil.printHeader("Welcome to RevPay");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.println("----------------------------------------");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                authService.register();
                ConsoleUtil.pause(sc);
            } else if ("2".equals(choice)) {
                User user = authService.login();
                if (user != null) {
                    if ("PERSONAL".equalsIgnoreCase(user.getAccountType())) {
                        PersonalMenu pm = new PersonalMenu(sc, user);
                        pm.show();
                    } else {
                        BusinessMenu bm = new BusinessMenu(sc, user);
                        bm.show();
                    }
                }
            } else if ("3".equals(choice)) {
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }
}
