package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.TransactionManager;
import com.campusgigs.exception.DataAccessException;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.function.Supplier;

/**
 * Runs a unit of work inside one Hibernate transaction: begin, commit, roll back on any
 * failure, and always close the session. The session is bound to the current thread, so
 * every DAO call made by the work joins the same transaction. Nested calls join the outer one.
 * <p>
 * It uses a {@link StatelessSession}: no first-level cache and no automatic dirty checking,
 * so every DAO call goes straight to the database, in the order the service makes it.
 */
public class HibernateTransactionManager implements TransactionManager {

    private static final ThreadLocal<StatelessSession> CURRENT = new ThreadLocal<>();

    private final SessionFactory sessionFactory;

    public HibernateTransactionManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /** The session of the transaction running on this thread, or null. */
    static StatelessSession currentSession() {
        return CURRENT.get();
    }

    @Override
    public <T> T call(Supplier<T> work) {
        if (CURRENT.get() != null) {
            return work.get();
        }
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            Transaction transaction = begin(session);
            CURRENT.set(session);
            try {
                T result = work.get();
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(transaction, e);
                if (e instanceof PersistenceException) {
                    throw new DataAccessException("Transaction failed and was rolled back", e);
                }
                throw e;
            } finally {
                CURRENT.remove();
            }
        }
    }

    @Override
    public void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }

    /** Starts the transaction; this is where the database connection is opened. */
    static Transaction begin(StatelessSession session) {
        try {
            return session.beginTransaction();
        } catch (PersistenceException e) {
            throw new DataAccessException("Could not connect to the database", e);
        }
    }

    private static void rollbackQuietly(Transaction transaction, RuntimeException original) {
        try {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } catch (RuntimeException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
