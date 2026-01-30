package revpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogForceTest {

    private static final Logger logger =
            LoggerFactory.getLogger(LogForceTest.class);

    public static void main(String[] args) {
        logger.info("FORCE LOG FILE CREATION TEST");
        System.out.println("Program ran");
    }
}
