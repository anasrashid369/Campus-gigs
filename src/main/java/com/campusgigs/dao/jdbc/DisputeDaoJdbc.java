package com.campusgigs.dao.jdbc;

import com.campusgigs.dao.DisputeDao;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link DisputeDao}. */
public class DisputeDaoJdbc extends BaseJdbcDao implements DisputeDao {

    private static final String SELECT = "SELECT dispute_id, gig_id, raised_by, resolved_by, reason, "
            + "status, decision, resolved_at FROM Disputes";

    public DisputeDaoJdbc(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public Dispute insert(long gigId, long raisedBy, String reason) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Disputes (gig_id, raised_by, reason) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, gigId);
                ps.setLong(2, raisedBy);
                ps.setString(3, reason);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Dispute(keys.getLong(1), gigId, raisedBy, null, reason,
                            DisputeStatus.OPEN, null, null);
                }
            }
        });
    }

    @Override
    public Optional<Dispute> findById(long disputeId) {
        return findOne(SELECT + " WHERE dispute_id = ?", disputeId);
    }

    @Override
    public Optional<Dispute> findByIdForUpdate(long disputeId) {
        return findOne(SELECT + " WHERE dispute_id = ? FOR UPDATE", disputeId);
    }

    @Override
    public Optional<Dispute> findOpenByGigId(long gigId) {
        return findOne(SELECT + " WHERE gig_id = ? AND status = 'OPEN'", gigId);
    }

    @Override
    public List<Dispute> findByStatus(DisputeStatus status) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    SELECT + " WHERE status = ? ORDER BY dispute_id DESC")) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    List<Dispute> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(map(rs));
                    }
                    return result;
                }
            }
        });
    }

    @Override
    public boolean resolve(long disputeId, long adminId, DisputeDecision decision) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Disputes SET status = 'RESOLVED', decision = ?, resolved_by = ?, "
                            + "resolved_at = CURRENT_TIMESTAMP WHERE dispute_id = ? AND status = 'OPEN'")) {
                ps.setString(1, decision.name());
                ps.setLong(2, adminId);
                ps.setLong(3, disputeId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private Optional<Dispute> findOne(String sql, long id) {
        return withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    private static Dispute map(ResultSet rs) throws SQLException {
        long resolver = rs.getLong("resolved_by");
        Long resolvedBy = rs.wasNull() ? null : resolver;
        String decision = rs.getString("decision");
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        return new Dispute(rs.getLong("dispute_id"), rs.getLong("gig_id"), rs.getLong("raised_by"), resolvedBy,
                rs.getString("reason"), DisputeStatus.valueOf(rs.getString("status")),
                decision == null ? null : DisputeDecision.valueOf(decision),
                resolvedAt == null ? null : resolvedAt.toLocalDateTime());
    }
}
