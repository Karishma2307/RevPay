package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 
import java.util.ArrayList;
import java.util.List;

import revpay.dao.MoneyRequestDao;
import revpay.model.MoneyRequest;
import revpay.util.DBConnection;

public class MoneyRequestDaoImpl implements MoneyRequestDao {

    public long createRequest(MoneyRequest req) {
        String sql = "INSERT INTO MONEY_REQUESTS (REQUEST_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, CURRENCY, NOTE, STATUS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', SYSDATE)";
        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_MONEY_REQUESTS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            try { rs.close(); } catch (Exception e) {}
            rs = null;
            try { st.close(); } catch (Exception e) {}
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setLong(2, req.getFromUserId());
            ps.setLong(3, req.getToUserId());
            ps.setDouble(4, req.getAmount());
            ps.setString(5, req.getCurrency());
            ps.setString(6, req.getNote());
            ps.executeUpdate();

            req.setRequestId(newId);
        } catch (SQLException e) {
            e.printStackTrace();
            newId = -1;
        } finally {
            close(null, ps, conn);
        }
        return newId;
    }

    public List<MoneyRequest> findIncoming(long userId) {
        String sql = "SELECT * FROM MONEY_REQUESTS WHERE TO_USER_ID = ? ORDER BY CREATED_AT DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<MoneyRequest> list = new ArrayList<MoneyRequest>();
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    public List<MoneyRequest> findOutgoing(long userId) {
        String sql = "SELECT * FROM MONEY_REQUESTS WHERE FROM_USER_ID = ? ORDER BY CREATED_AT DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<MoneyRequest> list = new ArrayList<MoneyRequest>();
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    public MoneyRequest findById(long requestId) {
        String sql = "SELECT * FROM MONEY_REQUESTS WHERE REQUEST_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        MoneyRequest req = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, requestId);
            rs = ps.executeQuery();
            if (rs.next()) {
                req = mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return req;
    }

    public void updateStatus(long requestId, String newStatus) {
        String sql = "UPDATE MONEY_REQUESTS SET STATUS = ?, UPDATED_AT = SYSDATE WHERE REQUEST_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    private MoneyRequest mapRow(ResultSet rs) throws SQLException {
        MoneyRequest m = new MoneyRequest();
        m.setRequestId(rs.getLong("REQUEST_ID"));
        m.setFromUserId(rs.getLong("FROM_USER_ID"));
        m.setToUserId(rs.getLong("TO_USER_ID"));
        m.setAmount(rs.getDouble("AMOUNT"));
        m.setCurrency(rs.getString("CURRENCY"));
        m.setNote(rs.getString("NOTE"));
        m.setStatus(rs.getString("STATUS"));
        m.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        m.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
        return m;
    }

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
