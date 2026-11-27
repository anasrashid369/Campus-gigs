package com.campusgigs.presentation.client;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigRequest;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.GigService;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Form to post a new gig (F14) or to change an open one (F15). Numbers and dates are read on
 * screen for convenience; every rule (lengths, budget range, deadline, category, skills,
 * ownership) is checked again by {@link GigService}.
 */
public class GigFormPanel extends BasePanel {

    /** Everything the form needs from the services, loaded in one background call. */
    private record Loaded(List<CategoryDto> categories, List<SkillDto> skills, List<SkillDto> chosenSkills) {
    }

    private final GigService gigService;
    private final ProfileService profileService;
    private final GigDto existing;
    private final Runnable onSaved;

    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    private final TextField budgetField = new TextField();
    private final DatePicker deadlinePicker = FormLayout.isoDatePicker();
    private final ListView<SkillDto> skillsList = new ListView<>();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Button saveButton = new Button("Post gig");

    /** A form for posting a new gig. */
    public GigFormPanel(GigService gigService, ProfileService profileService) {
        this(gigService, profileService, null, () -> { });
    }

    /**
     * A form for changing an open gig.
     *
     * @param existing the gig to change, or null to post a new one
     * @param onSaved  called after a successful save, for example to close the window the form is shown in
     */
    public GigFormPanel(GigService gigService, ProfileService profileService, GigDto existing, Runnable onSaved) {
        this.gigService = gigService;
        this.profileService = profileService;
        this.existing = existing;
        this.onSaved = onSaved;

        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(5);
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        FormLayout.showItemsAs(categoryBox, CategoryDto::name);
        deadlinePicker.setMaxWidth(Double.MAX_VALUE);
        skillsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        skillsList.setPrefHeight(150);

        VBox form = FormLayout.createForm(existing == null ? "Post a gig" : "Change gig");
        form.setPrefWidth(UiConstants.WIDE_FORM_WIDTH);
        FormLayout.addField(form, "Title (3 - 100 characters)", titleField, null);
        FormLayout.addField(form, "Description (10 - 2000 characters)", descriptionArea, null);
        FormLayout.addField(form, "Category", categoryBox, null);
        FormLayout.addField(form, "Budget in tokens (1 - 100000)", budgetField, null);
        FormLayout.addField(form, "Deadline (pick a day or type yyyy-MM-dd)", deadlinePicker, null);
        FormLayout.addField(form, "Skills needed (up to 10, Ctrl+click to choose several)", skillsList, null);
        FormLayout.addRow(form, messageLabel);
        saveButton.getStyleClass().add("primary");
        FormLayout.addRow(form, saveButton);
        setCenteredContent(form);

        if (existing != null) {
            saveButton.setText("Save changes");
            titleField.setText(existing.title());
            descriptionArea.setText(existing.description());
            budgetField.setText(String.valueOf(existing.budget()));
            deadlinePicker.setValue(existing.deadline());
        }
        saveButton.setOnAction(event -> save());
        load();
    }

    /**
     * Turns what the client typed into a request.
     *
     * @throws ValidationException with a message for the user if a number, the date or the category cannot be read
     */
    static GigRequest parseRequest(String title, String description, CategoryDto category, String budgetText,
                                   String deadlineText, List<SkillDto> skills) {
        if (category == null) {
            throw new ValidationException("Please choose a category.");
        }
        long budget;
        try {
            budget = Long.parseLong(budgetText.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Enter the budget as a whole number of tokens.");
        }
        LocalDate deadline;
        try {
            deadline = LocalDate.parse(deadlineText.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Enter the deadline as yyyy-MM-dd, for example 2026-12-31.");
        }
        return new GigRequest(title, description, category.categoryId(), budget, deadline,
                skills.stream().map(SkillDto::skillId).toList());
    }

    private void load() {
        saveButton.setDisable(true);
        runInBackground(() -> new Loaded(gigService.getActiveCategories(), profileService.getAvailableSkills(),
                existing == null ? List.of() : gigService.getGigSkills(existing.gigId())), loaded -> {
            categoryBox.getItems().setAll(loaded.categories());
            skillsList.getItems().setAll(loaded.skills());
            if (existing != null) {
                selectCategory(existing.categoryId());
                selectSkills(loaded.chosenSkills());
            } else {
                categoryBox.getSelectionModel().selectFirst();
            }
            saveButton.setDisable(false);
        });
    }

    private void selectCategory(long categoryId) {
        categoryBox.getItems().stream()
                .filter(category -> category.categoryId() == categoryId)
                .findFirst()
                .ifPresent(categoryBox::setValue);
    }

    private void selectSkills(List<SkillDto> chosen) {
        Set<Long> ids = chosen.stream().map(SkillDto::skillId).collect(Collectors.toSet());
        skillsList.getSelectionModel().clearSelection();
        for (SkillDto skill : skillsList.getItems()) {
            if (ids.contains(skill.skillId())) {
                skillsList.getSelectionModel().select(skill);
            }
        }
    }

    private void save() {
        GigRequest request;
        try {
            request = parseRequest(titleField.getText(), descriptionArea.getText(), categoryBox.getValue(),
                    budgetField.getText(), deadlinePicker.getEditor().getText(),
                    List.copyOf(skillsList.getSelectionModel().getSelectedItems()));
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        long clientId = SessionManager.getInstance().getCurrentUser().userId();
        saveButton.setDisable(true);
        runInBackground(() -> existing == null
                        ? gigService.createGig(clientId, request)
                        : gigService.updateGig(clientId, existing.gigId(), request),
                saved -> {
                    saveButton.setDisable(false);
                    if (existing == null) {
                        clearForm();
                        FormLayout.showSuccess(messageLabel, "Gig posted. Find it under My Gigs.");
                    } else {
                        FormLayout.showSuccess(messageLabel, "Gig saved");
                    }
                    onSaved.run();
                }, error -> {
                    saveButton.setDisable(false);
                    FormLayout.showFailure(this, messageLabel, error);
                });
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        budgetField.clear();
        deadlinePicker.setValue(null);
        deadlinePicker.getEditor().clear();
        skillsList.getSelectionModel().clearSelection();
        if (!categoryBox.getItems().isEmpty()) {
            categoryBox.getSelectionModel().selectFirst();
        }
    }
}
