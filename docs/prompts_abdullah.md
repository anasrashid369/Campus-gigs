# Abdullah Rasheed - step-by-step tasks, Claude Code prompts and git commands

Read `docs/team_setup.md` first and finish its section 1 (setup). You own: accounts, authentication, role-based access, the Swing shell, gig posting, applications, search and admin monitoring. Patterns you own: **Factory** (role dashboards) and **Strategy** (gig ranking and matching).

Every task below has: a START block (create the branch), a prompt to paste into Claude Code, a "done when" check, and a FINISH block (verify, commit, merge, clean up). Do the tasks in order. Do not start the next one until the previous is merged and you have posted the summary in the group chat.

Important dependencies:
- Anas publishes the `SettingsService` interface early (his task B2). You need it in task A3.
- Anas needs your `PasswordHasher` (task A2) to build the admin seed row. Do A2 right after A1 and tell him as soon as it is merged.
- Anas's admin user screen (F7) needs your `UserDao` (task A3). He is waiting on it.
- Huzaifa's `WalletService.createWallet(userId, initialBalance)` already exists. Call it from registration.

Files you must NOT edit: `db/01_schema.sql`, anything in `service/*Wallet*`, `service/*Transaction*`, `dao/*Wallet*`, `dao/*Transaction*`, `dao/jdbc/TransactionHelper.java`, `payment/`, `model/` (ask Huzaifa if a model field is missing), and Anas's classes.

---

## Task A1 - Swing shell, UI constants, shared helpers (Phase 1)

Branch: `feature/abdullah-swing-shell`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/abdullah-swing-shell
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Abdullah. Task: the Swing shell for Campus Gigs.

Create, under com.campusgigs:
- presentation/common/UiConstants: fonts, colours, spacing, standard sizes (constants only).
- presentation/common/BasePanel: abstract JPanel subclass with a protected helper to run a database
  call in a SwingWorker and then update the UI on the Event Dispatch Thread (accepts a Callable<T>
  and a Consumer<T> for success plus a Consumer<Exception> for failure).
- presentation/common/ErrorDialog: static show(Component parent, Throwable t). If t is a
  ValidationException or ResourceNotFoundException show its message; for anything else show the
  generic text "Something went wrong. Please try again." and never show technical details.
- presentation/MainFrame: JFrame with a CardLayout content area, methods showCard(String name),
  addCard(String name, JPanel panel), and a header area placeholder.
- presentation/auth/LoginFrame: placeholder window with a title label only (real login comes later).
- util/Validators: static helpers isBlank, isValidEmail, hasMinLength, isPositiveNumber.
- Update Main.java to start LoginFrame inside SwingUtilities.invokeLater.
- Package-level notes: DTOs in dto/ are immutable Java records, never contain passwordHash, never
  expose model/ records to Swing.

Do NOT add any business logic. Only touch: presentation/**, util/Validators, Main.java and the matching test files.
Write JUnit tests for Validators. First show me a short plan (files, signatures) and wait for approval.
After approval implement, run mvn test and fix failures.
```

Done when: `mvn test` passes and `mvn -q compile exec:java` opens the LoginFrame window.

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(ui): add Swing shell, base panel, error dialog and validators"
git push -u origin feature/abdullah-swing-shell
git checkout develop && git pull origin develop
git merge --no-ff feature/abdullah-swing-shell -m "merge: feature/abdullah-swing-shell into develop"
mvn test && git push origin develop
git branch -d feature/abdullah-swing-shell && git push origin --delete feature/abdullah-swing-shell
```
Run the FINISH lines one block at a time and stop if `mvn test` fails or the merge reports a conflict. Then tell Huzaifa the shell is merged (he is waiting on `MainFrame` and `BasePanel` to build the wallet screens).

---

## Task A2 - PasswordHasher (small, do immediately after A1)

Branch: `feature/abdullah-password-hasher`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/abdullah-password-hasher
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. Task: util/PasswordHasher using only the JDK (no new dependency).

- PBKDF2WithHmacSHA256, 120000 iterations, 16 byte random salt (SecureRandom), 32 byte key.
- String hash(String password): returns "pbkdf2$120000$<saltBase64>$<hashBase64>".
- boolean verify(String password, String stored): parses the stored string, recomputes, compares in
  constant time (MessageDigest.isEqual). Returns false for null, malformed or non-matching input; never throws.
- Never log or print passwords.
JUnit tests: same password verifies, wrong password fails, two hashes of the same password differ,
malformed stored value returns false.
Only touch util/PasswordHasher and its test. Show a plan first.
```

Done when: tests pass. Message Anas: "PasswordHasher is on develop, generate the admin hash with it for 02_seed.sql."

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(util): add salted PBKDF2 PasswordHasher"
git checkout develop && git pull origin develop
git merge --no-ff feature/abdullah-password-hasher -m "merge: feature/abdullah-password-hasher into develop"
mvn test && git push origin develop
git branch -d feature/abdullah-password-hasher
```

---

## Task A3 - Auth, users and session (F1, Phase 2)

Branch: `feature/abdullah-auth-service`  
Needs from Anas first: `SettingsService` interface merged (check `ls src/main/java/com/campusgigs/service`). If it is not there yet, ask him to merge it before you start.

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/abdullah-auth-service
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Abdullah. Task: registration, login, session and user data access (F1).

Existing and must not be changed: WalletService.createWallet(userId, initialBalance), TransactionManager,
BaseJdbcDao, PasswordHasher (util), SettingsService.getInitialTokenBalance() (Anas, interface only), model.User,
Role, AccountStatus, exceptions ValidationException / ResourceNotFoundException.

Create:
- dao/UserDao (interface) and dao/jdbc/UserDaoJdbc extending BaseJdbcDao, PreparedStatement only:
  Optional<User> findById(long id); Optional<User> findByEmail(String email); long insert(Role role,
  String fullName, String email, String passwordHash); List<User> findAll();
  void updateAccountStatus(long userId, AccountStatus status); void updateName(long userId, String fullName).
- dto/UserDto (record: userId, role, fullName, email, accountStatus - NO password hash) and
  dto/RegisterRequest (record: fullName, email, password, role).
- service/AuthService + service/impl/AuthServiceImpl:
  UserDto register(RegisterRequest) - validate (name not blank, valid email, password at least 8 chars with a
  letter and a digit, role CLIENT or FREELANCER only - ADMIN cannot self-register), reject duplicate email
  (case-insensitive) with ValidationException, hash with PasswordHasher, and in ONE transactions.call(...)
  insert the user and call walletService.createWallet(userId, settingsService.getInitialTokenBalance()).
  UserDto login(String email, String password) - same generic message "Invalid email or password" for unknown
  email and wrong password; reject SUSPENDED and DEACTIVATED accounts with a clear message.
- service/UserService + impl: UserDto getUser(long id); UserDto updateProfile(long userId, String fullName).
- util/SessionManager (singleton): setCurrentUser(UserDto), getCurrentUser(), clear(), isLoggedIn(). Plain
  in-memory holder, no database.
- Services depend on interfaces only; no java.sql or Swing in services.
Write JUnit tests with Mockito for AuthServiceImpl: successful register, duplicate email, weak password, admin role
rejected, wallet created with the settings balance, login success, wrong password, suspended account, unknown email.
Only touch: dao/UserDao*, dao/jdbc/UserDaoJdbc, dto/UserDto, dto/RegisterRequest, service/Auth*, service/User*,
service/impl/Auth*, service/impl/User*, util/SessionManager and their tests.
Show a plan first and wait for approval.
```

Done when: `mvn test` passes. Post the signatures of `AuthService`, `UserService` and `UserDao` in the group chat. Tell Anas his F7 admin user list can use `UserDao.findAll()` and `updateAccountStatus`.

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(auth): add registration, login, user DAO and session manager (F1)"
git push -u origin feature/abdullah-auth-service
git checkout develop && git pull origin develop
git merge --no-ff feature/abdullah-auth-service -m "merge: feature/abdullah-auth-service into develop"
mvn test && git push origin develop
git branch -d feature/abdullah-auth-service && git push origin --delete feature/abdullah-auth-service
```

---

## Task A4 - Login and register screens, role dashboards via Factory (F1 UI, F2, F3)

Branch: `feature/abdullah-dashboards`

START:
```bash
git checkout develop && git pull origin develop && git checkout -b feature/abdullah-dashboards
```

Prompt:
```text
Read CLAUDE.md and docs/team_setup.md first. I am Abdullah. Task: login/register screens, role dashboards (Factory
pattern) and basic profile (F1 UI, F2, F3).

Existing: MainFrame, BasePanel, ErrorDialog, UiConstants, Validators, AuthService, UserService, SessionManager.

Create:
- presentation/auth/LoginPanel and RegisterPanel (validation messages next to the fields; listeners only collect
  input and call the service; DB calls through BasePanel's SwingWorker helper; on success store the user in
  SessionManager and switch to the dashboard).
- pattern/factory/DashboardFactory: static JPanel create(UserDto user, MainFrame frame) returning a different panel
  per Role (ClientDashboardPanel, FreelancerDashboardPanel, AdminDashboardPanel under presentation/client,
  presentation/freelancer, presentation/admin). Each dashboard is a placeholder with a welcome label, a header
  showing name and role, a Logout button (clears SessionManager, back to login) and an empty navigation area
  where other members will add buttons later. Use a navigation list in MainFrame so others can register menu items
  without editing the dashboards.
- presentation/common/ProfilePanel: view and edit the user's full name through UserService (F3).
Replace the LoginFrame placeholder usage in Main.java with the real flow.
Only touch: presentation/**, pattern/factory/**, Main.java and tests for the factory.
Write a JUnit test that DashboardFactory returns the right panel class for each role.
Show a plan first and wait for approval.
```

Done when: you can register a client and a freelancer, log in as each, see the right dashboard, and logout works. A suspended user cannot log in (Anas's admin screen sets the status; until then test by changing the row in MySQL).

FINISH:
```bash
mvn test && git add -A src && git status --short
git commit -m "feat(ui): add login, register, role dashboards via DashboardFactory and profile (F1, F2, F3)"
git push -u origin feature/abdullah-dashboards
git checkout develop && git pull origin develop
git merge --no-ff feature/abdullah-dashboards -m "merge: feature/abdullah-dashboards into develop"
mvn test && git push origin develop
git branch -d feature/abdullah-dashboards && git push origin --delete feature/abdullah-dashboards
```

Phase 2 exit check (all three together): register a client and a freelancer, log in as each, top up the client wallet, see the transaction in history, and log in as admin to suspend a user.

---

## Later tasks (Phase 3 onward, same pattern: branch, prompt, FINISH)

Write the exact prompts with Claude Code using the template in the implementation plan, section 8.3. Branch names and key rules:

| Phase | Branch | Features | Key rules |
|---|---|---|---|
| 3 | `feature/abdullah-gigs` | F14, F15 create, edit, cancel gigs | Call Huzaifa's `GigStateMachine` before any status change; only the client who owns the gig; edit/cancel only while `OPEN`; budget must be positive; deadline not in the past |
| 3 | `feature/abdullah-applications` | F16, F23, F24 | Apply only to an `OPEN` gig, one application per freelancer per gig (DB unique key), enforce `max_applications_per_freelancer` from `SettingsService`, publish `ApplicationSubmitted` and `ApplicationWithdrawn` through Anas's `EventBus` |
| 3 | `feature/abdullah-gig-search` | F22 | Browse, search, filter by category/budget/skill; Strategy pattern: `GigRankingStrategy` interface with at least two implementations (e.g. newest first, best skill match) |
| 4 | `feature/abdullah-gig-screens` | Hire button, My Gigs, deadlines, status badges | Wire to Huzaifa's `GigLifecycleService` (no business logic in the UI); overdue marker when deadline passed and status is `ASSIGNED` |
| 5 | `feature/abdullah-admin-monitoring` | F9 | Lists of gigs, applications and the ledger with filters; admin role checked in the service |

Always: depend on interfaces, role check in the service, `SwingWorker` for database calls, tests for every service.
