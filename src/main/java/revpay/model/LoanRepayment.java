package revpay.model;

import java.util.Date;

public class LoanRepayment {

    private long repaymentId;
    private long loanId;
    private double amount;
    private Date paidAt;
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRepaymentId() {
        return repaymentId;
    }
    public void setRepaymentId(long repaymentId) {
        this.repaymentId = repaymentId;
    }
    public long getLoanId() {
        return loanId;
    }
    public void setLoanId(long loanId) {
        this.loanId = loanId;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public Date getPaidAt() {
        return paidAt;
    }
    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }
}