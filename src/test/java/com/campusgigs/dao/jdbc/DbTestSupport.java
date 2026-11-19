package com.campusgigs.dao.jdbc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;

/** Helpers for tests that need the local MySQL database; they skip when it is not configured. */
final class DbTestSupport {

    private DbTestSupport() {
    }

    static ConnectionProvider provider() {
        Assumptions.assumeTrue(Files.isRegularFile(Paths.get("config/config.properties")),
                "config/config.properties not found");
        return ConnectionProvider.getInstance();
    }

    /** Inserts a throwaway client user and returns its id. */
    static long createUser(ConnectionProvider provider) throws SQLException {
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Users (role, full_name, email, password_hash) VALUES ('CLIENT', 'Test', ?, 'x')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "test-" + UUID.randomUUID() + "@example.com");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Removes the throwaway user together with its wallet and ledger rows. */
    static void deleteUser(ConnectionProvider provider, long userId) throws SQLException {
        try (Connection c = provider.getConnection()) {
            run(c, "DELETE FROM Transactions WHERE wallet_id IN (SELECT wallet_id FROM Wallets WHERE user_id = ?)", userId);
            run(c, "DELETE FROM Wallets WHERE user_id = ?", userId);
            run(c, "DELETE FROM Users WHERE user_id = ?", userId);
        }
    }

    private static void run(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
