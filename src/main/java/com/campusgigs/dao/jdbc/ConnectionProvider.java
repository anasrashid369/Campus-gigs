package com.campusgigs.dao.jdbc;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.util.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Hands out a new JDBC connection on every call. Callers own the connection
 * and must close it (try-with-resources). One connection is never shared
 * across the application.
 */
public class ConnectionProvider {

    private static ConnectionProvider instance;

    private final String url;
    private final String user;
    private final String password;

    /** Creates a provider with explicit settings (used by tests). */
    public ConnectionProvider(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Returns the shared provider configured from config.properties. */
    public static synchronized ConnectionProvider getInstance() {
        if (instance == null) {
            ConfigLoader config = ConfigLoader.getInstance();
            instance = new ConnectionProvider(
                    config.getRequired("db.url"),
                    config.getRequired("db.user"),
                    config.getRequired("db.password"));
        }
        return instance;
    }

    /**
     * Opens a new connection.
     *
     * @throws DataAccessException if the database cannot be reached
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database", e);
        }
    }
}
