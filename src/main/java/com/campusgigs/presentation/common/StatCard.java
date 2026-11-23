package com.campusgigs.presentation.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** A white card with a small caption and one big figure, used for balances and totals. */
public class StatCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String NO_VALUE = "-";

    private final JLabel value = new JLabel(NO_VALUE);

    public StatCard(String caption) {
        super(new BorderLayout(0, UiConstants.SPACING_SMALL));
        setBackground(UiConstants.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_LARGE, UiConstants.SPACING_LARGE,
                UiConstants.SPACING_LARGE, UiConstants.SPACING_LARGE));
        JLabel title = new JLabel(caption);
        title.setFont(UiConstants.BODY_FONT);
        title.setForeground(UiConstants.MUTED_TEXT);
        value.setFont(UiConstants.TITLE_FONT);
        value.setForeground(UiConstants.PRIMARY);
        add(title, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    /** Shows a token amount with thousands separators. */
    public void setTokens(long amount) {
        value.setText(TokenFormat.tokens(amount));
    }

    /** The text currently shown as the figure. */
    public String getValueText() {
        return value.getText();
    }
}
