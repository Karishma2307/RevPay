package revpay.dao;

import revpay.model.User;

public interface UserDao {

    User findByEmail(String email);

    User findByPhone(String phone);

    User findByAccountId(String accountId);

    User findByUsername(String username);

    long createUser(User user);
    
    boolean updatePassword(long userId, String newPasswordHash);

    boolean updateTxnPin(long userId, String newTxnPinHash);


    void updateFailedAttempts(long userId, int attempts);

    void updateStatus(long userId, String status);

    void updatePasswordHash(long userId, String newHash);
}