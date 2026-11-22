package com.campusgigs.service.impl;

import static com.campusgigs.service.SettingsService.KEY_INITIAL_TOKEN_BALANCE;
import static com.campusgigs.service.SettingsService.KEY_MAX_APPLICATIONS_PER_FREELANCER;
import static com.campusgigs.service.SettingsService.KEY_PLATFORM_FEE_PERCENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.SettingsDao;
import com.campusgigs.exception.ValidationException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsServiceImplTest {

    private static final long ADMIN_ID = 1;
    private static final long CLIENT_ID = 2;

    @Mock SettingsDao settingsDao;

    private SettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SettingsServiceImpl(settingsDao, userId -> userId == ADMIN_ID);
    }

    @Test
    void usesDefaultsWhenRowsAreMissing() {
        when(settingsDao.find(anyString())).thenReturn(Optional.empty());

        assertEquals(1000, service.getInitialTokenBalance());
        assertEquals(5, service.getPlatformFeePercent());
        assertEquals(5, service.getMaxApplicationsPerFreelancer());
    }

    @Test
    void returnsStoredValues() {
        when(settingsDao.find(KEY_INITIAL_TOKEN_BALANCE)).thenReturn(Optional.of("2500"));
        when(settingsDao.find(KEY_PLATFORM_FEE_PERCENT)).thenReturn(Optional.of("10"));
        when(settingsDao.find(KEY_MAX_APPLICATIONS_PER_FREELANCER)).thenReturn(Optional.of("3"));

        assertEquals(2500, service.getInitialTokenBalance());
        assertEquals(10, service.getPlatformFeePercent());
        assertEquals(3, service.getMaxApplicationsPerFreelancer());
    }

    @Test
    void fallsBackToDefaultWhenStoredValueIsBroken() {
        when(settingsDao.find(KEY_INITIAL_TOKEN_BALANCE)).thenReturn(Optional.of("lots"));
        when(settingsDao.find(KEY_PLATFORM_FEE_PERCENT)).thenReturn(Optional.of("90"));

        assertEquals(1000, service.getInitialTokenBalance());
        assertEquals(5, service.getPlatformFeePercent());
    }

    @Test
    void getAllFillsDefaultsAndKeepsStoredValues() {
        when(settingsDao.findAll()).thenReturn(Map.of(KEY_PLATFORM_FEE_PERCENT, "8"));

        Map<String, String> all = service.getAll();

        assertEquals(Map.of(KEY_INITIAL_TOKEN_BALANCE, "1000", KEY_PLATFORM_FEE_PERCENT, "8",
                KEY_MAX_APPLICATIONS_PER_FREELANCER, "5"), all);
        assertThrows(UnsupportedOperationException.class, () -> all.put("x", "y"));
    }

    @Test
    void adminUpdateStoresTrimmedNumber() {
        service.update(ADMIN_ID, KEY_PLATFORM_FEE_PERCENT, " 7 ");

        verify(settingsDao).upsert(KEY_PLATFORM_FEE_PERCENT, "7");
    }

    @ParameterizedTest
    @CsvSource({
            "initial_token_balance, 0",
            "initial_token_balance, 100000",
            "platform_fee_percent, 0",
            "platform_fee_percent, 50",
            "max_applications_per_freelancer, 1",
            "max_applications_per_freelancer, 50"})
    void acceptsRangeBoundaries(String key, String value) {
        service.update(ADMIN_ID, key, value);

        verify(settingsDao).upsert(key, value);
    }

    @Test
    void nonAdminCannotUpdate() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(CLIENT_ID, KEY_PLATFORM_FEE_PERCENT, "7"));

        assertEquals("Only an admin can change platform settings", e.getMessage());
        verify(settingsDao, never()).upsert(anyString(), anyString());
    }

    @Test
    void rejectsUnknownOrMissingKey() {
        assertThrows(ValidationException.class, () -> service.update(ADMIN_ID, "theme", "dark"));
        assertThrows(ValidationException.class, () -> service.update(ADMIN_ID, null, "5"));
        verify(settingsDao, never()).upsert(anyString(), anyString());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "initial_token_balance, -1",
            "initial_token_balance, 100001",
            "platform_fee_percent, 51",
            "platform_fee_percent, -5",
            "max_applications_per_freelancer, 0",
            "max_applications_per_freelancer, 51",
            "platform_fee_percent, 7.5",
            "platform_fee_percent, abc",
            "platform_fee_percent, ''",
            "platform_fee_percent, NULL"}, nullValues = "NULL")
    void rejectsInvalidValues(String key, String value) {
        assertThrows(ValidationException.class, () -> service.update(ADMIN_ID, key, value));
        verify(settingsDao, never()).upsert(anyString(), anyString());
    }

    @Test
    void rangeMessageNamesTheSettingAndLimits() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(ADMIN_ID, KEY_PLATFORM_FEE_PERCENT, "60"));

        assertEquals("Platform fee percent must be between 0 and 50", e.getMessage());
    }
}
