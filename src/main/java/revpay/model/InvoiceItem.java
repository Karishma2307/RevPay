package revpay.model;

public class InvoiceItem {

    private long itemId;
    private long invoiceId;
    private String description;
    private double quantity;
    private double unitPrice;
    private double lineTotal;

    public long getItemId() {
        return itemId;
    }
    public void setItemId(long itemId) {
        this.itemId = itemId;
    }
    public long getInvoiceId() {
        return invoiceId;
    }
    public void setInvoiceId(long invoiceId) {
        this.invoiceId = invoiceId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public double getQuantity() {
        return quantity;
    }
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    public double getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
    public double getLineTotal() {
        return lineTotal;
    }
    public void setLineTotal(double lineTotal) {
        this.lineTotal = lineTotal;
    }
}
