package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.ReviewDao;
import com.campusgigs.dao.hibernate.entity.ReviewEntity;
import com.campusgigs.model.RatingStats;
import com.campusgigs.model.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;

/** Hibernate implementation of {@link ReviewDao}. */
public class ReviewDaoHibernate extends BaseHibernateDao implements ReviewDao {

    public ReviewDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public long insert(long gigId, long clientId, long freelancerId, int rating, String comment) {
        return withSession(session -> {
            ReviewEntity review = new ReviewEntity(gigId, clientId, freelancerId, rating, comment);
            session.insert(review);
            return review.getReviewId();
        });
    }

    @Override
    public Optional<Review> findByGigId(long gigId) {
        return withSession(session -> session
                .createSelectionQuery("from ReviewEntity where gigId = :gigId", ReviewEntity.class)
                .setParameter("gigId", gigId)
                .uniqueResultOptional()
                .map(ReviewEntity::toModel));
    }

    @Override
    public List<Review> findByFreelancerId(long freelancerId) {
        return withSession(session -> session
                .createSelectionQuery("from ReviewEntity where freelancerId = :freelancerId order by reviewId desc",
                        ReviewEntity.class)
                .setParameter("freelancerId", freelancerId)
                .getResultList().stream().map(ReviewEntity::toModel).toList());
    }

    @Override
    public List<Long> findGigIdsReviewedByClient(long clientId) {
        return withSession(session -> session
                .createSelectionQuery("select gigId from ReviewEntity where clientId = :clientId", Long.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    @Override
    public List<RatingStats> findRatingStats(Collection<Long> freelancerIds) {
        if (freelancerIds.isEmpty()) {
            return List.of();
        }
        return withSession(session -> session
                .createSelectionQuery("select freelancerId, avg(rating), count(*) from ReviewEntity "
                        + "where freelancerId in :ids group by freelancerId", Object[].class)
                .setParameterList("ids", freelancerIds)
                .getResultList().stream()
                .map(row -> new RatingStats(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).intValue()))
                .toList());
    }
}
