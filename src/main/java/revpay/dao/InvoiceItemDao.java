package revpay.dao;

import java.util.List;

import revpay.model.InvoiceItem;

public interface InvoiceItemDao {

    void createItem(InvoiceItem item);

    java.util.List<InvoiceItem> findByInvoiceId(long invoiceId);
}
