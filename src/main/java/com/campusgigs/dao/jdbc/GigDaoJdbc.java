package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.GigDao;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Skill;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link GigDao}. */
public class GigDaoJdbc extends BaseJdbcDao implements GigDao {

    private static final String SELECT_COLUMNS = "SELECT gig_id, client_id, category_id, assigned_freelancer_id, "
            + "title, description, budget, deadline, status, created_at FROM Gigs";
    private static final String SELECT = SELECT_COLUMNS + " WHERE gig_id = ?";

    public GigDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Optional<Gig> findById(long gigId) {
        return find(SELECT, gigId);
    }

    @Override
    public Optional<Gig> findByIdForUpdate(long gigId) {
        return find(SELECT + " FOR UPDATE", gigId);
    }

    @Override
    public void updateStatus(long gigId, GigStatus status) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Gigs SET status = ? WHERE gig_id = ?")) {
                ps.setString(1, status.name());
                ps.setLong(2, gigId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void assign(long gigId, long freelancerId, GigStatus status) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Gigs SET assigned_freelancer_id = ?, status = ? WHERE gig_id = ?")) {
                ps.setLong(1, freelancerId);
                ps.setString(2, status.name());
                ps.setLong(3, gigId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public long sumBudgetByFreelancerAndStatuses(long freelancerId, Collection<GigStatus> statuses) {
        if (statuses.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(statuses.size(), "?"));
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COALESCE(SUM(budget), 0) FROM Gigs WHERE assigned_freelancer_id = ? "
                            + "AND status IN (" + placeholders + ")")) {
                ps.setLong(1, freelancerId);
                int index = 2;
                for (GigStatus status : statuses) {
                    ps.setString(index++, status.name());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    @Override
    public long insert(long clientId, long categoryId, String title, String description, long budget,
                       LocalDate deadline) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Gigs (client_id, category_id, title, description, budget, deadline) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, clientId);
                ps.setLong(2, categoryId);
                ps.setString(3, title);
                ps.setString(4, description);
                ps.setLong(5, budget);
                ps.setDate(6, Date.valueOf(deadline));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public void update(long gigId, long categoryId, String title, String description, long budget,
                       LocalDate deadline) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Gigs SET category_id = ?, title = ?, description = ?, budget = ?, deadline = ? "
                            + "WHERE gig_id = ?")) {
                ps.setLong(1, categoryId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setLong(4, budget);
                ps.setDate(5, Date.valueOf(deadline));
                ps.setLong(6, gigId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public List<Gig> findByClientId(long clientId) {
        return findMany("client_id = ?", ps -> ps.setLong(1, clientId));
    }

    @Override
    public List<Gig> findByFreelancerId(long freelancerId) {
        return findMany("assigned_freelancer_id = ?", ps -> ps.setLong(1, freelancerId));
    }

    @Override
    public List<Gig> findByStatus(GigStatus status) {
        return findMany("status = ?", ps -> ps.setString(1, status.name()));
    }

    @Override
    public void replaceSkills(long gigId, List<Long> skillIds) {
        withConnection(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM GigSkills WHERE gig_id = ?")) {
                delete.setLong(1, gigId);
                delete.executeUpdate();
            }
            if (!skillIds.isEmpty()) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO GigSkills (gig_id, skill_id) VALUES (?, ?)")) {
                    for (long skillId : skillIds) {
                        insert.setLong(1, gigId);
                        insert.setLong(2, skillId);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }
            return null;
        });
    }

    @Override
    public List<Skill> findSkills(long gigId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT s.skill_id, s.name FROM GigSkills gs JOIN Skills s ON s.skill_id = gs.skill_id "
                            + "WHERE gs.gig_id = ? ORDER BY s.name")) {
                ps.setLong(1, gigId);
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

    /** Parameter setter that may throw {@link SQLException}. */
    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Gig> findMany(String condition, Binder binder) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    SELECT_COLUMNS + " WHERE " + condition + " ORDER BY created_at DESC, gig_id DESC")) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Gig> gigs = new ArrayList<>();
                    while (rs.next()) {
                        gigs.add(map(rs));
                    }
                    return gigs;
                }
            }
        });
    }

    private Optional<Gig> find(String sql, long gigId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, gigId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    private static Gig map(ResultSet rs) throws SQLException {
        long freelancer = rs.getLong("assigned_freelancer_id");
        Long freelancerId = rs.wasNull() ? null : freelancer;
        return new Gig(rs.getLong("gig_id"), rs.getLong("client_id"), rs.getLong("category_id"), freelancerId,
                rs.getString("title"), rs.getString("description"), rs.getLong("budget"),
                rs.getDate("deadline").toLocalDate(), GigStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
