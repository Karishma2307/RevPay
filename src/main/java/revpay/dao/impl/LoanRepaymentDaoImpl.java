package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 
import java.util.ArrayList;
import java.util.List;

import revpay.dao.LoanRepaymentDao;
import revpay.model.LoanRepayment;
import revpay.util.DBConnection;

public class LoanRepaymentDaoImpl implements LoanRepaymentDao {

    public long createRepayment(LoanRepayment repayment) {
        String sql = "INSERT INTO LOAN_REPAYMENTS (REPAYMENT_ID, LOAN_ID, AMOUNT, PAID_AT) "
                   + "VALUES (?, ?, ?, SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_LOAN_REPAYMENTS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            try { rs.close(); } catch (Exception e) {}
            rs = null;
            try { st.close(); } catch (Exception e) {}
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setLong(2, repayment.getLoanId());
            ps.setDouble(3, repayment.getAmount());
            ps.executeUpdate();

            repayment.setRepaymentId(newId);

        } catch (SQLException e) {
            e.printStackTrace();
            newId = -1;
        } finally {
            close(null, ps, conn);
        }
        return newId;
    }

    public List<LoanRepayment> findByLoanId(long loanId) {
        String sql = "SELECT * FROM LOAN_REPAYMENTS WHERE LOAN_ID = ? ORDER BY PAID_AT ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<LoanRepayment> list = new ArrayList<LoanRepayment>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, loanId);
            rs = ps.executeQuery();
            while (rs.next()) {
                LoanRepayment r = new LoanRepayment();
                r.setRepaymentId(rs.getLong("REPAYMENT_ID"));
                r.setLoanId(rs.getLong("LOAN_ID"));
                r.setAmount(rs.getDouble("AMOUNT"));
                r.setPaidAt(rs.getTimestamp("PAID_AT"));
                list.add(r);
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
