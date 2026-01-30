package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; 
import java.util.ArrayList;
import java.util.List;

import revpay.dao.LoanDao;
import revpay.model.Loan;
import revpay.util.DBConnection;

public class LoanDaoImpl implements LoanDao {

    public long createLoan(Loan loan) {
        String sql = "INSERT INTO LOANS (LOAN_ID, BUSINESS_USER_ID, AMOUNT, INTEREST_RATE, TERM_MONTHS, PURPOSE, STATUS, APPROVED_AMOUNT, OUTSTANDING_AMOUNT, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

        Connection conn = null;
        Statement st = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long newId = -1;

        try {
            conn = DBConnection.getConnection();

            st = conn.createStatement();
            rs = st.executeQuery("SELECT SEQ_LOANS.NEXTVAL FROM DUAL");
            if (rs.next()) {
                newId = rs.getLong(1);
            }
            try { rs.close(); } catch (Exception e) {}
            rs = null;
            try { st.close(); } catch (Exception e) {}
            st = null;

            ps = conn.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setLong(2, loan.getBusinessUserId());
            ps.setDouble(3, loan.getAmount());
            ps.setDouble(4, loan.getInterestRate());
            ps.setInt(5, loan.getTermMonths());
            ps.setString(6, loan.getPurpose());
            ps.setString(7, loan.getStatus());
            ps.setDouble(8, loan.getApprovedAmount());
            ps.setDouble(9, loan.getOutstandingAmount());
            ps.executeUpdate();

            loan.setLoanId(newId);

        } catch (SQLException e) {
            e.printStackTrace();
            newId = -1;
        } finally {
            close(null, ps, conn);
        }

        return newId;
    }

    public void updateLoan(Loan loan) {
        String sql = "UPDATE LOANS SET AMOUNT = ?, INTEREST_RATE = ?, TERM_MONTHS = ?, PURPOSE = ?, "
                   + "STATUS = ?, APPROVED_AMOUNT = ?, OUTSTANDING_AMOUNT = ?, UPDATED_AT = SYSDATE "
                   + "WHERE LOAN_ID = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, loan.getAmount());
            ps.setDouble(2, loan.getInterestRate());
            ps.setInt(3, loan.getTermMonths());
            ps.setString(4, loan.getPurpose());
            ps.setString(5, loan.getStatus());
            ps.setDouble(6, loan.getApprovedAmount());
            ps.setDouble(7, loan.getOutstandingAmount());
            ps.setLong(8, loan.getLoanId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, ps, conn);
        }
    }

    public Loan findById(long loanId) {
        String sql = "SELECT * FROM LOANS WHERE LOAN_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Loan loan = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, loanId);
            rs = ps.executeQuery();
            if (rs.next()) {
                loan = mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, ps, conn);
        }

        return loan;
    }

    public List<Loan> findByBusinessUser(long businessUserId) {
        String sql = "SELECT * FROM LOANS WHERE BUSINESS_USER_ID = ? ORDER BY CREATED_AT DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Loan> list = new ArrayList<Loan>();

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

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setLoanId(rs.getLong("LOAN_ID"));
        l.setBusinessUserId(rs.getLong("BUSINESS_USER_ID"));
        l.setAmount(rs.getDouble("AMOUNT"));
        l.setInterestRate(rs.getDouble("INTEREST_RATE"));
        l.setTermMonths(rs.getInt("TERM_MONTHS"));
        l.setPurpose(rs.getString("PURPOSE"));
        l.setStatus(rs.getString("STATUS"));
        l.setApprovedAmount(rs.getDouble("APPROVED_AMOUNT"));
        l.setOutstandingAmount(rs.getDouble("OUTSTANDING_AMOUNT"));
        l.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        l.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
        return l;
    }

    private void close(ResultSet rs, java.sql.Statement st, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (st != null) st.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
}