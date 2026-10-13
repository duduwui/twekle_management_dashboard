# Architecture review — 13 October 2026

The project is plain Spring MVC with a server-rendered Thymeleaf frontend. Its existing
folder hierarchy was mostly layer-first, but Java used `administrative` while assets used
`administration`; both now use `administration`. The old guide described a different system
and has been replaced with the owner's actual requirements.

## Findings addressed

| Finding | Change | Reason |
| --- | --- | --- |
| Dashboard renders a model then fetches the same data again | KPIs, tables and detail dialogs now render in Thymeleaf; JavaScript only opens dialogs and filters existing registration rows | Removes dashboard JSON bootstrap and duplicate reads |
| Shared layout refetches identity and presets on every page | Uses server-provided identity; presets are loaded from the model or explicitly when a filter is opened | Avoids startup request amplification |
| Every page loads every feature script | Angular and the legacy JSON clients were removed; each page loads only its small interaction helper | Reduces browser parsing, transfer, and duplicate requests |
| Delete/toggle interceptor treats a redirected error page as success | Native POST forms and server flash messages | The server decides whether a mutation succeeded |
| Default super-admin login on failed profile request | Removed; normal Spring Security login form | No hidden authentication fallback |
| Admin paths permitted and CSRF disabled | Protected route families, session CSRF, form login and logout | Rejects requests before controller/database work |
| Field injection plus multiple constructors | Single constructor and final dependencies | Dependencies are visible and tests supply them explicitly |
| Root component scan includes MVC configuration | Root scans services/security, child scans controllers/advice | Avoids duplicate application infrastructure |
| Dashboard and reports materialize complete histories | SQL performs aggregation and pages directory/report results in groups of 25 | Bounds request memory and entity hydration |
| Customer orders prefetch every checklist | Orders and checkpoints are separate paginated GET pages | Avoids loading unopened order histories |
| Checklist GET creates missing rows | GET is read-only; an explicit authorized POST reconciles presets while holding a parent-row lock | Makes reads predictable and prevents concurrent duplicates |
| Session authorities can become stale | `PortalAccess` loads current database permissions once per request | Permission revocation applies on the next request |
| Photo data is embedded in checklist HTML | Checklist projections include only `hasImage`; an authorized endpoint loads the image on demand | Keeps page responses bounded |
| Template parsing cache disabled; unused Boot configuration | Explicit properties and cached templates by default | Avoids repeated parsing and misleading configuration |
| Tests delete records matching broad name patterns | Transaction rollback replaces cleanup deletion | Tests do not delete unrelated application data |

## Deployment work outside this refactor

The application workflows and architectural blockers above are addressed. Deployment still
requires environment-specific database credentials, TLS termination, dependency/security
review, and a load test using production-sized data while the other hosted applications are
running. Monetary columns remain `DOUBLE`; changing them to `DECIMAL` requires a separately
reviewed data migration because it changes persisted values and reporting behavior.

## Acceptance checks

- Login and dashboard work without Angular and contain server-rendered data.
- Dashboard makes zero JSON data requests on page load.
- Forms include CSRF; missing-token POST is forbidden.
- Anonymous HTML requests redirect to login.
- Ordinary admin without staff-management permission cannot read staff directory.
- Existing mutations are tested within rollback transactions.
- Directory, checklist, staff, preset, and report pages are bounded; CSV uses 500-row keyset batches.
- Concurrent preset reconciliation creates at most one check per order and preset.

No load-test result or capacity guarantee is implied by these checks. Before deploying on
the shared host, measure p95 latency, query counts, pool wait time, CPU and heap at the
expected row counts and concurrency. Tune pool/worker limits within the host-wide budget.

## References

- [Spring Security CSRF and Thymeleaf integration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Thymeleaf template caching](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)

## Verification result for this pass

`mvn test` passed 40 tests, including separate root/MVC servlet contexts, permission
revocation, read-only GET behavior, CSRF failures, bounded pagination, audit identity,
legacy satisfaction normalization, image-body omission, and concurrent reconciliation.
`git diff --check` passed. Browser checks covered form login, dashboard, customer directory,
order navigation, checkpoint forms, photo controls, and satisfaction reporting. No page
loads Angular or performs a JSON data bootstrap.

The development instance runs at `http://localhost:8081` because another editor process
owns port 8080. It uses `THYMELEAF_CACHE=false` for local template edits; production default
remains cached. No production load benchmark has been performed.
