package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Skill;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link FreelancerProfileDao}. */
public class FreelancerProfileDaoJdbc extends BaseJdbcDao implements FreelancerProfileDao {

    public FreelancerProfileDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Optional<FreelancerProfile> findByUserId(long userId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT profile_id, user_id, biography, portfolio_links FROM FreelancerProfiles WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new FreelancerProfile(rs.getLong("profile_id"), rs.getLong("user_id"),
                            rs.getString("biography"), rs.getString("portfolio_links")));
                }
            }
        });
    }

    @Override
    public long insert(long userId, String biography, String portfolioLinks) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO FreelancerProfiles (user_id, biography, portfolio_links) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setString(2, biography);
                ps.setString(3, portfolioLinks);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public void update(long userId, String biography, String portfolioLinks) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE FreelancerProfiles SET biography = ?, portfolio_links = ? WHERE user_id = ?")) {
                ps.setString(1, biography);
                ps.setString(2, portfolioLinks);
                ps.setLong(3, userId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void replaceSkills(long userId, List<Long> skillIds) {
        withConnection(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM UserSkills WHERE user_id = ?")) {
                delete.setLong(1, userId);
                delete.executeUpdate();
            }
            if (skillIds.isEmpty()) {
                return null;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO UserSkills (user_id, skill_id) VALUES (?, ?)")) {
                for (long skillId : skillIds) {
                    insert.setLong(1, userId);
                    insert.setLong(2, skillId);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            return null;
        });
    }

    @Override
    public List<Skill> findSkills(long userId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT s.skill_id, s.name FROM Skills s JOIN UserSkills us ON us.skill_id = s.skill_id"
                            + " WHERE us.user_id = ? ORDER BY s.name")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Skill> skills = new ArrayList<>();
                    while (rs.next()) {
                        skills.add(new Skill(rs.getLong("skill_id"), rs.getString("name")));
                    }
                    return skills;
                }
            }
        });
    }
}
