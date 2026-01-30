package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.service.BusinessAnalyticsService;
import revpay.service.InvoiceService;
import revpay.service.LoanService;
import revpay.service.PaymentMethodService;
import revpay.service.TransactionHistoryService;
import revpay.service.WalletService;
import revpay.service.NotificationService;
import revpay.util.ConsoleUtil;
import revpay.util.SessionManager;

public class BusinessMenu {

    private final Scanner sc;
    private final User user;

    private final WalletService walletService = new WalletService();
    private final TransactionHistoryService txHistoryService = new TransactionHistoryService();
    private final PaymentMethodService paymentMethodService = new PaymentMethodService();
    private final NotificationService notificationService = new NotificationService();

    private final InvoiceService invoiceService = new InvoiceService();
    private final LoanService loanService = new LoanService();
    private final BusinessAnalyticsService analyticsService = new BusinessAnalyticsService();

    // ✅ Step 4: Session timeout tracker
    private final SessionManager session = new SessionManager();

    public BusinessMenu(Scanner sc, User user) {
        this.sc = sc;
        this.user = user;
    }

    public void show() {
        while (true) {

            // ✅ Auto logout on inactivity
            if (session.isExpired()) {
                ConsoleUtil.printHeader("Session Timeout");
                System.out.println("[INFO] You were logged out due to inactivity.");
                ConsoleUtil.pause(sc);
                return;
            }

            ConsoleUtil.printHeader("RevPay Dashboard - Business");
            System.out.println("Business User: " + user.getFullName() + "   Account ID: " + user.getAccountId());
            System.out.println("---------------------------------------------------");
            System.out.println("1. Create Invoice");
            System.out.println("2. Manage Invoices (Pay/Cancel/View)");
            System.out.println("3. Apply for Loan");
            System.out.println("4. Repay Loan");
            System.out.println("5. Wallet: Add Money (from Card)");
            System.out.println("6. Wallet: Withdraw (to Bank)");
            System.out.println("7. Manage Payment Methods");
            System.out.println("8. Transaction History (Filter/Search/Export)");
            System.out.println("9. Notifications");
            System.out.println("10. Business Analytics");
            System.out.println("11. Logout");
            System.out.println("---------------------------------------------------");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine();

            // ✅ user activity
            session.touch();

            switch (choice) {
                case "1":
                    invoiceService.createInvoice(sc, user);
                    session.touch();
                    break;

                case "2":
                    invoiceService.manageInvoices(sc, user);
                    session.touch();
                    break;

                case "3":
                    loanService.applyLoan(sc, user);
                    session.touch();
                    break;

                case "4":
                    loanService.repayLoan(sc, user);
                    session.touch();
                    break;

                case "5":
                    walletService.addMoneyFromCard(sc, user);
                    session.touch();
                    break;

                case "6":
                    walletService.withdrawToBank(sc, user);
                    session.touch();
                    break;

                case "7":
                    paymentMethodService.manage(sc, user.getUserId());
                    session.touch();
                    break;

                case "8":
                    txHistoryService.showFilteredHistory(sc, user);
                    session.touch();
                    break;

                case "9":
                    notificationService.showNotificationsMenu(sc, user.getUserId());
                    session.touch();
                    break;

                case "10":
                    analyticsService.showAnalytics(sc, user);
                    session.touch();
                    break;

                case "11":
                    System.out.println("[INFO] Logged out.");
                    ConsoleUtil.pause(sc);
                    return;

                default:
                    System.out.println("[ERROR] Invalid option.");
                    ConsoleUtil.pause(sc);
            }
        }
    }
}
