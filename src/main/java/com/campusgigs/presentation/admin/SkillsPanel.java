package com.campusgigs.presentation.admin;

import com.campusgigs.dto.SkillDto;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.util.SessionManager;
import java.util.List;

/** Admin screen for skills (F8): add, rename and delete skills nobody uses. */
public class SkillsPanel extends TablePanel<SkillDto> {

    private final AdminService adminService;

    public SkillsPanel(AdminService adminService) {
        super("Skills", List.of("Name"), skill -> new Object[] {skill.name()});
        this.adminService = adminService;
        addAction("Add", false, this::add).getStyleClass().add("primary");
        addAction("Rename", true, this::rename);
        addAction("Delete", true, this::delete).getStyleClass().add("danger");
        load();
    }

    private void load() {
        long adminId = adminId();
        runInBackground(() -> adminService.listSkills(adminId), this::setRows);
    }

    private void add() {
        Dialogs.input(this, "Add skill", "Name of the new skill:", "").ifPresent(name -> {
            long adminId = adminId();
            runInBackground(() -> adminService.addSkill(adminId, name), added -> load());
        });
    }

    private void rename() {
        selected().ifPresent(skill -> Dialogs.input(this, "Rename skill",
                "New name for \"" + skill.name() + "\":", skill.name()).ifPresent(name -> {
            long adminId = adminId();
            runInBackground(() -> adminService.renameSkill(adminId, skill.skillId(), name), renamed -> load());
        }));
    }

    private void delete() {
        selected().ifPresent(skill -> {
            if (Dialogs.confirm(this, "Delete skill", "Delete the skill \"" + skill.name() + "\"?")) {
                long adminId = adminId();
                runInBackground(() -> {
                    adminService.deleteSkill(adminId, skill.skillId());
                    return null;
                }, deleted -> load());
            }
        });
    }

    private static long adminId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
