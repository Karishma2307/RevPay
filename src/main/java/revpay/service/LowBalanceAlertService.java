package revpay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import revpay.dao.NotificationPrefDao;
import revpay.dao.WalletDao;
import revpay.dao.impl.NotificationPrefDaoImpl;
import revpay.dao.impl.WalletDaoImpl;
import revpay.model.Wallet;

public class LowBalanceAlertService {

    private static final Logger logger = LoggerFactory.getLogger(LowBalanceAlertService.class);

    private final WalletDao walletDao = new WalletDaoImpl();
    private final NotificationService notificationService = new NotificationService();
    private final NotificationPrefDao prefDao = new NotificationPrefDaoImpl();

    public void checkAndNotify(long userId) {

        logger.debug("Low balance check started (userId={})", userId);

       
        try {
            prefDao.ensureExists(userId);
        } catch (Exception e) {
            logger.error("Low balance check failed: ensureExists error (userId={})", userId, e);
            return;
        }

      
        boolean enabled;
        try {
            enabled = prefDao.isEnabled(userId, "ALERT");
        } catch (Exception e) {
            logger.error("Low balance check failed: isEnabled error (userId={})", userId, e);
            return;
        }

        if (!enabled) {
            logger.debug("Low balance alert disabled (userId={})", userId);
            return;
        }

      
        Wallet w;
        try {
            w = walletDao.getWalletByUserId(userId);
        } catch (Exception e) {
            logger.error("Low balance check failed: wallet fetch error (userId={})", userId, e);
            return;
        }

        if (w == null) {
            logger.warn("Low balance check skipped: wallet not found (userId={})", userId);
            return;
        }

        int threshold;
        try {
            threshold = prefDao.getLowBalanceThreshold(userId);
        } catch (Exception e) {
            logger.error("Low balance check failed: threshold fetch error (userId={})", userId, e);
            return;
        }

        double balance = w.getBalance();

        if (balance < threshold) {

            boolean shouldSend;
            try {
                shouldSend = prefDao.shouldSendLowBalanceAlert(userId);
            } catch (Exception e) {
                logger.error("Low balance check failed: shouldSendLowBalanceAlert error (userId={})", userId, e);
                return;
            }

            if (!shouldSend) {
                logger.debug("Low balance alert throttled (userId={}, balance={}, threshold={})",
                        userId, fmt(balance), threshold);
                return;
            }

            try {
                notificationService.notifyUser(
                        userId,
                        "ALERT",
                        "Low Balance Alert",
                        "Your wallet balance is low: $" + String.format("%.2f", balance)
                                + " (Threshold: $" + threshold + ")"
                );
                logger.info("Low balance alert sent (userId={}, balance={}, threshold={})",
                        userId, fmt(balance), threshold);
            } catch (Exception e) {
                logger.error("Failed to send low balance notification (userId={}, balance={}, threshold={})",
                        userId, fmt(balance), threshold, e);
                return;
            }

            try {
                prefDao.updateLastLowBalanceAlertNow(userId);
                logger.debug("Low balance alert timestamp updated (userId={})", userId);
            } catch (Exception e) {
                logger.error("Failed to update low balance alert timestamp (userId={})", userId, e);
            }

        } else {
            logger.debug("Low balance check OK (userId={}, balance={}, threshold={})",
                    userId, fmt(balance), threshold);
        }
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }
}
