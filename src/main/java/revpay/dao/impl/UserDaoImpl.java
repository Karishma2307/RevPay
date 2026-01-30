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
    
    @Override
    public boolean updatePassword(long userId, String newPasswordHash) {
        String sql = "UPDATE USERS SET PASSWORD_HASH = ? WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateTxnPin(long userId, String newTxnPinHash) {
        String sql = "UPDATE USERS SET TXN_PIN_HASH = ? WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newTxnPinHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
            if (rs.next()) return mapRow(rs);
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
            if (rs.next()) return mapRow(rs);
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
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM USERS WHERE USERNAME = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    public long createUser(User user) {
        String sql = "INSERT INTO USERS "
                + "(USER_ID, ACCOUNT_ID, ACCOUNT_TYPE, FULL_NAME, USERNAME, EMAIL, PHONE, PASSWORD_HASH, TXN_PIN_HASH, STATUS, FAILED_LOGIN_ATTEMPTS, CREATED_AT) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 0, SYSDATE)";

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        PreparedStatement ps = null;

        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            // 1) get new USER_ID from sequence
            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_USERS.NEXTVAL FROM DUAL");
            if (rs.next()) newId = rs.getLong(1);

            // close seq resources early
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (st != null) st.close(); } catch (Exception e) {}
            rs = null;
            st = null;

            // 2) insert row with that id
            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setString(2, user.getAccountId());
            ps.setString(3, user.getAccountType());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getUsername());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getPhone());
            ps.setString(8, user.getPasswordHash());
            ps.setString(9, user.getTxnPinHash());

            ps.executeUpdate();

            user.setUserId(newId);
            return newId;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;

        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (st != null) st.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    public void updateFailedAttempts(long userId, int attempts) {
        // Removed UPDATED_AT to avoid ORA-00904 if column doesn't exist
        String sql = "UPDATE USERS SET FAILED_LOGIN_ATTEMPTS = ? WHERE USER_ID = ?";
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
        // Removed UPDATED_AT to avoid ORA-00904 if column doesn't exist
        String sql = "UPDATE USERS SET STATUS = ? WHERE USER_ID = ?";
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

    public void updatePasswordHash(long userId, String newHash) {
        String sql = "UPDATE USERS SET PASSWORD_HASH = ? WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newHash);
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