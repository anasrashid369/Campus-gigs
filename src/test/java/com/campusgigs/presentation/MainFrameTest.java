package com.campusgigs.presentation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.client.ClientDashboardPanel;
import com.campusgigs.util.SessionManager;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainFrameTest {

    private MainFrame frame;

    @BeforeEach
    void setUp() {
        FxTestSupport.startToolkit();
        frame = new MainFrame();
    }

    @AfterEach
    void logout() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheRequestedCard() {
        Pane first = new Pane();
        Pane second = new Pane();
        frame.addCard("first", first);
        frame.addCard("second", second);

        frame.showCard("second");

        assertTrue(second.isVisible());
        assertFalse(first.isVisible());
    }

    @Test
    void rejectsDuplicateCardNames() {
        frame.addCard("wallet", new Pane());

        assertThrows(IllegalArgumentException.class, () -> frame.addCard("wallet", new Pane()));
    }

    @Test
    void rejectsUnknownCardName() {
        assertThrows(IllegalArgumentException.class, () -> frame.showCard("missing"));
        frame.addCard("home", new Pane());
        assertDoesNotThrow(() -> frame.showCard("home"));
    }

    @Test
    void removedCardCanBeAddedAgain() {
        frame.addCard("dashboard", new Pane());
        frame.removeCard("dashboard");
        frame.removeCard("never-added");

        assertThrows(IllegalArgumentException.class, () -> frame.showCard("dashboard"));
        assertDoesNotThrow(() -> frame.addCard("dashboard", new Pane()));
    }

    @Test
    void keepsNavigationItemsPerRoleInOrder() {
        frame.addNavigationItem(Role.CLIENT, "My Wallet", Pane::new);
        frame.addNavigationItem(Role.CLIENT, "My Gigs", Pane::new);
        frame.addNavigationItem(Role.ADMIN, "Users", Pane::new);

        assertEquals(2, frame.getNavigationItems(Role.CLIENT).size());
        assertEquals("My Gigs", frame.getNavigationItems(Role.CLIENT).get(1).label());
        assertTrue(frame.getNavigationItems(Role.FREELANCER).isEmpty());
    }

    @Test
    void sessionStartShowsDashboardAndEndReturnsToLogin() {
        Pane login = new Pane();
        frame.addCard(MainFrame.LOGIN_CARD, login);
        UserDto ali = new UserDto(3, Role.CLIENT, "Ali", "ali@uni.edu.pk", AccountStatus.ACTIVE);

        frame.startSession(ali);

        assertEquals(ali, SessionManager.getInstance().getCurrentUser());
        assertInstanceOf(ClientDashboardPanel.class, visibleCard());

        frame.startSession(ali);
        frame.endSession();

        assertFalse(SessionManager.getInstance().isLoggedIn());
        assertTrue(login.isVisible());
        assertEquals(1, frame.getRoot().getChildrenUnmodifiable().size());
    }

    private Node visibleCard() {
        return frame.getRoot().getChildrenUnmodifiable().stream().filter(Node::isVisible).findFirst().orElse(null);
    }

    @Test
    void keepsHomeWidgetsPerRoleInTheOrderTheyWereAdded() {
        Pane first = new Pane();
        Pane second = new Pane();
        frame.addHomeWidget(Role.CLIENT, () -> first);
        frame.addHomeWidget(Role.CLIENT, () -> second);
        frame.addHomeWidget(Role.ADMIN, Pane::new);

        assertEquals(2, frame.getHomeWidgets(Role.CLIENT).size());
        assertEquals(first, frame.getHomeWidgets(Role.CLIENT).get(0).get());
        assertEquals(second, frame.getHomeWidgets(Role.CLIENT).get(1).get());
        assertEquals(1, frame.getHomeWidgets(Role.ADMIN).size());
        assertTrue(frame.getHomeWidgets(Role.FREELANCER).isEmpty());
    }

    @Test
    void headerWidgetsAreShownOnlyInTheirRolesDashboard() {
        Label clientBadge = new Label("client badge");
        frame.addHeaderWidget(Role.CLIENT, () -> clientBadge);
        frame.addHeaderWidget(Role.ADMIN, Label::new);

        assertEquals(1, frame.getHeaderWidgets(Role.CLIENT).size());
        assertTrue(frame.getHeaderWidgets(Role.FREELANCER).isEmpty());

        frame.startSession(new UserDto(2, Role.CLIENT, "Ali Raza", "ali@uni.edu.pk", AccountStatus.ACTIVE));

        assertTrue(contains(frame.getRoot(), clientBadge));
        assertThrows(NullPointerException.class, () -> frame.addHeaderWidget(Role.CLIENT, null));
    }

    private static boolean contains(Parent parent, Node target) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child == target || child instanceof Parent inner && contains(inner, target)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void rejectsMissingHomeWidgetArguments() {
        assertThrows(NullPointerException.class, () -> frame.addHomeWidget(null, Pane::new));
        assertThrows(NullPointerException.class, () -> frame.addHomeWidget(Role.CLIENT, null));
    }
}
