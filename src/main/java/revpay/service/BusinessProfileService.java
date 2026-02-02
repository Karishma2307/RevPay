package revpay.service;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.BusinessProfileDao;
import revpay.dao.impl.BusinessProfileDaoImpl;
import revpay.util.ConsoleUtil;
import revpay.util.ValidationUtil;

public class BusinessProfileService {

    private static final Logger logger =
            LoggerFactory.getLogger(BusinessProfileService.class);

    private BusinessProfileDao dao = new BusinessProfileDaoImpl();

    public boolean collectAndSave(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Business Registration Details");
        logger.info("Business profile collection started (userId={})", userId);

        System.out.print("Business Name: ");
        String name = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(name)) {
            logger.warn("Business profile failed: empty business name (userId={})", userId);
            System.out.println("Business name cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Business Type (e.g., Retail/Services): ");
        String type = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(type)) {
            logger.warn("Business profile failed: empty business type (userId={})", userId);
            System.out.println("Business type cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Tax ID: ");
        String taxId = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(taxId)) {
            logger.warn("Business profile failed: empty tax ID (userId={})", userId);
            System.out.println("Tax ID cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Address: ");
        String address = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(address)) {
            logger.warn("Business profile failed: empty address (userId={})", userId);
            System.out.println("Address cannot be empty.");
            ConsoleUtil.pause(sc);
            return false;
        }

        System.out.print("Verification Document Filename (simulated): ");
        String doc = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(doc)) {
            doc = "N/A";
        }

        try {
            dao.create(
                    userId,
                    name.trim(),
                    type.trim(),
                    taxId.trim(),
                    address.trim(),
                    doc.trim(),
                    "PENDING"
            );
        } catch (Exception e) {
            logger.error("Business profile save failed due to DAO error (userId={})", userId, e);
            System.out.println("Failed to save business profile. Please try again.");
            ConsoleUtil.pause(sc);
            return false;
        }

        logger.info("Business profile saved successfully (userId={}, status=PENDING)", userId);
        System.out.println("Business profile saved (Status: PENDING verification).");
        ConsoleUtil.pause(sc);
        return true;
    }
}
