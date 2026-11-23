package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.SkillDao;
import com.campusgigs.model.Skill;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link SkillDao}. */
public class SkillDaoJdbc extends BaseJdbcDao implements SkillDao {

    private static final String SELECT = "SELECT skill_id, name FROM Skills";

    public SkillDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public List<Skill> findAll() {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {
                List<Skill> skills = new ArrayList<>();
                while (rs.next()) {
                    skills.add(map(rs));
                }
                return skills;
            }
        });
    }

    @Override
    public Optional<Skill> findById(long skillId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE skill_id = ?")) {
                ps.setLong(1, skillId);
                return findOne(ps);
            }
        });
    }

    @Override
    public Optional<Skill> findByName(String name) {
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
                    "INSERT INTO Skills (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
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
    public void rename(long skillId, String name) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Skills SET name = ? WHERE skill_id = ?")) {
                ps.setString(1, name);
                ps.setLong(2, skillId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public boolean deleteIfUnused(long skillId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM Skills WHERE skill_id = ?"
                            + " AND NOT EXISTS (SELECT 1 FROM UserSkills WHERE skill_id = ?)"
                            + " AND NOT EXISTS (SELECT 1 FROM GigSkills WHERE skill_id = ?)")) {
                ps.setLong(1, skillId);
                ps.setLong(2, skillId);
                ps.setLong(3, skillId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private static Optional<Skill> findOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private static Skill map(ResultSet rs) throws SQLException {
        return new Skill(rs.getLong("skill_id"), rs.getString("name"));
    }
}
