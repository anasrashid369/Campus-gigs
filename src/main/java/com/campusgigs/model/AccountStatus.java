package com.campusgigs.model;

/** Account state. Names match Users.account_status; only ACTIVE users can log in. */
public enum AccountStatus {
    ACTIVE, SUSPENDED, DEACTIVATED
}
