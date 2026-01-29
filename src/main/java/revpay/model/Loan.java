package revpay.model;

import java.util.Date;

public class Loan {

    private long loanId;
    private long businessUserId;
    private double amount;
    private double interestRate;
    private int termMonths;
    private String purpose;
    private String status;            // PENDING, APPROVED, ACTIVE, CLOSED, REJECTED
    private double approvedAmount;
    private double outstandingAmount;
    private Date createdAt;
    private Date updatedAt;

    public long getLoanId() {
        return loanId;
    }
    public void setLoanId(long loanId) {
        this.loanId = loanId;
    }
    public long getBusinessUserId() {
        return businessUserId;
    }
    public void setBusinessUserId(long businessUserId) {
        this.businessUserId = businessUserId;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public double getInterestRate() {
        return interestRate;
    }
    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }
    public int getTermMonths() {
        return termMonths;
    }
    public void setTermMonths(int termMonths) {
        this.termMonths = termMonths;
    }
    public String getPurpose() {
        return purpose;
    }
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public double getApprovedAmount() {
        return approvedAmount;
    }
    public void setApprovedAmount(double approvedAmount) {
        this.approvedAmount = approvedAmount;
    }
    public double getOutstandingAmount() {
        return outstandingAmount;
    }
    public void setOutstandingAmount(double outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
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
