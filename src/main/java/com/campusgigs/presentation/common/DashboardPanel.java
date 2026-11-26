package com.campusgigs.presentation.common;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.util.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Layout shared by the three role dashboards: a bar with the user's name, role and a
 * Logout button; a navigation column on the left; and the selected screen on the right.
 * The navigation buttons come from {@link MainFrame#getNavigationItems}, so team members
 * add screens there instead of editing the dashboards.
 */
public abstract class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String HOME = "home";
    private static final int NAV_WIDTH = 190;

    private final CardLayout screens = new CardLayout();
    private final JPanel screenArea = new JPanel(screens);
    private final Set<String> createdScreens = new HashSet<>();
    private final JLabel userLabel = new JLabel();

    protected DashboardPanel(UserDto user, MainFrame frame, String welcomeText) {
        super(new BorderLayout());
        setBackground(UiConstants.BACKGROUND);
        add(createUserBar(frame, user.role()), BorderLayout.NORTH);
        add(createNavigation(frame.getNavigationItems(user.role())), BorderLayout.WEST);
        screenArea.setBackground(UiConstants.BACKGROUND);
        screenArea.add(createWelcome(user, welcomeText, frame.getHomeWidgets(user.role())), HOME);
        add(screenArea, BorderLayout.CENTER);
        showUser(user);
    }

    /** Re-reads the user's name from the session, for example after a profile change. */
    public void refreshHeader() {
        showUser(SessionManager.getInstance().getCurrentUser());
    }

    private void showUser(UserDto user) {
        userLabel.setText(user.fullName() + "  |  " + roleName(user));
    }

    private JPanel createUserBar(MainFrame frame, Role role) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiConstants.SURFACE);
        bar.setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_MEDIUM, UiConstants.SPACING_LARGE,
                UiConstants.SPACING_MEDIUM, UiConstants.SPACING_LARGE));
        userLabel.setFont(UiConstants.BODY_FONT);
        userLabel.setForeground(UiConstants.TEXT);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> frame.endSession());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiConstants.SPACING_MEDIUM, 0));
        right.setOpaque(false);
        for (Supplier<JComponent> widget : frame.getHeaderWidgets(role)) {
            right.add(widget.get());
        }
        right.add(logout);
        bar.add(userLabel, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createNavigation(List<NavigationItem> items) {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(UiConstants.PRIMARY_DARK);
        nav.setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_LARGE, UiConstants.SPACING_MEDIUM,
                UiConstants.SPACING_LARGE, UiConstants.SPACING_MEDIUM));
        nav.setPreferredSize(new Dimension(NAV_WIDTH, 0));
        addNavButton(nav, "Home", () -> screens.show(screenArea, HOME));
        for (int i = 0; i < items.size(); i++) {
            NavigationItem item = items.get(i);
            String key = "screen-" + i;
            addNavButton(nav, item.label(), () -> open(key, item));
        }
        return nav;
    }

    private static void addNavButton(JPanel nav, String text, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.addActionListener(e -> action.run());
        nav.add(button);
        nav.add(Box.createVerticalStrut(UiConstants.SPACING_MEDIUM));
    }

    /** Creates the screen on first use, then shows it. */
    private void open(String key, NavigationItem item) {
        if (createdScreens.add(key)) {
            screenArea.add(item.screen().get(), key);
        }
        screens.show(screenArea, key);
    }

    private static JPanel createWelcome(UserDto user, String welcomeText, List<Supplier<JPanel>> widgets) {
        JPanel welcome = new JPanel(new BorderLayout(0, UiConstants.SPACING_XLARGE));
        welcome.setBackground(UiConstants.BACKGROUND);
        welcome.setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_XLARGE, UiConstants.SPACING_XLARGE,
                UiConstants.SPACING_XLARGE, UiConstants.SPACING_XLARGE));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel hello = new JLabel("Welcome, " + user.fullName());
        hello.setFont(UiConstants.TITLE_FONT);
        hello.setForeground(UiConstants.PRIMARY);
        hello.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = new JLabel(welcomeText);
        hint.setFont(UiConstants.BODY_FONT);
        hint.setForeground(UiConstants.MUTED_TEXT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(hello);
        text.add(Box.createVerticalStrut(UiConstants.SPACING_MEDIUM));
        text.add(hint);
        welcome.add(text, BorderLayout.NORTH);

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setOpaque(false);
        for (Supplier<JPanel> widget : widgets) {
            summary.add(widget.get());
        }
        welcome.add(summary, BorderLayout.CENTER);
        return welcome;
    }

    private static String roleName(UserDto user) {
        String name = user.role().name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
