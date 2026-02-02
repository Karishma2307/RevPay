package revpay.service;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.PaymentMethodDao;
import revpay.dao.impl.PaymentMethodDaoImpl;
import revpay.model.PaymentMethod;
import revpay.util.ConsoleUtil;

public class PaymentMethodService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMethodService.class);

    private final PaymentMethodDao paymentMethodDao = new PaymentMethodDaoImpl();

    public void manage(Scanner sc, long userId) {
        showMenu(sc, userId);
    }

    public void showMenu(Scanner sc, long userId) {
        logger.info("Payment methods menu opened (userId={})", userId);

        while (true) {
            ConsoleUtil.printHeader("Payment Methods");
            System.out.println("1. Add Card");
            System.out.println("2. View My Payment Methods");
            System.out.println("3. Delete Payment Method");
            System.out.println("4. Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    addCard(sc, userId);
                    break;
                case "2":
                    view(userId);
                    break;
                case "3":
                    delete(sc, userId);
                    break;
                case "4":
                    logger.info("Payment methods menu closed (userId={})", userId);
                    return;
                default:
                    logger.warn("Invalid payment methods menu option '{}' (userId={})", choice, userId);
                    System.out.println("Invalid choice.");
                    break;
            }
            ConsoleUtil.pause(sc);
        }
    }

    public void addCard(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Add Card");
        logger.info("Add card flow started (userId={})", userId);

        PaymentMethod pm = new PaymentMethod();
        pm.setUserId(userId);
        pm.setType("CARD");

        System.out.print("Label (e.g., My Visa): ");
        String label = sc.nextLine().trim();
        pm.setLabel(label);

        System.out.print("Provider (VISA/MASTERCARD/RUPAY): ");
        String provider = sc.nextLine().trim().toUpperCase();
        pm.setProvider(provider);

        System.out.print("Last 4 digits: ");
        String last4 = sc.nextLine().trim();
        if (last4.length() != 4 || !last4.matches("\\d{4}")) {
            logger.warn("Add card failed: invalid last4 (userId={}, last4='{}')", userId, safe(last4));
            System.out.println("Last4 must be exactly 4 digits.");
            return;
        }
        pm.setLast4(last4);

        System.out.print("Enter full card number (will be encrypted): ");
        String fullCardNumber = sc.nextLine().trim();

        // DO NOT LOG full card number
        if (!fullCardNumber.matches("\\d{12,19}")) {
            logger.warn("Add card failed: invalid card number length/format (userId={}, provider='{}', last4='{}')",
                    userId, safe(provider), safe(last4));
            System.out.println("Card number must be 12 to 19 digits.");
            return;
        }

        String encrypted;
        try {
            encrypted = revpay.util.CryptoUtil.encrypt(fullCardNumber);
        } catch (Exception e) {
            logger.error("Add card failed: encryption error (userId={}, provider='{}', last4='{}')",
                    userId, safe(provider), safe(last4), e);
            System.out.println("Failed to encrypt card number.");
            return;
        }

        pm.setEncNumber(encrypted);

        System.out.print("Set as default? (Y/N): ");
        String def = sc.nextLine().trim().toUpperCase();
        pm.setIsDefault("Y".equals(def) ? "Y" : "N");

        try {
            long id = paymentMethodDao.add(pm);
            logger.info("Card added (userId={}, methodId={}, provider='{}', label='{}', last4='{}', isDefault={})",
                    userId, id, safe(provider), safe(label), safe(last4), pm.getIsDefault());
            System.out.println("Card added. Method ID: " + id);

        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("ORA-00001")) {
                logger.warn("Add card failed: ORA-00001 duplicate/default conflict (userId={}, provider='{}', last4='{}')",
                        userId, safe(provider), safe(last4));
                System.out.println("Duplicate card/default conflict. Try different card details.");
            } else {
                logger.error("Add card failed: runtime exception (userId={}, provider='{}', last4='{}')",
                        userId, safe(provider), safe(last4), e);
                System.out.println("Error: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Add card failed: unexpected exception (userId={}, provider='{}', last4='{}')",
                    userId, safe(provider), safe(last4), e);
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void view(long userId) {
        ConsoleUtil.printHeader("My Payment Methods");

        List<PaymentMethod> list;
        try {
            list = paymentMethodDao.findByUserId(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch payment methods (userId={})", userId, e);
            System.out.println("Unable to load payment methods.");
            return;
        }

        if (list == null || list.isEmpty()) {
            logger.info("No payment methods found (userId={})", userId);
            System.out.println("No payment methods found.");
            return;
        }

        logger.debug("Payment methods fetched (userId={}, count={})", userId, list.size());

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
        try {
            methodId = Long.parseLong(in);
        } catch (NumberFormatException e) {
            logger.warn("Delete payment method failed: invalid methodId input '{}' (userId={})", safe(in), userId);
            System.out.println("Invalid ID.");
            return;
        }

        boolean ok;
        try {
            ok = paymentMethodDao.delete(userId, methodId);
        } catch (Exception e) {
            logger.error("Delete payment method failed: DAO error (userId={}, methodId={})", userId, methodId, e);
            System.out.println("Failed to delete payment method.");
            return;
        }

        if (ok) {
            logger.info("Payment method deleted (userId={}, methodId={})", userId, methodId);
        } else {
            logger.warn("Payment method delete: not found for user (userId={}, methodId={})", userId, methodId);
        }

        System.out.println(ok ? "Deleted." : "Method not found for this user.");
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
