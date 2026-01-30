package revpay.dao;

import java.util.Date;
import java.util.List;

import revpay.model.Transaction;

public interface TransactionDao {

    void createTransaction(long fromUserId, long toUserId, double amount,
                           String type, String status, String note, String refId);

    List<Transaction> searchTransactions(long userId,
                                        String type,
                                        String status,
                                        Date fromDate,
                                        Date toDate,
                                        Double minAmount,
                                        Double maxAmount,
                                        String keyword);
}