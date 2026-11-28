package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.ReviewDto;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.RatingFormat;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextFormat;
import com.campusgigs.service.ReviewService;
import com.campusgigs.util.SessionManager;
import java.util.List;

/** The reviews the freelancer received from clients (F19), with their average rating. */
public class ReviewsPanel extends TablePanel<ReviewDto> {

    private static final String TITLE = "My reviews";
    private static final int COMMENT_CHARS = 70;
    private static final String NO_COMMENT = "-";

    private final ReviewService reviewService;

    public ReviewsPanel(ReviewService reviewService) {
        super(TITLE, List.of("Gig", "Client", "Rating", "Comment"), ReviewsPanel::toCells);
        this.reviewService = reviewService;
        addAction("Read comment", true, this::readComment);
        addAction("Refresh", false, this::load);
        load();
    }

    /** The cell values of one review, in column order. */
    static Object[] toCells(ReviewDto review) {
        return new Object[] {
                review.gigTitle(),
                review.clientName(),
                RatingFormat.stars(review.rating()),
                review.comment() == null ? NO_COMMENT : TextFormat.oneLine(review.comment(), COMMENT_CHARS)
        };
    }

    /** The heading with the average, for example "My reviews: 4.5 ★ (2 reviews)". */
    static String title(RatingSummaryDto summary) {
        return TITLE + ": " + RatingFormat.summary(summary);
    }

    private record Loaded(List<ReviewDto> reviews, RatingSummaryDto summary) {
    }

    private void load() {
        long freelancerId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> new Loaded(reviewService.getReviewsForFreelancer(freelancerId),
                reviewService.getRatingSummary(freelancerId)), loaded -> {
            setTitle(title(loaded.summary()));
            setRows(loaded.reviews());
        });
    }

    private void readComment() {
        selected().ifPresent(review -> Dialogs.showText(this, "Review of \"" + review.gigTitle() + "\"",
                review.comment() == null ? "The client left no comment." : review.comment(),
                review.clientName() + " rated you " + RatingFormat.stars(review.rating())));
    }
}
