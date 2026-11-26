package com.campusgigs.dao.hibernate;

import com.campusgigs.exception.DataAccessException;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for Hibernate DAOs. Uses the session of the running transaction when there
 * is one; otherwise runs the single operation in its own short transaction. Hibernate
 * errors become {@link DataAccessException}, so services never see Hibernate types.
 */
abstract class BaseHibernateDao {

    private final SessionFactory sessionFactory;

    protected BaseHibernateDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected <T> T withSession(Function<StatelessSession, T> work) {
        StatelessSession current = HibernateTransactionManager.currentSession();
        if (current != null) {
            return apply(work, current);
        }
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            Transaction transaction = HibernateTransactionManager.begin(session);
            try {
                T result = apply(work, session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e instanceof PersistenceException
                        ? new DataAccessException("Database operation failed", e)
                        : e;
            }
        }
    }

    /** Same as {@link #withSession(Function)} for work with no result. */
    protected void inSession(Consumer<StatelessSession> work) {
        withSession(session -> {
            work.accept(session);
            return null;
        });
    }

    private static <T> T apply(Function<StatelessSession, T> work, StatelessSession session) {
        try {
            return work.apply(session);
        } catch (PersistenceException e) {
            throw new DataAccessException("Database operation failed", e);
        }
    }
}
