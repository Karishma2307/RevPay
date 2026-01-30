package revpay;

import java.util.Scanner;

import revpay.menu.MainMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger logger =
            LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		 logger.info("=== APPLICATION STARTED ===");

		Scanner sc = new Scanner(System.in);
		MainMenu mainMenu = new MainMenu(sc);
		mainMenu.show();
		sc.close();
	}
}