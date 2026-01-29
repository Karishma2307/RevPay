package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    public Wallet findByUserId(long userId) {
        String sql = "SELECT * FROM WALLETS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Wallet w = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                w = new Wallet();
                w.setWalletId(rs.getLong("WALLET_ID"));
                w.setUserId(rs.getLong("USER_ID"));
                w.setBalance(rs.getDouble("BALANCE"));
                w.setCurrency(rs.getString("CURRENCY"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return w;
    }

    public void updateBalance(long walletId, double newBalance) {
        String sql = "UPDATE WALLETS SET BALANCE = ?, UPDATED_AT = SYSDATE WHERE WALLET_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, newBalance);
            ps.setLong(2, walletId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
