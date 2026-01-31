package revpay.util;

import java.util.Base64;

public class KeyUtil {

    private static final String FALLBACK_B64 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="; 

    public static byte[] getAes256Key() {
        String b64 = System.getenv("REVPAY_AES_KEY_B64");
        if (b64 == null || b64.trim().isEmpty()) {
            b64 = FALLBACK_B64;
        }
        byte[] key = Base64.getDecoder().decode(b64);
        if (key.length != 32) {
            throw new RuntimeException("AES-256 key must be exactly 32 bytes");
        }
        return key;
    }
}