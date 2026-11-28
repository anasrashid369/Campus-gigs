package com.campusgigs.presentation.client;

import com.campusgigs.dto.GigDto;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.RatingFormat;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.ReviewService;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Asks the client for a rating (1 to 5 stars) and an optional comment for a completed gig.
 * The comment length is checked here for convenience; {@link ReviewService} checks everything again.
 */
final class ReviewDialog {

    /** What the client entered. */
    record Answer(int rating, String comment) {
    }

    private static final String[] WORDS = {"", "Poor", "Fair", "Good", "Very good", "Excellent"};

    private ReviewDialog() {
    }

    /** Shows the dialog; empty when the client cancelled. */
    static Optional<Answer> ask(Node owner, GigDto gig) {
        ComboBox<Integer> ratingBox = new ComboBox<>();
        for (int rating = ReviewService.MAX_RATING; rating >= ReviewService.MIN_RATING; rating--) {
            ratingBox.getItems().add(rating);
        }
        ratingBox.setValue(ReviewService.MAX_RATING);
        FormLayout.showItemsAs(ratingBox, ReviewDialog::label);
        ratingBox.setMaxWidth(Double.MAX_VALUE);

        TextArea commentArea = new TextArea();
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(5);
        commentArea.setPromptText("What went well? What could be better? (optional)");
        Label problem = FormLayout.messageLabel();

        Label intro = new Label("How did the freelancer do on \"" + gig.title() + "\"?");
        intro.setWrapText(true);
        VBox content = new VBox(UiConstants.SPACING_MEDIUM, intro, ratingBox,
                new Label("Comment (up to " + ReviewService.MAX_COMMENT_LENGTH + " characters)"), commentArea,
                problem);

        Dialog<Answer> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(content);
        ButtonType post = new ButtonType("Post review", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(post, ButtonType.CANCEL);
        Button postButton = (Button) dialog.getDialogPane().lookupButton(post);
        postButton.getStyleClass().add("primary");
        postButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (commentArea.getText().trim().length() > ReviewService.MAX_COMMENT_LENGTH) {
                FormLayout.showError(problem,
                        "The comment can be at most " + ReviewService.MAX_COMMENT_LENGTH + " characters.");
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == post
                ? new Answer(ratingBox.getValue(), commentArea.getText())
                : null);
        Dialogs.prepare(dialog, owner, "Review the freelancer");
        return dialog.showAndWait();
    }

    /** The text of a rating in the list, for example "★★★★☆  Very good". */
    static String label(int rating) {
        return RatingFormat.stars(rating) + "  " + WORDS[rating];
    }
}
