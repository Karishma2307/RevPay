package revpay.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // TODO: change to your real DB values
    private static final String URL  = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";
    private static final String PASS = "system";

    static {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] Oracle JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
