package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.GigDao;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

/** JDBC implementation of {@link GigDao}. */
public class GigDaoJdbc extends BaseJdbcDao implements GigDao {

    private static final String SELECT = "SELECT gig_id, client_id, category_id, assigned_freelancer_id, "
            + "title, description, budget, deadline, status, created_at FROM Gigs WHERE gig_id = ?";

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
