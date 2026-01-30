package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.NotificationDao;
import revpay.model.Notification;
import revpay.util.DBConnection;

public class NotificationDaoImpl implements NotificationDao {

    private Notification map(ResultSet rs) throws Exception {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("N_ID"));
        n.setUserId(rs.getLong("USER_ID"));
        n.setType(rs.getString("TYPE"));
        n.setTitle(rs.getString("TITLE"));
        n.setMessage(rs.getString("MESSAGE"));
        n.setRead("Y".equalsIgnoreCase(rs.getString("IS_READ")));
        n.setCreatedAt(rs.getDate("CREATED_AT"));
        return n;
    }

    public void create(long userId, String type, String title, String message) {
        String sql = "INSERT INTO NOTIFICATIONS (N_ID, USER_ID, TYPE, TITLE, MESSAGE, IS_READ, CREATED_AT) "
                   + "VALUES (SEQ_NOTIFICATIONS.NEXTVAL, ?, ?, ?, ?, 'N', SYSDATE)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, type);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    public List<Notification> findByUserId(long userId) {
        String sql = "SELECT * FROM NOTIFICATIONS WHERE USER_ID = ? ORDER BY N_ID DESC";
        List<Notification> list = new ArrayList<Notification>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return list;
    }

    public List<Notification> findUnreadByUserId(long userId) {
        String sql = "SELECT * FROM NOTIFICATIONS WHERE USER_ID = ? AND IS_READ = 'N' ORDER BY N_ID DESC";
        List<Notification> list = new ArrayList<Notification>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return list;
    }

    public void markAsRead(long userId, long nId) {
        String sql = "UPDATE NOTIFICATIONS SET IS_READ = 'Y' WHERE USER_ID = ? AND N_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setLong(2, nId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    public void markAllAsRead(long userId) {
        String sql = "UPDATE NOTIFICATIONS SET IS_READ = 'Y' WHERE USER_ID = ?";
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
}