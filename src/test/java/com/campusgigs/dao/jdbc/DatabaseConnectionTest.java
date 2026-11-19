package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Needs a local MySQL with db/01_schema.sql loaded and config/config.properties
 * filled in. Skipped when the config file is absent.
 */
class DatabaseConnectionTest {

    @Test
    void connectsAndSeesSchema() throws SQLException {
        Path config = Paths.get("config/config.properties");
        Assumptions.assumeTrue(Files.isRegularFile(config), "config/config.properties not found");

        try (Connection connection = ConnectionProvider.getInstance().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'campus_gigs'")) {
            assertTrue(rs.next());
            assertEquals(14, rs.getInt(1));
        }
    }
}
