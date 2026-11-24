package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigRequest;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigCategory;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.pattern.state.GigStateMachine;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GigServiceImplTest {

    private static final long CLIENT = 1;
    private static final long OTHER_CLIENT = 2;
    private static final long FREELANCER = 3;
    private static final long ADMIN = 4;
    private static final long GIG = 10;
    private static final long CATEGORY = 7;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Mock GigDao gigDao;
    @Mock CategoryDao categoryDao;
    @Mock SkillDao skillDao;
    @Mock UserDao userDao;

    private GigServiceImpl service;

    private final TransactionManager passThrough = new TransactionManager() {
        @Override
        public <T> T call(Supplier<T> work) {
            return work.get();
        }

        @Override
        public void run(Runnable work) {
            work.run();
        }
    };

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
        service = new GigServiceImpl(gigDao, categoryDao, skillDao, userDao, new GigStateMachine(), passThrough, clock);
        lenient().when(userDao.findById(CLIENT)).thenReturn(Optional.of(user(CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(OTHER_CLIENT)).thenReturn(Optional.of(user(OTHER_CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(ADMIN)).thenReturn(Optional.of(user(ADMIN, Role.ADMIN, AccountStatus.ACTIVE)));
        lenient().when(categoryDao.findById(CATEGORY)).thenReturn(Optional.of(new GigCategory(CATEGORY, "Design", true)));
    }

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, LocalDateTime.now());
    }

    private static GigRequest request() {
        return new GigRequest("Logo design", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of());
    }

    private static Gig gig(GigStatus status, Long freelancerId) {
        return new Gig(GIG, CLIENT, CATEGORY, freelancerId, "Logo design", "Design a simple logo for a club", 300,
                TODAY, status, LocalDateTime.now());
    }

    private void storedGig(GigStatus status, Long freelancerId) {
        lenient().when(gigDao.findById(GIG)).thenReturn(Optional.of(gig(status, freelancerId)));
        lenient().when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(gig(status, freelancerId)));
    }

    // ---- createGig

    @Test
    void createStoresTheGigWithItsSkillsAndReturnsIt() {
        when(skillDao.findById(5)).thenReturn(Optional.of(new Skill(5, "Photoshop")));
        when(gigDao.insert(CLIENT, CATEGORY, "Logo design", "Design a simple logo for a club", 300, TODAY)).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        GigDto result = service.createGig(CLIENT, new GigRequest("  Logo design ", " Design a simple logo for a club ",
                CATEGORY, 300, TODAY, List.of(5L)));

        assertEquals(GIG, result.gigId());
        assertEquals(GigStatus.OPEN, result.status());
        verify(gigDao).replaceSkills(GIG, List.of(5L));
    }

    @Test
    void createRejectsABadTitle() {
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("ab", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("   ", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("x".repeat(101), "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of())));
        verify(gigDao, never()).insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void createAcceptsTheTitleBoundaries() {
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, new GigRequest("abc", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of()));
        service.createGig(CLIENT, new GigRequest("x".repeat(100), "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of()));
    }

    @Test
    void createRejectsABadDescription() {
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "too short", CATEGORY, 300, TODAY, List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "x".repeat(2001), CATEGORY, 300, TODAY, List.of())));
    }

    @Test
    void createAcceptsTheDescriptionBoundaries() {
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, new GigRequest("Logo", "x".repeat(10), CATEGORY, 300, TODAY, List.of()));
        service.createGig(CLIENT, new GigRequest("Logo", "x".repeat(2000), CATEGORY, 300, TODAY, List.of()));
    }

    @Test
    void createRejectsABudgetOutsideTheRange() {
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 0, TODAY, List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 100_001, TODAY, List.of())));
    }

    @Test
    void createAcceptsTheBudgetBoundaries() {
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 1, TODAY, List.of()));
        service.createGig(CLIENT, new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 100_000, TODAY, List.of()));
    }

    @Test
    void createAcceptsADeadlineTodayButNotYesterday() {
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of()));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT, new GigRequest("Logo",
                "Design a simple logo for a club", CATEGORY, 300, TODAY.minusDays(1), List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT, new GigRequest("Logo",
                "Design a simple logo for a club", CATEGORY, 300, null, List.of())));
    }

    @Test
    void createRejectsAnUnknownOrInactiveCategory() {
        when(categoryDao.findById(99)).thenReturn(Optional.empty());
        when(categoryDao.findById(8)).thenReturn(Optional.of(new GigCategory(8, "Old", false)));

        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", 99, 300, TODAY, List.of())));
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", 8, 300, TODAY, List.of())));
    }

    @Test
    void createRejectsTooManyOrUnknownSkills() {
        List<Long> eleven = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 300, TODAY, eleven)));

        when(skillDao.findById(42)).thenReturn(Optional.empty());
        assertThrows(ValidationException.class, () -> service.createGig(CLIENT,
                new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of(42L))));
        verify(gigDao, never()).insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void createAcceptsTenSkillsAndIgnoresDuplicates() {
        for (long id = 1; id <= 10; id++) {
            lenient().when(skillDao.findById(id)).thenReturn(Optional.of(new Skill(id, "Skill " + id)));
        }
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, new GigRequest("Logo", "Design a simple logo for a club", CATEGORY, 300, TODAY,
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 10L, 1L)));

        verify(gigDao).replaceSkills(GIG, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
    }

    @Test
    void onlyAnActiveClientCanPost() {
        when(userDao.findById(5)).thenReturn(Optional.of(user(5, Role.CLIENT, AccountStatus.SUSPENDED)));
        when(userDao.findById(6)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.createGig(FREELANCER, request()));
        assertThrows(ValidationException.class, () -> service.createGig(ADMIN, request()));
        assertThrows(ValidationException.class, () -> service.createGig(5, request()));
        assertThrows(ResourceNotFoundException.class, () -> service.createGig(6, request()));
        verifyNoInteractions(gigDao);
    }

    // ---- updateGig

    @Test
    void theOwnerCanEditAnOpenGig() {
        storedGig(GigStatus.OPEN, null);

        GigDto result = service.updateGig(CLIENT, GIG, request());

        assertEquals(GigStatus.OPEN, result.status());
        verify(gigDao).update(GIG, CATEGORY, "Logo design", "Design a simple logo for a club", 300, TODAY);
        verify(gigDao).replaceSkills(GIG, List.of());
    }

    @Test
    void onlyTheOwnerCanEdit() {
        storedGig(GigStatus.OPEN, null);

        assertThrows(ValidationException.class, () -> service.updateGig(OTHER_CLIENT, GIG, request()));

        verify(gigDao, never()).update(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void aGigThatIsNotOpenCannotBeEdited() {
        storedGig(GigStatus.ASSIGNED, FREELANCER);

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.updateGig(CLIENT, GIG, request()));

        assertEquals("Only open gigs can be edited.", error.getMessage());
        verify(gigDao, never()).update(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    void editingAnUnknownGigIsReported() {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateGig(CLIENT, GIG, request()));
    }

    @Test
    void editingValidatesTheRequestBeforeTouchingTheGig() {
        assertThrows(ValidationException.class, () -> service.updateGig(CLIENT, GIG,
                new GigRequest("ab", "Design a simple logo for a club", CATEGORY, 300, TODAY, List.of())));

        verifyNoInteractions(gigDao);
    }

    // ---- cancelGig

    @Test
    void theOwnerCanCancelAnOpenGig() {
        storedGig(GigStatus.OPEN, null);

        GigDto result = service.cancelGig(CLIENT, GIG);

        assertEquals(GigStatus.CANCELLED, result.status());
        verify(gigDao).updateStatus(GIG, GigStatus.CANCELLED);
    }

    @Test
    void aGigThatIsNotOpenCannotBeCancelled() {
        storedGig(GigStatus.ASSIGNED, FREELANCER);

        assertThrows(InvalidGigTransitionException.class, () -> service.cancelGig(CLIENT, GIG));

        verify(gigDao, never()).updateStatus(anyLong(), any());
    }

    @Test
    void onlyTheOwnerCanCancel() {
        storedGig(GigStatus.OPEN, null);

        assertThrows(ValidationException.class, () -> service.cancelGig(OTHER_CLIENT, GIG));
        assertThrows(ValidationException.class, () -> service.cancelGig(FREELANCER, GIG));

        verify(gigDao, never()).updateStatus(anyLong(), any());
    }

    // ---- getGig

    @Test
    void anyoneCanReadAnOpenGig() {
        storedGig(GigStatus.OPEN, null);

        assertEquals(GIG, service.getGig(FREELANCER, GIG).gigId());
    }

    @Test
    void aGigInProgressIsVisibleOnlyToItsPeopleAndAdmins() {
        storedGig(GigStatus.ASSIGNED, FREELANCER);
        when(userDao.findById(9)).thenReturn(Optional.of(user(9, Role.FREELANCER, AccountStatus.ACTIVE)));

        assertEquals(GIG, service.getGig(CLIENT, GIG).gigId());
        assertEquals(GIG, service.getGig(FREELANCER, GIG).gigId());
        assertEquals(GIG, service.getGig(ADMIN, GIG).gigId());
        assertThrows(ValidationException.class, () -> service.getGig(9, GIG));
        assertThrows(ValidationException.class, () -> service.getGig(OTHER_CLIENT, GIG));
    }

    @Test
    void readingAnUnknownGigIsReported() {
        when(gigDao.findById(GIG)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGig(CLIENT, GIG));
    }

    // ---- lists and helpers

    @Test
    void clientsSeeTheirOwnGigsAndFreelancersTheirHiredOnes() {
        when(gigDao.findByClientId(CLIENT)).thenReturn(List.of(gig(GigStatus.OPEN, null)));
        when(gigDao.findByFreelancerId(FREELANCER)).thenReturn(List.of(gig(GigStatus.ASSIGNED, FREELANCER)));

        assertEquals(1, service.listClientGigs(CLIENT).size());
        assertEquals(1, service.listFreelancerGigs(FREELANCER).size());
        assertThrows(ValidationException.class, () -> service.listClientGigs(FREELANCER));
        assertThrows(ValidationException.class, () -> service.listFreelancerGigs(CLIENT));
    }

    @Test
    void listsOpenGigs() {
        when(gigDao.findByStatus(GigStatus.OPEN)).thenReturn(List.of(gig(GigStatus.OPEN, null)));

        List<GigDto> open = service.listOpenGigs();

        assertEquals(1, open.size());
        assertEquals(GigStatus.OPEN, open.get(0).status());
    }

    @Test
    void mapsGigSkillsAndActiveCategories() {
        when(gigDao.findSkills(GIG)).thenReturn(List.of(new Skill(5, "Photoshop")));
        when(categoryDao.findAll()).thenReturn(List.of(new GigCategory(1, "Design", true), new GigCategory(2, "Old", false)));

        assertEquals(List.of(new SkillDto(5, "Photoshop")), service.getGigSkills(GIG));
        assertEquals(List.of(new CategoryDto(1, "Design", true)), service.getActiveCategories());
    }

    @Test
    void allowedActionsComeFromTheStateMachine() {
        assertEquals(EnumSet.of(GigAction.HIRE, GigAction.CANCEL), service.allowedActions(GigStatus.OPEN));
        assertTrue(service.allowedActions(GigStatus.APPROVED).isEmpty());
    }

    @Test
    void noSkillsMeansNoSkillLookups() {
        when(gigDao.insert(anyLong(), anyLong(), anyString(), anyString(), anyLong(), any())).thenReturn(GIG);
        storedGig(GigStatus.OPEN, null);

        service.createGig(CLIENT, request());

        verifyNoInteractions(skillDao);
        verify(gigDao).replaceSkills(org.mockito.ArgumentMatchers.eq(GIG), anyList());
    }
}
