package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.model.LedgerEntry;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
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

    @Override
    public long sumByType(long walletId, TransactionType type) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) FROM Transactions WHERE wallet_id = ? AND type = ?")) {
                ps.setLong(1, walletId);
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    @Override
    public List<LedgerEntry> findLedger(TransactionType typeOrNull, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT t.transaction_id, u.user_id, u.full_name, t.type, t.amount, "
                + "t.gig_id, t.reference, t.created_at FROM Transactions t "
                + "JOIN Wallets w ON w.wallet_id = t.wallet_id JOIN Users u ON u.user_id = w.user_id WHERE 1 = 1");
        List<Object> values = new ArrayList<>();
        if (typeOrNull != null) {
            sql.append(" AND t.type = ?");
            values.add(typeOrNull.name());
        }
        if (from != null) {
            sql.append(" AND t.created_at >= ?");
            values.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sql.append(" AND t.created_at < ?");
            values.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY t.created_at DESC, t.transaction_id DESC LIMIT ").append(MAX_LEDGER_ROWS);
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    ps.setObject(i + 1, values.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<LedgerEntry> result = new ArrayList<>();
                    while (rs.next()) {
                        long gig = rs.getLong("gig_id");
                        Long gigId = rs.wasNull() ? null : gig;
                        result.add(new LedgerEntry(rs.getLong("transaction_id"), rs.getLong("user_id"),
                                rs.getString("full_name"), TransactionType.valueOf(rs.getString("type")),
                                rs.getLong("amount"), gigId, rs.getString("reference"),
                                rs.getTimestamp("created_at").toLocalDateTime()));
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
