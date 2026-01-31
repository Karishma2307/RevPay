package revpay.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class DateUtil {
    private DateUtil() {}

    
    public static final DateTimeFormatter YYYY_MM_DD =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    public static LocalDate parseStrict(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, YYYY_MM_DD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean isValidStrict(String input) {
        return parseStrict(input) != null;
    }
}
