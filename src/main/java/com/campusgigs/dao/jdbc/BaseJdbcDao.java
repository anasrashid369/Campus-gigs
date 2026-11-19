package com.campusgigs.dao.jdbc;

import com.campusgigs.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for JDBC DAOs. Uses the connection of the running transaction when
 * there is one; otherwise opens a new connection for the single operation.
 */
abstract class BaseJdbcDao {

    /** JDBC work that returns a value. */
    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    protected BaseJdbcDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    protected <T> T withConnection(SqlFunction<T> work) {
        Connection current = TransactionHelper.currentConnection();
        try {
            if (current != null) {
                return work.apply(current);
            }
            try (Connection connection = connectionProvider.getConnection()) {
                return work.apply(connection);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database operation failed", e);
        }
    }
}
