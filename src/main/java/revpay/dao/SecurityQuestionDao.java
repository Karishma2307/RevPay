package revpay.dao;

import java.util.List;
import revpay.model.SecurityQuestion;

public interface SecurityQuestionDao {
    List<SecurityQuestion> getAllQuestions();
}