package revpay.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
            try { qty = Double.parseDouble(sc.nextLine()); }
            catch (Exception e) { System.out.println("[ERROR] Invalid quantity."); continue; }

            System.out.print("  Unit Price  : ");
            double up;
            try { up = Double.parseDouble(sc.nextLine()); }
            catch (Exception e) { System.out.println("[ERROR] Invalid unit price."); continue; }

            if (qty <= 0 || up < 0) {
                System.out.println("[ERROR] Quantity must be > 0 and Unit Price must be >= 0.");
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
            System.out.println("[ERROR] Invoice must have at least one line item.");
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

        long id = invoiceDao.createInvoice(inv);
        if (id <= 0) {
            System.out.println("[ERROR] Failed to create invoice.");
            ConsoleUtil.pause(sc);
            return;
        }

        for (InvoiceItem it : items) {
            it.setInvoiceId(id);
            invoiceItemDao.createItem(it);
        }

        System.out.println("[INFO] Invoice created successfully.");
        System.out.println("Invoice ID   : " + id);
        System.out.printf("Total Amount : ₹%.2f%n", total);
        System.out.println("Status       : PENDING");

        notificationService.notifyUser(
                businessUser.getUserId(),
                "INVOICE",
                "Invoice Created",
                "Invoice #" + id + " created for " + customerName + " (₹" + String.format("%.2f", total) + ")."
        );

        ConsoleUtil.pause(sc);
    }

    public void manageInvoices(Scanner sc, User businessUser) {

        while (true) {
            ConsoleUtil.printHeader("Manage Invoices");

            List<Invoice> list = invoiceDao.findByBusinessUser(businessUser.getUserId());
            if (list == null || list.isEmpty()) {
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

            if ("4".equals(ch)) break;

            System.out.print("Enter Invoice ID: ");
            long invoiceId;
            try { invoiceId = Long.parseLong(sc.nextLine()); }
            catch (Exception e) { System.out.println("[ERROR] Invalid invoice ID."); ConsoleUtil.pause(sc); continue; }

            Invoice inv = invoiceDao.findById(invoiceId);
            if (inv == null || inv.getBusinessUserId() != businessUser.getUserId()) {
                System.out.println("[ERROR] Invoice not found for this business account.");
                ConsoleUtil.pause(sc);
                continue;
            }

            if ("1".equals(ch)) {
                viewInvoiceDetails(sc, inv);
            } else if ("2".equals(ch)) {
                markInvoicePaid(sc, businessUser, inv);
            } else if ("3".equals(ch)) {
                invoiceDao.updateStatus(inv.getInvoiceId(), "CANCELLED");

                notificationService.notifyUser(
                        businessUser.getUserId(),
                        "INVOICE",
                        "Invoice Cancelled",
                        "Invoice #" + inv.getInvoiceId() + " cancelled."
                );

                System.out.println("[INFO] Invoice marked as CANCELLED.");
                ConsoleUtil.pause(sc);
            } else {
                System.out.println("[ERROR] Invalid option.");
                ConsoleUtil.pause(sc);
            }
        }
    }

    private void viewInvoiceDetails(Scanner sc, Invoice inv) {

        ConsoleUtil.printHeader("Invoice #" + inv.getInvoiceId());

        System.out.println("Customer: " + inv.getCustomerName());
        System.out.println("Email   : " + inv.getCustomerEmail());
        System.out.println("Phone   : " + inv.getCustomerPhone());
        String dueStr = (inv.getDueDate() == null) ? "-" : String.valueOf(inv.getDueDate());
        System.out.println("Due Date: " + dueStr);
        System.out.println("Status  : " + inv.getStatus());
        System.out.printf("Total   : ₹%.2f%n", inv.getTotalAmount());
        System.out.println("--------------------------------------------------");

        List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(inv.getInvoiceId());
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
            System.out.println("[INFO] Invoice already PAID.");
            ConsoleUtil.pause(sc);
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(inv.getStatus())) {
            System.out.println("[INFO] Cannot pay a CANCELLED invoice.");
            ConsoleUtil.pause(sc);
            return;
        }

        Wallet wallet = walletDao.getWalletByUserId(businessUser.getUserId());
        if (wallet == null) {
            walletDao.createWalletForUser(businessUser.getUserId());
            wallet = walletDao.getWalletByUserId(businessUser.getUserId());
        }

        double newBalance = wallet.getBalance() + inv.getTotalAmount();
        walletDao.updateBalance(businessUser.getUserId(), newBalance);

     
        transactionDao.createTransaction(
                0L,
                businessUser.getUserId(),
                inv.getTotalAmount(),
                "INVOICE_PAYMENT",
                "SUCCESS",
                "Invoice #" + inv.getInvoiceId() + " paid"
        );

        invoiceDao.updateStatus(inv.getInvoiceId(), "PAID");

        notificationService.notifyUser(
                businessUser.getUserId(),
                "INVOICE",
                "Invoice Paid",
                "Invoice #" + inv.getInvoiceId() + " PAID. Amount ₹" + String.format("%.2f", inv.getTotalAmount())
        );

        System.out.println("[INFO] Invoice marked PAID. Wallet credited.");
        System.out.printf("New Wallet Balance: ₹%.2f%n", newBalance);
        ConsoleUtil.pause(sc);
    }

    private String shortStr(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + ".";
    }
}
