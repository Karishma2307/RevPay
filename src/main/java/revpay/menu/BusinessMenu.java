package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.model.Wallet;
import revpay.service.BusinessAnalyticsService;
import revpay.service.InvoiceService;
import revpay.service.LoanService;
import revpay.service.NotificationService;
import revpay.service.TransactionHistoryService;
import revpay.service.WalletService;
import revpay.util.ConsoleUtil;

public class BusinessMenu {

    private Scanner sc;
    private User user;

    private NotificationService notificationService = new NotificationService();
    private InvoiceService invoiceService = new InvoiceService(notificationService);
    private WalletService walletService = new WalletService();
    private LoanService loanService = new LoanService(notificationService);
    private TransactionHistoryService historyService = new TransactionHistoryService();
    private BusinessAnalyticsService analyticsService = new BusinessAnalyticsService();

    public BusinessMenu(Scanner sc, User user) {
        this.sc = sc;
        this.user = user;
    }

    public void show() {
        while (true) {

            Wallet wallet = walletService.getWallet(user.getUserId());

            System.out.println("========= RevPay Dashboard - Business =========");
            System.out.println("Business Account: " + user.getFullName() + " (" + user.getAccountId() + ")");
            if (wallet != null) {
                System.out.printf("Wallet Balance: $%.2f  Currency: %s%n",
                        wallet.getBalance(), wallet.getCurrency());
            }
            System.out.println("------------------------------------------------");
            System.out.println("1. Wallet: Add Money");
            System.out.println("2. Create Invoice");
            System.out.println("3. Manage Invoices");
            System.out.println("4. Business Loans");
            System.out.println("5. Transaction History");
            System.out.println("6. Business Analytics");
            System.out.println("7. Notifications");
            System.out.println("8. Security & Settings (TODO)");
            System.out.println("9. Logout");
            System.out.println("------------------------------------------------");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine();

            if ("1".equals(choice)) {
                walletService.addMoney(sc, wallet, notificationService);

            } else if ("2".equals(choice)) {
                invoiceService.createInvoice(sc, user);

            } else if ("3".equals(choice)) {
                invoiceService.manageInvoices(sc, user);

            } else if ("4".equals(choice)) {
                loanService.manageLoans(sc, user);

            } else if ("5".equals(choice)) {
                historyService.showHistory(sc, user);

            } else if ("6".equals(choice)) {
                analyticsService.showAnalytics(sc, user);

            } else if ("7".equals(choice)) {
                notificationService.showNotifications(sc, user.getUserId());

            } else if ("8".equals(choice)) {
                System.out.println("[TODO] Security & Settings for business account.");
                ConsoleUtil.pause(sc);

            } else if ("9".equals(choice)) {
                System.out.println("Logging out...");
                break;

            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }
}
