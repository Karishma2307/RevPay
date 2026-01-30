package revpay.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

	private static final byte[] SECRET_KEY_32 = new byte[] {
		    82, 69, 86, 80, 65, 89, 95, 65,
		    69, 83, 95, 50, 53, 54, 95, 75,
		    69, 89, 95, 51, 50, 95, 66, 89,
		    84, 69, 83, 95, 79, 75, 33, 33
		};



    private static final String ALGO = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN_BYTES = 12;

    private static SecretKey key() {
        if (SECRET_KEY_32.length != 32) {
            throw new IllegalStateException("AES key must be exactly 32 bytes for AES-256");
        }
        return new SecretKeySpec(SECRET_KEY_32, ALGO);
    }


    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // store: Base64(IV) + ":" + Base64(cipher)
            return Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    public static String decrypt(String enc) {
        try {
            String[] parts = enc.split(":");
            if (parts.length != 2) throw new IllegalArgumentException("Invalid encrypted format");

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    private CryptoUtil() {}
}
