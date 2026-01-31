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

    
    public void checkAndNotify(long userId) {

        
        prefDao.ensureExists(userId);

        
        if (!prefDao.isEnabled(userId, "ALERT")) return;

        Wallet w = walletDao.getWalletByUserId(userId);
        if (w == null) return;

        int threshold = prefDao.getLowBalanceThreshold(userId);

        if (w.getBalance() < threshold) {
            
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
