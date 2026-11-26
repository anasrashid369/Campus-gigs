package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.hibernate.entity.ApplicationEntity;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link ApplicationDao}. */
public class ApplicationDaoHibernate extends BaseHibernateDao implements ApplicationDao {

    private static final String NEWEST_FIRST = " order by submittedAt desc, applicationId desc";

    public ApplicationDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public long insert(long gigId, long freelancerId, String proposalMessage) {
        return withSession(session -> {
            ApplicationEntity application = new ApplicationEntity(gigId, freelancerId, proposalMessage);
            session.insert(application);
            return application.getApplicationId();
        });
    }

    @Override
    public Optional<GigApplication> findById(long applicationId) {
        return withSession(session -> Optional.ofNullable(session.get(ApplicationEntity.class, applicationId))
                .map(ApplicationEntity::toModel));
    }

    @Override
    public Optional<GigApplication> findByGigAndFreelancer(long gigId, long freelancerId) {
        return withSession(session -> session
                .createSelectionQuery("from ApplicationEntity where gigId = :gigId and freelancerId = :freelancerId",
                        ApplicationEntity.class)
                .setParameter("gigId", gigId)
                .setParameter("freelancerId", freelancerId)
                .uniqueResultOptional()
                .map(ApplicationEntity::toModel));
    }

    @Override
    public List<GigApplication> findByGigId(long gigId) {
        return withSession(session -> session
                .createSelectionQuery("from ApplicationEntity where gigId = :gigId "
                        + "order by submittedAt, applicationId", ApplicationEntity.class)
                .setParameter("gigId", gigId)
                .getResultList().stream().map(ApplicationEntity::toModel).toList());
    }

    @Override
    public List<GigApplication> findByFreelancerId(long freelancerId) {
        return withSession(session -> session
                .createSelectionQuery("from ApplicationEntity where freelancerId = :freelancerId" + NEWEST_FIRST,
                        ApplicationEntity.class)
                .setParameter("freelancerId", freelancerId)
                .getResultList().stream().map(ApplicationEntity::toModel).toList());
    }

    @Override
    public long countByFreelancerAndStatus(long freelancerId, ApplicationStatus status) {
        return withSession(session -> session
                .createSelectionQuery("select count(*) from ApplicationEntity "
                        + "where freelancerId = :freelancerId and status = :status", Long.class)
                .setParameter("freelancerId", freelancerId)
                .setParameter("status", status)
                .getSingleResult());
    }

    @Override
    public void updateStatus(long applicationId, ApplicationStatus status) {
        inSession(session -> session
                .createMutationQuery("update ApplicationEntity set status = :status where applicationId = :id")
                .setParameter("status", status)
                .setParameter("id", applicationId)
                .executeUpdate());
    }

    @Override
    public void reopen(long applicationId, String proposalMessage) {
        inSession(session -> session
                .createMutationQuery("update ApplicationEntity set status = :pending, proposalMessage = :message, "
                        + "submittedAt = current_timestamp where applicationId = :id")
                .setParameter("pending", ApplicationStatus.PENDING)
                .setParameter("message", proposalMessage)
                .setParameter("id", applicationId)
                .executeUpdate());
    }

    @Override
    public void rejectPendingExcept(long gigId, long acceptedApplicationId) {
        inSession(session -> session
                .createMutationQuery("update ApplicationEntity set status = :rejected "
                        + "where gigId = :gigId and status = :pending and applicationId <> :accepted")
                .setParameter("rejected", ApplicationStatus.REJECTED)
                .setParameter("gigId", gigId)
                .setParameter("pending", ApplicationStatus.PENDING)
                .setParameter("accepted", acceptedApplicationId)
                .executeUpdate());
    }

    @Override
    public List<GigApplication> findAll(ApplicationStatus statusOrNull) {
        return withSession(session -> {
            if (statusOrNull == null) {
                return session.createSelectionQuery("from ApplicationEntity" + NEWEST_FIRST, ApplicationEntity.class)
                        .getResultList().stream().map(ApplicationEntity::toModel).toList();
            }
            return session.createSelectionQuery("from ApplicationEntity where status = :status" + NEWEST_FIRST,
                            ApplicationEntity.class)
                    .setParameter("status", statusOrNull)
                    .getResultList().stream().map(ApplicationEntity::toModel).toList();
        });
    }
}
