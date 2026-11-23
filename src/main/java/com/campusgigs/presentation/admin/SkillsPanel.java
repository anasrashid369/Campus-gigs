package com.campusgigs.presentation.admin;

import com.campusgigs.dto.SkillDto;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.util.SessionManager;

import javax.swing.JOptionPane;
import java.util.List;

/** Admin screen for skills (F8): add, rename and delete skills nobody uses. */
public class SkillsPanel extends TablePanel<SkillDto> {

    private static final long serialVersionUID = 1L;

    private final transient AdminService adminService;

    public SkillsPanel(AdminService adminService) {
        super("Skills", List.of("Name"), skill -> new Object[] {skill.name()});
        this.adminService = adminService;
        addAction("Add", false, this::add);
        addAction("Rename", true, this::rename);
        addAction("Delete", true, this::delete);
        load();
    }

    private void load() {
        long adminId = adminId();
        runInBackground(() -> adminService.listSkills(adminId), this::setRows);
    }

    private void add() {
        String name = JOptionPane.showInputDialog(this, "Name of the new skill:", "Add skill",
                JOptionPane.QUESTION_MESSAGE);
        if (name != null) {
            long adminId = adminId();
            runInBackground(() -> adminService.addSkill(adminId, name), added -> load());
        }
    }

    private void rename() {
        selected().ifPresent(skill -> {
            Object name = JOptionPane.showInputDialog(this, "New name for \"" + skill.name() + "\":",
                    "Rename skill", JOptionPane.QUESTION_MESSAGE, null, null, skill.name());
            if (name != null) {
                long adminId = adminId();
                runInBackground(() -> adminService.renameSkill(adminId, skill.skillId(), name.toString()),
                        renamed -> load());
            }
        });
    }

    private void delete() {
        selected().ifPresent(skill -> {
            int answer = JOptionPane.showConfirmDialog(this, "Delete the skill \"" + skill.name() + "\"?",
                    "Delete skill", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
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
