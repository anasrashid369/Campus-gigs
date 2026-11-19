package com.campusgigs.dao.jdbc;

import com.campusgigs.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs a unit of work inside one JDBC transaction: begin, commit,
 * rollback on any failure, and always close the connection.
 */
public class TransactionHelper {

    /** Work executed with the transaction's connection. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /** Work with no result. */
    @FunctionalInterface
    public interface TransactionAction {
        void run(Connection connection) throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    public TransactionHelper(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Runs the work in a transaction and returns its result. The transaction
     * is rolled back if the work throws, and committed otherwise.
     *
     * @throws DataAccessException on SQL failure (rolled back)
     */
    public <T> T execute(TransactionWork<T> work) {
        try (Connection connection = connectionProvider.getConnection()) {
            try {
                connection.setAutoCommit(false);
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection, e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new DataAccessException("Transaction failed and was rolled back", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the database connection", e);
        }
    }

    /** Same as {@link #execute(TransactionWork)} for work with no result. */
    public void executeVoid(TransactionAction action) {
        execute(connection -> {
            action.run(connection);
            return null;
        });
    }

    private void rollbackQuietly(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
