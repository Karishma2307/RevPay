package revpay.dao;

import java.util.List;

import revpay.model.Invoice;

public interface InvoiceDao {

    long createInvoice(Invoice invoice);

    void updateStatus(long invoiceId, String newStatus);

    Invoice findById(long invoiceId);

    java.util.List<Invoice> findByBusinessUser(long businessUserId);
}