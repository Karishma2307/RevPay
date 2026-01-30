package revpay.dao;

import java.util.List;
import revpay.model.SecurityQuestion;

public interface UserSecurityAnswerDao {

    void saveAnswer(long userId, long qId, String answerHash);

    List<SecurityQuestion> getQuestionsForUser(long userId);

    boolean verifyAnswer(long userId, long qId, String plainAnswer);

    void deleteAnswersForUser(long userId);
}