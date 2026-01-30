package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import revpay.dao.TransactionDao;
import revpay.model.Transaction;
import revpay.util.DBConnection;

public class TransactionDaoImpl implements TransactionDao {

    private Transaction map(ResultSet rs) throws Exception {
        Transaction t = new Transaction();

        // ✅ FIX: Use TRANSACTION_ID (not TXN_ID)
        t.setTransactionId(rs.getLong("TRANSACTION_ID"));

        // Handle possible NULLs safely (Oracle getLong returns 0 when NULL)
        long fromId = rs.getLong("FROM_USER_ID");
        if (rs.wasNull()) t.setFromUserId(null);
        else t.setFromUserId(fromId);

        long toId = rs.getLong("TO_USER_ID");
        if (rs.wasNull()) t.setToUserId(null);
        else t.setToUserId(toId);

        t.setAmount(rs.getDouble("AMOUNT"));
        t.setType(rs.getString("TYPE"));
        t.setStatus(rs.getString("STATUS"));
        t.setNote(rs.getString("NOTE"));
        t.setRefId(rs.getString("REF_ID"));
        t.setCreatedAt(rs.getDate("CREATED_AT"));
        return t;
    }

    @Override
    public void createTransaction(long fromUserId, long toUserId, double amount,
                                  String type, String status, String note, String refId) {

        // ✅ FIX: Use TRANSACTION_ID (not TXN_ID)
        String sql = "INSERT INTO TRANSACTIONS "
                + "(TRANSACTION_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, TYPE, STATUS, NOTE, REF_ID, CREATED_AT) "
                + "VALUES (SEQ_TRANSACTIONS.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            ps.setDouble(3, amount);
            ps.setString(4, type);
            ps.setString(5, status);
            ps.setString(6, note);
            ps.setString(7, refId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Transaction> searchTransactions(long userId,
                                               String type,
                                               String status,
                                               Date fromDate,
                                               Date toDate,
                                               Double minAmount,
                                               Double maxAmount,
                                               String keyword) {

        List<Transaction> list = new ArrayList<Transaction>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM TRANSACTIONS WHERE (FROM_USER_ID = ? OR TO_USER_ID = ?)");

        List<Object> params = new ArrayList<Object>();
        params.add(Long.valueOf(userId));
        params.add(Long.valueOf(userId));

        if (type != null && type.trim().length() > 0) {
            sql.append(" AND TYPE = ?");
            params.add(type.trim().toUpperCase());
        }
        if (status != null && status.trim().length() > 0) {
            sql.append(" AND STATUS = ?");
            params.add(status.trim().toUpperCase());
        }
        if (fromDate != null) {
            sql.append(" AND CREATED_AT >= ?");
            params.add(new java.sql.Date(fromDate.getTime()));
        }
        if (toDate != null) {
            sql.append(" AND CREATED_AT <= ?");
            params.add(new java.sql.Date(toDate.getTime()));
        }
        if (minAmount != null) {
            sql.append(" AND AMOUNT >= ?");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append(" AND AMOUNT <= ?");
            params.add(maxAmount);
        }
        if (keyword != null && keyword.trim().length() > 0) {
            sql.append(" AND (UPPER(NOTE) LIKE ? OR UPPER(TYPE) LIKE ? OR UPPER(STATUS) LIKE ?)");
            String k = "%" + keyword.trim().toUpperCase() + "%";
            params.add(k);
            params.add(k);
            params.add(k);
        }

        // ✅ FIX: ORDER BY TRANSACTION_ID (not TXN_ID)
        sql.append(" ORDER BY TRANSACTION_ID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, ((Long) p).longValue());
                else if (p instanceof Double) ps.setDouble(i + 1, ((Double) p).doubleValue());
                else if (p instanceof java.sql.Date) ps.setDate(i + 1, (java.sql.Date) p);
                else ps.setString(i + 1, String.valueOf(p)); // keep as string
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
