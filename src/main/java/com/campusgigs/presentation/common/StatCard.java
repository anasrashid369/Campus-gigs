package com.campusgigs.presentation.common;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** A white card with a small caption and one big figure, used for balances and totals. */
public class StatCard extends VBox {

    private static final String NO_VALUE = "-";

    private final Label value = new Label(NO_VALUE);

    public StatCard(String caption) {
        getStyleClass().add("stat-card");
        Label title = new Label(caption);
        title.getStyleClass().add("stat-caption");
        value.getStyleClass().add("stat-value");
        getChildren().addAll(title, value);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    /** Shows a token amount with thousands separators. */
    public void setTokens(long amount) {
        value.setText(TokenFormat.tokens(amount));
    }

    /** The text currently shown as the figure. */
    public String getValueText() {
        return value.getText();
    }

    /** A row of cards sharing the width equally. */
    public static HBox row(StatCard... cards) {
        return new HBox(UiConstants.SPACING_LARGE, cards);
    }
}
