package com.campusgigs.presentation;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.Role;
import com.campusgigs.pattern.factory.DashboardFactory;
import com.campusgigs.presentation.common.NavigationItem;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.util.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Main application window: a header at the top and a content area that shows
 * one screen (card) at a time. Screens are registered once with
 * {@link #addCard(String, JPanel)} and switched with {@link #showCard(String)}.
 * <p>
 * It also keeps the navigation list per role. Team members add their screens with
 * {@link #addNavigationItem(Role, String, Supplier)} at startup, and every dashboard
 * shows a button for each item of its role, so nobody has to edit the dashboards.
 * Build and use it on the Event Dispatch Thread only.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String LOGIN_CARD = "login";
    public static final String REGISTER_CARD = "register";
    public static final String DASHBOARD_CARD = "dashboard";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JPanel> cardsByName = new HashMap<>();
    private final Map<Role, List<NavigationItem>> navigation = new EnumMap<>(Role.class);

    public MainFrame() {
        super(UiConstants.APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(UiConstants.FRAME_WIDTH, UiConstants.FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        content.setBackground(UiConstants.BACKGROUND);
        add(content, BorderLayout.CENTER);
    }

    /**
     * Registers a screen under a unique name.
     *
     * @throws IllegalArgumentException if a card with this name already exists
     */
    public void addCard(String name, JPanel panel) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(panel, "panel");
        if (cardsByName.containsKey(name)) {
            throw new IllegalArgumentException("A card named '" + name + "' is already registered");
        }
        cardsByName.put(name, panel);
        content.add(panel, name);
    }

    /** Removes the named screen; does nothing if there is none. */
    public void removeCard(String name) {
        JPanel panel = cardsByName.remove(name);
        if (panel != null) {
            content.remove(panel);
        }
    }

    /**
     * Brings the named screen to the front.
     *
     * @throws IllegalArgumentException if no card with this name was added
     */
    public void showCard(String name) {
        if (!cardsByName.containsKey(name)) {
            throw new IllegalArgumentException("No card named '" + name + "'");
        }
        cards.show(content, name);
    }

    /**
     * Adds a navigation button to every dashboard of the given role.
     *
     * @param role   whose dashboard shows the button
     * @param label  the button text
     * @param screen creates the screen; called once per login, on first click
     */
    public void addNavigationItem(Role role, String label, Supplier<JPanel> screen) {
        Objects.requireNonNull(role, "role");
        navigation.computeIfAbsent(role, r -> new ArrayList<>()).add(new NavigationItem(label, screen));
    }

    /** The navigation items for a role, in the order they were added. */
    public List<NavigationItem> getNavigationItems(Role role) {
        return List.copyOf(navigation.getOrDefault(role, List.of()));
    }

    /** Remembers the user and shows a fresh dashboard for their role. */
    public void startSession(UserDto user) {
        SessionManager.getInstance().setCurrentUser(user);
        removeCard(DASHBOARD_CARD);
        addCard(DASHBOARD_CARD, DashboardFactory.create(user, this));
        showCard(DASHBOARD_CARD);
    }

    /** Logs out: forgets the user, drops their dashboard and returns to the login screen. */
    public void endSession() {
        SessionManager.getInstance().clear();
        showCard(LOGIN_CARD);
        removeCard(DASHBOARD_CARD);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(UiConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_MEDIUM, UiConstants.SPACING_LARGE,
                UiConstants.SPACING_MEDIUM, UiConstants.SPACING_LARGE));
        JLabel title = new JLabel(UiConstants.APP_TITLE);
        title.setFont(UiConstants.HEADING_FONT);
        title.setForeground(UiConstants.SURFACE);
        header.add(title);
        return header;
    }
}
