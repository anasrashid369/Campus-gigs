package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ApplicationDao}. */
public class ApplicationDaoJdbc extends BaseJdbcDao implements ApplicationDao {

    private static final String SELECT = "SELECT application_id, gig_id, freelancer_id, proposal_message, "
            + "status, submitted_at FROM GigApplications";

    public ApplicationDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public long insert(long gigId, long freelancerId, String proposalMessage) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO GigApplications (gig_id, freelancer_id, proposal_message) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, gigId);
                ps.setLong(2, freelancerId);
                ps.setString(3, proposalMessage);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    @Override
    public Optional<GigApplication> findById(long applicationId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE application_id = ?")) {
                ps.setLong(1, applicationId);
                return first(ps);
            }
        });
    }

    @Override
    public Optional<GigApplication> findByGigAndFreelancer(long gigId, long freelancerId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    SELECT + " WHERE gig_id = ? AND freelancer_id = ?")) {
                ps.setLong(1, gigId);
                ps.setLong(2, freelancerId);
                return first(ps);
            }
        });
    }

    @Override
    public List<GigApplication> findByGigId(long gigId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    SELECT + " WHERE gig_id = ? ORDER BY submitted_at, application_id")) {
                ps.setLong(1, gigId);
                return all(ps);
            }
        });
    }

    @Override
    public List<GigApplication> findByFreelancerId(long freelancerId) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    SELECT + " WHERE freelancer_id = ? ORDER BY submitted_at DESC, application_id DESC")) {
                ps.setLong(1, freelancerId);
                return all(ps);
            }
        });
    }

    @Override
    public long countByFreelancerAndStatus(long freelancerId, ApplicationStatus status) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM GigApplications WHERE freelancer_id = ? AND status = ?")) {
                ps.setLong(1, freelancerId);
                ps.setString(2, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    @Override
    public void updateStatus(long applicationId, ApplicationStatus status) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE GigApplications SET status = ? WHERE application_id = ?")) {
                ps.setString(1, status.name());
                ps.setLong(2, applicationId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void reopen(long applicationId, String proposalMessage) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE GigApplications SET status = 'PENDING', proposal_message = ?, "
                            + "submitted_at = CURRENT_TIMESTAMP WHERE application_id = ?")) {
                ps.setString(1, proposalMessage);
                ps.setLong(2, applicationId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    @Override
    public void rejectPendingExcept(long gigId, long acceptedApplicationId) {
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE GigApplications SET status = 'REJECTED' "
                            + "WHERE gig_id = ? AND status = 'PENDING' AND application_id <> ?")) {
                ps.setLong(1, gigId);
                ps.setLong(2, acceptedApplicationId);
                ps.executeUpdate();
                return null;
            }
        });
    }

    private static Optional<GigApplication> first(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    private static List<GigApplication> all(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<GigApplication> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    private static GigApplication map(ResultSet rs) throws SQLException {
        return new GigApplication(rs.getLong("application_id"), rs.getLong("gig_id"), rs.getLong("freelancer_id"),
                rs.getString("proposal_message"), ApplicationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("submitted_at").toLocalDateTime());
    }
}
