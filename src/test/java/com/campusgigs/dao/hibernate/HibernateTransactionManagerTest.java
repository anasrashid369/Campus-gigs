package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.exception.DataAccessException;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class HibernateTransactionManagerTest {

    private StatelessSession session;
    private Transaction transaction;
    private HibernateTransactionManager manager;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        session = mock(StatelessSession.class);
        transaction = mock(Transaction.class);
        when(sessionFactory.openStatelessSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        manager = new HibernateTransactionManager(sessionFactory);
    }

    @Test
    void commitsAndClosesOnSuccess() {
        String result = manager.call(() -> "done");

        assertEquals("done", result);
        InOrder order = inOrder(session, transaction);
        order.verify(session).beginTransaction();
        order.verify(transaction).commit();
        order.verify(session).close();
        verify(transaction, never()).rollback();
    }

    @Test
    void daoCallsInsideTheWorkShareTheSession() {
        StatelessSession seen = manager.call(HibernateTransactionManager::currentSession);

        assertSame(session, seen);
        assertNull(HibernateTransactionManager.currentSession());
    }

    @Test
    void nestedCallsJoinTheOuterTransaction() {
        manager.run(() -> manager.run(() -> { }));

        verify(session, times(1)).beginTransaction();
        verify(transaction, times(1)).commit();
    }

    @Test
    void rollsBackAndRethrowsBusinessErrors() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> manager.run(() -> {
                    throw new IllegalStateException("insufficient funds");
                }));

        assertEquals("insufficient funds", e.getMessage());
        verify(transaction).rollback();
        verify(transaction, never()).commit();
        verify(session).close();
        assertNull(HibernateTransactionManager.currentSession());
    }

    @Test
    void rollsBackAndWrapsDatabaseErrors() {
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> manager.run(() -> {
                    throw new PersistenceException("boom");
                }));

        assertEquals("Transaction failed and was rolled back", e.getMessage());
        verify(transaction).rollback();
        verify(session).close();
    }

    @Test
    void reportsAnUnreachableDatabase() {
        when(session.beginTransaction()).thenThrow(new PersistenceException("connection refused"));

        DataAccessException e = assertThrows(DataAccessException.class, () -> manager.run(() -> { }));

        assertEquals("Could not connect to the database", e.getMessage());
        verify(session).close();
        assertNull(HibernateTransactionManager.currentSession());
    }
}
