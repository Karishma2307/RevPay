package revpay.menu;

import java.util.Scanner;

import revpay.model.User;
import revpay.service.MoneyRequestService;
import revpay.service.NotificationService;
import revpay.service.PaymentMethodService;
import revpay.service.SecurityService;
import revpay.service.TransactionHistoryService;
import revpay.service.TransferService;
import revpay.service.WalletService;
import revpay.util.ConsoleUtil;
import revpay.util.SessionManager;

public class PersonalMenu {

    private final Scanner sc;
    private final User user;

    private final WalletService walletService = new WalletService();
    private final TransferService transferService = new TransferService();
    private final MoneyRequestService moneyRequestService = new MoneyRequestService();
    private final PaymentMethodService paymentMethodService = new PaymentMethodService();
    private final TransactionHistoryService txHistoryService = new TransactionHistoryService();
    private final NotificationService notificationService = new NotificationService();
    private final SecurityService securityService = new SecurityService();

    // ✅ Step 4: Session timeout tracker
    private final SessionManager session = new SessionManager();

    public PersonalMenu(Scanner sc, User user) {
        this.sc = sc;
        this.user = user;
    }

    public void show() {
        while (true) {

            // ✅ If session expired before showing menu
            if (session.isExpired()) {
                ConsoleUtil.printHeader("Session Timeout");
                System.out.println("[INFO] You were logged out due to inactivity.");
                ConsoleUtil.pause(sc);
                return;
            }

            ConsoleUtil.printHeader("RevPay Dashboard - Personal");
            System.out.println("1. Wallet");
            System.out.println("2. Send Money");
            System.out.println("3. Request Money");
            System.out.println("4. Manage Payment Methods");
            System.out.println("5. Transaction History");
            System.out.println("6. Notifications");
            System.out.println("7. Security Settings");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine();

            // ✅ user performed activity
            session.touch();

            switch (choice) {

                case "1":
                    walletService.addMoneyFromCard(sc, user);
                    session.touch();
                    break;

                case "2":
                    transferService.sendMoney(sc, user);
                    session.touch();
                    break;

                case "3":
                    ConsoleUtil.printHeader("Money Requests");
                    System.out.println("1. Create Request");
                    System.out.println("2. View Incoming");
                    System.out.println("3. View Outgoing");
                    System.out.println("4. Accept Request");
                    System.out.println("5. Decline Request");
                    System.out.println("6. Cancel Outgoing Request");
                    System.out.println("7. Back");
                    System.out.print("Choice: ");

                    String ch = sc.nextLine();
                    session.touch();

                    switch (ch) {
                        case "1": moneyRequestService.createRequest(sc, user); break;
                        case "2": moneyRequestService.viewIncoming(sc, user); break;
                        case "3": moneyRequestService.viewOutgoing(sc, user); break;
                        case "4": moneyRequestService.acceptRequest(sc, user); break;
                        case "5": moneyRequestService.declineRequest(sc, user); break;
                        case "6": moneyRequestService.cancelRequest(sc, user); break;
                        default: break;
                    }
                    session.touch();
                    break;

                case "4":
                    paymentMethodService.manage(sc, user.getUserId());
                    session.touch();
                    break;

                case "5":
                    txHistoryService.showFilteredHistory(sc, user);
                    session.touch();
                    break;

                case "6":
                    notificationService.showNotificationsMenu(sc, user.getUserId());
                    session.touch();
                    break;

                case "7":
                    ConsoleUtil.printHeader("Security Settings");
                    System.out.println("1. Change Password");
                    System.out.println("2. Change Transaction PIN");
                    System.out.println("3. Back");
                    System.out.print("Choice: ");

                    String sec = sc.nextLine();
                    session.touch();

                    if ("1".equals(sec)) securityService.changePassword(sc, user);
                    else if ("2".equals(sec)) securityService.changeTxnPin(sc, user);

                    session.touch();
                    break;

                case "8":
                    System.out.println("Logged out.");
                    ConsoleUtil.pause(sc);
                    return;

                default:
                    System.out.println("Invalid option.");
                    ConsoleUtil.pause(sc);
            }
        }
    }
}
