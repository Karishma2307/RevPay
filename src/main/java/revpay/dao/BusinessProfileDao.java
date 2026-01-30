package revpay.dao;

public interface BusinessProfileDao {
    void create(long userId, String name, String type, String taxId, String address, String doc, String verifiedStatus);
}