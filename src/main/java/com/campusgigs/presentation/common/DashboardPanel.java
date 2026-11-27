package com.campusgigs.presentation.common;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.util.SessionManager;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Layout shared by the three role dashboards: a navigation column on the left, a bar with the
 * user's name, role and a Logout button at the top, and the selected screen in the middle.
 * The navigation buttons come from {@link MainFrame#getNavigationItems}, so team members
 * add screens there instead of editing the dashboards.
 */
public abstract class DashboardPanel extends BorderPane {

    private static final String HOME = "home";
    private static final String ACTIVE = "active";

    private final StackPane screenArea = new StackPane();
    private final Map<String, Node> createdScreens = new HashMap<>();
    private final Map<String, Button> navButtons = new HashMap<>();
    private final Label userName = new Label();
    private final Label roleChip = new Label();

    protected DashboardPanel(UserDto user, MainFrame frame, String welcomeText) {
        getStyleClass().add("dashboard");
        setLeft(createNavigation(frame.getNavigationItems(user.role())));
        BorderPane main = new BorderPane();
        main.setTop(createUserBar(frame, user.role()));
        main.setCenter(screenArea);
        setCenter(main);
        createdScreens.put(HOME, createWelcome(user, welcomeText, frame.getHomeWidgets(user.role())));
        open(HOME, null);
        showUser(user);
    }

    /** Re-reads the user's name from the session, for example after a profile change. */
    public void refreshHeader() {
        showUser(SessionManager.getInstance().getCurrentUser());
    }

    private void showUser(UserDto user) {
        userName.setText(user.fullName());
        String role = user.role().name();
        roleChip.setText(role.charAt(0) + role.substring(1).toLowerCase(Locale.ROOT));
    }

    private HBox createUserBar(MainFrame frame, Role role) {
        userName.getStyleClass().add("user-name");
        roleChip.getStyleClass().add("role-chip");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(userName, roleChip, spacer);
        bar.getStyleClass().add("top-bar");
        for (Supplier<? extends Node> widget : frame.getHeaderWidgets(role)) {
            bar.getChildren().add(widget.get());
        }
        Button logout = new Button("Logout");
        logout.setOnAction(event -> frame.endSession());
        bar.getChildren().add(logout);
        return bar;
    }

    private VBox createNavigation(List<NavigationItem> items) {
        Label brand = new Label(UiConstants.APP_TITLE);
        brand.getStyleClass().add("brand");
        VBox nav = new VBox(brand);
        nav.getStyleClass().add("sidebar");
        nav.setMinWidth(Region.USE_PREF_SIZE);
        addNavButton(nav, HOME, "Home", null);
        for (int i = 0; i < items.size(); i++) {
            NavigationItem item = items.get(i);
            addNavButton(nav, "screen-" + i, item.label(), item.screen());
        }
        return nav;
    }

    private void addNavButton(VBox nav, String key, String text, Supplier<? extends Node> screen) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> open(key, screen));
        navButtons.put(key, button);
        nav.getChildren().add(button);
    }

    /** Creates the screen on first use, then shows it and marks its button. */
    private void open(String key, Supplier<? extends Node> screen) {
        Node node = createdScreens.computeIfAbsent(key, k -> screen.get());
        screenArea.getChildren().setAll(node);
        navButtons.forEach((k, button) -> {
            button.getStyleClass().remove(ACTIVE);
            if (k.equals(key)) {
                button.getStyleClass().add(ACTIVE);
            }
        });
    }

    private static Node createWelcome(UserDto user, String welcomeText, List<Supplier<? extends Node>> widgets) {
        Label hello = new Label("Welcome, " + user.fullName());
        hello.getStyleClass().add("welcome-title");
        Label hint = new Label(welcomeText);
        hint.getStyleClass().add("muted");
        VBox welcome = new VBox(UiConstants.SPACING_MEDIUM, hello, hint);
        welcome.setPadding(new Insets(UiConstants.SPACING_XLARGE + 8));
        VBox summary = new VBox(UiConstants.SPACING_LARGE);
        VBox.setMargin(summary, new Insets(UiConstants.SPACING_XLARGE, 0, 0, 0));
        for (Supplier<? extends Node> widget : widgets) {
            summary.getChildren().add(widget.get());
        }
        welcome.getChildren().add(summary);
        ScrollPane scroll = new ScrollPane(welcome);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("form-scroll");
        return scroll;
    }
}
