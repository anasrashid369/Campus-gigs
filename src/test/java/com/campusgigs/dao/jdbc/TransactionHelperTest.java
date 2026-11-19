package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class TransactionHelperTest {

    private ConnectionProvider provider;
    private Connection connection;
    private TransactionHelper helper;

    @BeforeEach
    void setUp() {
        provider = mock(ConnectionProvider.class);
        connection = mock(Connection.class);
        when(provider.getConnection()).thenReturn(connection);
        helper = new TransactionHelper(provider);
    }

    @Test
    void commitsAndClosesOnSuccess() throws SQLException {
        String result = helper.execute(c -> "done");

        assertEquals("done", result);
        InOrder order = inOrder(connection);
        order.verify(connection).setAutoCommit(false);
        order.verify(connection).commit();
        order.verify(connection).close();
        verify(connection, never()).rollback();
    }

    @Test
    void rollsBackAndWrapsSqlException() throws SQLException {
        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> helper.executeVoid(c -> {
                    throw new SQLException("boom");
                }));

        assertEquals("Transaction failed and was rolled back", ex.getMessage());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void rollsBackAndRethrowsRuntimeException() throws SQLException {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> helper.executeVoid(c -> {
                    throw new IllegalStateException("insufficient funds");
                }));

        assertEquals("insufficient funds", ex.getMessage());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }
}
