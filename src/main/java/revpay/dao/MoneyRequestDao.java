package revpay.dao;

import java.util.List;

import revpay.model.MoneyRequest;

public interface MoneyRequestDao {

    long createRequest(MoneyRequest req);

    java.util.List<MoneyRequest> findIncoming(long userId);

    java.util.List<MoneyRequest> findOutgoing(long userId);

    MoneyRequest findById(long requestId);

    void updateStatus(long requestId, String newStatus);
}
