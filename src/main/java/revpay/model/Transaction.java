package revpay.model;

import java.sql.Timestamp;

public class Transaction {

    private long transactionId;
    private long fromUserId;
    private long toUserId;
    private double amount;
    private String currency;
    private String type;
    private String status;
    private String note;
    private Timestamp createdAt;

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public long getFromUserId() { return fromUserId; }
    public void setFromUserId(long fromUserId) { this.fromUserId = fromUserId; }

    public long getToUserId() { return toUserId; }
    public void setToUserId(long toUserId) { this.toUserId = toUserId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
