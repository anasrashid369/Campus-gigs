package com.campusgigs.presentation.admin;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Locale;

/** Admin screen for user accounts (F7): activate, suspend or deactivate the selected user. */
public class UsersPanel extends TablePanel<UserDto> {

    private final AdminService adminService;

    public UsersPanel(AdminService adminService) {
        super("Users", List.of("Name", "Email", "Role", "Status"),
                user -> new Object[] {user.fullName(), user.email(), user.role(), user.accountStatus()});
        this.adminService = adminService;
        addAction("Activate", true, () -> changeStatus(AccountStatus.ACTIVE));
        addAction("Suspend", true, () -> changeStatus(AccountStatus.SUSPENDED));
        addAction("Deactivate", true, () -> changeStatus(AccountStatus.DEACTIVATED)).getStyleClass().add("danger");
        addAction("Refresh", false, this::load);
        load();
    }

    private void load() {
        long adminId = adminId();
        runInBackground(() -> adminService.listUsers(adminId), this::setRows);
    }

    private void changeStatus(AccountStatus status) {
        selected().ifPresent(user -> {
            String verb = status.name().charAt(0) + status.name().substring(1).toLowerCase(Locale.ROOT);
            String action = verb.endsWith("e") ? verb.substring(0, verb.length() - 1) : verb;
            if (Dialogs.confirm(this, "Change account status",
                    action + " the account of " + user.fullName() + " (" + user.email() + ")?")) {
                long adminId = adminId();
                runInBackground(() -> {
                    adminService.setAccountStatus(adminId, user.userId(), status);
                    return null;
                }, changed -> load());
            }
        });
    }

    private static long adminId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
