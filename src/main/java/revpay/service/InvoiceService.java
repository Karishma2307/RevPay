package revpay.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.InvoiceDao;
import revpay.dao.InvoiceItemDao;
import revpay.dao.TransactionDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.InvoiceDaoImpl;
import revpay.dao.impl.InvoiceItemDaoImpl;
import revpay.dao.impl.TransactionDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Invoice;
import revpay.model.InvoiceItem;
import revpay.model.User;
import revpay.model.Wallet;
import revpay.util.ConsoleUtil;

public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceDao invoiceDao = new InvoiceDaoImpl();
    private final InvoiceItemDao invoiceItemDao = new InvoiceItemDaoImpl();
    private final WalletDao walletDao = new WalletDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final NotificationService notificationService;

    public InvoiceService() {
        this.notificationService = new NotificationService();
    }

    public InvoiceService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void createInvoice(Scanner sc, User businessUser) {
        ConsoleUtil.printHeader("Create Invoice");

        if (businessUser == null) {
            logger.warn("createInvoice called with null businessUser");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Create invoice started (businessUserId={}, username='{}')",
                businessUser.getUserId(), safe(businessUser.getUsername()));

        System.out.print("Customer Name : ");
        String customerName = sc.nextLine();

        System.out.print("Customer Email: ");
        String customerEmail = sc.nextLine();

        System.out.print("Customer Phone: ");
        String customerPhone = sc.nextLine();

        LocalDate dueLocal = ConsoleUtil.readOptionalDate(sc, "Due Date (yyyy-MM-dd, blank for none): ");
        Date dueDate = (dueLocal == null) ? null : Date.valueOf(dueLocal);

        List<InvoiceItem> items = new ArrayList<>();
        double total = 0.0;

        while (true) {
            System.out.print("Add line item? (y/n): ");
            String ch = sc.nextLine();
            if (!"y".equalsIgnoreCase(ch)) break;

            InvoiceItem item = new InvoiceItem();

            System.out.print("  Description : ");
            item.setDescription(sc.nextLine());

            System.out.print("  Quantity    : ");
            double qty;
            try {
                qty = Double.parseDouble(sc.nextLine());
            } catch (Exception e) {
                logger.warn("Invalid quantity entered while creating invoice (businessUserId={})",
                        businessUser.getUserId());
                System.out.println("Invalid quantity.");
                continue;
            }

            System.out.print("  Unit Price  : ");
            double up;
            try {
                up = Double.parseDouble(sc.nextLine());
            } catch (Exception e) {
                logger.warn("Invalid unit price entered while creating invoice (businessUserId={})",
                        businessUser.getUserId());
                System.out.println("Invalid unit price.");
                continue;
            }

            if (qty <= 0 || up < 0) {
                logger.warn("Invalid item values qty={} unitPrice={} (businessUserId={})",
                        qty, up, businessUser.getUserId());
                System.out.println("Quantity must be > 0 and Unit Price must be >= 0.");
                continue;
            }

            double lineTotal = qty * up;

            item.setQuantity(qty);
            item.setUnitPrice(up);
            item.setLineTotal(lineTotal);

            items.add(item);
            total += lineTotal;

            System.out.printf("  Line total: ₹%.2f (current invoice total: ₹%.2f)%n", lineTotal, total);
        }

        if (items.isEmpty()) {
            logger.warn("Invoice creation failed: no line items (businessUserId={})", businessUser.getUserId());
            System.out.println("Invoice must have at least one line item.");
            ConsoleUtil.pause(sc);
            return;
        }

        Invoice inv = new Invoice();
        inv.setBusinessUserId(businessUser.getUserId());
        inv.setCustomerName(customerName);
        inv.setCustomerEmail(customerEmail);
        inv.setCustomerPhone(customerPhone);
        inv.setDueDate(dueDate);
        inv.setTotalAmount(total);
        inv.setStatus("PENDING");

        long id;
        try {
            id = invoiceDao.createInvoice(inv);
        } catch (Exception e) {
            logger.error("Invoice creation failed: DAO exception while creating invoice (businessUserId={})",
                    businessUser.getUserId(), e);
            System.out.println("Failed to create invoice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (id <= 0) {
            logger.error("Invoice creation failed: createInvoice returned non-positive id (businessUserId={}, returnedId={})",
                    businessUser.getUserId(), id);
            System.out.println("Failed to create invoice.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            for (InvoiceItem it : items) {
                it.setInvoiceId(id);
                invoiceItemDao.createItem(it);
            }
        } catch (Exception e) {
            logger.error("Invoice items creation failed (invoiceId={}, businessUserId={})",
                    id, businessUser.getUserId(), e);
            System.out.println("Invoice created but failed to save line items.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Invoice created successfully (invoiceId={}, businessUserId={}, total={})",
                id, businessUser.getUserId(), fmt(total));

        System.out.println("[INFO] Invoice created successfully.");
        System.out.println("Invoice ID   : " + id);
        System.out.printf("Total Amount : ₹%.2f%n", total);
        System.out.println("Status       : PENDING");

        try {
            notificationService.notifyUser(
                    businessUser.getUserId(),
                    "INVOICE",
                    "Invoice Created",
                    "Invoice #" + id + " created for " + customerName + " (₹" + String.format("%.2f", total) + ")."
            );
            logger.debug("Notification sent for invoice created (invoiceId={}, businessUserId={})",
                    id, businessUser.getUserId());
        } catch (Exception e) {
            logger.error("Failed to send notification for invoice creation (invoiceId={}, businessUserId={})",
                    id, businessUser.getUserId(), e);
        }

        ConsoleUtil.pause(sc);
    }

    public void manageInvoices(Scanner sc, User businessUser) {

        if (businessUser == null) {
            logger.warn("manageInvoices called with null businessUser");
            System.out.println("Invalid user.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Manage invoices opened (businessUserId={}, username='{}')",
                businessUser.getUserId(), safe(businessUser.getUsername()));

        while (true) {
            ConsoleUtil.printHeader("Manage Invoices");

            List<Invoice> list;
            try {
                list = invoiceDao.findByBusinessUser(businessUser.getUserId());
            } catch (Exception e) {
                logger.error("Failed to fetch invoices list (businessUserId={})",
                        businessUser.getUserId(), e);
                System.out.println("Unable to load invoices right now.");
                ConsoleUtil.pause(sc);
                return;
            }

            if (list == null || list.isEmpty()) {
                logger.info("No invoices found (businessUserId={})", businessUser.getUserId());
                System.out.println("No invoices found.");
                ConsoleUtil.pause(sc);
                return;
            }

            System.out.println("ID   | Customer        | Total   | Status   | Due Date");
            System.out.println("-----------------------------------------------------------");
            for (Invoice inv : list) {
                String dueStr = (inv.getDueDate() == null) ? "-" : String.valueOf(inv.getDueDate());
                System.out.printf("%-4d | %-14s | ₹%-7.2f | %-8s | %s%n",
                        inv.getInvoiceId(),
                        shortStr(inv.getCustomerName(), 14),
                        inv.getTotalAmount(),
                        inv.getStatus(),
                        dueStr);
            }

            System.out.println("-----------------------------------------------------------");
            System.out.println("1. View Invoice Details");
            System.out.println("2. Mark Invoice as PAID (credit wallet)");
            System.out.println("3. Mark Invoice as CANCELLED");
            System.out.println("4. Back");
            System.out.print("Choice: ");
            String ch = sc.nextLine();

            if ("4".equals(ch)) {
                logger.info("Manage invoices closed (businessUserId={})", businessUser.getUserId());
                break;
            }

            System.out.print("Enter Invoice ID: ");
            long invoiceId;
            try {
                invoiceId = Long.parseLong(sc.nextLine());
            } catch (Exception e) {
                logger.warn("Invalid invoice id input while managing invoices (businessUserId={})",
                        businessUser.getUserId());
                System.out.println("Invalid invoice ID.");
                ConsoleUtil.pause(sc);
                continue;
            }

            Invoice inv;
            try {
                inv = invoiceDao.findById(invoiceId);
            } catch (Exception e) {
                logger.error("DAO error while fetching invoice by id (invoiceId={}, businessUserId={})",
                        invoiceId, businessUser.getUserId(), e);
                System.out.println("Unable to fetch invoice.");
                ConsoleUtil.pause(sc);
                continue;
            }

            if (inv == null || inv.getBusinessUserId() != businessUser.getUserId()) {
                logger.warn("Invoice not found for business user (invoiceId={}, businessUserId={})",
                        invoiceId, businessUser.getUserId());
                System.out.println("Invoice not found for this business account.");
                ConsoleUtil.pause(sc);
                continue;
            }

            if ("1".equals(ch)) {
                viewInvoiceDetails(sc, inv);

            } else if ("2".equals(ch)) {
                markInvoicePaid(sc, businessUser, inv);

            } else if ("3".equals(ch)) {
                try {
                    invoiceDao.updateStatus(inv.getInvoiceId(), "CANCELLED");
                    logger.info("Invoice marked CANCELLED (invoiceId={}, businessUserId={})",
                            inv.getInvoiceId(), businessUser.getUserId());
                } catch (Exception e) {
                    logger.error("Failed to cancel invoice (invoiceId={}, businessUserId={})",
                            inv.getInvoiceId(), businessUser.getUserId(), e);
                    System.out.println("Failed to cancel invoice.");
                    ConsoleUtil.pause(sc);
                    continue;
                }

                try {
                    notificationService.notifyUser(
                            businessUser.getUserId(),
                            "INVOICE",
                            "Invoice Cancelled",
                            "Invoice #" + inv.getInvoiceId() + " cancelled."
                    );
                } catch (Exception e) {
                    logger.error("Failed to send cancel notification (invoiceId={}, businessUserId={})",
                            inv.getInvoiceId(), businessUser.getUserId(), e);
                }

                System.out.println("Invoice marked as CANCELLED.");
                ConsoleUtil.pause(sc);

            } else {
                logger.warn("Invalid manage invoices option '{}' (businessUserId={})", ch, businessUser.getUserId());
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private void viewInvoiceDetails(Scanner sc, Invoice inv) {

        ConsoleUtil.printHeader("Invoice #" + inv.getInvoiceId());
        logger.debug("Viewing invoice details (invoiceId={})", inv.getInvoiceId());

        System.out.println("Customer: " + inv.getCustomerName());
        System.out.println("Email   : " + inv.getCustomerEmail());
        System.out.println("Phone   : " + inv.getCustomerPhone());
        String dueStr = (inv.getDueDate() == null) ? "-" : String.valueOf(inv.getDueDate());
        System.out.println("Due Date: " + dueStr);
        System.out.println("Status  : " + inv.getStatus());
        System.out.printf("Total   : ₹%.2f%n", inv.getTotalAmount());
        System.out.println("--------------------------------------------------");

        List<InvoiceItem> items;
        try {
            items = invoiceItemDao.findByInvoiceId(inv.getInvoiceId());
        } catch (Exception e) {
            logger.error("Failed to fetch invoice items (invoiceId={})", inv.getInvoiceId(), e);
            System.out.println("Unable to load invoice items.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (items == null || items.isEmpty()) {
            System.out.println("No items.");
        } else {
            System.out.println("Description          | Qty    | Unit   | Line Total");
            System.out.println("--------------------------------------------------");
            for (InvoiceItem it : items) {
                System.out.printf("%-20s | %-6.2f | %-6.2f | %-10.2f%n",
                        shortStr(it.getDescription(), 20),
                        it.getQuantity(),
                        it.getUnitPrice(),
                        it.getLineTotal());
            }
        }

        ConsoleUtil.pause(sc);
    }

    private void markInvoicePaid(Scanner sc, User businessUser, Invoice inv) {

        if ("PAID".equalsIgnoreCase(inv.getStatus())) {
            logger.info("Mark paid skipped: already PAID (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId());
            System.out.println("Invoice already PAID.");
            ConsoleUtil.pause(sc);
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(inv.getStatus())) {
            logger.warn("Mark paid blocked: invoice CANCELLED (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId());
            System.out.println("Cannot pay a CANCELLED invoice.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet;
        try {
            wallet = walletDao.getWalletByUserId(businessUser.getUserId());
            if (wallet == null) {
                walletDao.createWalletForUser(businessUser.getUserId());
                wallet = walletDao.getWalletByUserId(businessUser.getUserId());
            }
        } catch (Exception e) {
            logger.error("Wallet access failed while marking invoice paid (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId(), e);
            System.out.println("Wallet not available.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (wallet == null) {
            logger.error("Wallet is still null after creation attempt (businessUserId={})", businessUser.getUserId());
            System.out.println("Wallet not available.");
            ConsoleUtil.pause(sc);
            return;
        }

        double newBalance = wallet.getBalance() + inv.getTotalAmount();

        try {
            walletDao.updateBalance(businessUser.getUserId(), newBalance);
        } catch (Exception e) {
            logger.error("Failed to update wallet balance (businessUserId={}, newBalance={})",
                    businessUser.getUserId(), fmt(newBalance), e);
            System.out.println("Failed to credit wallet.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            transactionDao.createTransaction(
                    0L,
                    businessUser.getUserId(),
                    inv.getTotalAmount(),
                    "INVOICE_PAYMENT",
                    "SUCCESS",
                    "Invoice #" + inv.getInvoiceId() + " paid"
            );
        } catch (Exception e) {
            logger.error("Failed to create transaction for invoice payment (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId(), e);
            System.out.println("Payment recorded failed.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            invoiceDao.updateStatus(inv.getInvoiceId(), "PAID");
        } catch (Exception e) {
            logger.error("Failed to update invoice status to PAID (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId(), e);
            System.out.println("Failed to mark invoice PAID.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            notificationService.notifyUser(
                    businessUser.getUserId(),
                    "INVOICE",
                    "Invoice Paid",
                    "Invoice #" + inv.getInvoiceId() + " PAID. Amount ₹" + String.format("%.2f", inv.getTotalAmount())
            );
        } catch (Exception e) {
            logger.error("Failed to send paid notification (invoiceId={}, businessUserId={})",
                    inv.getInvoiceId(), businessUser.getUserId(), e);
        }

        logger.info("Invoice marked PAID and wallet credited (invoiceId={}, businessUserId={}, amount={}, newBalance={})",
                inv.getInvoiceId(), businessUser.getUserId(), fmt(inv.getTotalAmount()), fmt(newBalance));

        System.out.println("Invoice marked PAID. Wallet credited.");
        System.out.printf("New Wallet Balance: ₹%.2f%n", newBalance);
        ConsoleUtil.pause(sc);
    }

    private String shortStr(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + ".";
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }
}
