package pl.netia.tests.ttapi.qa.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                TestEnvironment.DB_URL, TestEnvironment.DB_USERNAME, TestEnvironment.DB_PASSWORD);
    }
}
