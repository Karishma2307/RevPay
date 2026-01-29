package revpay;

import java.util.Scanner;

import revpay.menu.MainMenu;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        MainMenu mainMenu = new MainMenu(sc);
        mainMenu.show();
        sc.close();
    }
}

