package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 
import java.util.ArrayList;
import java.util.List;

import revpay.dao.InvoiceDao;
import revpay.model.Invoice;
import revpay.util.DBConnection;

public class InvoiceDaoImpl implements InvoiceDao {

    public long createInvoice(Invoice invoice) {
        String sql = "INSERT INTO INVOICES (INVOICE_ID, BUSINESS_USER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_PHONE, DUE_DATE, TOTAL_AMOUNT, STATUS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_INVOICES.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            try { rs.close(); } catch (Exception e) {}
            rs = null;
            try { st.close(); } catch (Exception e) {}
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setLong(2, invoice.getBusinessUserId());
            ps.setString(3, invoice.getCustomerName());
            ps.setString(4, invoice.getCustomerEmail());
            ps.setString(5, invoice.getCustomerPhone());
            ps.setDate(6, invoice.getDueDate() == null ? null : new java.sql.Date(invoice.getDueDate().getTime()));
            ps.setDouble(7, invoice.getTotalAmount());
            ps.executeUpdate();

            invoice.setInvoiceId(newId);

        } catch (SQLException e) {
            e.printStackTrace();
            newId = -1;
        } finally {
            close(null, ps, conn);
        }

        return newId;
    }

    public void updateStatus(long invoiceId, String newStatus) {
        String sql = "UPDATE INVOICES SET STATUS = ?, UPDATED_AT = SYSDATE WHERE INVOICE_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setLong(2, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    public Invoice findById(long invoiceId) {
        String sql = "SELECT * FROM INVOICES WHERE INVOICE_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Invoice inv = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, invoiceId);
            rs = ps.executeQuery();
            if (rs.next()) {
                inv = mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }
        return inv;
    }

    public List<Invoice> findByBusinessUser(long businessUserId) {
        String sql = "SELECT * FROM INVOICES WHERE BUSINESS_USER_ID = ? ORDER BY CREATED_AT DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Invoice> list = new ArrayList<Invoice>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, businessUserId);
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

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getLong("INVOICE_ID"));
        inv.setBusinessUserId(rs.getLong("BUSINESS_USER_ID"));
        inv.setCustomerName(rs.getString("CUSTOMER_NAME"));
        inv.setCustomerEmail(rs.getString("CUSTOMER_EMAIL"));
        inv.setCustomerPhone(rs.getString("CUSTOMER_PHONE"));
        inv.setDueDate(rs.getDate("DUE_DATE"));
        inv.setTotalAmount(rs.getDouble("TOTAL_AMOUNT"));
        inv.setStatus(rs.getString("STATUS"));
        inv.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        inv.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
        return inv;
    }

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}
