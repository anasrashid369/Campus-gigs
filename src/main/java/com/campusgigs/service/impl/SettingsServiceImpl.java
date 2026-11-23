package com.campusgigs.service.impl;

import com.campusgigs.dao.SettingsDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.Role;
import com.campusgigs.service.SettingsService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Default {@link SettingsService}. Missing or unreadable rows fall back to the agreed
 * defaults, so a fresh database still works before the seed has run.
 */
public class SettingsServiceImpl implements SettingsService {

    private static final Logger LOG = Logger.getLogger(SettingsServiceImpl.class.getName());

    /** Label shown in messages, allowed range and default for one setting. */
    private record Rule(String label, long min, long max, long defaultValue) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            KEY_INITIAL_TOKEN_BALANCE, new Rule("Initial token balance", 0, 100_000, 1000),
            KEY_PLATFORM_FEE_PERCENT, new Rule("Platform fee percent", 0, 50, 5),
            KEY_MAX_APPLICATIONS_PER_FREELANCER, new Rule("Max applications per freelancer", 1, 50, 5));

    private static final List<String> KEYS_IN_ORDER = List.of(KEY_INITIAL_TOKEN_BALANCE,
            KEY_PLATFORM_FEE_PERCENT, KEY_MAX_APPLICATIONS_PER_FREELANCER);

    private final SettingsDao settingsDao;
    private final UserDao userDao;

    public SettingsServiceImpl(SettingsDao settingsDao, UserDao userDao) {
        this.settingsDao = Objects.requireNonNull(settingsDao, "settingsDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
    }

    @Override
    public long getInitialTokenBalance() {
        return read(KEY_INITIAL_TOKEN_BALANCE);
    }

    @Override
    public int getPlatformFeePercent() {
        return (int) read(KEY_PLATFORM_FEE_PERCENT);
    }

    @Override
    public int getMaxApplicationsPerFreelancer() {
        return (int) read(KEY_MAX_APPLICATIONS_PER_FREELANCER);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> all = new LinkedHashMap<>();
        for (String key : KEYS_IN_ORDER) {
            all.put(key, String.valueOf(RULES.get(key).defaultValue()));
        }
        all.putAll(settingsDao.findAll());
        return Collections.unmodifiableMap(all);
    }

    @Override
    public void update(long actingAdminId, String key, String value) {
        requireAdmin(actingAdminId);
        Rule rule = key == null ? null : RULES.get(key);
        if (rule == null) {
            throw new ValidationException("Unknown setting: " + key);
        }
        if (value == null || value.isBlank()) {
            throw new ValidationException(rule.label() + " is required");
        }
        long number;
        try {
            number = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(rule.label() + " must be a whole number");
        }
        if (number < rule.min() || number > rule.max()) {
            throw new ValidationException(rule.label() + " must be between "
                    + rule.min() + " and " + rule.max());
        }
        settingsDao.upsert(key, String.valueOf(number));
    }

    private void requireAdmin(long userId) {
        boolean admin = userDao.findById(userId).map(user -> user.role() == Role.ADMIN).orElse(false);
        if (!admin) {
            throw new ValidationException("Only an admin can change platform settings");
        }
    }

    /** Stored value when it is a valid number in range, otherwise the default. */
    private long read(String key) {
        Rule rule = RULES.get(key);
        String stored = settingsDao.find(key).orElse(null);
        if (stored == null) {
            return rule.defaultValue();
        }
        try {
            long number = Long.parseLong(stored.trim());
            if (number >= rule.min() && number <= rule.max()) {
                return number;
            }
        } catch (NumberFormatException e) {
            // fall through to the default below
        }
        LOG.warning(() -> "Ignoring invalid value for setting " + key + "; using default "
                + rule.defaultValue());
        return rule.defaultValue();
    }
}
