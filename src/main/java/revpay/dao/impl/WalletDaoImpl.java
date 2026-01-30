package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import revpay.dao.WalletDao;
import revpay.model.Wallet;
import revpay.util.DBConnection;

public class WalletDaoImpl implements WalletDao {

    public void createWalletForUser(long userId) {
        String sql = "INSERT INTO WALLETS (WALLET_ID, USER_ID, BALANCE, CURRENCY, CREATED_AT) "
                   + "VALUES (SEQ_WALLETS.NEXTVAL, ?, 0, 'USD', SYSDATE)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    public Wallet getWalletByUserId(long userId) {
        String sql = "SELECT * FROM WALLETS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Wallet w = new Wallet();
                w.setWalletId(rs.getLong("WALLET_ID"));
                w.setUserId(rs.getLong("USER_ID"));
                w.setBalance(rs.getDouble("BALANCE"));
                w.setCurrency(rs.getString("CURRENCY"));
                return w;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return null;
    }

    public void updateBalance(long userId, double newBalance) {
        String sql = "UPDATE WALLETS SET BALANCE = ? WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, newBalance);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}