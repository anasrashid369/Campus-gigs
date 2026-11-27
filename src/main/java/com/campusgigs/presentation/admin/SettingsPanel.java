package com.campusgigs.presentation.admin;

import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.service.SettingsService;
import com.campusgigs.util.SessionManager;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Admin settings screen (F12): initial token balance, platform fee and the application
 * limit. All rules (admin only, whole numbers, ranges) are checked by {@link SettingsService}.
 */
public class SettingsPanel extends BasePanel {

    private final SettingsService settingsService;

    /** Setting key to its input field, in the order shown. */
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private Map<String, String> loadedValues = Map.of();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Button saveButton = new Button("Save settings");

    public SettingsPanel(SettingsService settingsService) {
        this.settingsService = settingsService;

        VBox form = FormLayout.createForm("Platform settings");
        addSetting(form, SettingsService.KEY_INITIAL_TOKEN_BALANCE, "Initial token balance for new users (0 - 100000)");
        addSetting(form, SettingsService.KEY_PLATFORM_FEE_PERCENT, "Platform fee percent (0 - 50)");
        addSetting(form, SettingsService.KEY_MAX_APPLICATIONS_PER_FREELANCER,
                "Max open applications per freelancer (1 - 50)");
        FormLayout.addRow(form, messageLabel);
        saveButton.getStyleClass().add("primary");
        FormLayout.addRow(form, saveButton);
        setCenteredContent(form);

        saveButton.setOnAction(event -> save());
        load(true);
    }

    private void addSetting(VBox form, String key, String label) {
        TextField field = new TextField();
        fields.put(key, field);
        FormLayout.addField(form, label, field, null);
    }

    /**
     * Reads the stored values. After a failed save the fields keep what the admin typed
     * ({@code fillFields} false) so it can be corrected; only the stored copy is refreshed.
     */
    private void load(boolean fillFields) {
        saveButton.setDisable(true);
        runInBackground(settingsService::getAll, values -> {
            loadedValues = values;
            if (fillFields) {
                fields.forEach((key, field) -> field.setText(values.getOrDefault(key, "")));
            }
            saveButton.setDisable(false);
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
            FormLayout.showInfo(messageLabel, "Nothing to save");
            return;
        }
        saveButton.setDisable(true);
        runInBackground(() -> {
            changed.forEach((key, value) -> settingsService.update(adminId, key, value));
            return null;
        }, done -> {
            load(true);
            FormLayout.showSuccess(messageLabel, "Settings saved");
        }, error -> {
            load(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }
}
