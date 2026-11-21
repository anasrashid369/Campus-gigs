# Campus Gigs

Campus Gigs is a Java Swing desktop marketplace where students post small paid
tasks (gigs) and other students apply to do them. Payment uses **simulated
tokens**: the client's tokens are held in escrow when a freelancer is hired and
released when the work is approved, with every movement written to a ledger.
No real money is involved.

Group G06, BSE-5B.

## Tech stack

| Item       | Choice                                                     |
|------------|------------------------------------------------------------|
| Language   | Java 17, Swing                                             |
| Database   | MySQL 8.0.16+ through JDBC (`PreparedStatement` only, no ORM) |
| Build      | Maven                                                      |
| Tests      | JUnit 5, Mockito                                           |
| Patterns   | State, Strategy, Factory, Observer, DAO, limited Singleton |

## Prerequisites

- JDK 17 or newer: `java -version`
- Maven 3.8 or newer: `mvn -v`
- MySQL Server 8.0.16 or newer: `mysql --version`
  (older versions ignore `CHECK` constraints, which the wallet rules rely on)
- Git

On Ubuntu / Debian:

```bash
sudo apt update && sudo apt install -y git mysql-server maven openjdk-17-jdk
sudo systemctl enable --now mysql
```

On Windows, install the JDK, Apache Maven and MySQL Installer (Server + Workbench),
and add the Maven and MySQL `bin` folders to `PATH`.

## Setup

1. Clone the repository and switch to `develop`:

   ```bash
   git clone https://github.com/anasrashid369/Campus-gigs.git
   cd Campus-gigs
   git checkout develop
   ```

2. Create a database user (pick your own password and never commit it):

   ```bash
   sudo mysql -e "CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY 'CHOOSE_A_PASSWORD'; GRANT ALL PRIVILEGES ON campus_gigs.* TO 'campus_user'@'localhost'; FLUSH PRIVILEGES;"
   ```

3. Create your local configuration:

   ```bash
   cp config/config.example.properties config/config.properties
   ```

   Edit `config/config.properties` and set `db.user` and `db.password`.
   This file is git-ignored.

4. Build the database (the schema script creates the `campus_gigs` database itself):

   ```bash
   sudo mysql < db/01_schema.sql
   sudo mysql < db/02_seed.sql
   ```

   Then run any scripts in `db/changes/` in number order. On Windows use
   `mysql -u root -p < db/01_schema.sql`, or open each file in MySQL Workbench and run it.

   The seed adds the platform settings, the default categories and skills, and one
   administrator account (`admin@campusgigs.local`). The admin password is shared
   in the group chat, not in the repository.

5. To reset the database at any time:

   ```bash
   sudo mysql -e "DROP DATABASE IF EXISTS campus_gigs;"
   sudo mysql < db/01_schema.sql
   sudo mysql < db/02_seed.sql
   ```

## How to run

```bash
mvn test                  # all unit tests; database tests skip if config.properties is missing
mvn -q compile exec:java  # starts the application (com.campusgigs.Main)
```

## Project structure

```
pom.xml
config/config.example.properties   template for the local config.properties
db/01_schema.sql                   all tables, keys and CHECK constraints
db/02_seed.sql                     settings, admin account, categories, skills
db/changes/                        numbered change scripts, e.g. 003_add_dispute_notes.sql
docs/                              proposal, implementation plan, team setup and task guides
src/main/java/com/campusgigs/
  Main.java                        application entry point
  presentation/                    Swing screens: auth, client, freelancer, admin, common
  service/                         service interfaces; implementations in service/impl
  dao/                             DAO interfaces and TransactionManager; JDBC classes in dao/jdbc
  model/                           domain records and enums matching the schema
  dto/                             data passed from services to screens
  pattern/                         factory, strategy, state and observer (EventBus) code
  payment/                         PaymentGateway and SimulatedPaymentGateway
  util/                            ConfigLoader, CsvExporter and other helpers
  exception/                       ValidationException, ResourceNotFoundException, ...
src/test/java/com/campusgigs/      tests, mirroring the main packages
```

## Layering rules

- Presentation -> Service -> DAO. Screens never call a DAO and hold no SQL or
  business rules.
- Services never import `javax.swing` or `java.sql`; they depend on interfaces
  (DAOs, `PaymentGateway`, `TransactionManager`), never on JDBC classes.
- DAOs contain SQL and row mapping only, always with `PreparedStatement`.
- Every multi-step database change runs inside `transactions.call(...)`; every
  token movement updates the wallet and writes a ledger row in the same transaction.
- Screens receive DTOs, never `model/` records, and never a password hash.
- Role checks happen in the service, not only by hiding buttons.
- Swing: build components on the Event Dispatch Thread and run database calls in a
  `SwingWorker`.
- Services publish events with `EventBus.getInstance().publish(...)` only after their
  transaction has committed. Event names: `ApplicationSubmitted`,
  `ApplicationWithdrawn`, `FreelancerHired`, `WorkSubmitted`, `WorkApproved`,
  `RevisionRequested`, `DisputeRaised`, `DisputeResolved`, `WalletToppedUp`,
  `ReviewPosted`.
- Never edit `db/01_schema.sql` or a shared seed script; add a numbered script in
  `db/changes/` and tell the group.

## Branch and commit rules

- `main`: stable, demo-ready; receives `develop` at the end of a phase when all
  three agree.
- `develop`: integration branch; must always compile and pass `mvn test`.
- `feature/<name>-<topic>`: one task each, branched from the latest `develop`,
  deleted after merging.
- Commit messages: `type(scope): short description`, where type is `feat`, `fix`,
  `test`, `docs`, `refactor` or `chore`.
- Stage only your own files and check `git status --short` before committing. It must
  never list `config/config.properties`, `target/` or IDE folders.
- Merge with `git merge --no-ff` into an up-to-date `develop`, run `mvn test`, then
  push. Stop and discuss together if a conflict touches money, state or schema code.

The full step-by-step workflow is in [docs/team_setup.md](docs/team_setup.md).

## Team and ownership

| Member           | Area                                                         | Main packages / classes |
|------------------|--------------------------------------------------------------|-------------------------|
| Muhammad Huzaifa | Database, wallet, escrow, ledger, gig lifecycle, disputes     | `db/01_schema.sql`, `dao.jdbc` base classes, `WalletService`, `TransactionService`, `payment`, gig state machine, `DisputeService` |
| Abdullah Rasheed | Accounts, Swing shell, gigs, applications, admin monitoring   | `AuthService`, `UserService`, `SessionManager`, `GigService`, `ApplicationService`, main frame and dashboards |
| Muhammad Anas    | Notifications, admin tools, reviews, profiles, reports, docs  | `EventBus` and events, `NotificationService`, `AdminService`, `SettingsService`, `ReviewService`, `ReportService`, `CsvExporter`, `db/02_seed.sql`, README |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Configuration file not found: config/config.properties` | Copy `config/config.example.properties` to `config/config.properties` and fill in your MySQL user and password. |
| `Access denied for user ...` | The user or password in `config.properties` does not match MySQL. Re-run the `CREATE USER ... GRANT` command from setup step 2, then check both values. |
| Negative balances are accepted, or `CHECK` rules seem to do nothing | MySQL is older than 8.0.16. Upgrade MySQL, then reset the database. |
| `Unknown database 'campus_gigs'` | Run `db/01_schema.sql` first; it creates the database. |
| Seed fails with `Table ... doesn't exist` | The schema was not loaded. Reset the database (setup step 5). |
