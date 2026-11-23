package com.campusgigs.presentation.admin;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.Optional;

/** Admin screen for gig categories (F8): add, rename, activate and deactivate. */
public class CategoriesPanel extends TablePanel<CategoryDto> {

    private static final long serialVersionUID = 1L;

    private final transient AdminService adminService;
    private final JButton toggleButton;

    public CategoriesPanel(AdminService adminService) {
        super("Gig categories", List.of("Name", "Status"),
                category -> new Object[] {category.name(), category.active() ? "Active" : "Inactive"});
        this.adminService = adminService;
        addAction("Add", false, this::add);
        addAction("Rename", true, this::rename);
        toggleButton = addAction("Deactivate", true, this::toggle);
        load();
    }

    @Override
    protected void onSelectionChanged(Optional<CategoryDto> row) {
        if (toggleButton != null) {
            toggleButton.setText(row.map(c -> c.active() ? "Deactivate" : "Activate").orElse("Deactivate"));
        }
    }

    private void load() {
        long adminId = adminId();
        runInBackground(() -> adminService.listCategories(adminId), this::setRows);
    }

    private void add() {
        String name = JOptionPane.showInputDialog(this, "Name of the new category:", "Add category",
                JOptionPane.QUESTION_MESSAGE);
        if (name != null) {
            long adminId = adminId();
            runInBackground(() -> adminService.addCategory(adminId, name), added -> load());
        }
    }

    private void rename() {
        selected().ifPresent(category -> {
            Object name = JOptionPane.showInputDialog(this, "New name for \"" + category.name() + "\":",
                    "Rename category", JOptionPane.QUESTION_MESSAGE, null, null, category.name());
            if (name != null) {
                long adminId = adminId();
                runInBackground(() -> adminService.renameCategory(adminId, category.categoryId(), name.toString()),
                        renamed -> load());
            }
        });
    }

    private void toggle() {
        selected().ifPresent(category -> {
            long adminId = adminId();
            runInBackground(() -> adminService.setCategoryActive(adminId, category.categoryId(), !category.active()),
                    changed -> load());
        });
    }

    private static long adminId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }
}
