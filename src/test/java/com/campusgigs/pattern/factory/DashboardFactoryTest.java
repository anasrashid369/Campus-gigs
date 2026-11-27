package com.campusgigs.pattern.factory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.FxTestSupport;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.admin.AdminDashboardPanel;
import com.campusgigs.presentation.client.ClientDashboardPanel;
import com.campusgigs.presentation.common.DashboardPanel;
import com.campusgigs.presentation.freelancer.FreelancerDashboardPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardFactoryTest {

    private MainFrame frame;

    @BeforeEach
    void setUp() {
        FxTestSupport.startToolkit();
        frame = new MainFrame();
    }

    private static UserDto user(Role role) {
        return new UserDto(1, role, "Test User", "test@uni.edu.pk", AccountStatus.ACTIVE);
    }

    @Test
    void createsTheDashboardForEachRole() {
        assertInstanceOf(ClientDashboardPanel.class, DashboardFactory.create(user(Role.CLIENT), frame));
        assertInstanceOf(FreelancerDashboardPanel.class, DashboardFactory.create(user(Role.FREELANCER), frame));
        assertInstanceOf(AdminDashboardPanel.class, DashboardFactory.create(user(Role.ADMIN), frame));
    }

    @Test
    void dashboardShowsOnlyTheNavigationItemsOfItsRole() {
        frame.addNavigationItem(Role.CLIENT, "My Wallet", Pane::new);
        frame.addNavigationItem(Role.ADMIN, "Users", Pane::new);

        DashboardPanel client = DashboardFactory.create(user(Role.CLIENT), frame);

        assertTrue(hasButton(client, "My Wallet"));
        assertTrue(hasButton(client, "Logout"));
        assertFalse(hasButton(client, "Users"));
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(NullPointerException.class, () -> DashboardFactory.create(null, frame));
        assertThrows(NullPointerException.class, () -> DashboardFactory.create(user(Role.CLIENT), null));
    }

    private static boolean hasButton(Parent parent, String text) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof ButtonBase button && text.equals(button.getText())) {
                return true;
            }
            if (child instanceof Parent inner && hasButton(inner, text)) {
                return true;
            }
        }
        return false;
    }
}
