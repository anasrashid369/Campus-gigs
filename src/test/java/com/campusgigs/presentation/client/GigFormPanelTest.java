package com.campusgigs.presentation.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigRequest;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.GigService;
import com.campusgigs.service.ProfileService;
import com.campusgigs.util.SessionManager;
import com.campusgigs.presentation.FxTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GigFormPanelTest {

    private static final CategoryDto DESIGN = new CategoryDto(7, "Design", true);

    @Mock GigService gigService;
    @Mock ProfileService profileService;

    @BeforeEach
    void logIn() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(1, Role.CLIENT, "Client", "c@example.com", AccountStatus.ACTIVE));
        lenient().when(gigService.getActiveCategories()).thenReturn(List.of(DESIGN));
        lenient().when(profileService.getAvailableSkills()).thenReturn(List.of(new SkillDto(5, "Photoshop")));
    }

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void parsesWhatTheClientTyped() {
        GigRequest request = GigFormPanel.parseRequest("Logo", "Design a logo", DESIGN, " 450 ", " 2026-12-31 ",
                List.of(new SkillDto(5, "Photoshop"), new SkillDto(6, "Canva")));

        assertEquals("Logo", request.title());
        assertEquals(7, request.categoryId());
        assertEquals(450, request.budget());
        assertEquals(LocalDate.of(2026, 12, 31), request.deadline());
        assertEquals(List.of(5L, 6L), request.skillIds());
    }

    @Test
    void anUnreadableBudgetGivesAClearMessage() {
        for (String bad : new String[] {"", "abc", "12.5", "1e3"}) {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> GigFormPanel.parseRequest("Logo", "Design a logo", DESIGN, bad, "2026-12-31", List.of()));
            assertEquals("Enter the budget as a whole number of tokens.", error.getMessage());
        }
    }

    @Test
    void anUnreadableDeadlineGivesAClearMessage() {
        for (String bad : new String[] {"", "31/12/2026", "2026-13-40", "tomorrow"}) {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> GigFormPanel.parseRequest("Logo", "Design a logo", DESIGN, "100", bad, List.of()));
            assertTrue(error.getMessage().startsWith("Enter the deadline as yyyy-MM-dd"));
        }
    }

    @Test
    void aMissingCategoryGivesAClearMessage() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> GigFormPanel.parseRequest("Logo", "Design a logo", null, "100", "2026-12-31", List.of()));

        assertEquals("Please choose a category.", error.getMessage());
    }

    @Test
    void theCreateFormLoadsCategoriesAndSkills() {
        FxTestSupport.startToolkit();
        new GigFormPanel(gigService, profileService);

        verify(gigService, timeout(3000)).getActiveCategories();
        verify(profileService, timeout(3000)).getAvailableSkills();
    }

    @Test
    void theEditFormAlsoLoadsTheSkillsOfTheGig() {
        FxTestSupport.startToolkit();
        GigDto gig = new GigDto(10, 1, 7, null, "Logo", "Design a logo", 300, LocalDate.of(2026, 12, 31),
                GigStatus.OPEN, LocalDateTime.now());

        new GigFormPanel(gigService, profileService, gig, () -> { });

        verify(gigService, timeout(3000)).getGigSkills(10);
    }
}
