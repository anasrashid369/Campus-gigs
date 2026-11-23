package com.campusgigs.service.impl;

import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.User;
import com.campusgigs.service.UserService;

import java.util.Objects;

/** Default {@link UserService}. */
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
    }

    @Override
    public UserDto getUser(long id) {
        return UserMapper.toDto(find(id));
    }

    @Override
    public UserDto updateProfile(long userId, String fullName) {
        String name = fullName == null ? "" : fullName.trim();
        if (name.isEmpty()) {
            throw new ValidationException("Full name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Full name can be at most " + MAX_NAME_LENGTH + " characters");
        }
        User user = find(userId);
        userDao.updateName(userId, name);
        return new UserDto(user.userId(), user.role(), name, user.email(), user.accountStatus());
    }

    private User find(long id) {
        return userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
