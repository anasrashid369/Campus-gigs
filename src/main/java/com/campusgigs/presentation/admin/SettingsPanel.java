package com.campusgigs.presentation.admin;

import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.SettingsService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin settings screen (F12): initial token balance, platform fee and the application
 * limit. All rules (admin only, whole numbers, ranges) are checked by {@link SettingsService}.
 */
public class SettingsPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final transient SettingsService settingsService;

    /** Setting key to its input field, in the order shown. */
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private transient Map<String, String> loadedValues = Map.of();
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final JButton saveButton = new JButton("Save settings");

    public SettingsPanel(SettingsService settingsService) {
        this.settingsService = settingsService;
        setLayout(new GridBagLayout());

        JPanel form = FormLayout.createForm("Platform settings");
        addSetting(form, SettingsService.KEY_INITIAL_TOKEN_BALANCE, "Initial token balance for new users (0 - 100000)");
        addSetting(form, SettingsService.KEY_PLATFORM_FEE_PERCENT, "Platform fee percent (0 - 50)");
        addSetting(form, SettingsService.KEY_MAX_APPLICATIONS_PER_FREELANCER,
                "Max open applications per freelancer (1 - 50)");
        FormLayout.addRow(form, messageLabel, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, saveButton, 0);
        add(form);

        saveButton.addActionListener(e -> save());
        load(true);
    }

    private void addSetting(JPanel form, String key, String label) {
        JTextField field = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
        fields.put(key, field);
        FormLayout.addField(form, label, field, null);
    }

    /**
     * Reads the stored values. After a failed save the fields keep what the admin typed
     * ({@code fillFields} false) so it can be corrected; only the stored copy is refreshed.
     */
    private void load(boolean fillFields) {
        saveButton.setEnabled(false);
        runInBackground(settingsService::getAll, values -> {
            loadedValues = values;
            if (fillFields) {
                fields.forEach((key, field) -> field.setText(values.getOrDefault(key, "")));
            }
            saveButton.setEnabled(true);
        });
    }

    /** Sends only the values that changed; the service validates each one. */
    private void save() {
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        Map<String, String> changed = new LinkedHashMap<>();
        fields.forEach((key, field) -> {
            if (!field.getText().trim().equals(loadedValues.get(key))) {
                changed.put(key, field.getText());
            }
        });
        if (changed.isEmpty()) {
            messageLabel.setForeground(UiConstants.MUTED_TEXT);
            messageLabel.setText("Nothing to save");
            return;
        }
        saveButton.setEnabled(false);
        runInBackground(() -> {
            changed.forEach((key, value) -> settingsService.update(adminId, key, value));
            return null;
        }, done -> {
            load(true);
            messageLabel.setForeground(UiConstants.SUCCESS);
            messageLabel.setText("Settings saved");
        }, error -> {
            load(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }
}
