package com.campusgigs.presentation;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.client.ClientDashboardPanel;
import com.campusgigs.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MainFrameTest {

    private MainFrame frame;

    @BeforeEach
    void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        frame = new MainFrame();
    }

    @AfterEach
    void logout() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheRequestedCard() {
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        frame.addCard("first", first);
        frame.addCard("second", second);

        frame.showCard("second");

        assertTrue(second.isVisible());
        assertFalse(first.isVisible());
    }

    @Test
    void rejectsDuplicateCardNames() {
        frame.addCard("wallet", new JPanel());

        assertThrows(IllegalArgumentException.class, () -> frame.addCard("wallet", new JPanel()));
    }

    @Test
    void rejectsUnknownCardName() {
        assertThrows(IllegalArgumentException.class, () -> frame.showCard("missing"));
        frame.addCard("home", new JPanel());
        assertDoesNotThrow(() -> frame.showCard("home"));
    }

    @Test
    void removedCardCanBeAddedAgain() {
        frame.addCard("dashboard", new JPanel());
        frame.removeCard("dashboard");
        frame.removeCard("never-added");

        assertThrows(IllegalArgumentException.class, () -> frame.showCard("dashboard"));
        assertDoesNotThrow(() -> frame.addCard("dashboard", new JPanel()));
    }

    @Test
    void keepsNavigationItemsPerRoleInOrder() {
        frame.addNavigationItem(Role.CLIENT, "My Wallet", JPanel::new);
        frame.addNavigationItem(Role.CLIENT, "My Gigs", JPanel::new);
        frame.addNavigationItem(Role.ADMIN, "Users", JPanel::new);

        assertEquals(2, frame.getNavigationItems(Role.CLIENT).size());
        assertEquals("My Gigs", frame.getNavigationItems(Role.CLIENT).get(1).label());
        assertTrue(frame.getNavigationItems(Role.FREELANCER).isEmpty());
    }

    @Test
    void sessionStartShowsDashboardAndEndReturnsToLogin() {
        JPanel login = new JPanel();
        frame.addCard(MainFrame.LOGIN_CARD, login);
        UserDto ali = new UserDto(3, Role.CLIENT, "Ali", "ali@uni.edu.pk", AccountStatus.ACTIVE);

        frame.startSession(ali);
        assertEquals(ali, SessionManager.getInstance().getCurrentUser());
        assertInstanceOf(ClientDashboardPanel.class, visibleCard());

        frame.startSession(ali);
        frame.endSession();
        assertFalse(SessionManager.getInstance().isLoggedIn());
        assertTrue(login.isVisible());
    }

    private Component visibleCard() {
        JPanel content = (JPanel) frame.getContentPane().getComponent(1);
        for (Component card : content.getComponents()) {
            if (card.isVisible()) {
                return card;
            }
        }
        return null;
    }

    @Test
    void keepsHomeWidgetsPerRoleInTheOrderTheyWereAdded() {
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        frame.addHomeWidget(Role.CLIENT, () -> first);
        frame.addHomeWidget(Role.CLIENT, () -> second);
        frame.addHomeWidget(Role.ADMIN, JPanel::new);

        assertEquals(2, frame.getHomeWidgets(Role.CLIENT).size());
        assertEquals(first, frame.getHomeWidgets(Role.CLIENT).get(0).get());
        assertEquals(second, frame.getHomeWidgets(Role.CLIENT).get(1).get());
        assertEquals(1, frame.getHomeWidgets(Role.ADMIN).size());
        assertTrue(frame.getHomeWidgets(Role.FREELANCER).isEmpty());
    }

    @Test
    void headerWidgetsAreShownOnlyInTheirRolesDashboard() {
        JLabel clientBadge = new JLabel("client badge");
        frame.addHeaderWidget(Role.CLIENT, () -> clientBadge);
        frame.addHeaderWidget(Role.ADMIN, JLabel::new);

        assertEquals(1, frame.getHeaderWidgets(Role.CLIENT).size());
        assertTrue(frame.getHeaderWidgets(Role.FREELANCER).isEmpty());

        frame.startSession(new UserDto(2, Role.CLIENT, "Ali Raza", "ali@uni.edu.pk", AccountStatus.ACTIVE));
        assertTrue(contains(frame.getContentPane(), clientBadge));
        assertThrows(NullPointerException.class, () -> frame.addHeaderWidget(Role.CLIENT, null));
    }

    private static boolean contains(Container parent, Component target) {
        for (Component child : parent.getComponents()) {
            if (child == target || child instanceof Container c && contains(c, target)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void rejectsMissingHomeWidgetArguments() {
        assertThrows(NullPointerException.class, () -> frame.addHomeWidget(null, JPanel::new));
        assertThrows(NullPointerException.class, () -> frame.addHomeWidget(Role.CLIENT, null));
    }
}
