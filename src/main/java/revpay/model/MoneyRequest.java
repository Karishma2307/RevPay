package revpay.model;

import java.util.Date;

public class MoneyRequest {

    private long requestId;
    private long fromUserId; // requester
    private long toUserId;   // target who should pay
    private double amount;
    private String currency;
    private String note;
    private String status;   // PENDING, ACCEPTED, DECLINED, CANCELLED
    private Date createdAt;
    private Date updatedAt;

    public long getRequestId() {
        return requestId;
    }
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    public long getFromUserId() {
        return fromUserId;
    }
    public void setFromUserId(long fromUserId) {
        this.fromUserId = fromUserId;
    }
    public long getToUserId() {
        return toUserId;
    }
    public void setToUserId(long toUserId) {
        this.toUserId = toUserId;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
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
