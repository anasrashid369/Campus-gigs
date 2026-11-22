package com.campusgigs.service;

import java.util.Map;

/**
 * Platform settings stored in {@code PlatformSettings}. Reads never fail because of
 * a missing row: the agreed default is returned instead.
 */
public interface SettingsService {

    /** Tokens a new client or freelancer wallet starts with. Default 1000. */
    String KEY_INITIAL_TOKEN_BALANCE = "initial_token_balance";

    /** Percent of a payout kept by the platform. Default 5. */
    String KEY_PLATFORM_FEE_PERCENT = "platform_fee_percent";

    /** Open applications one freelancer may have at a time. Default 5. */
    String KEY_MAX_APPLICATIONS_PER_FREELANCER = "max_applications_per_freelancer";

    /** Returns the starting balance for a new wallet, in tokens. */
    long getInitialTokenBalance();

    /** Returns the platform fee as a whole percent (0 to 50). */
    int getPlatformFeePercent();

    /** Returns how many open applications a freelancer may have. */
    int getMaxApplicationsPerFreelancer();

    /**
     * Returns every setting as key to value, with defaults filled in for agreed keys
     * that have no row yet. The map cannot be modified.
     */
    Map<String, String> getAll();

    /**
     * Changes one setting.
     *
     * @param actingAdminId the user making the change; must be an ADMIN
     * @param key           one of the {@code KEY_*} constants
     * @param value         a whole number inside the allowed range for the key
     * @throws com.campusgigs.exception.ValidationException if the caller is not an admin,
     *         the key is unknown, or the value is not a whole number in range
     */
    void update(long actingAdminId, String key, String value);
}
