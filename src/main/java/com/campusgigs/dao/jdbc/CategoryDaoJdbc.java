package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.model.GigCategory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link CategoryDao}. */
public class CategoryDaoJdbc extends BaseJdbcDao implements CategoryDao {

    private static final String SELECT = "SELECT category_id, name, is_active FROM GigCategories";

    public CategoryDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public List<GigCategory> findAll() {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {
                List<GigCategory> categories = new ArrayList<>();
                while (rs.next()) {
                    categories.add(map(rs));
                }
                return categories;
            }
        });
    }

    @Override
    public Optional<GigCategory> findById(long categoryId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE category_id = ?")) {
                ps.setLong(1, categoryId);
                return findOne(ps);
            }
        });
    }

    @Override
    public Optional<GigCategory> findByName(String name) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE name = ?")) {
                ps.setString(1, name);
                return findOne(ps);
            }
        });
    }

    @Override
    public long insert(String name) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO GigCategories (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public void rename(long categoryId, String name) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE GigCategories SET name = ? WHERE category_id = ?")) {
                ps.setString(1, name);
                ps.setLong(2, categoryId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void setActive(long categoryId, boolean active) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE GigCategories SET is_active = ? WHERE category_id = ?")) {
                ps.setBoolean(1, active);
                ps.setLong(2, categoryId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    private static Optional<GigCategory> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private static GigCategory map(ResultSet rs) throws SQLException {
        return new GigCategory(rs.getLong("category_id"), rs.getString("name"), rs.getBoolean("is_active"));
    }
}
