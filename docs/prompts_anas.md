# Muhammad Anas - step-by-step tasks, Claude Code prompts and git commands

Read `docs/team_setup.md` first and finish its section 1 (setup). You own: notifications, EventBus (**Observer** pattern), admin tools (users, categories, skills, settings), reviews, freelancer profile, reports and CSV export, seed data, README and the user guide.

Every task has: a START block (create the branch), a prompt to paste into Claude Code, a "done when" check, and a FINISH block (verify, commit, merge, clean up). Do the tasks in order and post the summary in the group chat after each one.

Dependencies:
- Abdullah needs your `SettingsService` **interface** early (task B2). Merge it first, even before the full implementation if you can.
- Your admin seed row needs Abdullah's `PasswordHasher` (his task A2). Do B3 only after he tells you it is merged.
- Your F7 admin user list uses Abdullah's `UserDao` (`findAll`, `updateAccountStatus`, `findById`). Wait for his task A3.
- Huzaifa's schema is fixed. The settings table is `PlatformSettings(setting_key, setting_value, updated_at)`.

Files you must NOT edit: `db/01_schema.sql`, wallet / transaction / payment classes, `TransactionHelper`, `model/` (ask Huzaifa if a model field is missing), and Abdullah's auth and presentation-shell classes.

---

## Task B1 - EventBus and event types (Observer, Phase 1)

Branch: `feature/anas-eventbus`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-eventbus
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: the Observer infrastructure.

Create under com.campusgigs/pattern/observer:
- AppEvent: a sealed interface (or abstract class) for events.
- One immutable record per event with exactly these names: ApplicationSubmitted, ApplicationWithdrawn,
  FreelancerHired, WorkSubmitted, WorkApproved, RevisionRequested, DisputeRaised, DisputeResolved,
  WalletToppedUp, ReviewPosted. Each carries ids needed to notify the right user (for example gigId,
  clientId, freelancerId, applicationId, amount) - keep fields to the minimum.
- EventListener<E extends AppEvent> functional interface: void onEvent(E event).
- EventBus (singleton plus a public constructor for tests): <E extends AppEvent> void subscribe(Class<E> type,
  EventListener<E> listener); void publish(AppEvent event). Thread-safe (CopyOnWriteArrayList / ConcurrentHashMap).
  A listener that throws must not stop other listeners or the publisher: catch, and record the problem through
  java.util.logging; never rethrow.
Write JUnit tests: listeners receive only their event type, multiple listeners all run, a throwing listener does
not block the others, publish with no subscribers does nothing.
Only touch pattern/observer/** and its tests. Show a plan first and wait for approval.
```

Done when: tests pass. Post the event class names and fields in the group chat. Others will only call `EventBus.getInstance().publish(...)`.

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(observer): add EventBus and event types"
git push -u origin feature/anas-eventbus
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-eventbus -m "merge: feature/anas-eventbus into develop"
mvn test && git push origin develop
git branch -d feature/anas-eventbus && git push origin --delete feature/anas-eventbus
```

---

## Task B2 - SettingsService (F12 base, Phase 1-2). Merge the interface FIRST.

Branch: `feature/anas-settings-service`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-settings-service
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: SettingsService (F12 base).

Table (already exists, do not change): PlatformSettings(setting_key VARCHAR PK, setting_value VARCHAR, updated_at).
Agreed keys and defaults: initial_token_balance=1000, platform_fee_percent=5, max_applications_per_freelancer=5.

Create:
- dao/SettingsDao (interface) + dao/jdbc/SettingsDaoJdbc extending BaseJdbcDao (PreparedStatement only):
  Optional<String> find(String key); Map<String,String> findAll(); void upsert(String key, String value)
  using INSERT ... ON DUPLICATE KEY UPDATE.
- service/SettingsService (interface with JavaDoc) - THIS EXACT API, others depend on it:
    long getInitialTokenBalance();
    int getPlatformFeePercent();
    int getMaxApplicationsPerFreelancer();
    Map<String,String> getAll();
    void update(long actingAdminId, String key, String value);
- service/impl/SettingsServiceImpl: reads from the DAO, falls back to the defaults above when a key is missing.
  update(): only an ADMIN may update (look the user up through UserDao.findById once Abdullah's UserDao exists; until
  then keep the role check behind a small private method and tell me), reject unknown keys, reject non-numeric or
  negative values, fee must be 0..50, initial balance 0..100000, application limit 1..50, all with ValidationException.
JUnit tests with a fake or mocked SettingsDao: defaults used when missing, stored values returned, invalid updates rejected.
Only touch dao/SettingsDao*, dao/jdbc/SettingsDaoJdbc, service/Settings*, service/impl/Settings* and tests.
Show a plan first and wait for approval.
```

Tip: if the full implementation takes longer, first merge only the `SettingsService` interface in its own commit and tell Abdullah, then continue.

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(settings): add SettingsService with defaults and validation (F12)"
git push -u origin feature/anas-settings-service
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-settings-service -m "merge: feature/anas-settings-service into develop"
mvn test && git push origin develop
git branch -d feature/anas-settings-service && git push origin --delete feature/anas-settings-service
```

---

## Task B3 - Seed data db/02_seed.sql (Phase 1). Wait for Abdullah's PasswordHasher.

Branch: `feature/anas-seed-data`

First generate the admin password hash. After Abdullah's `PasswordHasher` is on `develop`, ask Claude Code to write a tiny throw-away JUnit test or `jshell` snippet (not committed) that prints `PasswordHasher.hash("<an admin password you choose>")`. Copy the printed string; it looks like `pbkdf2$120000$...$...`. Never write the plain password in any file.

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-seed-data
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: db/02_seed.sql.

Write an idempotent script (safe to run twice) that starts with USE campus_gigs; and inserts:
- One ADMIN user: full_name 'Platform Admin', email 'admin@campusgigs.local', role ADMIN, account_status ACTIVE,
  password_hash '<PASTE THE HASH I GENERATED>'. Admin gets NO wallet.
- Gig categories (about 8): Web Development, Graphic Design, Writing and Translation, Tutoring, Video and Audio,
  Data Entry, Programming Help, Other. Use INSERT IGNORE on the unique name.
- Skills (about 20): Java, Python, JavaScript, SQL, HTML/CSS, Photoshop, Illustrator, Canva, Video Editing, Copywriting,
  Proofreading, Translation, Math Tutoring, Excel, Data Analysis, UI Design, Public Speaking, Research, Typing, LaTeX.
- PlatformSettings rows: initial_token_balance=1000, platform_fee_percent=5, max_applications_per_freelancer=5
  using INSERT ... ON DUPLICATE KEY UPDATE setting_value = setting_value.
Table and column names must match db/01_schema.sql exactly. Do not modify 01_schema.sql.
Then tell me the exact commands to load it and a query to verify the counts.
Only create db/02_seed.sql. Show a plan first.
```

Check: reset the database (command in `docs/team_setup.md`), run both scripts, then:
```bash
sudo mysql campus_gigs -e "SELECT COUNT(*) FROM GigCategories; SELECT COUNT(*) FROM Skills; SELECT setting_key, setting_value FROM PlatformSettings; SELECT email, role FROM Users;"
```
Run `02_seed.sql` a second time: it must not error and must not duplicate rows.

FINISH:
```bash
git add db/02_seed.sql && git status --short
git commit -m "feat(db): add seed data for admin, categories, skills and settings"
git push -u origin feature/anas-seed-data
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-seed-data -m "merge: feature/anas-seed-data into develop"
git push origin develop
git branch -d feature/anas-seed-data && git push origin --delete feature/anas-seed-data
```
Then tell the group to run `sudo mysql < db/02_seed.sql`.

---

## Task B4 - CSV export utility (Phase 1)

Branch: `feature/anas-csv-exporter`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-csv-exporter
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: util/CsvExporter, JDK only.

- static void write(Path file, List<String> headers, List<List<String>> rows) throws IOException, UTF-8 with a
  BOM so Excel opens it correctly, header row first.
- RFC 4180 escaping: a field containing a comma, a double quote or a line break is wrapped in double quotes and
  inner quotes are doubled. Null becomes an empty field.
- static String toCsv(List<String> headers, List<List<String>> rows) used by write() and by tests.
JUnit tests: plain rows, commas, quotes, line breaks, null, empty list, file written with the BOM (use @TempDir).
Only touch util/CsvExporter and its test. Show a plan first.
```

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(util): add CsvExporter with RFC 4180 escaping"
git push -u origin feature/anas-csv-exporter
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-csv-exporter -m "merge: feature/anas-csv-exporter into develop"
mvn test && git push origin develop
git branch -d feature/anas-csv-exporter && git push origin --delete feature/anas-csv-exporter
```

---

## Task B5 - README (Phase 1)

Branch: `feature/anas-readme`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-readme
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: write README.md at the repository root.
Sections: project summary (one paragraph), tech stack, prerequisites, setup steps (taken from docs/team_setup.md
section 1 and tested by following them on a clean database), how to run (mvn test, mvn -q compile exec:java),
folder structure with one line per package, layering rules summary, branch and commit rules summary, team and
ownership table, and a short troubleshooting list (config.properties missing, MySQL below 8.0.16, access denied).
Do not invent features that are not implemented yet. Only edit README.md. Show an outline first.
```

FINISH:
```bash
git add README.md && git status --short
git commit -m "docs(readme): add setup, run and team rules"
git push -u origin feature/anas-readme
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-readme -m "merge: feature/anas-readme into develop"
git push origin develop
git branch -d feature/anas-readme && git push origin --delete feature/anas-readme
```

Phase 1 exit check: each of you runs `01_schema.sql` and `02_seed.sql` locally, the app window opens, and `mvn test` connects to the database.

---

## Task B6 - Admin categories and skills (F8, Phase 2)

Branch: `feature/anas-admin-reference-data`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-admin-reference-data
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: admin management of categories and skills (F8).

Tables GigCategories(category_id, name UNIQUE, is_active) and Skills(skill_id, name UNIQUE) already exist.
Create (DAO interface + JDBC class extending BaseJdbcDao, DTO records, service interface + impl):
- CategoryDao/SkillDao: findAll, findById, insert(name), rename(id, name), setActive(id, boolean) for categories,
  delete for skills only when no UserSkills/GigSkills row references it (check in the service, or let the FK stop it
  and translate the failure to a ValidationException).
- AdminService (reference-data part): list, add, rename, activate/deactivate categories; list, add, rename, delete
  skills. EVERY method takes long actingAdminId and verifies the caller is an ADMIN using UserDao.findById (Abdullah's)
  - never rely on the UI. Reject blank or duplicate names with ValidationException.
- Swing: presentation/admin/CategoriesPanel and SkillsPanel (table + add / rename / toggle / delete buttons),
  using Abdullah's BasePanel for background DB calls. Register them in the admin navigation without editing the
  dashboards (use the MainFrame navigation list Abdullah created).
JUnit tests with Mockito: non-admin rejected, duplicate name rejected, happy paths.
Only touch: dao/*Category*, dao/*Skill*, dao/jdbc/*Category*, dao/jdbc/*Skill*, dto/*Category*, dto/*Skill*,
service/AdminService + impl, presentation/admin/CategoriesPanel + SkillsPanel, and tests.
Show a plan first and wait for approval. Do the service and tests first; do the screens last.
```

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(admin): manage categories and skills (F8)"
git push -u origin feature/anas-admin-reference-data
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-admin-reference-data -m "merge: feature/anas-admin-reference-data into develop"
mvn test && git push origin develop
git branch -d feature/anas-admin-reference-data && git push origin --delete feature/anas-admin-reference-data
```

---

## Task B7 - Admin user list: activate, suspend, deactivate (F7, Phase 2). Needs Abdullah's UserDao.

Branch: `feature/anas-admin-users`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-admin-users
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: admin user management (F7).

Use Abdullah's existing UserDao (findAll, findById, updateAccountStatus) and UserDto. Do not edit them.
Add to AdminService (same interface as the reference-data part): List<UserDto> listUsers(long actingAdminId);
void setAccountStatus(long actingAdminId, long userId, AccountStatus status).
Rules: caller must be an ADMIN (check through UserDao.findById); an admin cannot change their own status; an ADMIN
account's status cannot be changed; unknown user -> ResourceNotFoundException.
Swing: presentation/admin/UsersPanel: table of users (name, email, role, status) and Activate / Suspend / Deactivate
buttons for the selected row, with a confirm dialog; background DB calls via BasePanel.
JUnit tests with Mockito for the rules above.
Only touch AdminService + impl (user part), presentation/admin/UsersPanel and tests. Show a plan first.
```

Done when: you can suspend a freelancer and then that user cannot log in.

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(admin): list users and change account status (F7)"
git push -u origin feature/anas-admin-users
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-admin-users -m "merge: feature/anas-admin-users into develop"
mvn test && git push origin develop
git branch -d feature/anas-admin-users && git push origin --delete feature/anas-admin-users
```

---

## Task B8 - Freelancer profile (F21, Phase 2)

Branch: `feature/anas-freelancer-profile`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/anas-freelancer-profile
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: freelancer profile with bio, skills and portfolio links (F21).

Tables FreelancerProfiles(profile_id, user_id UNIQUE, biography, portfolio_links), UserSkills(user_id, skill_id),
Skills already exist. model.FreelancerProfile exists.
Create: FreelancerProfileDao + JDBC (findByUserId, insert, update, replaceSkills(userId, skillIds) inside the caller's
transaction, findSkills(userId)); dto/FreelancerProfileDto (biography, portfolioLinks, List<SkillDto>);
ProfileService + impl: getProfile(long userId) (creates an empty profile row on first access), updateProfile(long
actingUserId, long userId, String biography, String links, List<Long> skillIds) - only the owner may update, only a
FREELANCER has a profile, biography max 1000 chars, at most 10 skills, links max 500 chars. Run the update inside
transactions.call(...).
Swing: presentation/freelancer/ProfilePanel with biography text area, multi-select skills list and links field,
background DB calls via BasePanel.
JUnit tests with Mockito: owner only, role check, limits, first-access creates a row.
Only touch the files for this feature and their tests. Show a plan first.
```

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(profile): freelancer profile with skills and portfolio (F21)"
git push -u origin feature/anas-freelancer-profile
git checkout develop && git pull origin develop
git merge --no-ff feature/anas-freelancer-profile -m "merge: feature/anas-freelancer-profile into develop"
mvn test && git push origin develop
git branch -d feature/anas-freelancer-profile && git push origin --delete feature/anas-freelancer-profile
```

---

## Task B9 - Settings screen skeleton (F12, Phase 2)

Branch: `feature/anas-settings-screen`

Prompt (same START and FINISH pattern):
```text
Read CLAUDE.md and docs/team_setup.md first. I am Anas. Task: admin settings screen skeleton (F12).
presentation/admin/SettingsPanel: three labelled fields (initial token balance, platform fee percent, max
applications per freelancer) loaded through SettingsService.getAll(), a Save button calling update(...) per key with
the current admin from SessionManager, validation messages from ValidationException shown with ErrorDialog, background
DB calls via BasePanel. No business logic in the panel. Only touch presentation/admin/SettingsPanel. Show a plan first.
```
Commit message: `feat(settings): admin settings screen (F12)`.

---

## Later tasks (Phase 3 onward)

| Phase | Branch | Feature | Key rules |
|---|---|---|---|
| 3 | `feature/anas-notifications` | F4 `NotificationService` + panel | Subscribe to `ApplicationSubmitted` and `ApplicationWithdrawn` (client is told); `getUnread(userId)`, `getAll(userId)`, `markAsRead(id)` (only the owner); unread badge in the dashboard header; handlers write a Notifications row with a readable message |
| 4 | `feature/anas-reviews` | F19 `ReviewService` | Only the client of an `APPROVED` gig may review; one review per gig (DB unique key); rating 1..5; publish `ReviewPosted`; show average rating on the freelancer profile and in the application list |
| 4 | `feature/anas-notification-handlers` | Handlers | `FreelancerHired`, `WorkSubmitted`, `WorkApproved`, `RevisionRequested`, `WalletToppedUp` notifications |
| 5 | `feature/anas-reports` | F13 `ReportService` | Platform activity, token movements, disputes; CSV via `CsvExporter`; admin only; `JFileChooser` in the panel |
| 5 | `feature/anas-dispute-notifications` | Handlers | `DisputeRaised` / `DisputeResolved` |
| 6 | `feature/anas-demo-and-docs` | Demo data, demo script, user guide | Update README, log results in `docs/status-log.md` |

Always: depend on interfaces, role check in the service, `SwingWorker` for database calls, tests for every service, one summary message in the group chat after each task.
