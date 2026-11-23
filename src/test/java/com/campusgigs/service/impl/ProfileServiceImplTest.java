package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.FreelancerProfileDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final long FREELANCER_ID = 4;
    private static final long OTHER_FREELANCER_ID = 5;
    private static final long CLIENT_ID = 3;

    @Mock UserDao userDao;
    @Mock FreelancerProfileDao profileDao;
    @Mock SkillDao skillDao;

    private int transactionsStarted;
    private ProfileServiceImpl service;

    private final TransactionManager countingTransactions = new TransactionManager() {
        @Override
        public <T> T call(Supplier<T> work) {
            transactionsStarted++;
            return work.get();
        }

        @Override
        public void run(Runnable work) {
            transactionsStarted++;
            work.run();
        }
    };

    @BeforeEach
    void setUp() {
        stubUser(FREELANCER_ID, Role.FREELANCER);
        stubUser(OTHER_FREELANCER_ID, Role.FREELANCER);
        stubUser(CLIENT_ID, Role.CLIENT);
        service = new ProfileServiceImpl(userDao, profileDao, skillDao, countingTransactions);
    }

    private void stubUser(long id, Role role) {
        lenient().when(userDao.findById(id)).thenReturn(Optional.of(new User(id, role, "User " + id,
                "user" + id + "@uni.edu.pk", "pbkdf2$x", AccountStatus.ACTIVE, LocalDateTime.of(2026, 11, 1, 9, 0))));
    }

    private void stubSkillsExist(long... ids) {
        for (long id : ids) {
            when(skillDao.findById(id)).thenReturn(Optional.of(new Skill(id, "Skill " + id)));
        }
    }

    // ---- getProfile ----

    @Test
    void firstAccessCreatesEmptyProfileRow() {
        when(profileDao.findByUserId(FREELANCER_ID)).thenReturn(Optional.empty());

        FreelancerProfileDto profile = service.getProfile(FREELANCER_ID);

        verify(profileDao).insert(FREELANCER_ID, "", "");
        assertEquals(new FreelancerProfileDto(FREELANCER_ID, "", "", List.of()), profile);
    }

    @Test
    void existingProfileIsReturnedWithSkills() {
        when(profileDao.findByUserId(FREELANCER_ID)).thenReturn(Optional.of(
                new FreelancerProfile(1, FREELANCER_ID, "I teach maths", null)));
        when(profileDao.findSkills(FREELANCER_ID)).thenReturn(List.of(new Skill(13, "Math Tutoring")));

        FreelancerProfileDto profile = service.getProfile(FREELANCER_ID);

        assertEquals(new FreelancerProfileDto(FREELANCER_ID, "I teach maths", "",
                List.of(new SkillDto(13, "Math Tutoring"))), profile);
        verify(profileDao, never()).insert(anyLong(), anyString(), anyString());
    }

    @Test
    void onlyFreelancersHaveAProfile() {
        assertThrows(ValidationException.class, () -> service.getProfile(CLIENT_ID));
        when(userDao.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(99));
    }

    // ---- updateProfile ----

    @Test
    void ownerUpdatesProfileAndSkillsInOneTransaction() {
        stubSkillsExist(1, 2);
        when(profileDao.findByUserId(FREELANCER_ID)).thenReturn(Optional.of(
                new FreelancerProfile(1, FREELANCER_ID, "", "")));

        service.updateProfile(FREELANCER_ID, FREELANCER_ID, "  Java dev ", "github.com/sara", List.of(1L, 2L, 1L));

        verify(profileDao).update(FREELANCER_ID, "Java dev", "github.com/sara");
        verify(profileDao).replaceSkills(FREELANCER_ID, List.of(1L, 2L));
        assertEquals(1, transactionsStarted);
    }

    @Test
    void updateCreatesTheRowWhenMissing() {
        when(profileDao.findByUserId(FREELANCER_ID)).thenReturn(Optional.empty());

        service.updateProfile(FREELANCER_ID, FREELANCER_ID, "Bio", null, null);

        verify(profileDao).insert(FREELANCER_ID, "Bio", "");
        verify(profileDao).replaceSkills(FREELANCER_ID, List.of());
    }

    @Test
    void onlyTheOwnerCanUpdate() {
        assertThrows(ValidationException.class,
                () -> service.updateProfile(OTHER_FREELANCER_ID, FREELANCER_ID, "Bio", "", List.of()));
        verifyNothingSaved();
    }

    @Test
    void clientCannotHaveAProfile() {
        assertThrows(ValidationException.class,
                () -> service.updateProfile(CLIENT_ID, CLIENT_ID, "Bio", "", List.of()));
        verifyNothingSaved();
    }

    @Test
    void enforcesTextLimits() {
        assertThrows(ValidationException.class, () -> service.updateProfile(FREELANCER_ID, FREELANCER_ID,
                "x".repeat(1001), "", List.of()));
        assertThrows(ValidationException.class, () -> service.updateProfile(FREELANCER_ID, FREELANCER_ID,
                "", "x".repeat(501), List.of()));
        verifyNothingSaved();
    }

    @Test
    void acceptsTextAtTheLimits() {
        when(profileDao.findByUserId(FREELANCER_ID)).thenReturn(Optional.empty());

        service.updateProfile(FREELANCER_ID, FREELANCER_ID, "x".repeat(1000), "y".repeat(500), List.of());

        verify(profileDao).insert(FREELANCER_ID, "x".repeat(1000), "y".repeat(500));
    }

    @Test
    void atMostTenSkills() {
        List<Long> eleven = new ArrayList<>(LongStream.rangeClosed(1, 11).boxed().toList());

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.updateProfile(FREELANCER_ID, FREELANCER_ID, "", "", eleven));

        assertEquals("You can choose at most 10 skills", e.getMessage());
        verifyNothingSaved();
    }

    @Test
    void unknownSkillIsRejected() {
        when(skillDao.findById(404)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.updateProfile(FREELANCER_ID, FREELANCER_ID, "", "", List.of(404L)));
        assertThrows(ValidationException.class,
                () -> service.updateProfile(FREELANCER_ID, FREELANCER_ID, "", "", Arrays.asList((Long) null)));
        verifyNothingSaved();
    }

    @Test
    void listsAvailableSkills() {
        when(skillDao.findAll()).thenReturn(List.of(new Skill(1, "Java"), new Skill(2, "SQL")));

        assertEquals(List.of(new SkillDto(1, "Java"), new SkillDto(2, "SQL")), service.getAvailableSkills());
    }

    private void verifyNothingSaved() {
        verify(profileDao, never()).insert(anyLong(), anyString(), anyString());
        verify(profileDao, never()).update(anyLong(), anyString(), anyString());
        verify(profileDao, never()).replaceSkills(anyLong(), anyList());
    }
}
