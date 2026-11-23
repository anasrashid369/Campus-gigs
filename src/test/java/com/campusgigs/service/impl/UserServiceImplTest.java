package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final User ALI = new User(3, Role.CLIENT, "Ali Raza", "ali@uni.edu.pk", "pbkdf2$x",
            AccountStatus.ACTIVE, LocalDateTime.of(2026, 11, 1, 9, 0));

    @Mock UserDao userDao;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userDao);
    }

    @Test
    void getUserMapsToDto() {
        when(userDao.findById(3)).thenReturn(Optional.of(ALI));

        assertEquals(new UserDto(3, Role.CLIENT, "Ali Raza", "ali@uni.edu.pk", AccountStatus.ACTIVE),
                service.getUser(3));
    }

    @Test
    void unknownUserIsNotFound() {
        when(userDao.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUser(99));
        assertThrows(ResourceNotFoundException.class, () -> service.updateProfile(99, "Someone"));
        verify(userDao, never()).updateName(anyLong(), anyString());
    }

    @Test
    void updateProfileSavesTrimmedName() {
        when(userDao.findById(3)).thenReturn(Optional.of(ALI));

        UserDto updated = service.updateProfile(3, "  Ali Hassan ");

        verify(userDao).updateName(3, "Ali Hassan");
        assertEquals("Ali Hassan", updated.fullName());
    }

    @Test
    void updateProfileRejectsBlankOrLongName() {
        assertThrows(ValidationException.class, () -> service.updateProfile(3, "   "));
        assertThrows(ValidationException.class, () -> service.updateProfile(3, null));
        assertThrows(ValidationException.class, () -> service.updateProfile(3, "x".repeat(101)));
        verify(userDao, never()).updateName(anyLong(), anyString());
    }
}
