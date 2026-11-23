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
        return createUser(provider, "CLIENT");
    }

    /** Inserts a throwaway user with the given role (ADMIN, CLIENT or FREELANCER) and returns its id. */
    static long createUser(ConnectionProvider provider, String role) throws SQLException {
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Users (role, full_name, email, password_hash) VALUES (?, 'Test', ?, 'x')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, role);
            ps.setString(2, "test-" + UUID.randomUUID() + "@example.com");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Inserts a throwaway gig category and returns its id. */
    static long createCategory(ConnectionProvider provider) throws SQLException {
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO GigCategories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "test-" + UUID.randomUUID());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Inserts an OPEN gig of the client with the given budget and returns its id. */
    static long createGig(ConnectionProvider provider, long clientId, long categoryId, long budget)
            throws SQLException {
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO Gigs (client_id, category_id, title, description, budget, deadline) "
                             + "VALUES (?, ?, 'Test gig', 'Test description', ?, '2030-01-01')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, clientId);
            ps.setLong(2, categoryId);
            ps.setLong(3, budget);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Removes a test gig together with its disputes and ledger rows. */
    static void deleteGig(ConnectionProvider provider, long gigId) throws SQLException {
        try (Connection c = provider.getConnection()) {
            run(c, "DELETE FROM Disputes WHERE gig_id = ?", gigId);
            run(c, "DELETE FROM Transactions WHERE gig_id = ?", gigId);
            run(c, "DELETE FROM Gigs WHERE gig_id = ?", gigId);
        }
    }

    /** Removes a test category. */
    static void deleteCategory(ConnectionProvider provider, long categoryId) throws SQLException {
        try (Connection c = provider.getConnection()) {
            run(c, "DELETE FROM GigCategories WHERE category_id = ?", categoryId);
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
