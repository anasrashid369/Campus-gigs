package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.SettingsDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** JDBC implementation of {@link SettingsDao}. */
public class SettingsDaoJdbc extends BaseJdbcDao implements SettingsDao {

    public SettingsDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Optional<String> find(String key) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT setting_value FROM PlatformSettings WHERE setting_key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getString("setting_value")) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Map<String, String> findAll() {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT setting_key, setting_value FROM PlatformSettings ORDER BY setting_key");
                 ResultSet rs = ps.executeQuery()) {
                Map<String, String> settings = new LinkedHashMap<>();
                while (rs.next()) {
                    settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }
                return settings;
            }
        });
    }

    @Override
    public void upsert(String key, String value) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO PlatformSettings (setting_key, setting_value) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE setting_value = ?")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.setString(3, value);
                ps.executeUpdate();
                return null;
            }
        });
    }
}
