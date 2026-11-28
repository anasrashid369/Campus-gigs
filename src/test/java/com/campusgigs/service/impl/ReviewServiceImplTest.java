package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.ReviewDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.ReviewDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.RatingStats;
import com.campusgigs.model.Review;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.ReviewPosted;
import com.campusgigs.service.ReviewService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final long CLIENT = 1;
    private static final long FREELANCER = 2;
    private static final long OTHER_CLIENT = 3;
    private static final long GIG = 10;

    @Mock ReviewDao reviewDao;
    @Mock GigDao gigDao;
    @Mock UserDao userDao;

    private final List<ReviewPosted> events = new ArrayList<>();
    private ReviewServiceImpl service;

    private final TransactionManager passThrough = new TransactionManager() {
        @Override
        public <T> T call(Supplier<T> work) {
            return work.get();
        }

        @Override
        public void run(Runnable work) {
            work.run();
        }
    };

    @BeforeEach
    void setUp() {
        EventBus bus = new EventBus();
        bus.subscribe(ReviewPosted.class, events::add);
        service = new ReviewServiceImpl(reviewDao, gigDao, userDao, passThrough, bus);
        lenient().when(userDao.findById(CLIENT)).thenReturn(Optional.of(user(CLIENT, Role.CLIENT)));
        lenient().when(userDao.findById(OTHER_CLIENT)).thenReturn(Optional.of(user(OTHER_CLIENT, Role.CLIENT)));
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER)));
    }

    private static User user(long id, Role role) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", AccountStatus.ACTIVE,
                LocalDateTime.now());
    }

    private static Gig gig(GigStatus status, Long freelancerId) {
        return new Gig(GIG, CLIENT, 7, freelancerId, "Logo design", "Description", 900, LocalDate.of(2026, 12, 31),
                status, LocalDateTime.now());
    }

    private void gigIs(GigStatus status) {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(gig(status, FREELANCER)));
    }

    @Test
    void theClientReviewsAnApprovedGigAndTheFreelancerIsAnnounced() {
        gigIs(GigStatus.APPROVED);
        when(reviewDao.findByGigId(GIG)).thenReturn(Optional.empty());
        when(reviewDao.insert(GIG, CLIENT, FREELANCER, 5, "Great work")).thenReturn(44L);

        ReviewDto review = service.postReview(CLIENT, GIG, 5, "  Great work  ");

        assertEquals(new ReviewDto(44, GIG, "Logo design", "User 1", FREELANCER, 5, "Great work"), review);
        assertEquals(List.of(new ReviewPosted(44, GIG, CLIENT, FREELANCER, 5)), events);
    }

    @Test
    void aBlankCommentIsSavedAsNoComment() {
        gigIs(GigStatus.APPROVED);
        when(reviewDao.findByGigId(GIG)).thenReturn(Optional.empty());
        when(reviewDao.insert(GIG, CLIENT, FREELANCER, 3, null)).thenReturn(45L);

        assertNull(service.postReview(CLIENT, GIG, 3, "   ").comment());
        assertNull(service.postReview(CLIENT, GIG, 3, null).comment());
    }

    @Test
    void ratingsOutsideOneToFiveAreRejectedBeforeTouchingTheDatabase() {
        for (int rating : new int[] {0, 6, -1}) {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.postReview(CLIENT, GIG, rating, "ok"));
            assertEquals("Choose a rating from 1 to 5 stars", error.getMessage());
        }
        verifyNoInteractions(gigDao, reviewDao);
    }

    @Test
    void aTooLongCommentIsRejected() {
        String comment = "x".repeat(ReviewService.MAX_COMMENT_LENGTH + 1);

        assertThrows(ValidationException.class, () -> service.postReview(CLIENT, GIG, 4, comment));
        verifyNoInteractions(gigDao, reviewDao);
    }

    @Test
    void onlyTheClientWhoPostedTheGigCanReviewIt() {
        gigIs(GigStatus.APPROVED);

        ValidationException other = assertThrows(ValidationException.class,
                () -> service.postReview(OTHER_CLIENT, GIG, 4, null));
        ValidationException freelancer = assertThrows(ValidationException.class,
                () -> service.postReview(FREELANCER, GIG, 4, null));

        assertEquals("Only the client who posted the gig can review it", other.getMessage());
        assertEquals("Only the client who posted the gig can review it", freelancer.getMessage());
        verify(reviewDao, never()).insert(anyLong(), anyLong(), anyLong(), anyInt(), any());
        assertTrue(events.isEmpty());
    }

    @Test
    void aGigCanBeReviewedOnlyAfterTheWorkWasApproved() {
        for (GigStatus status : new GigStatus[] {GigStatus.OPEN, GigStatus.ASSIGNED, GigStatus.SUBMITTED,
                GigStatus.DISPUTED, GigStatus.RESOLVED, GigStatus.CANCELLED}) {
            gigIs(status);
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.postReview(CLIENT, GIG, 4, null));
            assertEquals("You can review a gig only after you approved the work", error.getMessage());
        }
        verify(reviewDao, never()).insert(anyLong(), anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void aGigCanBeReviewedOnlyOnce() {
        gigIs(GigStatus.APPROVED);
        when(reviewDao.findByGigId(GIG)).thenReturn(Optional.of(new Review(44, GIG, CLIENT, FREELANCER, 5, null)));

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.postReview(CLIENT, GIG, 2, "Changed my mind"));

        assertEquals("You have already reviewed this gig", error.getMessage());
        verify(reviewDao, never()).insert(anyLong(), anyLong(), anyLong(), anyInt(), any());
        assertTrue(events.isEmpty());
    }

    @Test
    void aMissingGigIsReported() {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.postReview(CLIENT, GIG, 4, null));
    }

    @Test
    void listsTheFreelancersReviewsWithGigTitleAndClientName() {
        when(reviewDao.findByFreelancerId(FREELANCER)).thenReturn(List.of(
                new Review(44, GIG, CLIENT, FREELANCER, 5, "Great"),
                new Review(43, 99, OTHER_CLIENT, FREELANCER, 3, null)));
        when(gigDao.findById(GIG)).thenReturn(Optional.of(gig(GigStatus.APPROVED, FREELANCER)));
        when(gigDao.findById(99)).thenReturn(Optional.empty());

        List<ReviewDto> reviews = service.getReviewsForFreelancer(FREELANCER);

        assertEquals(new ReviewDto(44, GIG, "Logo design", "User 1", FREELANCER, 5, "Great"), reviews.get(0));
        assertEquals(new ReviewDto(43, 99, "Gig #99", "User 3", FREELANCER, 3, null), reviews.get(1));
    }

    @Test
    void summariesCoverEveryFreelancerAskedFor() {
        when(reviewDao.findRatingStats(Set.of(FREELANCER, 8L))).thenReturn(List.of(new RatingStats(FREELANCER, 4.5, 2)));

        Map<Long, RatingSummaryDto> summaries = service.getRatingSummaries(List.of(FREELANCER, 8L, FREELANCER));

        assertEquals(2, summaries.size());
        assertEquals(new RatingSummaryDto(FREELANCER, 4.5, 2), summaries.get(FREELANCER));
        assertFalse(summaries.get(8L).hasReviews());
    }

    @Test
    void aFreelancerWithoutReviewsHasAnEmptySummary() {
        when(reviewDao.findRatingStats(Set.of(FREELANCER))).thenReturn(List.of());

        assertEquals(RatingSummaryDto.none(FREELANCER), service.getRatingSummary(FREELANCER));
    }

    @Test
    void knowsWhichGigsTheClientAlreadyReviewed() {
        when(reviewDao.findGigIdsReviewedByClient(CLIENT)).thenReturn(List.of(GIG, 12L));

        assertEquals(Set.of(GIG, 12L), service.getReviewedGigIds(CLIENT));
    }
}
