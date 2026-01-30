package revpay.model;

public class PaymentMethod {
    private long methodId;
    private long userId;
    private String type;      // CARD / BANK etc
    private String label;     // "My Visa Card"
    private String provider;  // VISA / MASTERCARD
    private String last4;     // "1234"
    private String encNumber; // encrypted card/bank number
    private String isDefault; // 'Y' or 'N'

    public long getMethodId() { return methodId; }
    public void setMethodId(long methodId) { this.methodId = methodId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }

    public String getEncNumber() { return encNumber; }
    public void setEncNumber(String encNumber) { this.encNumber = encNumber; }

    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }
}
