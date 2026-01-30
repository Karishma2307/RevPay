package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;

import revpay.dao.BusinessProfileDao;
import revpay.util.DBConnection;

public class BusinessProfileDaoImpl implements BusinessProfileDao {

    public void create(long userId, String name, String type, String taxId, String address, String doc, String verifiedStatus) {
        String sql = "INSERT INTO BUSINESS_PROFILES "
                   + "(USER_ID, BUSINESS_NAME, BUSINESS_TYPE, TAX_ID, ADDRESS, VERIFICATION_DOC, VERIFIED_STATUS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, taxId);
            ps.setString(5, address);
            ps.setString(6, doc);
            ps.setString(7, verifiedStatus);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}