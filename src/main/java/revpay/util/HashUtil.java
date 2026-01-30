package revpay.util;

import org.mindrot.jbcrypt.BCrypt;

public class HashUtil {

    private static final int WORKLOAD = 10;

    public static String hash(String plainText) {
        if (plainText == null) return null;
        return BCrypt.hashpw(plainText, BCrypt.gensalt(WORKLOAD));
    }

    public static boolean check(String plainText, String storedHash) {
        if (plainText == null || storedHash == null || storedHash.length() == 0) {
            return false;
        }
        return BCrypt.checkpw(plainText, storedHash);
    }
}