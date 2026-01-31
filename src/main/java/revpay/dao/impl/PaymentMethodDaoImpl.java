package revpay.dao.impl;

import revpay.dao.PaymentMethodDao;
import revpay.model.PaymentMethod;
import revpay.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodDaoImpl implements PaymentMethodDao {

    @Override
    public long add(PaymentMethod pm) {
        String sql = "INSERT INTO PAYMENT_METHODS " +
                "(METHOD_ID, USER_ID, TYPE, LABEL, PROVIDER, LAST4, ENC_NUMBER, IS_DEFAULT) " +
                "VALUES (PAYMENT_METHODS_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, pm.getUserId());
            ps.setString(2, pm.getType());
            ps.setString(3, pm.getLabel());
            ps.setString(4, pm.getProvider());
            ps.setString(5, pm.getLast4());
            ps.setString(6, pm.getEncNumber());
            ps.setString(7, pm.getIsDefault());

            ps.executeUpdate();

            
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT PAYMENT_METHODS_SEQ.CURRVAL AS ID FROM DUAL")) {
                if (rs.next()) return rs.getLong("ID");
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add payment method: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PaymentMethod> findByUserId(long userId) {
        String sql = "SELECT METHOD_ID, USER_ID, TYPE, LABEL, PROVIDER, LAST4, ENC_NUMBER, IS_DEFAULT " +
                "FROM PAYMENT_METHODS WHERE USER_ID = ? " +
                "ORDER BY CASE WHEN IS_DEFAULT='Y' THEN 0 ELSE 1 END, METHOD_ID DESC";

        List<PaymentMethod> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentMethod pm = new PaymentMethod();
                    pm.setMethodId(rs.getLong("METHOD_ID"));
                    pm.setUserId(rs.getLong("USER_ID"));
                    pm.setType(rs.getString("TYPE"));
                    pm.setLabel(rs.getString("LABEL"));
                    pm.setProvider(rs.getString("PROVIDER"));
                    pm.setLast4(rs.getString("LAST4"));
                    pm.setEncNumber(rs.getString("ENC_NUMBER"));
                    pm.setIsDefault(rs.getString("IS_DEFAULT"));
                    list.add(pm);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch payment methods: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public PaymentMethod findDefaultByUserId(long userId) {
        String sql = "SELECT METHOD_ID, USER_ID, TYPE, LABEL, PROVIDER, LAST4, ENC_NUMBER, IS_DEFAULT " +
                "FROM PAYMENT_METHODS WHERE USER_ID = ? AND IS_DEFAULT = 'Y'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PaymentMethod pm = new PaymentMethod();
                    pm.setMethodId(rs.getLong("METHOD_ID"));
                    pm.setUserId(rs.getLong("USER_ID"));
                    pm.setType(rs.getString("TYPE"));
                    pm.setLabel(rs.getString("LABEL"));
                    pm.setProvider(rs.getString("PROVIDER"));
                    pm.setLast4(rs.getString("LAST4"));
                    pm.setEncNumber(rs.getString("ENC_NUMBER"));
                    pm.setIsDefault(rs.getString("IS_DEFAULT"));
                    return pm;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch default payment method: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean setDefault(long userId, long methodId) {
        String sql = "UPDATE PAYMENT_METHODS SET IS_DEFAULT = 'Y' WHERE USER_ID = ? AND METHOD_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, methodId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set default: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long userId, long methodId) {
        String sql = "DELETE FROM PAYMENT_METHODS WHERE USER_ID = ? AND METHOD_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, methodId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete payment method: " + e.getMessage(), e);
        }
    }
}
