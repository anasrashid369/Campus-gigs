package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.RatingStats;
import com.campusgigs.model.Review;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when it is not configured or not running). */
class ReviewDaoHibernateTest {

    private SessionFactory db;
    private ReviewDaoHibernate reviews;
    private long clientId;
    private long freelancerId;
    private long otherFreelancerId;
    private long categoryId;
    private long firstGig;
    private long secondGig;

    @BeforeEach
    void setUp() {
        db = DbTestSupport.sessionFactory();
        reviews = new ReviewDaoHibernate(db);
        clientId = DbTestSupport.createUser(db, "CLIENT");
        freelancerId = DbTestSupport.createUser(db, "FREELANCER");
        otherFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        firstGig = DbTestSupport.createGig(db, clientId, categoryId, 100);
        secondGig = DbTestSupport.createGig(db, clientId, categoryId, 200);
    }

    @AfterEach
    void tearDown() {
        if (db == null) {
            return;
        }
        DbTestSupport.deleteGig(db, firstGig);
        DbTestSupport.deleteGig(db, secondGig);
        DbTestSupport.deleteCategory(db, categoryId);
        DbTestSupport.deleteUser(db, clientId);
        DbTestSupport.deleteUser(db, freelancerId);
        DbTestSupport.deleteUser(db, otherFreelancerId);
    }

    @Test
    void savesAReviewAndFindsItByGig() {
        long id = reviews.insert(firstGig, clientId, freelancerId, 4, "Good work");

        assertEquals(new Review(id, firstGig, clientId, freelancerId, 4, "Good work"),
                reviews.findByGigId(firstGig).orElseThrow());
        assertTrue(reviews.findByGigId(secondGig).isEmpty());
    }

    @Test
    void aGigHasAtMostOneReview() {
        reviews.insert(firstGig, clientId, freelancerId, 4, null);

        assertThrows(DataAccessException.class, () -> reviews.insert(firstGig, clientId, freelancerId, 1, null));
    }

    @Test
    void listsAFreelancersReviewsNewestFirstAndTheGigsTheClientReviewed() {
        long first = reviews.insert(firstGig, clientId, freelancerId, 5, null);
        long second = reviews.insert(secondGig, clientId, freelancerId, 2, "Late");

        assertEquals(List.of(second, first),
                reviews.findByFreelancerId(freelancerId).stream().map(Review::reviewId).toList());
        assertEquals(List.of(firstGig, secondGig),
                reviews.findGigIdsReviewedByClient(clientId).stream().sorted().toList());
    }

    @Test
    void averagesTheRatingsPerFreelancer() {
        reviews.insert(firstGig, clientId, freelancerId, 5, null);
        reviews.insert(secondGig, clientId, freelancerId, 2, null);

        List<RatingStats> stats = reviews.findRatingStats(List.of(freelancerId, otherFreelancerId));

        assertEquals(List.of(new RatingStats(freelancerId, 3.5, 2)), stats);
        assertTrue(reviews.findRatingStats(List.of()).isEmpty());
    }
}
