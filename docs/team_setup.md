# Campus Gigs - Team Setup and Git Workflow (everyone reads this first)

Repository: https://github.com/anasrashid369/Campus-gigs  
Integration branch: `develop` (all work lands here). `main` only receives `develop` at the end of a phase, after all three agree.

## 1. One-time setup (Ubuntu / Debian)

```bash
sudo apt update && sudo apt install -y git mysql-server maven openjdk-17-jdk
sudo systemctl enable --now mysql
java -version && mvn -v | head -1 && mysql --version
```

Java 17 or newer, MySQL 8.0.16 or newer (older MySQL silently ignores CHECK constraints, which the wallet rules rely on).

Clone and get on `develop`:

```bash
git clone https://github.com/anasrashid369/Campus-gigs.git
cd Campus-gigs
git checkout develop
git config user.name "Your Name"
git config user.email "the-email-on-your-github-account@example.com"
```

Create the database user (choose your own password, never commit it):

```bash
sudo mysql -e "CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY 'CHOOSE_A_PASSWORD'; GRANT ALL PRIVILEGES ON campus_gigs.* TO 'campus_user'@'localhost'; FLUSH PRIVILEGES;"
```

Create your local config (git-ignored, must never be committed):

```bash
cp config/config.example.properties config/config.properties
```

Edit `config/config.properties`: set `db.user=campus_user` and `db.password=<your password>`.

Build the database and run the tests:

```bash
sudo mysql < db/01_schema.sql
mvn test
```

Expected: `BUILD SUCCESS`. If a test fails, tell the group before writing anything.

To reset your local database at any time:

```bash
sudo mysql -e "DROP DATABASE IF EXISTS campus_gigs;" && sudo mysql < db/01_schema.sql
```
(Run `db/02_seed.sql` after it once Anas has merged it.)

## 2. What already exists on develop (Huzaifa, Phase 1 and 2 backend)

| Area | Classes |
|---|---|
| Config / DB | `util.ConfigLoader`, `dao.jdbc.ConnectionProvider` (new connection per call) |
| Transactions | `dao.TransactionManager` (interface, use this in services), `dao.jdbc.TransactionHelper` |
| DAO base | `dao.jdbc.BaseJdbcDao` (extend it for JDBC DAOs; it joins the running transaction automatically) |
| Models | records in `model/`: `User`, `Gig`, `GigApplication`, `Wallet`, `Transaction`, `Review`, `Dispute`, `Notification`, `FreelancerProfile`, `Skill`, `GigCategory`, enums `Role`, `AccountStatus`, `GigStatus`, `ApplicationStatus`, `TransactionType`, `DisputeStatus`, `DisputeDecision` |
| Exceptions | `ValidationException` and `ResourceNotFoundException` (messages are safe to show the user), `DataAccessException`, `ConfigurationException` (technical; log them, show a generic message) |
| Wallet and escrow | `service.WalletService` (createWallet, getWallet, topUp for clients, withdraw for freelancers), `service.EscrowService` (holdInEscrow, releaseToFreelancer, refundToClient), `service.TransactionService`, `service.EarningsService`, DTOs `WalletDto`, `TransactionDto`, `PayoutDto`, `EarningsDto` |
| Gig lifecycle | `pattern.state.GigStateMachine` + `GigAction`, `service.GigLifecycleService` (hire, submitWork, approve, requestRevision, raiseDispute), `service.DisputeService` (admin review and resolve), `dao.GigDao`, `dao.DisputeDao`, DTOs `GigDto`, `DisputeDto` |
| Payment | `payment.PaymentGateway`, `SimulatedPaymentGateway` |
| Schema | `db/01_schema.sql` (14 tables) |

Wallet contract (call these, do not rewrite them):

```java
WalletDto createWallet(long userId, long initialBalance);   // at registration, CLIENT and FREELANCER only
WalletDto getWallet(long userId);
WalletDto topUp(long userId, long amount);                  // 1..10000 tokens
List<TransactionDto> getHistory(long userId);               // TransactionService
```

How a service runs several DAO calls atomically (no `java.sql` in services):

```java
return transactions.call(() -> {
    long id = userDao.insert(...);
    walletService.createWallet(id, initial);   // nested call joins the same transaction
    return dto;
});
```

## 3. Agreed names (do not change without telling the group)

- Settings table is `PlatformSettings` (columns `setting_key`, `setting_value`), per the ERD.
- Setting keys (strings): `initial_token_balance` (default 1000), `platform_fee_percent` (default 5), `max_applications_per_freelancer` (default 5).
- Gig status values: `OPEN, ASSIGNED, SUBMITTED, APPROVED, DISPUTED, RESOLVED, CANCELLED` (cancel only from OPEN). Every status change goes through `GigStateMachine`.
- Application status: `PENDING, ACCEPTED, REJECTED, WITHDRAWN`.
- Password hash stored in `Users.password_hash` as `pbkdf2$<iterations>$<saltBase64>$<hashBase64>` (PBKDF2WithHmacSHA256, 120000 iterations, 16 byte salt, 32 byte key). Owner: Abdullah (`util.PasswordHasher`).
- EventBus event names, identical everywhere: `ApplicationSubmitted, ApplicationWithdrawn, FreelancerHired, WorkSubmitted, WorkApproved, RevisionRequested, DisputeRaised, DisputeResolved, WalletToppedUp, ReviewPosted`.
- Commit format: `type(scope): short description`. Types: feat, fix, test, docs, refactor, chore.
- Branch names: `feature/<yourname>-<topic>`, e.g. `feature/abdullah-swing-shell`.

## 4. The layering rules (strict)

- Presentation (Swing) -> Service -> DAO. Presentation never calls a DAO and has no SQL or business rules.
- Services never import `javax.swing` or `java.sql`. DAOs hold SQL and row mapping only.
- Services depend on interfaces (DAO, `PaymentGateway`, `TransactionManager`), never on JDBC classes.
- Layers pass DTOs (`dto/`), not `model/` records, to the screens. Never put `passwordHash` in a DTO.
- `PreparedStatement` for every query, never string-built SQL.
- Every multi-step database change runs inside `transactions.call(...)`.
- Swing: build components on the Event Dispatch Thread; run database calls in a `SwingWorker`; action listeners only collect input, call a service, and show the result.
- Check the caller's role inside the service, not only by hiding buttons.
- No `System.out.println`, no commented-out code, no unused code in committed files.
- JavaDoc on public service and DAO interfaces. JUnit tests for every service you own.

## 5. Git workflow for every task

Pick one task, make one branch, keep it small.

```bash
# 1. start from the latest develop
git checkout develop && git pull origin develop
git checkout -b feature/<yourname>-<topic>

# 2. work, then verify
mvn test

# 3. stage ONLY your files, look at the list, then commit
git add <paths you own>
git status --short
git commit -m "feat(scope): short description"

# 4. back up the branch on GitHub
git push -u origin feature/<yourname>-<topic>

# 5. merge into develop (pull first so you merge against the latest)
git checkout develop && git pull origin develop
git merge --no-ff feature/<yourname>-<topic> -m "merge: feature/<yourname>-<topic> into develop"
mvn test
git push origin develop

# 6. clean up
git branch -d feature/<yourname>-<topic>
git push origin --delete feature/<yourname>-<topic>
```

Rules:
- Never edit a file another person owns. Never edit `db/01_schema.sql`; request a numbered script in `db/changes/` instead and tell Huzaifa.
- `git status --short` must never list `config/config.properties`, `target/` or `.idea/`.
- If step 5 reports a merge conflict, stop. Do not guess. Conflicts touching money, state or schema code are resolved together on a call.
- `develop` must always compile and pass `mvn test`. If `mvn test` fails after the merge in step 5, do NOT push; fix it first.
- Do not use GitHub's merge button. Merge only with the commands above.
- After every finished task, post the summary message in the group chat (template in the implementation plan, section 7.2).

## 6. Using an AI assistant safely

1. Keep your own local rules file with the project rules from this guide. It stays on your machine only: list it in your local `.git/info/exclude` and never commit it. Start each prompt by telling the AI to read it and `docs/team_setup.md`.
2. One task per session. Always ask for a plan first and approve it before it writes code.
3. Tell it exactly which files and packages it may touch. Tell it NOT to edit anyone else's files or `db/01_schema.sql`.
4. Ask it to write tests and run `mvn test`.
5. Run all git commands yourself and read every diff before you commit. You must be able to explain every line in the viva.
6. Never paste passwords or real credentials into a prompt.
