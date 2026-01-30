package revpay.dao;

import java.util.List;
import revpay.model.PaymentMethod;

public interface PaymentMethodDao {
    long add(PaymentMethod pm);
    List<PaymentMethod> findByUserId(long userId);
    PaymentMethod findDefaultByUserId(long userId);
    boolean setDefault(long userId, long methodId);
    boolean delete(long userId, long methodId);
}
