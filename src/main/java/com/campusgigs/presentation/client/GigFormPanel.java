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

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Form to post a new gig (F14) or to change an open one (F15). Numbers and dates are read on
 * screen for convenience; every rule (lengths, budget range, deadline, category, skills,
 * ownership) is checked again by {@link GigService}.
 */
public class GigFormPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /** Everything the form needs from the services, loaded in one background call. */
    private record Loaded(List<CategoryDto> categories, List<SkillDto> skills, List<SkillDto> chosenSkills) {
    }

    private final transient GigService gigService;
    private final transient ProfileService profileService;
    private final transient GigDto existing;
    private final transient Runnable onSaved;

    private final JTextField titleField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS + 12);
    private final JTextArea descriptionArea = new JTextArea(6, UiConstants.TEXT_FIELD_COLUMNS + 12);
    private final JComboBox<CategoryDto> categoryBox = new JComboBox<>();
    private final JTextField budgetField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JTextField deadlineField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final DefaultListModel<SkillDto> skillsModel = new DefaultListModel<>();
    private final JList<SkillDto> skillsList = new JList<>(skillsModel);
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final JButton saveButton = new JButton("Post gig");

    /** A form for posting a new gig. */
    public GigFormPanel(GigService gigService, ProfileService profileService) {
        this(gigService, profileService, null, () -> { });
    }

    /**
     * A form for changing an open gig.
     *
     * @param existing the gig to change, or null to post a new one
     * @param onSaved  called after a successful save, for example to close the dialog the form is shown in
     */
    public GigFormPanel(GigService gigService, ProfileService profileService, GigDto existing, Runnable onSaved) {
        this.gigService = gigService;
        this.profileService = profileService;
        this.existing = existing;
        this.onSaved = onSaved;
        setLayout(new GridBagLayout());

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        skillsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        skillsList.setVisibleRowCount(6);
        categoryBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                  boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof CategoryDto category ? category.name() : value, index, selected, focus);
            }
        });

        JPanel form = FormLayout.createForm(existing == null ? "Post a gig" : "Change gig");
        FormLayout.addField(form, "Title (3 - 100 characters)", titleField, null);
        FormLayout.addField(form, "Description (10 - 2000 characters)", new JScrollPane(descriptionArea), null);
        FormLayout.addField(form, "Category", categoryBox, null);
        FormLayout.addField(form, "Budget in tokens (1 - 100000)", budgetField, null);
        FormLayout.addField(form, "Deadline (yyyy-MM-dd, for example 2026-12-31)", deadlineField, null);
        FormLayout.addField(form, "Skills needed (up to 10, Ctrl+click to choose several)",
                new JScrollPane(skillsList), null);
        FormLayout.addRow(form, messageLabel, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, saveButton, 0);
        add(form);

        if (existing != null) {
            saveButton.setText("Save changes");
            titleField.setText(existing.title());
            descriptionArea.setText(existing.description());
            budgetField.setText(String.valueOf(existing.budget()));
            deadlineField.setText(existing.deadline().toString());
        }
        saveButton.addActionListener(e -> save());
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
        saveButton.setEnabled(false);
        runInBackground(() -> new Loaded(gigService.getActiveCategories(), profileService.getAvailableSkills(),
                existing == null ? List.of() : gigService.getGigSkills(existing.gigId())), loaded -> {
            categoryBox.removeAllItems();
            loaded.categories().forEach(categoryBox::addItem);
            skillsModel.clear();
            loaded.skills().forEach(skillsModel::addElement);
            if (existing != null) {
                selectCategory(existing.categoryId());
                selectSkills(loaded.chosenSkills());
            }
            saveButton.setEnabled(true);
        });
    }

    private void selectCategory(long categoryId) {
        for (int i = 0; i < categoryBox.getItemCount(); i++) {
            if (categoryBox.getItemAt(i).categoryId() == categoryId) {
                categoryBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectSkills(List<SkillDto> chosen) {
        Set<Long> ids = chosen.stream().map(SkillDto::skillId).collect(Collectors.toSet());
        skillsList.setSelectedIndices(IntStream.range(0, skillsModel.size())
                .filter(i -> ids.contains(skillsModel.get(i).skillId()))
                .toArray());
    }

    private void save() {
        GigRequest request;
        try {
            request = parseRequest(titleField.getText(), descriptionArea.getText(),
                    (CategoryDto) categoryBox.getSelectedItem(), budgetField.getText(), deadlineField.getText(),
                    skillsList.getSelectedValuesList());
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        long clientId = SessionManager.getInstance().getCurrentUser().userId();
        saveButton.setEnabled(false);
        runInBackground(() -> existing == null
                        ? gigService.createGig(clientId, request)
                        : gigService.updateGig(clientId, existing.gigId(), request),
                saved -> {
                    saveButton.setEnabled(true);
                    messageLabel.setForeground(UiConstants.SUCCESS);
                    if (existing == null) {
                        clearForm();
                        messageLabel.setText("Gig posted. Find it under My Gigs.");
                    } else {
                        messageLabel.setText("Gig saved");
                    }
                    onSaved.run();
                }, error -> {
                    saveButton.setEnabled(true);
                    FormLayout.showFailure(this, messageLabel, error);
                });
    }

    private void clearForm() {
        titleField.setText("");
        descriptionArea.setText("");
        budgetField.setText("");
        deadlineField.setText("");
        skillsList.clearSelection();
        if (categoryBox.getItemCount() > 0) {
            categoryBox.setSelectedIndex(0);
        }
    }
}
