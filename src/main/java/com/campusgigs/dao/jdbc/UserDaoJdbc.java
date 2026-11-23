package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.UserDao;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link UserDao}. */
public class UserDaoJdbc extends BaseJdbcDao implements UserDao {

    private static final String COLUMNS =
            "user_id, role, full_name, email, password_hash, account_status, created_at";

    public UserDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Optional<User> findById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM Users WHERE user_id = ?")) {
                ps.setLong(1, id);
                return findOne(ps);
            }
        });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM Users WHERE email = ?")) {
                ps.setString(1, email);
                return findOne(ps);
            }
        });
    }

    @Override
    public long insert(Role role, String fullName, String email, String passwordHash) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Users (role, full_name, email, password_hash) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, role.name());
                ps.setString(2, fullName);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public List<User> findAll() {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM Users ORDER BY user_id");
                 ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(map(rs));
                }
                return users;
            }
        });
    }

    @Override
    public void updateAccountStatus(long userId, AccountStatus status) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Users SET account_status = ? WHERE user_id = ?")) {
                ps.setString(1, status.name());
                ps.setLong(2, userId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void updateName(long userId, String fullName) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Users SET full_name = ? WHERE user_id = ?")) {
                ps.setString(1, fullName);
                ps.setLong(2, userId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    private static Optional<User> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("user_id"),
                Role.valueOf(rs.getString("role")),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                AccountStatus.valueOf(rs.getString("account_status")),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
