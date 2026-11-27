package com.campusgigs.presentation.admin;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.presentation.common.Dialogs;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Button;

/** Admin screen for gig categories (F8): add, rename, activate and deactivate. */
public class CategoriesPanel extends TablePanel<CategoryDto> {

    private final AdminService adminService;
    private final Button toggleButton;

    public CategoriesPanel(AdminService adminService) {
        super("Gig categories", List.of("Name", "Status"),
                category -> new Object[] {category.name(), category.active() ? "Active" : "Inactive"});
        this.adminService = adminService;
        addAction("Add", false, this::add).getStyleClass().add("primary");
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
        Dialogs.input(this, "Add category", "Name of the new category:", "").ifPresent(name -> {
            long adminId = adminId();
            runInBackground(() -> adminService.addCategory(adminId, name), added -> load());
        });
    }

    private void rename() {
        selected().ifPresent(category -> Dialogs.input(this, "Rename category",
                "New name for \"" + category.name() + "\":", category.name()).ifPresent(name -> {
            long adminId = adminId();
            runInBackground(() -> adminService.renameCategory(adminId, category.categoryId(), name),
                    renamed -> load());
        }));
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
