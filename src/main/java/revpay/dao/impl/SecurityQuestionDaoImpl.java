package revpay.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import revpay.dao.SecurityQuestionDao;
import revpay.model.SecurityQuestion;
import revpay.util.DBConnection;

public class SecurityQuestionDaoImpl implements SecurityQuestionDao {

    public List<SecurityQuestion> getAllQuestions() {
        List<SecurityQuestion> list = new ArrayList<SecurityQuestion>();

        String sql = "SELECT Q_ID, QUESTION FROM SECURITY_QUESTIONS ORDER BY Q_ID";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
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
}