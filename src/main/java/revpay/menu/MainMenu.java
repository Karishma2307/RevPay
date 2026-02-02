package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.service.AuthService;
import revpay.util.ConsoleUtil;

public class MainMenu {

    private Scanner sc;

    public MainMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        AuthService authService = new AuthService(sc);

        while (true) {
            ConsoleUtil.printHeader("☺️Welcome to RevPay☺️");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Forgot Password");
            System.out.println("4. Exit");
            System.out.println("----------------------------------------");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                authService.register();

            } else if ("2".equals(choice)) {
                User user = authService.login();
                if (user != null) {
                    if ("BUSINESS".equalsIgnoreCase(user.getAccountType())) {
                        new BusinessMenu(sc, user).show();
                    } else {
                        new PersonalMenu(sc, user).show();
                    }
                }

            } else if ("3".equals(choice)) {
                authService.forgotPassword();

            } else if ("4".equals(choice)) {
                System.out.println("❤️❤️Bye!!!Have a nice day❤️❤️");
                break;

            } else {
                System.out.println("Invalid choice....😑");
                ConsoleUtil.pause(sc);
            }
        }
    }
}