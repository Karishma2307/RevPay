package revpay.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferTxHelper {

    private static final Logger logger = LoggerFactory.getLogger(TransferTxHelper.class);

    private TransferTxHelper() {}

    public static boolean performTransfer(
            Connection con,
            long fromUserId,
            long toUserId,
            double amount,
            String note
    ) throws Exception {

        logger.debug("Performing transfer (fromUserId={}, toUserId={}, amount={})",
                fromUserId, toUserId, fmt(amount));

        Double fromBal = getBalance(con, fromUserId);
        if (fromBal == null) {
            logger.warn("Transfer failed: sender wallet not found (fromUserId={})", fromUserId);
            return false;
        }

        if (fromBal < amount) {
            logger.warn("Transfer failed: insufficient balance (fromUserId={}, balance={}, amount={})",
                    fromUserId, fmt(fromBal), fmt(amount));
            return false;
        }

        Double toBal = getBalance(con, toUserId);
        if (toBal == null) {
            logger.info("Receiver wallet not found, creating wallet (toUserId={})", toUserId);
            createWallet(con, toUserId);
            toBal = 0.0;
        }

        updateBalance(con, fromUserId, fromBal - amount);
        updateBalance(con, toUserId, toBal + amount);

        insertTransaction(con, fromUserId, toUserId, amount, note);

        logger.info("Transfer DB operations completed (fromUserId={}, toUserId={}, amount={})",
                fromUserId, toUserId, fmt(amount));

        return true;
    }

    private static Double getBalance(Connection con, long userId) throws Exception {
        String sql = "SELECT BALANCE FROM WALLETS WHERE USER_ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double bal = rs.getDouble("BALANCE");
                    logger.debug("Wallet balance fetched (userId={}, balance={})", userId, fmt(bal));
                    return bal;
                }

                logger.debug("Wallet not found (userId={})", userId);
                return null;
            }
        }
    }

    private static void createWallet(Connection con, long userId) throws Exception {
        String sql =
            "INSERT INTO WALLETS (WALLET_ID, USER_ID, BALANCE) " +
            "VALUES (WALLETS_SEQ.NEXTVAL, ?, 0)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
            logger.info("Wallet created (userId={})", userId);
        }
    }

    private static void updateBalance(Connection con, long userId, double bal) throws Exception {
        String sql = "UPDATE WALLETS SET BALANCE = ? WHERE USER_ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, bal);
            ps.setLong(2, userId);
            ps.executeUpdate();
            logger.debug("Wallet balance updated (userId={}, newBalance={})", userId, fmt(bal));
        }
    }

    private static void insertTransaction(
            Connection con,
            long fromUserId,
            long toUserId,
            double amount,
            String note
    ) throws Exception {

        String sql =
            "INSERT INTO TRANSACTIONS (" +
            "TRANSACTION_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, CURRENCY, TYPE, STATUS, NOTE, CREATED_AT" +
            ") VALUES (TRANSACTIONS_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            ps.setDouble(3, amount);
            ps.setString(4, "INR");
            ps.setString(5, "TRANSFER");
            ps.setString(6, "SUCCESS");
            ps.setString(7, note);
            ps.executeUpdate();

            logger.debug("Transaction record inserted (fromUserId={}, toUserId={}, amount={})",
                    fromUserId, toUserId, fmt(amount));
        }
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
