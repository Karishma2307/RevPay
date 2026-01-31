package revpay.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransferTxHelper {

    private TransferTxHelper() {}

    public static boolean performTransfer(
            Connection con,
            long fromUserId,
            long toUserId,
            double amount,
            String note
    ) throws Exception {

        Double fromBal = getBalance(con, fromUserId);
        if (fromBal == null || fromBal < amount) return false;

        Double toBal = getBalance(con, toUserId);
        if (toBal == null) {
            createWallet(con, toUserId);
            toBal = 0.0;
        }

        updateBalance(con, fromUserId, fromBal - amount);
        updateBalance(con, toUserId, toBal + amount);

        insertTransaction(con, fromUserId, toUserId, amount, note);
        return true;
    }

    private static Double getBalance(Connection con, long userId) throws Exception {
        String sql = "SELECT BALANCE FROM WALLETS WHERE USER_ID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("BALANCE");
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
        }
    }

    private static void updateBalance(Connection con, long userId, double bal) throws Exception {
        String sql = "UPDATE WALLETS SET BALANCE = ? WHERE USER_ID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, bal);
            ps.setLong(2, userId);
            ps.executeUpdate();
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
        }
    }
}
