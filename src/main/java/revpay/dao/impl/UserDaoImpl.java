package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import revpay.dao.UserDao;
import revpay.model.User;
import revpay.util.DBConnection;

public class UserDaoImpl implements UserDao {

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("USER_ID"));
        u.setAccountId(rs.getString("ACCOUNT_ID"));
        u.setAccountType(rs.getString("ACCOUNT_TYPE"));
        u.setFullName(rs.getString("FULL_NAME"));
        u.setUsername(rs.getString("USERNAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setPhone(rs.getString("PHONE"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setTxnPinHash(rs.getString("TXN_PIN_HASH"));
        u.setStatus(rs.getString("STATUS"));
        u.setFailedLoginAttempts(rs.getInt("FAILED_LOGIN_ATTEMPTS"));
        return u;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM USERS WHERE EMAIL = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    public User findByPhone(String phone) {
        String sql = "SELECT * FROM USERS WHERE PHONE = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, phone);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    public User findByAccountId(String accountId) {
        String sql = "SELECT * FROM USERS WHERE ACCOUNT_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, accountId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    public long createUser(User user) {
        String sql = "INSERT INTO USERS (USER_ID, ACCOUNT_ID, ACCOUNT_TYPE, FULL_NAME, USERNAME, EMAIL, PHONE, PASSWORD_HASH, TXN_PIN_HASH, STATUS, FAILED_LOGIN_ATTEMPTS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 0, SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            // 1) Get ONE new id from the sequence
            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_USERS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            rs.close();
            rs = null;
            st.close();
            st = null;

            // 2) Insert using THAT id - no SEQ_USERS.NEXTVAL in SQL now
            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);                          // USER_ID
            ps.setString(2, user.getAccountId());          // ACCOUNT_ID
            ps.setString(3, user.getAccountType());        // ACCOUNT_TYPE
            ps.setString(4, user.getFullName());           // FULL_NAME
            ps.setString(5, user.getUsername());           // USERNAME
            ps.setString(6, user.getEmail());              // EMAIL
            ps.setString(7, user.getPhone());              // PHONE
            ps.setString(8, user.getPasswordHash());       // PASSWORD_HASH
            ps.setString(9, user.getTxnPinHash());         // TXN_PIN_HASH
            ps.executeUpdate();

            user.setUserId(newId);

        } catch (SQLException e) {
            e.printStackTrace();
            newId = -1;
        } finally {
            // close ps and conn (st and rs already closed above)
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }

        return newId;
    }


    public void updateFailedAttempts(long userId, int attempts) {
        String sql = "UPDATE USERS SET FAILED_LOGIN_ATTEMPTS = ?, UPDATED_AT = SYSDATE WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, attempts);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    public void updateStatus(long userId, String status) {
        String sql = "UPDATE USERS SET STATUS = ?, UPDATED_AT = SYSDATE WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    private void close(ResultSet rs, Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
