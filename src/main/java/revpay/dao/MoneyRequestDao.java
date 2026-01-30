package revpay.dao;

import java.util.List;
import revpay.model.MoneyRequest;

public interface MoneyRequestDao {

    long createRequest(MoneyRequest req);

    List<MoneyRequest> findIncoming(long userId);

    
    List<MoneyRequest> findOutgoing(long userId);

    
    boolean updateStatus(long requestId, String status);

    MoneyRequest findById(long requestId);
}
