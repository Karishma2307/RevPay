package revpay.dao;

import revpay.model.Wallet;

public interface WalletDao {

    void createWalletForUser(long userId);

    Wallet findByUserId(long userId);

    void updateBalance(long walletId, double newBalance);
}
