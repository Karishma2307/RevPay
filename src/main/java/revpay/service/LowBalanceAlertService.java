package revpay.service;

import revpay.dao.NotificationPrefDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.NotificationPrefDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Wallet;

public class LowBalanceAlertService {

    private final WalletDao walletDao = new WalletDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final NotificationPrefDao prefDao = new NotificationPrefDaoImpl();

    // Sends low balance alert if below threshold and not spammed
    public void checkAndNotify(long userId) {

        // Ensure preference row exists
        prefDao.ensureExists(userId);

        // If alerts disabled, do nothing
        if (!prefDao.isEnabled(userId, "ALERT")) return;

        Wallet w = walletDao.getWalletByUserId(userId);
        if (w == null) return;

        int threshold = prefDao.getLowBalanceThreshold(userId);

        if (w.getBalance() < threshold) {
            // Anti-spam: only once per 10 minutes
            if (!prefDao.shouldSendLowBalanceAlert(userId)) return;

            notificationService.notifyUser(
                    userId,
                    "ALERT",
                    "Low Balance Alert",
                    "Your wallet balance is low: $" + String.format("%.2f", w.getBalance())
                            + " (Threshold: $" + threshold + ")"
            );

            prefDao.updateLastLowBalanceAlertNow(userId);
        }
    }
}
