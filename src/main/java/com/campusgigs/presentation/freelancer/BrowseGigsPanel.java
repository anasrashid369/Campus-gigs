package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;
import com.campusgigs.dto.GigSort;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.TablePanel;
import com.campusgigs.presentation.common.TextPrompt;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigSearchService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Find Gigs (F22, F23): search the open gigs by word, category and budget, order them, read the
 * details and apply. The filter is read on screen; the rules (who may search, budget limits) are
 * checked by {@link GigSearchService}, and the application rules by {@link ApplicationService}.
 */
public class BrowseGigsPanel extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final CategoryDto ALL_CATEGORIES = new CategoryDto(0, "All categories", true);

    private final transient GigSearchService searchService;
    private final transient GigService gigService;
    private final transient ApplicationService applicationService;

    private final JTextField keywordField = new JTextField(14);
    private final JComboBox<CategoryDto> categoryBox = new JComboBox<>();
    private final JTextField minField = new JTextField(6);
    private final JTextField maxField = new JTextField(6);
    private final JComboBox<GigSort> sortBox = new JComboBox<>(GigSort.values());
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final Results results;

    public BrowseGigsPanel(GigSearchService searchService, GigService gigService,
                           ApplicationService applicationService) {
        this.searchService = searchService;
        this.gigService = gigService;
        this.applicationService = applicationService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_MEDIUM));

        categoryBox.addItem(ALL_CATEGORIES);
        categoryBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                         boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value instanceof CategoryDto category ? category.name() : value, index, selected, focus);
            }
        });
        results = new Results(this::viewDetails, this::apply, this::search);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(createFilterBar());
        top.add(messageLabel);
        add(top, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);

        loadCategories();
        search();
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Search:"));
        bar.add(keywordField);
        bar.add(categoryBox);
        bar.add(new JLabel("Budget"));
        bar.add(minField);
        bar.add(new JLabel("to"));
        bar.add(maxField);
        bar.add(sortBox);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clear());
        bar.add(searchButton);
        bar.add(clearButton);
        return bar;
    }

    /**
     * Turns what the freelancer typed into a filter.
     *
     * @throws ValidationException with a message for the user if a budget limit is not a whole number
     */
    static GigFilter parseFilter(String keyword, CategoryDto category, String minText, String maxText, GigSort sort) {
        Long categoryId = category == null || category.categoryId() == ALL_CATEGORIES.categoryId()
                ? null : category.categoryId();
        return new GigFilter(keyword, categoryId, parseLimit(minText), parseLimit(maxText), sort);
    }

    private static Long parseLimit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Enter the budget limits as whole numbers of tokens.");
        }
    }

    private void loadCategories() {
        runInBackground(gigService::getActiveCategories,
                categories -> categories.forEach(categoryBox::addItem));
    }

    private void search() {
        GigFilter filter;
        try {
            filter = parseFilter(keywordField.getText(), (CategoryDto) categoryBox.getSelectedItem(),
                    minField.getText(), maxField.getText(), (GigSort) sortBox.getSelectedItem());
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        long freelancerId = currentUserId();
        runInBackground(() -> searchService.search(freelancerId, filter), found -> {
            results.show(found);
            messageLabel.setForeground(UiConstants.MUTED_TEXT);
            messageLabel.setText(found.size() + (found.size() == 1 ? " gig found" : " gigs found"));
        }, error -> FormLayout.showFailure(this, messageLabel, error));
    }

    private void clear() {
        keywordField.setText("");
        categoryBox.setSelectedIndex(0);
        minField.setText("");
        maxField.setText("");
        sortBox.setSelectedItem(GigSort.NEWEST);
        search();
    }

    private void viewDetails() {
        results.selectedGig().ifPresent(gig ->
                runInBackground(() -> gigService.getGigSkills(gig.gigId()), skills -> showDetails(gig, skills)));
    }

    private void showDetails(GigDto gig, List<SkillDto> skills) {
        JTextArea description = new JTextArea(gig.description(), 8, 40);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new JLabel("Budget: " + TokenFormat.tokens(gig.budget()) + " tokens    Deadline: " + gig.deadline()));
        content.add(new JLabel("Skills: " + (skills.isEmpty() ? "none asked"
                : skills.stream().map(SkillDto::name).collect(Collectors.joining(", ")))));
        content.add(new JScrollPane(description));
        JOptionPane.showMessageDialog(this, content, gig.title(), JOptionPane.PLAIN_MESSAGE);
    }

    private void apply() {
        results.selectedGig().ifPresent(gig -> {
            Optional<String> proposal = TextPrompt.ask(this, "Apply for \"" + gig.title() + "\"",
                    "Why are you a good fit? (" + ApplicationService.MIN_PROPOSAL_LENGTH + " - "
                            + ApplicationService.MAX_PROPOSAL_LENGTH + " characters)",
                    ApplicationService.MIN_PROPOSAL_LENGTH, ApplicationService.MAX_PROPOSAL_LENGTH);
            proposal.ifPresent(text -> {
                long freelancerId = currentUserId();
                runInBackground(() -> applicationService.apply(freelancerId, gig.gigId(), text), sent -> {
                    JOptionPane.showMessageDialog(this, "Your application was sent.", "Application sent",
                            JOptionPane.INFORMATION_MESSAGE);
                    search();
                });
            });
        });
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** The table of results with its buttons. */
    private static final class Results extends TablePanel<GigDto> {

        private static final long serialVersionUID = 1L;

        Results(Runnable viewDetails, Runnable apply, Runnable refresh) {
            super("Open gigs", List.of("Title", "Budget", "Deadline"), gig -> toCells(gig));
            addAction("View details", true, viewDetails);
            addAction("Apply", true, apply);
            addAction("Refresh", false, refresh);
        }

        void show(List<GigDto> gigs) {
            setRows(gigs);
        }

        Optional<GigDto> selectedGig() {
            return selected();
        }
    }

    /** The cell values of one gig, in column order. */
    static Object[] toCells(GigDto gig) {
        return new Object[] {gig.title(), TokenFormat.tokens(gig.budget()), gig.deadline()};
    }
}
