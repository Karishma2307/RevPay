package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.UserSecurityAnswerDao;
import revpay.model.SecurityQuestion;
import revpay.util.DBConnection;
import revpay.util.HashUtil;

public class UserSecurityAnswerDaoImpl implements UserSecurityAnswerDao {

    public void saveAnswer(long userId, long qId, String answerHash) {
        String sql = "INSERT INTO USER_SECURITY_ANSWERS (USER_ID, Q_ID, ANSWER_HASH) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setLong(2, qId);
            ps.setString(3, answerHash);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    public List<SecurityQuestion> getQuestionsForUser(long userId) {
       
        String sql = "SELECT q.Q_ID, q.QUESTION "
                   + "FROM SECURITY_QUESTIONS q "
                   + "JOIN USER_SECURITY_ANSWERS a ON a.Q_ID = q.Q_ID "
                   + "WHERE a.USER_ID = ? "
                   + "ORDER BY q.Q_ID";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<SecurityQuestion> list = new ArrayList<SecurityQuestion>();

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                SecurityQuestion q = new SecurityQuestion();
                q.setqId(rs.getLong("Q_ID"));
                q.setQuestion(rs.getString("QUESTION"));
                list.add(q);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return list;
    }

    public boolean verifyAnswer(long userId, long qId, String plainAnswer) {
        String sql = "SELECT ANSWER_HASH FROM USER_SECURITY_ANSWERS WHERE USER_ID = ? AND Q_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setLong(2, qId);
            rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("ANSWER_HASH");
                return HashUtil.check(plainAnswer, hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return false;
    }

    public void deleteAnswersForUser(long userId) {
        String sql = "DELETE FROM USER_SECURITY_ANSWERS WHERE USER_ID = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}