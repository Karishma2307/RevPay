package revpay.service;

import java.util.List;
import java.util.Scanner;

import revpay.dao.PaymentMethodDao;
import revpay.dao.impl.PaymentMethodDaoImpl;
import revpay.model.PaymentMethod;
import revpay.util.ConsoleUtil;

public class PaymentMethodService {

    private final PaymentMethodDao paymentMethodDao = new PaymentMethodDaoImpl();

   
    public void manage(Scanner sc, long userId) {
        showMenu(sc, userId);
    }

    public void showMenu(Scanner sc, long userId) {
        while (true) {
            ConsoleUtil.printHeader("Payment Methods");
            System.out.println("1. Add Card");
            System.out.println("2. View My Payment Methods");
            
            System.out.println("3. Delete Payment Method");
            System.out.println("4. Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1": addCard(sc, userId); break;
                case "2": view(userId); break;
                case "3": delete(sc, userId); break;
                case "4": return;
                default: System.out.println("Invalid choice."); break;
            }
            ConsoleUtil.pause(sc);
        }
    }

    public void addCard(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Add Card");
        PaymentMethod pm = new PaymentMethod();
        pm.setUserId(userId);
        pm.setType("CARD");

        System.out.print("Label (e.g., My Visa): ");
        pm.setLabel(sc.nextLine().trim());

        System.out.print("Provider (VISA/MASTERCARD/RUPAY): ");
        pm.setProvider(sc.nextLine().trim().toUpperCase());

        System.out.print("Last 4 digits: ");
        String last4 = sc.nextLine().trim();
        if (last4.length() != 4) {
            System.out.println("Last4 must be exactly 4 digits.");
            return;
        }
        pm.setLast4(last4);

        System.out.print("Enter full card number (will be encrypted): ");
        String fullCardNumber = sc.nextLine().trim();

        if (!fullCardNumber.matches("\\d{12,19}")) {
            System.out.println("[ERROR] Card number must be 12 to 19 digits.");
            return;
        }

        String encrypted = revpay.util.CryptoUtil.encrypt(fullCardNumber);
        pm.setEncNumber(encrypted);


        System.out.print("Set as default? (Y/N): ");
        String def = sc.nextLine().trim().toUpperCase();
        pm.setIsDefault("Y".equals(def) ? "Y" : "N");

        try {
            long id = paymentMethodDao.add(pm);
            System.out.println("Card added. Method ID: " + id);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("ORA-00001")) {
                System.out.println("Duplicate card/default conflict. Try different card details.");
            } else {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public void view(long userId) {
        ConsoleUtil.printHeader("My Payment Methods");
        List<PaymentMethod> list = paymentMethodDao.findByUserId(userId);
        if (list.isEmpty()) {
            System.out.println("No payment methods found.");
            return;
        }
        for (PaymentMethod pm : list) {
            System.out.printf("ID: %d | %s %s ****%s | Default: %s%n",
                    pm.getMethodId(),
                    pm.getProvider(),
                    pm.getLabel(),
                    pm.getLast4(),
                    pm.getIsDefault());
        }
    }

    

    public void delete(Scanner sc, long userId) {
        view(userId);
        System.out.print("Enter Method ID to delete: ");
        String in = sc.nextLine().trim();
        long methodId;
        try { methodId = Long.parseLong(in); }
        catch (NumberFormatException e) { System.out.println("Invalid ID."); return; }

        boolean ok = paymentMethodDao.delete(userId, methodId);
        System.out.println(ok ? "Deleted." : "Method not found for this user.");
    }
}
