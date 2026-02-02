package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.NotificationDao;
import revpay.model.Notification;
import revpay.util.DbUtil;

public class NotificationDaoImpl implements NotificationDao {

    @Override
    public void createNotification(Notification n) {
        String sql =
            "INSERT INTO NOTIFICATIONS " +
            "(NOTIFICATION_ID, USER_ID, TYPE, TITLE, MESSAGE, IS_READ, CREATED_AT) " +
            "VALUES (NOTIFICATIONS_SEQ.NEXTVAL, ?, ?, ?, ?, 0, SYSDATE)";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, n.getUserId());
            ps.setString(2, n.getType());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getMessage());
            ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("Failed to create notification.");
            System.out.println("[NOTHING] " + e.getMessage());
        }
    }

    @Override
    public List<Notification> findByUserId(long userId, boolean unreadOnly) {
        List<Notification> list = new ArrayList<>();

        String sql =
            "SELECT NOTIFICATION_ID, USER_ID, TYPE, TITLE, MESSAGE, IS_READ, CREATED_AT " +
            "FROM NOTIFICATIONS " +
            "WHERE USER_ID = ? " +
            (unreadOnly ? "AND IS_READ = 0 " : "") +
            "ORDER BY CREATED_AT DESC";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setNotificationId(rs.getLong("NOTIFICATION_ID"));
                    n.setUserId(rs.getLong("USER_ID"));
                    n.setType(rs.getString("TYPE"));
                    n.setTitle(rs.getString("TITLE"));
                    n.setMessage(rs.getString("MESSAGE"));
                    n.setRead(rs.getInt("IS_READ") == 1);
                    n.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    list.add(n);
                }
            }

        } catch (Exception e) {
            System.out.println("Failed to fetch notifications.");
            System.out.println("[NOTHING] " + e.getMessage());
        }

        return list;
    }

    @Override
    public void markAllAsRead(long userId) {
        String sql = "UPDATE NOTIFICATIONS SET IS_READ = 1 WHERE USER_ID = ?";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("Failed to mark all notifications as read.");
            System.out.println("[NOTHING] " + e.getMessage());
        }
    }
}
