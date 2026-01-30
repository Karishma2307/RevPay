package revpay.service;

import java.util.Scanner;

import revpay.dao.BusinessProfileDao;
import revpay.dao.impl.BusinessProfileDaoImpl;
import revpay.util.ConsoleUtil;
import revpay.util.ValidationUtil;

public class BusinessProfileService {

    private BusinessProfileDao dao = new BusinessProfileDaoImpl();

    public boolean collectAndSave(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Business Registration Details");

        System.out.print("Business Name: ");
        String name = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(name)) {
            System.out.println("[ERROR] Business name cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Business Type (e.g., Retail/Services): ");
        String type = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(type)) {
            System.out.println("[ERROR] Business type cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Tax ID: ");
        String taxId = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(taxId)) {
            System.out.println("[ERROR] Tax ID cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Address: ");
        String address = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(address)) {
            System.out.println("[ERROR] Address cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Verification Document Filename (simulated): ");
        String doc = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(doc)) doc = "N/A";

        dao.create(userId, name.trim(), type.trim(), taxId.trim(), address.trim(), doc.trim(), "PENDING");

        System.out.println("[INFO] Business profile saved (Status: PENDING verification).");
        ConsoleUtil.pause(sc);
        return true;
    }
}