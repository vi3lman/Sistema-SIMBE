package py.una.BD;

import java.sql.Connection;
import java.sql.DriverManager;

public class Bd {

    private static final String URL = System.getProperty("simbe.db.url", "jdbc:postgresql://localhost:5432/simbe");
    private static final String USER = System.getProperty("simbe.db.user", "postgres");
    private static final String PASSWORD = System.getProperty("simbe.db.password", "admin");

    public static Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}