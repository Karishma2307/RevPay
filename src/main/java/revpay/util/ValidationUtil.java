package revpay.util;

public class ValidationUtil {

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return phone.matches("^[0-9]{10}$");  
    }

    public static boolean isNonEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    public static boolean isValidPassword(String pw) {
        if (pw == null) return false;
        
        return pw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_])[A-Za-z\\d@$!%*?&_]{8,}$");
    }

    public static boolean isValidTxnPin(String pin) {
        if (pin == null) return false;
        return pin.matches("^[0-9]{4}$");
    }
}