package com.campusgigs.presentation.freelancer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;
import com.campusgigs.dto.GigSort;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigSearchService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrowseGigsPanelTest {

    @Mock GigSearchService searchService;
    @Mock GigService gigService;
    @Mock ApplicationService applicationService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void parsesTheFilterTheFreelancerTyped() {
        GigFilter filter = BrowseGigsPanel.parseFilter("  logo ", new CategoryDto(7, "Design", true), " 100 ", "900",
                GigSort.BUDGET_HIGH);

        assertEquals("logo", filter.keyword());
        assertEquals(7L, filter.categoryId());
        assertEquals(100L, filter.minBudget());
        assertEquals(900L, filter.maxBudget());
        assertEquals(GigSort.BUDGET_HIGH, filter.sort());
    }

    @Test
    void blankFieldsAndTheAllCategoriesEntryMeanNoCondition() {
        GigFilter filter = BrowseGigsPanel.parseFilter("   ", new CategoryDto(0, "All categories", true), "", "  ", null);

        assertNull(filter.keyword());
        assertNull(filter.categoryId());
        assertNull(filter.minBudget());
        assertNull(filter.maxBudget());
        assertEquals(GigSort.NEWEST, filter.sort());
    }

    @Test
    void budgetLimitsThatAreNotWholeNumbersGiveAClearMessage() {
        for (String bad : new String[] {"abc", "12.5", "1e3", "-"}) {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> BrowseGigsPanel.parseFilter("", null, bad, "", GigSort.NEWEST));
            assertEquals("Enter the budget limits as whole numbers of tokens.", error.getMessage());
        }
    }

    @Test
    void showsTitleBudgetAndDeadline() {
        GigDto gig = new GigDto(10, 1, 7, null, "Logo design", "Description", 1500, LocalDate.of(2026, 12, 31),
                GigStatus.OPEN, LocalDateTime.now());

        assertArrayEquals(new Object[] {"Logo design", "1,500", LocalDate.of(2026, 12, 31)},
                BrowseGigsPanel.toCells(gig));
    }

    @Test
    void loadsTheCategoriesAndSearchesWithNoConditionsOnOpening() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(3, Role.FREELANCER, "Free", "f@example.com", AccountStatus.ACTIVE));
        lenient().when(searchService.search(3, GigFilter.none())).thenReturn(List.of());
        lenient().when(gigService.getActiveCategories()).thenReturn(List.of());

        new BrowseGigsPanel(searchService, gigService, applicationService);

        verify(gigService, timeout(3000)).getActiveCategories();
        verify(searchService, timeout(3000)).search(3, GigFilter.none());
    }
}
