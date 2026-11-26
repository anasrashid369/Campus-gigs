package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.DisputeDao;
import com.campusgigs.dao.hibernate.entity.DisputeEntity;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link DisputeDao}. */
public class DisputeDaoHibernate extends BaseHibernateDao implements DisputeDao {

    public DisputeDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Dispute insert(long gigId, long raisedBy, String reason) {
        return withSession(session -> {
            DisputeEntity dispute = new DisputeEntity(gigId, raisedBy, reason);
            session.insert(dispute);
            return dispute.toModel();
        });
    }

    @Override
    public Optional<Dispute> findById(long disputeId) {
        return withSession(session -> Optional.ofNullable(session.get(DisputeEntity.class, disputeId))
                .map(DisputeEntity::toModel));
    }

    @Override
    public Optional<Dispute> findByIdForUpdate(long disputeId) {
        return withSession(session -> Optional
                .ofNullable(session.get(DisputeEntity.class, disputeId, LockMode.PESSIMISTIC_WRITE))
                .map(DisputeEntity::toModel));
    }

    @Override
    public Optional<Dispute> findOpenByGigId(long gigId) {
        return withSession(session -> session
                .createSelectionQuery("from DisputeEntity where gigId = :gigId and status = :open", DisputeEntity.class)
                .setParameter("gigId", gigId)
                .setParameter("open", DisputeStatus.OPEN)
                .uniqueResultOptional()
                .map(DisputeEntity::toModel));
    }

    @Override
    public List<Dispute> findByStatus(DisputeStatus status) {
        return withSession(session -> session
                .createSelectionQuery("from DisputeEntity where status = :status order by disputeId desc",
                        DisputeEntity.class)
                .setParameter("status", status)
                .getResultList().stream().map(DisputeEntity::toModel).toList());
    }

    @Override
    public boolean resolve(long disputeId, long adminId, DisputeDecision decision) {
        // The status check in the same statement makes sure a dispute is resolved only once.
        return withSession(session -> session
                .createMutationQuery("update DisputeEntity set status = :resolved, decision = :decision, "
                        + "resolvedBy = :adminId, resolvedAt = current_timestamp "
                        + "where disputeId = :id and status = :open")
                .setParameter("resolved", DisputeStatus.RESOLVED)
                .setParameter("decision", decision)
                .setParameter("adminId", adminId)
                .setParameter("id", disputeId)
                .setParameter("open", DisputeStatus.OPEN)
                .executeUpdate() == 1);
    }
}
