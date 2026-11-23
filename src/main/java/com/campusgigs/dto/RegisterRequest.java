package com.campusgigs.dto;

import com.campusgigs.model.Role;

/** What the register screen sends to {@code AuthService.register}. */
public record RegisterRequest(String fullName, String email, String password, Role role) {

    /** Hides the password so it can never end up in a log. */
    @Override
    public String toString() {
        return "RegisterRequest[fullName=" + fullName + ", email=" + email + ", role=" + role + "]";
    }
}
