package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigCategory;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    private static final long ADMIN_ID = 1;
    private static final long OTHER_ADMIN_ID = 2;
    private static final long CLIENT_ID = 3;
    private static final long FREELANCER_ID = 4;

    @Mock UserDao userDao;
    @Mock CategoryDao categoryDao;
    @Mock SkillDao skillDao;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        stubUser(ADMIN_ID, Role.ADMIN);
        stubUser(OTHER_ADMIN_ID, Role.ADMIN);
        stubUser(CLIENT_ID, Role.CLIENT);
        stubUser(FREELANCER_ID, Role.FREELANCER);
        service = new AdminServiceImpl(userDao, categoryDao, skillDao);
    }

    private void stubUser(long id, Role role) {
        lenient().when(userDao.findById(id)).thenReturn(Optional.of(user(id, role)));
    }

    private static User user(long id, Role role) {
        return new User(id, role, "User " + id, "user" + id + "@uni.edu.pk", "pbkdf2$x",
                AccountStatus.ACTIVE, LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    // ---- role check ----

    @Test
    void nonAdminIsRejectedEverywhere() {
        assertThrows(ValidationException.class, () -> service.listCategories(CLIENT_ID));
        assertThrows(ValidationException.class, () -> service.addCategory(CLIENT_ID, "Music"));
        assertThrows(ValidationException.class, () -> service.renameCategory(CLIENT_ID, 1, "Music"));
        assertThrows(ValidationException.class, () -> service.setCategoryActive(CLIENT_ID, 1, false));
        assertThrows(ValidationException.class, () -> service.listSkills(FREELANCER_ID));
        assertThrows(ValidationException.class, () -> service.addSkill(FREELANCER_ID, "Rust"));
        assertThrows(ValidationException.class, () -> service.renameSkill(FREELANCER_ID, 1, "Rust"));
        assertThrows(ValidationException.class, () -> service.deleteSkill(FREELANCER_ID, 1));
        assertThrows(ValidationException.class, () -> service.listUsers(CLIENT_ID));
        assertThrows(ValidationException.class,
                () -> service.setAccountStatus(CLIENT_ID, FREELANCER_ID, AccountStatus.SUSPENDED));
        verifyNoInteractions(categoryDao, skillDao);
        verify(userDao, never()).updateAccountStatus(anyLong(), any());
    }

    @Test
    void unknownCallerIsRejected() {
        when(userDao.findById(99)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.listCategories(99));
    }

    // ---- categories ----

    @Test
    void listsCategories() {
        when(categoryDao.findAll()).thenReturn(List.of(new GigCategory(1, "Tutoring", true),
                new GigCategory(2, "Writing", false)));

        assertEquals(List.of(new CategoryDto(1, "Tutoring", true), new CategoryDto(2, "Writing", false)),
                service.listCategories(ADMIN_ID));
    }

    @Test
    void addsTrimmedCategory() {
        when(categoryDao.findByName("Music Lessons")).thenReturn(Optional.empty());
        when(categoryDao.insert("Music Lessons")).thenReturn(9L);

        assertEquals(new CategoryDto(9, "Music Lessons", true), service.addCategory(ADMIN_ID, "  Music Lessons "));
    }

    @Test
    void rejectsDuplicateCategoryName() {
        when(categoryDao.findByName("tutoring")).thenReturn(Optional.of(new GigCategory(1, "Tutoring", true)));

        assertThrows(ValidationException.class, () -> service.addCategory(ADMIN_ID, "tutoring"));
        verify(categoryDao, never()).insert(anyString());
    }

    @Test
    void rejectsBlankOrLongNames() {
        assertThrows(ValidationException.class, () -> service.addCategory(ADMIN_ID, "   "));
        assertThrows(ValidationException.class, () -> service.addCategory(ADMIN_ID, null));
        assertThrows(ValidationException.class, () -> service.addSkill(ADMIN_ID, "x".repeat(81)));
        verify(categoryDao, never()).insert(anyString());
        verify(skillDao, never()).insert(anyString());
    }

    @Test
    void renameCategoryAllowsChangingCaseOfItsOwnName() {
        when(categoryDao.findById(1)).thenReturn(Optional.of(new GigCategory(1, "tutoring", true)));
        when(categoryDao.findByName("Tutoring")).thenReturn(Optional.of(new GigCategory(1, "tutoring", true)));

        assertEquals(new CategoryDto(1, "Tutoring", true), service.renameCategory(ADMIN_ID, 1, "Tutoring"));
        verify(categoryDao).rename(1, "Tutoring");
    }

    @Test
    void renameCategoryRejectsNameOfAnotherCategory() {
        when(categoryDao.findById(1)).thenReturn(Optional.of(new GigCategory(1, "Tutoring", true)));
        when(categoryDao.findByName("Writing")).thenReturn(Optional.of(new GigCategory(2, "Writing", true)));

        assertThrows(ValidationException.class, () -> service.renameCategory(ADMIN_ID, 1, "Writing"));
        verify(categoryDao, never()).rename(anyLong(), anyString());
    }

    @Test
    void togglesCategory() {
        when(categoryDao.findById(1)).thenReturn(Optional.of(new GigCategory(1, "Tutoring", true)));

        assertEquals(new CategoryDto(1, "Tutoring", false), service.setCategoryActive(ADMIN_ID, 1, false));
        verify(categoryDao).setActive(1, false);
    }

    @Test
    void missingCategoryIsNotFound() {
        when(categoryDao.findById(42)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.setCategoryActive(ADMIN_ID, 42, true));
        verify(categoryDao, never()).setActive(anyLong(), anyBoolean());
    }

    // ---- skills ----

    @Test
    void addsAndRenamesSkill() {
        when(skillDao.findByName("Rust")).thenReturn(Optional.empty());
        when(skillDao.insert("Rust")).thenReturn(30L);
        when(skillDao.findById(30)).thenReturn(Optional.of(new Skill(30, "Rust")));
        when(skillDao.findByName("Rust Programming")).thenReturn(Optional.empty());

        assertEquals(new SkillDto(30, "Rust"), service.addSkill(ADMIN_ID, "Rust"));
        assertEquals(new SkillDto(30, "Rust Programming"), service.renameSkill(ADMIN_ID, 30, "Rust Programming"));
        verify(skillDao).rename(30, "Rust Programming");
    }

    @Test
    void rejectsDuplicateSkillName() {
        when(skillDao.findByName("java")).thenReturn(Optional.of(new Skill(1, "Java")));

        assertThrows(ValidationException.class, () -> service.addSkill(ADMIN_ID, "java"));
    }

    @Test
    void deletesUnusedSkill() {
        when(skillDao.findById(5)).thenReturn(Optional.of(new Skill(5, "LaTeX")));
        when(skillDao.deleteIfUnused(5)).thenReturn(true);

        service.deleteSkill(ADMIN_ID, 5);

        verify(skillDao).deleteIfUnused(5);
    }

    @Test
    void skillInUseCannotBeDeleted() {
        when(skillDao.findById(1)).thenReturn(Optional.of(new Skill(1, "Java")));
        when(skillDao.deleteIfUnused(1)).thenReturn(false);

        ValidationException e = assertThrows(ValidationException.class, () -> service.deleteSkill(ADMIN_ID, 1));

        assertEquals("\"Java\" is used by a freelancer profile or a gig and cannot be deleted", e.getMessage());
    }

    @Test
    void missingSkillIsNotFound() {
        when(skillDao.findById(77)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteSkill(ADMIN_ID, 77));
        verify(skillDao, never()).deleteIfUnused(anyLong());
    }

    // ---- users ----

    @Test
    void listsUsersWithoutHashes() {
        when(userDao.findAll()).thenReturn(List.of(user(ADMIN_ID, Role.ADMIN), user(CLIENT_ID, Role.CLIENT)));

        List<UserDto> users = service.listUsers(ADMIN_ID);

        assertEquals(2, users.size());
        assertEquals(new UserDto(CLIENT_ID, Role.CLIENT, "User 3", "user3@uni.edu.pk", AccountStatus.ACTIVE),
                users.get(1));
    }

    @Test
    void suspendsAFreelancer() {
        service.setAccountStatus(ADMIN_ID, FREELANCER_ID, AccountStatus.SUSPENDED);

        verify(userDao).updateAccountStatus(FREELANCER_ID, AccountStatus.SUSPENDED);
    }

    @Test
    void adminCannotChangeOwnStatus() {
        assertThrows(ValidationException.class,
                () -> service.setAccountStatus(ADMIN_ID, ADMIN_ID, AccountStatus.DEACTIVATED));
        verify(userDao, never()).updateAccountStatus(anyLong(), any());
    }

    @Test
    void anotherAdminsStatusCannotBeChanged() {
        assertThrows(ValidationException.class,
                () -> service.setAccountStatus(ADMIN_ID, OTHER_ADMIN_ID, AccountStatus.SUSPENDED));
        verify(userDao, never()).updateAccountStatus(anyLong(), any());
    }

    @Test
    void unknownUserOrMissingStatus() {
        when(userDao.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.setAccountStatus(ADMIN_ID, 99, AccountStatus.ACTIVE));
        assertThrows(ValidationException.class, () -> service.setAccountStatus(ADMIN_ID, CLIENT_ID, null));
        verify(userDao, never()).updateAccountStatus(anyLong(), any());
    }
}
