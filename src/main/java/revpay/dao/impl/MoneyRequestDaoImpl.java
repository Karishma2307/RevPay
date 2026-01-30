package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.MoneyRequestDao;
import revpay.model.MoneyRequest;
import revpay.util.DBConnection;

public class MoneyRequestDaoImpl implements MoneyRequestDao {


    @Override
    public long createRequest(MoneyRequest req) {
        String sql = "INSERT INTO MONEY_REQUESTS "
                   + "(REQUEST_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, STATUS, NOTE, CREATED_AT) "
                   + "VALUES (SEQ_MONEY_REQUESTS.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, req.getFromUserId());
            ps.setLong(2, req.getToUserId());
            ps.setDouble(3, req.getAmount());
            ps.setString(4, req.getStatus());
            ps.setString(5, req.getNote());

            ps.executeUpdate();

            // Get generated REQUEST_ID (Oracle safe way)
            try (PreparedStatement ps2 = con.prepareStatement(
                    "SELECT SEQ_MONEY_REQUESTS.CURRVAL FROM DUAL");
                 ResultSet rs = ps2.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    @Override
    public List<MoneyRequest> findIncoming(long userId) {
        List<MoneyRequest> list = new ArrayList<>();

        String sql = "SELECT * FROM MONEY_REQUESTS "
                   + "WHERE TO_USER_ID = ? "
                   + "ORDER BY REQUEST_ID DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public List<MoneyRequest> findOutgoing(long userId) {
        List<MoneyRequest> list = new ArrayList<>();

        String sql = "SELECT * FROM MONEY_REQUESTS "
                   + "WHERE FROM_USER_ID = ? "
                   + "ORDER BY REQUEST_ID DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

 
    @Override
    public MoneyRequest findById(long requestId) {
        String sql = "SELECT * FROM MONEY_REQUESTS WHERE REQUEST_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public boolean updateStatus(long requestId, String status) {
        String sql = "UPDATE MONEY_REQUESTS SET STATUS = ? WHERE REQUEST_ID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, requestId);

            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private MoneyRequest map(ResultSet rs) throws Exception {
        MoneyRequest r = new MoneyRequest();

        r.setRequestId(rs.getLong("REQUEST_ID"));
        r.setFromUserId(rs.getLong("FROM_USER_ID"));
        r.setToUserId(rs.getLong("TO_USER_ID"));
        r.setAmount(rs.getDouble("AMOUNT"));
        r.setStatus(rs.getString("STATUS"));
        r.setNote(rs.getString("NOTE"));
        r.setCreatedAt(rs.getDate("CREATED_AT"));

        return r;
    }
}
