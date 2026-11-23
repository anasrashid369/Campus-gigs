package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.FreelancerProfileDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** The freelancer's own profile (F21): biography, skills and portfolio links. */
public class FreelancerProfilePanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    /** Everything the screen needs, loaded in one background call. */
    private record Loaded(List<SkillDto> skills, FreelancerProfileDto profile) {
    }

    private final transient ProfileService profileService;

    private final JTextArea biographyArea = new JTextArea(6, UiConstants.TEXT_FIELD_COLUMNS + 12);
    private final DefaultListModel<SkillDto> skillsModel = new DefaultListModel<>();
    private final JList<SkillDto> skillsList = new JList<>(skillsModel);
    private final JTextField linksField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS + 12);
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final JButton saveButton = new JButton("Save profile");

    public FreelancerProfilePanel(ProfileService profileService) {
        this.profileService = profileService;
        setLayout(new GridBagLayout());

        biographyArea.setLineWrap(true);
        biographyArea.setWrapStyleWord(true);
        skillsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        skillsList.setVisibleRowCount(7);

        JPanel form = FormLayout.createForm("My freelancer profile");
        FormLayout.addField(form, "Biography (up to " + ProfileService.MAX_BIOGRAPHY_LENGTH + " characters)",
                new JScrollPane(biographyArea), null);
        FormLayout.addField(form, "Skills (up to " + ProfileService.MAX_SKILLS + ", Ctrl+click to choose several)",
                new JScrollPane(skillsList), null);
        FormLayout.addField(form, "Portfolio links (separate with spaces or commas)", linksField, null);
        FormLayout.addRow(form, messageLabel, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, saveButton, 0);
        add(form);

        saveButton.addActionListener(e -> save());
        load();
    }

    private void load() {
        long userId = currentUserId();
        saveButton.setEnabled(false);
        runInBackground(() -> new Loaded(profileService.getAvailableSkills(), profileService.getProfile(userId)),
                loaded -> {
                    skillsModel.clear();
                    loaded.skills().forEach(skillsModel::addElement);
                    display(loaded.profile());
                    saveButton.setEnabled(true);
                });
    }

    private void save() {
        long userId = currentUserId();
        String biography = biographyArea.getText();
        String links = linksField.getText();
        List<Long> skillIds = skillsList.getSelectedValuesList().stream().map(SkillDto::skillId).toList();
        saveButton.setEnabled(false);
        runInBackground(() -> profileService.updateProfile(userId, userId, biography, links, skillIds), saved -> {
            saveButton.setEnabled(true);
            display(saved);
            messageLabel.setForeground(UiConstants.SUCCESS);
            messageLabel.setText("Profile saved");
        }, error -> {
            saveButton.setEnabled(true);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private void display(FreelancerProfileDto profile) {
        biographyArea.setText(profile.biography());
        linksField.setText(profile.portfolioLinks());
        Set<Long> chosen = profile.skills().stream().map(SkillDto::skillId).collect(Collectors.toSet());
        int[] indices = IntStream.range(0, skillsModel.size())
                .filter(i -> chosen.contains(skillsModel.get(i).skillId()))
                .toArray();
        skillsList.setSelectedIndices(indices);
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
