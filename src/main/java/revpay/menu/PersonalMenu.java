package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.model.Wallet;
import revpay.service.NotificationService;
import revpay.service.TransactionHistoryService;
import revpay.service.TransferService;
import revpay.service.WalletService;
import revpay.util.ConsoleUtil;
import revpay.service.MoneyRequestService;

public class PersonalMenu {

    private Scanner sc;
    private User user;

    private WalletService walletService = new WalletService();
    private NotificationService notificationService = new NotificationService();
    private TransferService transferService = new TransferService(notificationService);
    private TransactionHistoryService historyService = new TransactionHistoryService();
    private MoneyRequestService moneyRequestService = new MoneyRequestService(notificationService);
    
    public PersonalMenu(Scanner sc, User user) {
        this.sc = sc;
        this.user = user;
    }

    public void show() {
        while (true) {
            Wallet wallet = walletService.getWallet(user.getUserId());

            System.out.println("=========== RevPay Dashboard - Personal ===========");
            System.out.println("User: " + user.getFullName() + "      Account ID: " + user.getAccountId());
            if (wallet != null) {
                System.out.printf("Wallet Balance: $%.2f      Currency: %s%n",
                        wallet.getBalance(), wallet.getCurrency());
            }
            System.out.println("---------------------------------------------------");
            System.out.println("1. Send Money");
            System.out.println("2. Request Money (TODO)");
            System.out.println("3. Manage Money Requests (TODO)");
            System.out.println("4. Wallet: Add Money");
            System.out.println("5. Transaction History");
            System.out.println("6. Notifications");
            System.out.println("7. Security & Settings (TODO)");
            System.out.println("8. Logout");
            System.out.println("---------------------------------------------------");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                transferService.sendMoney(sc, user);
            } else if ("2".equals(choice)) {
                moneyRequestService.createRequest(sc, user);
            } else if ("3".equals(choice)) {
                moneyRequestService.manageRequests(sc, user);
            } else if ("4".equals(choice)) {
                walletService.addMoney(sc, wallet, notificationService);
            } else if ("5".equals(choice)) {
                historyService.showHistory(sc, user);
            } else if ("6".equals(choice)) {
                notificationService.showNotifications(sc, user.getUserId());
            } else if ("7".equals(choice)) {
                System.out.println("[TODO] Security & Settings.");
                ConsoleUtil.pause(sc);
            } else if ("8".equals(choice)) {
                System.out.println("Logging out...");
                break;
            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }
}
