package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Row of the Users table. */
@Entity
@Table(name = "Users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserEntity() {
    }

    /** A new ACTIVE user; the database fills in the id and creation time. */
    public UserEntity(Role role, String fullName, String email, String passwordHash) {
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public long getUserId() {
        return userId;
    }

    public User toModel() {
        return new User(userId, role, fullName, email, passwordHash, accountStatus, createdAt);
    }
}
