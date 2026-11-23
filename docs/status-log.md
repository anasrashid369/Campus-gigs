# Status Log

One entry per finished task, newest at the bottom.
Format: date - member - phase - task - branch - result.

## Phase 1: Setup and Foundations

- 2026-11-21 - Anas - Phase 1 - B1 EventBus and event records (Observer) - `feature/anas-eventbus` - Done, 14 tests passing.
- 2026-11-21 - Anas - Phase 1 - B4 CsvExporter (RFC 4180, UTF-8 with BOM) - `feature/anas-csv-exporter` - Done, 12 tests passing.
- 2026-11-21 - Anas - Phase 1 - B5 README - `feature/anas-readme` - Done.
- 2026-11-21 - Anas - Phase 1 - B3 Seed data `db/02_seed.sql` - `feature/anas-seed-data` - Ready; waiting for Abdullah's PasswordHasher to confirm the admin hash before merging.
- 2026-11-21 - Anas - Phase 1 - B3 Seed data - merged into develop; admin hash verified with `PasswordHasher`.
- 2026-11-21 - Anas (covering for Abdullah, who is ill) - Phase 1 - A1 Swing shell: MainFrame, BasePanel, ErrorDialog, UiConstants, LoginFrame, Validators - `feature/abdullah-swing-shell` - Done, 15 tests passing. Abdullah to review when back.
- 2026-11-21 - Anas (covering for Abdullah, who is ill) - Phase 1 - A2 PasswordHasher - `feature/abdullah-password-hasher` - Done, 7 tests passing. Abdullah to review when back.

## Phase 2: Accounts, Wallets and Admin

- 2026-11-22 - Anas - Phase 2 - B2 SettingsService interface (merged first so Abdullah can start A3) - `feature/anas-settings-interface` - Done.
- 2026-11-22 - Anas - Phase 2 - B2 SettingsDao, SettingsDaoJdbc, SettingsServiceImpl (defaults, admin-only update, range checks) - `feature/anas-settings-service` - Done, 24 service tests passing, 3 DAO tests need MySQL. Admin check switches to Abdullah's `UserDao.findById` when A3 lands.
- 2026-11-23 - Anas (covering for Abdullah, who is ill) - Phase 2 - A3 UserDao, AuthService (register/login), UserService, SessionManager - `feature/abdullah-auth-service` - Done, 27 unit tests passing, 3 DAO tests need MySQL. Abdullah to review when back.
- 2026-11-23 - Anas - Phase 2 - B2 follow-up: SettingsService admin check now uses `UserDao.findById` - `feature/anas-settings-admin-check` - Done.
- 2026-11-23 - Anas (covering for Abdullah, who is ill) - Phase 2 - A4 Login/register screens, role dashboards via DashboardFactory, navigation list in MainFrame, profile screen (F1 UI, F2, F3) - `feature/abdullah-dashboards` - Done, factory and MainFrame tests passing; screens checked by rendering them. Needs MySQL for the end-to-end check.
- 2026-11-23 - Anas - Phase 2 - B6 categories and skills (F8) + B7 user accounts (F7): CategoryDao, SkillDao, AdminService, CategoriesPanel, SkillsPanel, UsersPanel - `feature/anas-admin-management` - Done, 19 service tests passing, 5 DAO tests need MySQL. Skills in use cannot be deleted (checked in one DELETE because the foreign keys cascade).
- 2026-11-23 - Anas - Phase 2 - B8 freelancer profile (F21): FreelancerProfileDao, ProfileService, FreelancerProfilePanel - `feature/anas-freelancer-profile` - Done, 12 service tests passing, 2 DAO tests need MySQL.
- 2026-11-23 - Anas - Phase 2 - B9 settings screen (F12) and menu entries for all admin and freelancer screens - `feature/anas-settings-screen` - Done. Anas's Phase 2 code complete; Phase 2 exit check needs MySQL and Huzaifa's wallet screens.
