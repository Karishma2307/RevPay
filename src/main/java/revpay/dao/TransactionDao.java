package revpay.dao;

import java.util.List;

import revpay.model.Transaction;

public interface TransactionDao {

    long createTransaction(Transaction txn);

    java.util.List<Transaction> findByUserId(long userId);
}
