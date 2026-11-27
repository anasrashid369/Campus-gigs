package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.FreelancerProfileDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** The freelancer's own profile (F21): biography, skills and portfolio links. */
public class FreelancerProfilePanel extends BasePanel {

    /** Everything the screen needs, loaded in one background call. */
    private record Loaded(List<SkillDto> skills, FreelancerProfileDto profile) {
    }

    private final ProfileService profileService;

    private final TextArea biographyArea = new TextArea();
    private final ListView<SkillDto> skillsList = new ListView<>();
    private final TextField linksField = new TextField();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Button saveButton = new Button("Save profile");

    public FreelancerProfilePanel(ProfileService profileService) {
        this.profileService = profileService;
        biographyArea.setWrapText(true);
        biographyArea.setPrefRowCount(5);
        skillsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        skillsList.setPrefHeight(170);

        VBox form = FormLayout.createForm("My freelancer profile");
        form.setPrefWidth(UiConstants.WIDE_FORM_WIDTH);
        FormLayout.addField(form, "Biography (up to " + ProfileService.MAX_BIOGRAPHY_LENGTH + " characters)",
                biographyArea, null);
        FormLayout.addField(form, "Skills (up to " + ProfileService.MAX_SKILLS + ", Ctrl+click to choose several)",
                skillsList, null);
        FormLayout.addField(form, "Portfolio links (separate with spaces or commas)", linksField, null);
        FormLayout.addRow(form, messageLabel);
        saveButton.getStyleClass().add("primary");
        FormLayout.addRow(form, saveButton);
        setCenteredContent(form);

        saveButton.setOnAction(event -> save());
        load();
    }

    private void load() {
        long userId = currentUserId();
        saveButton.setDisable(true);
        runInBackground(() -> new Loaded(profileService.getAvailableSkills(), profileService.getProfile(userId)),
                loaded -> {
                    skillsList.getItems().setAll(loaded.skills());
                    display(loaded.profile());
                    saveButton.setDisable(false);
                });
    }

    private void save() {
        long userId = currentUserId();
        String biography = biographyArea.getText();
        String links = linksField.getText();
        List<Long> skillIds = skillsList.getSelectionModel().getSelectedItems().stream()
                .map(SkillDto::skillId)
                .toList();
        saveButton.setDisable(true);
        runInBackground(() -> profileService.updateProfile(userId, userId, biography, links, skillIds), saved -> {
            saveButton.setDisable(false);
            display(saved);
            FormLayout.showSuccess(messageLabel, "Profile saved");
        }, error -> {
            saveButton.setDisable(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private void display(FreelancerProfileDto profile) {
        biographyArea.setText(profile.biography());
        linksField.setText(profile.portfolioLinks());
        Set<Long> chosen = profile.skills().stream().map(SkillDto::skillId).collect(Collectors.toSet());
        skillsList.getSelectionModel().clearSelection();
        for (SkillDto skill : skillsList.getItems()) {
            if (chosen.contains(skill.skillId())) {
                skillsList.getSelectionModel().select(skill);
            }
        }
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
