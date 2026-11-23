package com.campusgigs.presentation.common;

import com.campusgigs.model.TransactionType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Turns token amounts, dates and ledger types into the text shown on the wallet screens. */
public final class TokenFormat {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TokenFormat() {
    }

    /** An amount with thousands separators, for example {@code 1,250}. */
    public static String tokens(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    /** A timestamp as {@code yyyy-MM-dd HH:mm}. */
    public static String dateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    /** A readable name for a ledger entry type. */
    public static String typeLabel(TransactionType type) {
        return switch (type) {
            case TOPUP -> "Top-up";
            case ESCROW_HOLD -> "Escrow hold";
            case ESCROW_RELEASE -> "Escrow release";
            case ESCROW_REFUND -> "Escrow refund";
            case PLATFORM_FEE -> "Platform fee";
            case WITHDRAWAL -> "Withdrawal";
        };
    }
}
