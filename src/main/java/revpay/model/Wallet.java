package revpay.model;

public class Wallet {

    private long walletId;
    private long userId;
    private double balance;
    private String currency;

    public long getWalletId() {
        return walletId;
    }
    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }
    public long getUserId() {
        return userId;
    }
    public void setUserId(long userId) {
        this.userId = userId;
    }
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
