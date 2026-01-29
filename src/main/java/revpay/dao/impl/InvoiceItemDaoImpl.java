package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 
import java.util.ArrayList;
import java.util.List;

import revpay.dao.InvoiceItemDao;
import revpay.model.InvoiceItem;
import revpay.util.DBConnection;

public class InvoiceItemDaoImpl implements InvoiceItemDao {

    public void createItem(InvoiceItem item) {
        String sql = "INSERT INTO INVOICE_ITEMS (ITEM_ID, INVOICE_ID, DESCRIPTION, QUANTITY, UNIT_PRICE, LINE_TOTAL) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_INVOICE_ITEMS.NEXTVAL FROM DUAL");
            long newId = -1;
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            try { rs.close(); } catch (Exception e) {}
            rs = null;
            try { st.close(); } catch (Exception e) {}
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setLong(2, item.getInvoiceId());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getQuantity());
            ps.setDouble(5, item.getUnitPrice());
            ps.setDouble(6, item.getLineTotal());
            ps.executeUpdate();

            item.setItemId(newId);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    public java.util.List<InvoiceItem> findByInvoiceId(long invoiceId) {
        String sql = "SELECT * FROM INVOICE_ITEMS WHERE INVOICE_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        java.util.List<InvoiceItem> list = new ArrayList<InvoiceItem>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, invoiceId);
            rs = ps.executeQuery();
            while (rs.next()) {
                InvoiceItem it = new InvoiceItem();
                it.setItemId(rs.getLong("ITEM_ID"));
                it.setInvoiceId(rs.getLong("INVOICE_ID"));
                it.setDescription(rs.getString("DESCRIPTION"));
                it.setQuantity(rs.getDouble("QUANTITY"));
                it.setUnitPrice(rs.getDouble("UNIT_PRICE"));
                it.setLineTotal(rs.getDouble("LINE_TOTAL"));
                list.add(it);
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
