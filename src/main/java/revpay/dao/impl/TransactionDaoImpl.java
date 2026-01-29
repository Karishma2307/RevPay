package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.TransactionDao;
import revpay.model.Transaction;
import revpay.util.DBConnection;

public class TransactionDaoImpl implements TransactionDao {

    public long createTransaction(Transaction txn) {
        String sql = "INSERT INTO TRANSACTIONS (TRANSACTION_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, CURRENCY, TYPE, STATUS, NOTE, CREATED_AT) "
                   + "VALUES (SEQ_TRANSACTIONS.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_TRANSACTIONS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            rs.close();
            rs = null;
            st.close();
            st = null;

            ps = conn.prepareStatement(sql);

            if (txn.getFromUserId() == null) {
                ps.setNull(1, Types.NUMERIC);
            } else {
                ps.setLong(1, txn.getFromUserId().longValue());
            }

            if (txn.getToUserId() == null) {
                ps.setNull(2, Types.NUMERIC);
            } else {
                ps.setLong(2, txn.getToUserId().longValue());
            }

            ps.setDouble(3, txn.getAmount());
            ps.setString(4, txn.getCurrency());
            ps.setString(5, txn.getType());
            ps.setString(6, txn.getStatus());
            ps.setString(7, txn.getNote());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }

        return newId;
    }

    public List<Transaction> findByUserId(long userId) {
        String sql = "SELECT * FROM TRANSACTIONS "
                   + "WHERE FROM_USER_ID = ? OR TO_USER_ID = ? "
                   + "ORDER BY CREATED_AT DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Transaction> list = new ArrayList<Transaction>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setTransactionId(rs.getLong("TRANSACTION_ID"));

                long fromId = rs.getLong("FROM_USER_ID");
                if (rs.wasNull()) {
                    t.setFromUserId(null);
                } else {
                    t.setFromUserId(new Long(fromId));
                }

                long toId = rs.getLong("TO_USER_ID");
                if (rs.wasNull()) {
                    t.setToUserId(null);
                } else {
                    t.setToUserId(new Long(toId));
                }

                t.setAmount(rs.getDouble("AMOUNT"));
                t.setCurrency(rs.getString("CURRENCY"));
                t.setType(rs.getString("TYPE"));
                t.setStatus(rs.getString("STATUS"));
                t.setNote(rs.getString("NOTE"));
                t.setCreatedAt(rs.getTimestamp("CREATED_AT"));

                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }

        return list;
    }

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
