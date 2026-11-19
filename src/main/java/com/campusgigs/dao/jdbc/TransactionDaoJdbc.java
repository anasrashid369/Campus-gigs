package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation of {@link TransactionDao}. */
public class TransactionDaoJdbc extends BaseJdbcDao implements TransactionDao {

    public TransactionDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Transaction insert(long walletId, Long gigId, TransactionType type, long amount, String reference) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Transactions (wallet_id, gig_id, type, amount, reference) "
                            + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, walletId);
                if (gigId == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, gigId);
                }
                ps.setString(3, type.name());
                ps.setLong(4, amount);
                ps.setString(5, reference);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Transaction(keys.getLong(1), walletId, gigId, type, amount,
                            reference, LocalDateTime.now());
                }
            }
        });
    }

    @Override
    public List<Transaction> findByWalletId(long walletId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT transaction_id, wallet_id, gig_id, type, amount, reference, created_at "
                            + "FROM Transactions WHERE wallet_id = ? "
                            + "ORDER BY created_at DESC, transaction_id DESC")) {
                ps.setLong(1, walletId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Transaction> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(map(rs));
                    }
                    return result;
                }
            }
        });
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        long gig = rs.getLong("gig_id");
        Long gigId = rs.wasNull() ? null : gig;
        return new Transaction(rs.getLong("transaction_id"), rs.getLong("wallet_id"), gigId,
                TransactionType.valueOf(rs.getString("type")), rs.getLong("amount"),
                rs.getString("reference"), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
