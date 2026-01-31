package revpay.model;

import java.util.Date;

public class Invoice {

    private long invoiceId;
    private long businessUserId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Date dueDate;
    private double totalAmount;
    private String status; 
    private Date createdAt;
    private Date updatedAt;

    public long getInvoiceId() {
        return invoiceId;
    }
    public void setInvoiceId(long invoiceId) {
        this.invoiceId = invoiceId;
    }
    public long getBusinessUserId() {
        return businessUserId;
    }
    public void setBusinessUserId(long businessUserId) {
        this.businessUserId = businessUserId;
    }
    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getCustomerEmail() {
        return customerEmail;
    }
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    public String getCustomerPhone() {
        return customerPhone;
    }
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    public Date getDueDate() {
        return dueDate;
    }
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
    public double getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public Date getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}