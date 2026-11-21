package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.TransactionManager;
import com.campusgigs.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Runs a unit of work inside one JDBC transaction: begin, commit,
 * rollback on any failure, and always close the connection.
 * The connection is bound to the current thread so every DAO call made
 * by the work joins the same transaction. Nested calls join the outer one.
 */
public class TransactionHelper implements TransactionManager {

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

    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private final ConnectionProvider connectionProvider;

    public TransactionHelper(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /** Returns the connection of the transaction running on this thread, or null. */
    static Connection currentConnection() {
        return CURRENT.get();
    }

    /**
     * Runs the work in a transaction and returns its result. The transaction
     * is rolled back if the work throws, and committed otherwise.
     *
     * @throws DataAccessException on SQL failure (rolled back)
     */
    public <T> T execute(TransactionWork<T> work) {
        Connection existing = CURRENT.get();
        if (existing != null) {
            return runJoined(existing, work);
        }
        try (Connection connection = connectionProvider.getConnection()) {
            CURRENT.set(connection);
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
            } finally {
                CURRENT.remove();
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

    @Override
    public <T> T call(Supplier<T> work) {
        return execute(connection -> work.get());
    }

    @Override
    public void run(Runnable work) {
        execute(connection -> {
            work.run();
            return null;
        });
    }

    private <T> T runJoined(Connection connection, TransactionWork<T> work) {
        try {
            return work.run(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed and was rolled back", e);
        }
    }

    private void rollbackQuietly(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
