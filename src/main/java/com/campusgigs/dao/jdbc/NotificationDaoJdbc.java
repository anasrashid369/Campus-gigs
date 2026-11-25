package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.NotificationDao;
import com.campusgigs.model.Notification;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link NotificationDao}. */
public class NotificationDaoJdbc extends BaseJdbcDao implements NotificationDao {

    private static final String SELECT =
            "SELECT notification_id, user_id, message, is_read, created_at FROM Notifications";
    private static final String NEWEST_FIRST = " ORDER BY created_at DESC, notification_id DESC";

    public NotificationDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public long insert(long userId, String message) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Notifications (user_id, message) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setString(2, message);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public Optional<Notification> findById(long notificationId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE notification_id = ?")) {
                ps.setLong(1, notificationId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Notification> findByUserId(long userId) {
        return findList(SELECT + " WHERE user_id = ?" + NEWEST_FIRST, userId);
    }

    @Override
    public List<Notification> findUnreadByUserId(long userId) {
        return findList(SELECT + " WHERE user_id = ? AND is_read = FALSE" + NEWEST_FIRST, userId);
    }

    @Override
    public int countUnread(long userId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM Notifications WHERE user_id = ? AND is_read = FALSE")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    @Override
    public void markAsRead(long notificationId) {
        update("UPDATE Notifications SET is_read = TRUE WHERE notification_id = ?", notificationId);
    }

    @Override
    public void markAllAsRead(long userId) {
        update("UPDATE Notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE", userId);
    }

    private List<Notification> findList(String sql, long userId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Notification> notifications = new ArrayList<>();
                    while (rs.next()) {
                        notifications.add(map(rs));
                    }
                    return notifications;
                }
            }
        });
    }

    private void update(String sql, long id) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
                return null;
            }
        });
    }

    private static Notification map(ResultSet rs) throws SQLException {
        return new Notification(rs.getLong("notification_id"), rs.getLong("user_id"), rs.getString("message"),
                rs.getBoolean("is_read"), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
