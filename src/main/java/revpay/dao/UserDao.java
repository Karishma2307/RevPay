package revpay.dao;

import revpay.model.User;

public interface UserDao {

    User findByEmail(String email);

    User findByPhone(String phone);

    User findByAccountId(String accountId);

    long createUser(User user);

    void updateFailedAttempts(long userId, int attempts);

    void updateStatus(long userId, String status);
}
