package revpay.dao;

public interface NotificationPrefDao {

    void ensureExists(long userId);

    boolean isEnabled(long userId, String type);

    int getLowBalanceThreshold(long userId);

    boolean shouldSendLowBalanceAlert(long userId);

    void updateLastLowBalanceAlertNow(long userId);

    void updatePref(long userId, String type, boolean enabled);

    void updateThreshold(long userId, int threshold);
}
