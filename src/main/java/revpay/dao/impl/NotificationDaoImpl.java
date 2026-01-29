package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.NotificationDao;
import revpay.model.Notification;
import revpay.util.DBConnection;

public class NotificationDaoImpl implements NotificationDao {

    public long createNotification(Notification n) {
        String sql = "INSERT INTO NOTIFICATIONS (NOTIFICATION_ID, USER_ID, TYPE, TITLE, MESSAGE, IS_READ, CREATED_AT) "
                   + "VALUES (SEQ_NOTIFICATIONS.NEXTVAL, ?, ?, ?, ?, 'N', SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_NOTIFICATIONS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            rs.close();
            rs = null;
            st.close();
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, n.getUserId());
            ps.setString(2, n.getType());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getMessage());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }

        return newId;
    }

    public List<Notification> findByUserId(long userId, boolean unreadOnly) {
        String sql = "SELECT * FROM NOTIFICATIONS WHERE USER_ID = ? ";
        if (unreadOnly) {
            sql += "AND IS_READ = 'N' ";
        }
        sql += "ORDER BY CREATED_AT DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Notification> list = new ArrayList<Notification>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Notification n = new Notification();
                n.setNotificationId(rs.getLong("NOTIFICATION_ID"));
                n.setUserId(rs.getLong("USER_ID"));
                n.setType(rs.getString("TYPE"));
                n.setTitle(rs.getString("TITLE"));
                n.setMessage(rs.getString("MESSAGE"));
                n.setRead("Y".equalsIgnoreCase(rs.getString("IS_READ")));
                n.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }

        return list;
    }

    public void markAllRead(long userId) {
        String sql = "UPDATE NOTIFICATIONS SET IS_READ = 'Y' WHERE USER_ID = ?";

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

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
