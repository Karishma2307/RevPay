package revpay.model;

import java.util.Date;

public class Transaction {

    private long transactionId;
    private Long fromUserId;
    private Long toUserId;
    private double amount;
    private String currency;
    private String type;    // TRANSFER, WALLET_TOPUP, WALLET_WITHDRAW
    private String status;  // SUCCESS, FAILED
    private String note;
    private Date createdAt;

    public long getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }
    public Long getFromUserId() {
        return fromUserId;
    }
    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }
    public Long getToUserId() {
        return toUserId;
    }
    public void setToUserId(Long toUserId) {
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
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    private String refId;  // optional reference id

    public String getRefId() {
        return refId;
    }
    public void setRefId(String refId) {
        this.refId = refId;
    }

    // Backward-compatible aliases used by some DAO/Export code
    public long getTxnId() {
        return getTransactionId();
    }
    public void setTxnId(long txnId) {
        setTransactionId(txnId);
    }
}