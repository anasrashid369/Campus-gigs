package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.WalletDao;
import com.campusgigs.model.Wallet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** JDBC implementation of {@link WalletDao}. */
public class WalletDaoJdbc extends BaseJdbcDao implements WalletDao {

    private static final String COLUMNS =
            "wallet_id, user_id, available_balance, escrow_balance, row_version";

    public WalletDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Optional<Wallet> findByUserId(long userId) {
        return findByUserId(userId, "SELECT " + COLUMNS + " FROM Wallets WHERE user_id = ?");
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(long userId) {
        return findByUserId(userId, "SELECT " + COLUMNS + " FROM Wallets WHERE user_id = ? FOR UPDATE");
    }

    @Override
    public Wallet create(long userId, long availableBalance) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Wallets (user_id, available_balance, escrow_balance) VALUES (?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, availableBalance);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Wallet(keys.getLong(1), userId, availableBalance, 0, 0);
                }
            }
        });
    }

    @Override
    public void updateBalances(long walletId, long availableBalance, long escrowBalance) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Wallets SET available_balance = ?, escrow_balance = ?, "
                            + "row_version = row_version + 1 WHERE wallet_id = ?")) {
                ps.setLong(1, availableBalance);
                ps.setLong(2, escrowBalance);
                ps.setLong(3, walletId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    private Optional<Wallet> findByUserId(long userId, String sql) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    private static Wallet map(ResultSet rs) throws SQLException {
        return new Wallet(rs.getLong("wallet_id"), rs.getLong("user_id"),
                rs.getLong("available_balance"), rs.getLong("escrow_balance"),
                rs.getInt("row_version"));
    }
}
