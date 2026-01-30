package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import revpay.dao.NotificationPrefDao;
import revpay.util.DBConnection;

public class NotificationPrefDaoImpl implements NotificationPrefDao {

    @Override
    public void ensureExists(long userId) {
        String check = "SELECT USER_ID FROM NOTIFICATION_PREFS WHERE USER_ID = ?";
        String insert = "INSERT INTO NOTIFICATION_PREFS (USER_ID) VALUES (?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(check)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }

            try (PreparedStatement ins = con.prepareStatement(insert)) {
                ins.setLong(1, userId);
                ins.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String columnForType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase();
        switch (t) {
            case "TRANSACTION": return "TRANSACTION_ON";
            case "REQUEST":     return "REQUEST_ON";
            case "INVOICE":     return "INVOICE_ON";
            case "LOAN":        return "LOAN_ON";
            case "ALERT":       return "ALERT_ON";
            default:            return null;
        }
    }

    @Override
    public boolean isEnabled(long userId, String type) {
        ensureExists(userId);
        String col = columnForType(type);
        if (col == null) return true; // unknown types allowed by default

        String sql = "SELECT " + col + " AS FLAG FROM NOTIFICATION_PREFS WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString("FLAG");
                    return "Y".equalsIgnoreCase(v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public int getLowBalanceThreshold(long userId) {
        ensureExists(userId);
        String sql = "SELECT LOW_BALANCE_THRESHOLD FROM NOTIFICATION_PREFS WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 500;
    }

    // ✅ anti-spam: send alert only if last alert was null OR older than 10 minutes
    @Override
    public boolean shouldSendLowBalanceAlert(long userId) {
        ensureExists(userId);
        String sql = "SELECT LAST_LOW_BAL_ALERT_AT FROM NOTIFICATION_PREFS WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp last = rs.getTimestamp(1);
                    if (last == null) return true;

                    long diffMs = System.currentTimeMillis() - last.getTime();
                    return diffMs > (10 * 60 * 1000); // 10 minutes
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void updateLastLowBalanceAlertNow(long userId) {
        ensureExists(userId);
        String sql = "UPDATE NOTIFICATION_PREFS SET LAST_LOW_BAL_ALERT_AT = SYSDATE WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePref(long userId, String type, boolean enabled) {
        ensureExists(userId);
        String col = columnForType(type);
        if (col == null) return;

        String sql = "UPDATE NOTIFICATION_PREFS SET " + col + " = ? WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enabled ? "Y" : "N");
            ps.setLong(2, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateThreshold(long userId, int threshold) {
        ensureExists(userId);
        String sql = "UPDATE NOTIFICATION_PREFS SET LOW_BALANCE_THRESHOLD = ? WHERE USER_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, threshold);
            ps.setLong(2, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
