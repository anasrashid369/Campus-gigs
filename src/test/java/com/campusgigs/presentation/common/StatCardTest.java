package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campusgigs.presentation.FxTestSupport;
import org.junit.jupiter.api.Test;

class StatCardTest {

    @Test
    void showsADashUntilAFigureIsSet() {
        FxTestSupport.startToolkit();
        assertEquals("-", new StatCard("Available").getValueText());
    }

    @Test
    void showsTokenAmountsWithThousandsSeparators() {
        FxTestSupport.startToolkit();
        StatCard card = new StatCard("Available");

        card.setTokens(1250);

        assertEquals("1,250", card.getValueText());
    }
}
