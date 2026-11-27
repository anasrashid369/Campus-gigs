package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;
import com.campusgigs.dto.GigSort;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextPrompt;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigSearchService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Find Gigs (F22, F23): search the open gigs by word, category and budget, order them, read the
 * details and apply. The filter is read on screen; the rules (who may search, budget limits) are
 * checked by {@link GigSearchService}, and the application rules by {@link ApplicationService}.
 */
public class BrowseGigsPanel extends BasePanel {

    private static final CategoryDto ALL_CATEGORIES = new CategoryDto(0, "All categories", true);

    private final GigSearchService searchService;
    private final GigService gigService;
    private final ApplicationService applicationService;

    private final TextField keywordField = new TextField();
    private final ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    private final TextField minField = new TextField();
    private final TextField maxField = new TextField();
    private final ComboBox<GigSort> sortBox = new ComboBox<>();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Results results;

    public BrowseGigsPanel(GigSearchService searchService, GigService gigService,
                           ApplicationService applicationService) {
        this.searchService = searchService;
        this.gigService = gigService;
        this.applicationService = applicationService;

        categoryBox.getItems().add(ALL_CATEGORIES);
        categoryBox.getSelectionModel().selectFirst();
        FormLayout.showItemsAs(categoryBox, CategoryDto::name);
        sortBox.getItems().setAll(GigSort.values());
        sortBox.setValue(GigSort.NEWEST);

        results = new Results(this::viewDetails, this::apply, this::search);
        VBox top = new VBox(UiConstants.SPACING_SMALL, createFilterBar(), messageLabel);
        setMargin(top, new Insets(0, 0, UiConstants.SPACING_LARGE, 0));
        setTop(top);
        setCenter(results);
        loadCategories();
        search();
    }

    private FlowPane createFilterBar() {
        keywordField.setPromptText("Search by title or description");
        keywordField.setPrefColumnCount(18);
        keywordField.setOnAction(event -> search());
        minField.setPromptText("Min");
        minField.setPrefColumnCount(5);
        maxField.setPromptText("Max");
        maxField.setPrefColumnCount(5);
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("primary");
        searchButton.setOnAction(event -> search());
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> clear());
        FlowPane bar = new FlowPane(UiConstants.SPACING_MEDIUM, UiConstants.SPACING_MEDIUM, keywordField,
                categoryBox, new Label("Budget"), minField, new Label("to"), maxField, sortBox, searchButton,
                clearButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    /**
     * Turns what the freelancer typed into a filter.
     *
     * @throws ValidationException with a message for the user if a budget limit is not a whole number
     */
    static GigFilter parseFilter(String keyword, CategoryDto category, String minText, String maxText, GigSort sort) {
        Long categoryId = category == null || category.categoryId() == ALL_CATEGORIES.categoryId()
                ? null : category.categoryId();
        return new GigFilter(keyword, categoryId, parseLimit(minText), parseLimit(maxText), sort);
    }

    private static Long parseLimit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Enter the budget limits as whole numbers of tokens.");
        }
    }

    private void loadCategories() {
        runInBackground(gigService::getActiveCategories, categories -> categoryBox.getItems().addAll(categories));
    }

    private void search() {
        GigFilter filter;
        try {
            filter = parseFilter(keywordField.getText(), categoryBox.getValue(), minField.getText(),
                    maxField.getText(), sortBox.getValue());
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        long freelancerId = currentUserId();
        runInBackground(() -> searchService.search(freelancerId, filter), found -> {
            results.show(found);
            FormLayout.showInfo(messageLabel, found.size() + (found.size() == 1 ? " gig found" : " gigs found"));
        }, error -> FormLayout.showFailure(this, messageLabel, error));
    }

    private void clear() {
        keywordField.clear();
        categoryBox.getSelectionModel().selectFirst();
        minField.clear();
        maxField.clear();
        sortBox.setValue(GigSort.NEWEST);
        search();
    }

    private void viewDetails() {
        results.selectedGig().ifPresent(gig ->
                runInBackground(() -> gigService.getGigSkills(gig.gigId()), skills -> showDetails(gig, skills)));
    }

    private void showDetails(GigDto gig, List<SkillDto> skills) {
        Dialogs.showText(this, gig.title(), gig.description(),
                "Budget: " + TokenFormat.tokens(gig.budget()) + " tokens    Deadline: " + gig.deadline(),
                "Skills: " + (skills.isEmpty() ? "none asked"
                        : skills.stream().map(SkillDto::name).collect(Collectors.joining(", "))));
    }

    private void apply() {
        results.selectedGig().ifPresent(gig -> {
            Optional<String> proposal = TextPrompt.ask(this, "Apply for \"" + gig.title() + "\"",
                    "Why are you a good fit? (" + ApplicationService.MIN_PROPOSAL_LENGTH + " - "
                            + ApplicationService.MAX_PROPOSAL_LENGTH + " characters)",
                    ApplicationService.MIN_PROPOSAL_LENGTH, ApplicationService.MAX_PROPOSAL_LENGTH);
            proposal.ifPresent(text -> {
                long freelancerId = currentUserId();
                runInBackground(() -> applicationService.apply(freelancerId, gig.gigId(), text), sent -> {
                    Dialogs.info(this, "Application sent", "Your application was sent.");
                    search();
                });
            });
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** The table of results with its buttons. */
    private static final class Results extends TablePanel<GigDto> {

        Results(Runnable viewDetails, Runnable apply, Runnable refresh) {
            super("Open gigs", List.of("Title", "Budget", "Deadline"), BrowseGigsPanel::toCells);
            addAction("View details", true, viewDetails);
            addAction("Apply", true, apply).getStyleClass().add("primary");
            addAction("Refresh", false, refresh);
        }

        void show(List<GigDto> gigs) {
            setRows(gigs);
        }

        Optional<GigDto> selectedGig() {
            return selected();
        }
    }

    /** The cell values of one gig, in column order. */
    static Object[] toCells(GigDto gig) {
        return new Object[] {gig.title(), TokenFormat.tokens(gig.budget()), gig.deadline()};
    }
}
