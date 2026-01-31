package revpay.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.TransactionDao;
import revpay.model.Transaction;
import revpay.util.DbUtil;

public class TransactionDaoImpl implements TransactionDao {

    @Override
    public boolean createTransaction(long fromUserId,
                                     long toUserId,
                                     double amount,
                                     String type,
                                     String status,
                                     String note) {

        String sql =
            "INSERT INTO TRANSACTIONS (" +
            "TRANSACTION_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, CURRENCY, TYPE, STATUS, NOTE, CREATED_AT" +
            ") VALUES (TRANSACTIONS_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            ps.setDouble(3, amount);
            ps.setString(4, "INR");
            ps.setString(5, type);
            ps.setString(6, status);
            ps.setString(7, note);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Transaction save failed.");
            System.out.println("[DEV] " + e.getMessage());
            return false;
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

        List<Transaction> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT TRANSACTION_ID, FROM_USER_ID, TO_USER_ID, AMOUNT, CURRENCY, TYPE, STATUS, NOTE, CREATED_AT " +
            "FROM TRANSACTIONS " +
            "WHERE (FROM_USER_ID = ? OR TO_USER_ID = ?) "
        );

        if (type != null && !type.trim().isEmpty()) {
            sql.append("AND UPPER(TYPE) = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND UPPER(STATUS) = ? ");
        }
        if (fromDate != null) {
            sql.append("AND CREATED_AT >= ? ");
        }
        if (toDate != null) {
            sql.append("AND CREATED_AT <= ? ");
        }
        if (minAmount != null) {
            sql.append("AND AMOUNT >= ? ");
        }
        if (maxAmount != null) {
            sql.append("AND AMOUNT <= ? ");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND UPPER(NOTE) LIKE ? ");
        }

        sql.append("ORDER BY CREATED_AT DESC");

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int idx = 1;
            ps.setLong(idx++, userId);
            ps.setLong(idx++, userId);

            if (type != null && !type.trim().isEmpty()) {
                ps.setString(idx++, type.trim().toUpperCase());
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(idx++, status.trim().toUpperCase());
            }
            if (fromDate != null) {
                ps.setDate(idx++, fromDate);
            }
            if (toDate != null) {
                ps.setDate(idx++, toDate);
            }
            if (minAmount != null) {
                ps.setDouble(idx++, minAmount);
            }
            if (maxAmount != null) {
                ps.setDouble(idx++, maxAmount);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setString(idx++, "%" + keyword.trim().toUpperCase() + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setTransactionId(rs.getLong("TRANSACTION_ID"));
                    t.setFromUserId(rs.getLong("FROM_USER_ID"));
                    t.setToUserId(rs.getLong("TO_USER_ID"));
                    t.setAmount(rs.getDouble("AMOUNT"));
                    t.setCurrency(rs.getString("CURRENCY"));
                    t.setType(rs.getString("TYPE"));
                    t.setStatus(rs.getString("STATUS"));
                    t.setNote(rs.getString("NOTE"));
                    t.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to search transactions.");
            System.out.println("[DEV] " + e.getMessage());
        }

        return list;
    }
}
