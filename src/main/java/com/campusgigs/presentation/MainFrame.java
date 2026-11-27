package com.campusgigs.presentation;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.Role;
import com.campusgigs.pattern.factory.DashboardFactory;
import com.campusgigs.presentation.common.NavigationItem;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.util.SessionManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main application window: shows one screen (card) at a time. Screens are registered once with
 * {@link #addCard(String, Node)} and switched with {@link #showCard(String)}.
 * <p>
 * It also keeps the navigation list per role. Team members add their screens with
 * {@link #addNavigationItem(Role, String, Supplier)} at startup, and every dashboard
 * shows a button for each item of its role, so nobody has to edit the dashboards.
 * Use it on the JavaFX Application Thread only.
 */
public class MainFrame {

    public static final String LOGIN_CARD = "login";
    public static final String REGISTER_CARD = "register";
    public static final String DASHBOARD_CARD = "dashboard";

    private final StackPane content = new StackPane();
    private final Map<String, Node> cardsByName = new HashMap<>();
    private final Map<Role, List<NavigationItem>> navigation = new EnumMap<>(Role.class);
    private final Map<Role, List<Supplier<? extends Node>>> homeWidgets = new EnumMap<>(Role.class);
    private final Map<Role, List<Supplier<? extends Node>>> headerWidgets = new EnumMap<>(Role.class);

    public MainFrame() {
        content.getStyleClass().add("app-root");
    }

    /** Puts the window content into the stage, applies the stylesheet and shows it. */
    public void show(Stage stage) {
        Scene scene = new Scene(content, UiConstants.FRAME_WIDTH, UiConstants.FRAME_HEIGHT);
        scene.getStylesheets().add(UiConstants.stylesheet());
        stage.setTitle(UiConstants.APP_TITLE);
        stage.setMinWidth(UiConstants.FRAME_MIN_WIDTH);
        stage.setMinHeight(UiConstants.FRAME_MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    /** The node holding every card; the scene's root. */
    public Parent getRoot() {
        return content;
    }

    /**
     * Registers a screen under a unique name. It stays hidden until {@link #showCard} picks it.
     *
     * @throws IllegalArgumentException if a card with this name already exists
     */
    public void addCard(String name, Node screen) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(screen, "screen");
        if (cardsByName.containsKey(name)) {
            throw new IllegalArgumentException("A card named '" + name + "' is already registered");
        }
        cardsByName.put(name, screen);
        screen.setVisible(false);
        content.getChildren().add(screen);
    }

    /** Removes the named screen; does nothing if there is none. */
    public void removeCard(String name) {
        Node screen = cardsByName.remove(name);
        if (screen != null) {
            content.getChildren().remove(screen);
        }
    }

    /**
     * Brings the named screen to the front and hides the others.
     *
     * @throws IllegalArgumentException if no card with this name was added
     */
    public void showCard(String name) {
        Node shown = cardsByName.get(name);
        if (shown == null) {
            throw new IllegalArgumentException("No card named '" + name + "'");
        }
        cardsByName.values().forEach(card -> card.setVisible(card == shown));
    }

    /**
     * Adds a navigation button to every dashboard of the given role.
     *
     * @param role   whose dashboard shows the button
     * @param label  the button text
     * @param screen creates the screen; called once per login, on first click
     */
    public void addNavigationItem(Role role, String label, Supplier<? extends Node> screen) {
        Objects.requireNonNull(role, "role");
        navigation.computeIfAbsent(role, r -> new ArrayList<>()).add(new NavigationItem(label, screen));
    }

    /**
     * Adds a small panel (for example summary cards) to the home screen of every dashboard of the role.
     *
     * @param widget creates the panel; called once per login, when the dashboard is built
     */
    public void addHomeWidget(Role role, Supplier<? extends Node> widget) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(widget, "widget");
        homeWidgets.computeIfAbsent(role, r -> new ArrayList<>()).add(widget);
    }

    /**
     * Adds a small control (for example the notification badge) to the user bar of every
     * dashboard of the role, left of the Logout button.
     *
     * @param widget creates the control; called once per login, when the dashboard is built
     */
    public void addHeaderWidget(Role role, Supplier<? extends Node> widget) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(widget, "widget");
        headerWidgets.computeIfAbsent(role, r -> new ArrayList<>()).add(widget);
    }

    /** The header widgets for a role, in the order they were added. */
    public List<Supplier<? extends Node>> getHeaderWidgets(Role role) {
        return List.copyOf(headerWidgets.getOrDefault(role, List.of()));
    }

    /** The home widgets for a role, in the order they were added. */
    public List<Supplier<? extends Node>> getHomeWidgets(Role role) {
        return List.copyOf(homeWidgets.getOrDefault(role, List.of()));
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
}
