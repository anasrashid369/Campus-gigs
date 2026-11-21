package com.campusgigs.presentation;

import com.campusgigs.presentation.common.UiConstants;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Main application window: a header at the top and a content area that shows
 * one screen (card) at a time. Screens are registered once with
 * {@link #addCard(String, JPanel)} and switched with {@link #showCard(String)}.
 * Build and use it on the Event Dispatch Thread only.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<String> cardNames = new HashSet<>();

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
        if (!cardNames.add(name)) {
            throw new IllegalArgumentException("A card named '" + name + "' is already registered");
        }
        content.add(panel, name);
    }

    /**
     * Brings the named screen to the front.
     *
     * @throws IllegalArgumentException if no card with this name was added
     */
    public void showCard(String name) {
        if (!cardNames.contains(name)) {
            throw new IllegalArgumentException("No card named '" + name + "'");
        }
        cards.show(content, name);
    }

    /** Header placeholder; the logged-in user and navigation are added with the dashboards. */
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
