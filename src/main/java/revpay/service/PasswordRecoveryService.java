package revpay.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryService.class);

    private UserDao userDao = new UserDaoImpl();
    private SecurityQuestionDao questionDao = new SecurityQuestionDaoImpl();

    public void setupSecurityQuestions(Scanner sc, long userId) {
        ConsoleUtil.printHeader("Security Questions Setup");
        logger.info("Security questions setup started (userId={})", userId);

        List<SecurityQuestion> questions;
        try {
            questions = questionDao.getAllQuestions();
        } catch (Exception e) {
            logger.error("Failed to load security questions from DB (userId={})", userId, e);
            System.out.println("Unable to load security questions.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (questions == null || questions.size() < 2) {
            logger.warn("Not enough security questions in DB (userId={}, count={})",
                    userId, (questions == null ? 0 : questions.size()));
            System.out.println("Not enough security questions in DB.");
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
            logger.warn("Security questions setup failed: same question selected twice (userId={}, qIndex={})", userId, q1);
            System.out.println("[ERROR] Questions must be different.");
            ConsoleUtil.pause(sc);
            return;
        }

        SecurityQuestion sq1 = questions.get(q1 - 1);
        SecurityQuestion sq2 = questions.get(q2 - 1);

        System.out.print("Answer for Q1: ");
        String a1 = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(a1)) {
            logger.warn("Security questions setup failed: empty answer for Q1 (userId={}, qId={})", userId, sq1.getqId());
            System.out.println("Answer cannot be empty.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Answer for Q2: ");
        String a2 = sc.nextLine();
        if (!ValidationUtil.isNonEmpty(a2)) {
            logger.warn("Security questions setup failed: empty answer for Q2 (userId={}, qId={})", userId, sq2.getqId());
            System.out.println("Answer cannot be empty.");
            ConsoleUtil.pause(sc);
            return;
        }

        boolean saved1 = saveAnswer(userId, sq1.getqId(), HashUtil.hash(a1.trim().toLowerCase()));
        boolean saved2 = saveAnswer(userId, sq2.getqId(), HashUtil.hash(a2.trim().toLowerCase()));

        if (!saved1 || !saved2) {
            logger.error("Security questions setup incomplete (userId={}, savedQ1={}, savedQ2={})", userId, saved1, saved2);
            System.out.println("Failed to save security answers. Try again.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Security questions saved successfully (userId={}, q1Id={}, q2Id={})",
                userId, sq1.getqId(), sq2.getqId());

        System.out.println("Security questions saved.");
        ConsoleUtil.pause(sc);
    }

    public void forgotPassword(Scanner sc) {
        ConsoleUtil.printHeader("Forgot Password");
        logger.info("Forgot password started");

        System.out.println("Recover using:");
        System.out.println("1. Email");
        System.out.println("2. Phone");
        System.out.print("Choice: ");
        String c = sc.nextLine();

        User user = null;

        try {
            if ("1".equals(c)) {
                System.out.print("Email: ");
                String email = sc.nextLine();
                if (!ValidationUtil.isValidEmail(email)) {
                    logger.warn("Forgot password failed: invalid email format");
                    System.out.println("Invalid email format.");
                    ConsoleUtil.pause(sc);
                    return;
                }
                user = userDao.findByEmail(email.trim());

            } else if ("2".equals(c)) {
                System.out.print("Phone: ");
                String phone = sc.nextLine();
                if (!ValidationUtil.isValidPhone(phone)) {
                    logger.warn("Forgot password failed: invalid phone format");
                    System.out.println("Phone must be 10 digits.");
                    ConsoleUtil.pause(sc);
                    return;
                }
                user = userDao.findByPhone(phone.trim());

            } else {
                logger.warn("Forgot password failed: invalid choice='{}'", c);
                System.out.println("Invalid choice.");
                ConsoleUtil.pause(sc);
                return;
            }
        } catch (Exception e) {
            logger.error("Forgot password failed: DAO error while finding user (choice='{}')", c, e);
            System.out.println("Unable to find user right now.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (user == null) {
            logger.warn("Forgot password failed: user not found (choice='{}')", c);
            System.out.println("User not found.");
            ConsoleUtil.pause(sc);
            return;
        }

        long userId = user.getUserId();
        logger.info("Forgot password user found (userId={}, username='{}')", userId, safe(user.getUsername()));

        UserQuestion uq1 = getFirstUserQuestion(userId);
        UserQuestion uq2 = getSecondUserQuestion(userId, uq1 == null ? -1 : uq1.qId);

        if (uq1 == null || uq2 == null) {
            logger.warn("Forgot password failed: no security questions set (userId={})", userId);
            System.out.println("No security questions set for this user.");
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
            logger.warn("Forgot password failed: security answers mismatch (userId={})", userId);
            System.out.println("Answers do not match. Recovery failed.");
            ConsoleUtil.pause(sc);
            return;
        }

        System.out.print("Enter NEW password: ");
        String p1 = sc.nextLine();
        System.out.print("Confirm NEW password: ");
        String p2 = sc.nextLine();

        if (!p1.equals(p2)) {
            logger.warn("Forgot password failed: passwords do not match (userId={})", userId);
            System.out.println("Passwords do not match.");
            ConsoleUtil.pause(sc);
            return;
        }

        if (!ValidationUtil.isValidPassword(p1)) {
            logger.warn("Forgot password failed: weak password (userId={})", userId);
            System.out.println("Weak password. Must contain uppercase/lowercase/digit/special and >= 8 chars.");
            ConsoleUtil.pause(sc);
            return;
        }

        try {
            userDao.updatePasswordHash(userId, HashUtil.hash(p1));
        } catch (Exception e) {
            logger.error("Forgot password failed: DAO error updating password hash (userId={})", userId, e);
            System.out.println("Failed to update password. Try again.");
            ConsoleUtil.pause(sc);
            return;
        }

        logger.info("Password updated successfully (userId={})", userId);
        System.out.println("Password updated successfully. You can login now.");
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

    private boolean saveAnswer(long userId, long qId, String answerHash) {
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
            logger.debug("Security answer saved (userId={}, qId={})", userId, qId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to save security answer (userId={}, qId={})", userId, qId, e);
            return false;

        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {
                logger.debug("Failed to close PreparedStatement", e);
            }
            try { if (conn != null) conn.close(); } catch (Exception e) {
                logger.debug("Failed to close Connection", e);
            }
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
            logger.error("Failed to fetch user security question (userId={}, excludeQId={})", userId, excludeQId, e);

        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {
                logger.debug("Failed to close ResultSet", e);
            }
            try { if (ps != null) ps.close(); } catch (Exception e) {
                logger.debug("Failed to close PreparedStatement", e);
            }
            try { if (conn != null) conn.close(); } catch (Exception e) {
                logger.debug("Failed to close Connection", e);
            }
        }

        return null;
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    private static class UserQuestion {
        long qId;
        String question;
        String answerHash;
    }
}
