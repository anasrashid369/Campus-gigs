-- Campus Gigs - schema (MySQL 8.0.16+, CHECK constraints are enforced).
-- Follows the ERD in docs/ (14 tables). Money is stored as whole tokens (BIGINT).
-- Never edit this file once shared: add numbered scripts under db/changes/.

CREATE DATABASE IF NOT EXISTS campus_gigs
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_gigs;

CREATE TABLE Users (
    user_id         BIGINT       NOT NULL AUTO_INCREMENT,
    role            ENUM('ADMIN', 'CLIENT', 'FREELANCER') NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    account_status  ENUM('ACTIVE', 'SUSPENDED', 'DEACTIVATED') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE FreelancerProfiles (
    profile_id       BIGINT NOT NULL AUTO_INCREMENT,
    user_id          BIGINT NOT NULL,
    biography        TEXT NULL,
    portfolio_links  TEXT NULL,
    PRIMARY KEY (profile_id),
    UNIQUE KEY uq_profiles_user (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES Users (user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Skills (
    skill_id  BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(80)  NOT NULL,
    PRIMARY KEY (skill_id),
    UNIQUE KEY uq_skills_name (name)
) ENGINE=InnoDB;

CREATE TABLE UserSkills (
    user_id   BIGINT NOT NULL,
    skill_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    CONSTRAINT fk_userskills_user  FOREIGN KEY (user_id)  REFERENCES Users (user_id)   ON DELETE CASCADE,
    CONSTRAINT fk_userskills_skill FOREIGN KEY (skill_id) REFERENCES Skills (skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE GigCategories (
    category_id  BIGINT      NOT NULL AUTO_INCREMENT,
    name         VARCHAR(80) NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB;

CREATE TABLE Gigs (
    gig_id                 BIGINT       NOT NULL AUTO_INCREMENT,
    client_id              BIGINT       NOT NULL,
    category_id            BIGINT       NOT NULL,
    assigned_freelancer_id BIGINT       NULL,
    title                  VARCHAR(150) NOT NULL,
    description            TEXT         NOT NULL,
    budget                 BIGINT       NOT NULL,
    deadline               DATE         NOT NULL,
    status                 ENUM('OPEN', 'ASSIGNED', 'SUBMITTED', 'APPROVED',
                                'DISPUTED', 'RESOLVED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (gig_id),
    KEY idx_gigs_client (client_id),
    KEY idx_gigs_category (category_id),
    KEY idx_gigs_freelancer (assigned_freelancer_id),
    KEY idx_gigs_status (status),
    CONSTRAINT fk_gigs_client     FOREIGN KEY (client_id)              REFERENCES Users (user_id),
    CONSTRAINT fk_gigs_category   FOREIGN KEY (category_id)            REFERENCES GigCategories (category_id),
    CONSTRAINT fk_gigs_freelancer FOREIGN KEY (assigned_freelancer_id) REFERENCES Users (user_id),
    CONSTRAINT chk_gigs_budget CHECK (budget > 0),
    CONSTRAINT chk_gigs_assigned CHECK (
        (status IN ('OPEN', 'CANCELLED')) OR assigned_freelancer_id IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE GigSkills (
    gig_id    BIGINT NOT NULL,
    skill_id  BIGINT NOT NULL,
    PRIMARY KEY (gig_id, skill_id),
    CONSTRAINT fk_gigskills_gig   FOREIGN KEY (gig_id)   REFERENCES Gigs (gig_id)     ON DELETE CASCADE,
    CONSTRAINT fk_gigskills_skill FOREIGN KEY (skill_id) REFERENCES Skills (skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE GigApplications (
    application_id    BIGINT    NOT NULL AUTO_INCREMENT,
    gig_id            BIGINT    NOT NULL,
    freelancer_id     BIGINT    NOT NULL,
    proposal_message  TEXT      NOT NULL,
    status            ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING',
    submitted_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id),
    UNIQUE KEY uq_application_gig_freelancer (gig_id, freelancer_id),
    KEY idx_applications_freelancer (freelancer_id),
    CONSTRAINT fk_applications_gig        FOREIGN KEY (gig_id)        REFERENCES Gigs (gig_id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_freelancer FOREIGN KEY (freelancer_id) REFERENCES Users (user_id)
) ENGINE=InnoDB;

CREATE TABLE Wallets (
    wallet_id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id            BIGINT NOT NULL,
    available_balance  BIGINT NOT NULL DEFAULT 0,
    escrow_balance     BIGINT NOT NULL DEFAULT 0,
    row_version        INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (wallet_id),
    UNIQUE KEY uq_wallets_user (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES Users (user_id),
    CONSTRAINT chk_wallets_available CHECK (available_balance >= 0),
    CONSTRAINT chk_wallets_escrow    CHECK (escrow_balance >= 0)
) ENGINE=InnoDB;

-- Ledger: one row per token movement, written in the same transaction as the wallet update.
-- PLATFORM_FEE rows are recorded against the wallet the fee was deducted from.
CREATE TABLE Transactions (
    transaction_id  BIGINT       NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT       NOT NULL,
    gig_id          BIGINT       NULL,
    type            ENUM('TOPUP', 'ESCROW_HOLD', 'ESCROW_RELEASE', 'ESCROW_REFUND',
                         'PLATFORM_FEE', 'WITHDRAWAL') NOT NULL,
    amount          BIGINT       NOT NULL,
    reference       VARCHAR(255) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    KEY idx_transactions_wallet (wallet_id, created_at),
    KEY idx_transactions_gig (gig_id),
    KEY idx_transactions_type (type),
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES Wallets (wallet_id),
    CONSTRAINT fk_transactions_gig    FOREIGN KEY (gig_id)    REFERENCES Gigs (gig_id),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0)
) ENGINE=InnoDB;

CREATE TABLE Reviews (
    review_id      BIGINT   NOT NULL AUTO_INCREMENT,
    gig_id         BIGINT   NOT NULL,
    client_id      BIGINT   NOT NULL,
    freelancer_id  BIGINT   NOT NULL,
    rating         TINYINT  NOT NULL,
    comment        TEXT     NULL,
    PRIMARY KEY (review_id),
    UNIQUE KEY uq_reviews_gig (gig_id),
    KEY idx_reviews_freelancer (freelancer_id),
    CONSTRAINT fk_reviews_gig        FOREIGN KEY (gig_id)        REFERENCES Gigs (gig_id),
    CONSTRAINT fk_reviews_client     FOREIGN KEY (client_id)     REFERENCES Users (user_id),
    CONSTRAINT fk_reviews_freelancer FOREIGN KEY (freelancer_id) REFERENCES Users (user_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE Disputes (
    dispute_id   BIGINT    NOT NULL AUTO_INCREMENT,
    gig_id       BIGINT    NOT NULL,
    raised_by    BIGINT    NOT NULL,
    resolved_by  BIGINT    NULL,
    reason       TEXT      NOT NULL,
    status       ENUM('OPEN', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
    decision     ENUM('RELEASE', 'REFUND') NULL,
    resolved_at  TIMESTAMP NULL,
    PRIMARY KEY (dispute_id),
    KEY idx_disputes_gig (gig_id),
    KEY idx_disputes_status (status),
    CONSTRAINT fk_disputes_gig         FOREIGN KEY (gig_id)      REFERENCES Gigs (gig_id),
    CONSTRAINT fk_disputes_raised_by   FOREIGN KEY (raised_by)   REFERENCES Users (user_id),
    CONSTRAINT fk_disputes_resolved_by FOREIGN KEY (resolved_by) REFERENCES Users (user_id),
    CONSTRAINT chk_disputes_resolution CHECK (
        (status = 'OPEN' AND decision IS NULL AND resolved_by IS NULL AND resolved_at IS NULL)
        OR (status = 'RESOLVED' AND decision IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE Notifications (
    notification_id  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    message          VARCHAR(500) NOT NULL,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    KEY idx_notifications_user (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES Users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE PlatformSettings (
    setting_key    VARCHAR(80)  NOT NULL,
    setting_value  VARCHAR(255) NOT NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB;
