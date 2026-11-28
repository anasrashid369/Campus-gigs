package com.campusgigs.service.impl;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.ReviewDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.ReviewDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.RatingStats;
import com.campusgigs.model.Review;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.ReviewPosted;
import com.campusgigs.service.ReviewService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Default {@link ReviewService}. */
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDao reviewDao;
    private final GigDao gigDao;
    private final UserDao userDao;
    private final TransactionManager transactions;
    private final EventBus eventBus;

    public ReviewServiceImpl(ReviewDao reviewDao, GigDao gigDao, UserDao userDao, TransactionManager transactions,
                             EventBus eventBus) {
        this.reviewDao = Objects.requireNonNull(reviewDao, "reviewDao");
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    /** The saved review and the event to announce once it is committed. */
    private record Outcome(ReviewDto review, ReviewPosted event) {
    }

    @Override
    public ReviewDto postReview(long clientId, long gigId, int rating, String comment) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new ValidationException("Choose a rating from " + MIN_RATING + " to " + MAX_RATING + " stars");
        }
        String text = comment == null ? "" : comment.trim();
        if (text.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("The comment can be at most " + MAX_COMMENT_LENGTH + " characters");
        }
        String savedComment = text.isEmpty() ? null : text;
        Outcome outcome = transactions.call(() -> {
            // Locking the gig makes a second, simultaneous review of the same gig wait and then fail the check.
            Gig gig = gigDao.findByIdForUpdate(gigId)
                    .orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
            User client = findUser(clientId);
            if (client.role() != Role.CLIENT || gig.clientId() != clientId) {
                throw new ValidationException("Only the client who posted the gig can review it");
            }
            if (gig.status() != GigStatus.APPROVED || gig.assignedFreelancerId() == null) {
                throw new ValidationException("You can review a gig only after you approved the work");
            }
            if (reviewDao.findByGigId(gigId).isPresent()) {
                throw new ValidationException("You have already reviewed this gig");
            }
            long freelancerId = gig.assignedFreelancerId();
            long reviewId = reviewDao.insert(gigId, clientId, freelancerId, rating, savedComment);
            return new Outcome(new ReviewDto(reviewId, gigId, gig.title(), client.fullName(), freelancerId, rating,
                    savedComment), new ReviewPosted(reviewId, gigId, clientId, freelancerId, rating));
        });
        eventBus.publish(outcome.event());
        return outcome.review();
    }

    @Override
    public List<ReviewDto> getReviewsForFreelancer(long freelancerId) {
        return reviewDao.findByFreelancerId(freelancerId).stream().map(this::toDto).toList();
    }

    @Override
    public RatingSummaryDto getRatingSummary(long freelancerId) {
        return getRatingSummaries(List.of(freelancerId)).get(freelancerId);
    }

    @Override
    public Map<Long, RatingSummaryDto> getRatingSummaries(Collection<Long> freelancerIds) {
        Set<Long> ids = new LinkedHashSet<>(freelancerIds);
        Map<Long, RatingSummaryDto> summaries = new LinkedHashMap<>();
        for (long id : ids) {
            summaries.put(id, RatingSummaryDto.none(id));
        }
        for (RatingStats stats : reviewDao.findRatingStats(ids)) {
            summaries.put(stats.freelancerId(),
                    new RatingSummaryDto(stats.freelancerId(), stats.averageRating(), stats.reviewCount()));
        }
        return summaries;
    }

    @Override
    public Set<Long> getReviewedGigIds(long clientId) {
        return Set.copyOf(reviewDao.findGigIdsReviewedByClient(clientId));
    }

    private User findUser(long userId) {
        return userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewDto toDto(Review review) {
        String gigTitle = gigDao.findById(review.gigId()).map(Gig::title).orElse("Gig #" + review.gigId());
        String clientName = userDao.findById(review.clientId()).map(User::fullName).orElse("A client");
        return new ReviewDto(review.reviewId(), review.gigId(), gigTitle, clientName, review.freelancerId(),
                review.rating(), review.comment());
    }
}
