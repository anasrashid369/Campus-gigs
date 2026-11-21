# Campus Gigs - project context

Java 17 Swing desktop app, MySQL via JDBC, Maven, JUnit 5. Student freelance
marketplace with simulated tokens, escrow and disputes. No real payments.

## Architecture (strict)
- Layers: presentation (Swing) -> service -> dao. Package root com.campusgigs.
- Presentation never calls DAOs and has no SQL or business rules.
- Services never import Swing. DAOs contain SQL and mapping only.
- Services depend on interfaces (DAO, PaymentGateway), not concrete classes.
- Layers exchange DTOs; domain models are in model/.

## Database rules
- PreparedStatement only. New connection per operation, try-with-resources.
- Every money movement = one JDBC transaction: wallet update(s) + ledger insert.
  Use SELECT ... FOR UPDATE on wallet rows. Roll back on any failure.
- Schema changes only via new numbered script in db/changes/.

## Business rules
- Gig lifecycle: Open -> Assigned -> Submitted -> Approved/Closed, plus
  Disputed -> Resolved. Revision returns to Assigned. All changes go through
  the State-pattern state machine; invalid transitions throw.
- One freelancer per gig. Fund only with sufficient available tokens.
- Only client approves/revises; only admin resolves disputes.
- Reviews only by the client of a completed gig.
- Payment: single method, SimulatedPaymentGateway (top-up only). Escrow and
  internal transfers do not use the gateway.

## Patterns
State (gig lifecycle), Strategy (gig ranking), Factory (role dashboards),
Observer (EventBus + notifications), DAO, limited Singleton (config).

## Swing
EDT only for UI updates; DB calls in SwingWorker; no logic in listeners.

## Style and testing
PascalCase classes, camelCase methods, JavaDoc on public interfaces.
Write JUnit tests for each service. Run mvn test before finishing.
Never commit config/config.properties or credentials.
Do not modify files outside the task scope; do not change agreed interfaces.
