package revpay.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Scanner;

import revpay.dao.SecurityQuestionDao;
import revpay.dao.UserDao;
import revpay.dao.impl.SecurityQuestionDaoImpl;
import revpay.dao.impl.UserDaoImpl;
import revpay.model.SecurityQuestion;
import revpay.model.User;
import revpay.util.ConsoleUtil;
import revpay.util.DBConnection;
import revpay.util.HashUtil;
import revpay.util.ValidationUtil;

public class PasswordRecoveryService {

    private UserDao userDao = new UserDaoImpl();
    private SecurityQuestionDao questionDao = new SecurityQuestionDaoImpl();

    
    public void setupSecurityQuestions(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Security Questions Setup");

        List<SecurityQuestion> questions = questionDao.getAllQuestions();
        if (questions == null || questions.size() < 2) {
            System.out.println("[WARN] Not enough security questions in DB.");
            ConsoleUtil.pause(sc);
            return;
        }

      
        System.out.println("Choose 2 security questions (enter number).");
        for (int i = 0; i < questions.size(); i++) {
            System.out.println((i + 1) + ". " + questions.get(i).getQuestion());
        }

        int q1 = pickQuestion(sc, questions.size(), "Select Question 1: ");
        int q2 = pickQuestion(sc, questions.size(), "Select Question 2 (different): ");

        if (q1 == q2) {
            System.out.println("[ERROR] Questions must be different.");
            ConsoleUtil.pause(sc);
            return;
        }

        SecurityQuestion sq1 = questions.get(q1 - 1);
        SecurityQuestion sq2 = questions.get(q2 - 1);

        System.out.print("Answer for Q1: ");
        String a1 = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(a1)) {
            System.out.println("[ERROR] Answer cannot be empty.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Answer for Q2: ");
        String a2 = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(a2)) {
            System.out.println("[ERROR] Answer cannot be empty.");
            ConsoleUtil.pause(sc);
            return;
        }

        saveAnswer(userId, sq1.getqId(), HashUtil.hash(a1.trim().toLowerCase()));
        saveAnswer(userId, sq2.getqId(), HashUtil.hash(a2.trim().toLowerCase()));

        System.out.println("[INFO] Security questions saved.");
        ConsoleUtil.pause(sc);
    }

    
    public void forgotPassword(Scanner sc) {
        ConsoleUtil.printHeader("Forgot Password");

        System.out.println("Recover using:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.print("Choice: ");
        String c = sc.nextLine();

        User user = null;

        if ("1".equals(c)) {
            System.out.print("Email: ");
            String email = sc.nextLine();
            if (!ValidationUtil.isValidEmail(email)) {
                System.out.println("[ERROR] Invalid email format.");
                ConsoleUtil.pause(sc);
                return;
            }
            user = userDao.findByEmail(email.trim());

        } else if ("2".equals(c)) {
            System.out.print("Phone: ");
            String phone = sc.nextLine();
            if (!ValidationUtil.isValidPhone(phone)) {
                System.out.println("[ERROR] Phone must be 10 digits.");
                ConsoleUtil.pause(sc);
                return;
            }
            user = userDao.findByPhone(phone.trim());

        } else {
            System.out.println("[ERROR] Invalid choice.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (user == null) {
            System.out.println("[ERROR] User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        
        UserQuestion uq1 = getFirstUserQuestion(user.getUserId());
        UserQuestion uq2 = getSecondUserQuestion(user.getUserId(), uq1 == null ? -1 : uq1.qId);

        if (uq1 == null || uq2 == null) {
            System.out.println("[ERROR] No security questions set for this user.");
            ConsoleUtil.pause(sc);
            return;
        }

        
        System.out.println("Answer the following security questions:");

        System.out.println("Q1: " + uq1.question);
        System.out.print("Answer: ");
        String a1 = sc.nextLine();

        System.out.println("Q2: " + uq2.question);
        System.out.print("Answer: ");
        String a2 = sc.nextLine();

        boolean ok1 = HashUtil.check(a1.trim().toLowerCase(), uq1.answerHash);
        boolean ok2 = HashUtil.check(a2.trim().toLowerCase(), uq2.answerHash);

        if (!ok1 || !ok2) {
            System.out.println("[ERROR] Answers do not match. Recovery failed.");
            ConsoleUtil.pause(sc);
            return;
        }

        
        System.out.print("Enter NEW password: ");
        String p1 = sc.nextLine();
        System.out.print("Confirm NEW password: ");
        String p2 = sc.nextLine();

        if (!p1.equals(p2)) {
            System.out.println("[ERROR] Passwords do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!ValidationUtil.isValidPassword(p1)) {
            System.out.println("[ERROR] Weak password. Must contain uppercase/lowercase/digit/special and >= 8 chars.");
            ConsoleUtil.pause(sc);
            return;
        }

        userDao.updatePasswordHash(user.getUserId(), HashUtil.hash(p1));
        System.out.println("[INFO] Password updated successfully. You can login now.");
        ConsoleUtil.pause(sc);
    }

    

    private int pickQuestion(Scanner sc, int max, String prompt) {
        System.out.print(prompt);
        String s = sc.nextLine();
        try {
            int x = Integer.parseInt(s.trim());
            if (x < 1 || x > max) return 1;
            return x;
        } catch (Exception e) {
            return 1;
        }
    }

    private void saveAnswer(long userId, long qId, String answerHash) {
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

    private UserQuestion getFirstUserQuestion(long userId) {
        String sql = "SELECT usa.Q_ID, sq.QUESTION, usa.ANSWER_HASH "
                   + "FROM USER_SECURITY_ANSWERS usa "
                   + "JOIN SECURITY_QUESTIONS sq ON sq.Q_ID = usa.Q_ID "
                   + "WHERE usa.USER_ID = ? ORDER BY usa.Q_ID";
        return fetchOne(sql, userId, -1);
    }

    private UserQuestion getSecondUserQuestion(long userId, long excludeQId) {
        String sql = "SELECT usa.Q_ID, sq.QUESTION, usa.ANSWER_HASH "
                   + "FROM USER_SECURITY_ANSWERS usa "
                   + "JOIN SECURITY_QUESTIONS sq ON sq.Q_ID = usa.Q_ID "
                   + "WHERE usa.USER_ID = ? AND usa.Q_ID <> ? ORDER BY usa.Q_ID";
        return fetchOne(sql, userId, excludeQId);
    }

    private UserQuestion fetchOne(String sql, long userId, long excludeQId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, userId);
            if (excludeQId != -1) ps.setLong(2, excludeQId);

            rs = ps.executeQuery();
            if (rs.next()) {
                UserQuestion uq = new UserQuestion();
                uq.qId = rs.getLong(1);
                uq.question = rs.getString(2);
                uq.answerHash = rs.getString(3);
                return uq;
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }

        return null;
    }

    private static class UserQuestion {
        long qId;
        String question;
        String answerHash;
    }
}