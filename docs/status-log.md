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
