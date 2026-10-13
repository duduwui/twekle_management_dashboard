# Architecture review — 13 October 2026

The project is plain Spring MVC with a partially migrated Thymeleaf frontend. Its existing
folder hierarchy was mostly layer-first, but Java used `administrative` while assets used
`administration`; both now use `administration`. The old guide described a different system
and has been replaced with the owner's actual requirements.

## Findings addressed

| Finding | Change | Reason |
| --- | --- | --- |
| Dashboard renders a model then fetches the same data again | KPIs, tables and detail dialogs now render in Thymeleaf; JavaScript only opens dialogs and filters existing registration rows | Removes dashboard JSON bootstrap and duplicate reads |
| Shared layout refetches identity and presets on every page | Uses server-provided identity; presets are loaded from the model or explicitly when a filter is opened | Avoids startup request amplification |
| Every page loads every feature script | Feature scripts are conditional; Angular is loaded only for the remaining follow-up/report screens | Reduces browser parsing and transfer |
| Delete/toggle interceptor treats a redirected error page as success | Native POST forms and server flash messages | The server decides whether a mutation succeeded |
| Default super-admin login on failed profile request | Removed; normal Spring Security login form | No hidden authentication fallback |
| Admin paths permitted and CSRF disabled | Protected route families, session CSRF, form login and logout | Rejects requests before controller/database work |
| Field injection plus multiple constructors | Single constructor and final dependencies | Dependencies are visible and tests supply them explicitly |
| Root component scan includes MVC configuration | Root scans services/security, child scans controllers/advice | Avoids duplicate application infrastructure |
| Dashboard calculates customer summaries twice | Reuses the summaries for its statistics | Avoids repeated customer/order/check reads |
| Filtered follow-up lookup loads all customers and dependent tables | Customer cutoff runs in SQL, dependent rows restricted to matching IDs | Avoids reading unrelated histories |
| Customer orders prefetch every checklist | Fetch checklist when expanded | Avoids one request per unopened order |
| Template parsing cache disabled; unused Boot configuration | Explicit properties and cached templates by default | Avoids repeated parsing and misleading configuration |
| Tests delete records matching broad name patterns | Transaction rollback replaces cleanup deletion | Tests do not delete unrelated application data |

## Remaining release blockers and migration work

These are known limitations, not claims that the whole application is now production-ready.

1. **Unbounded database reads remain.** Dashboard statistics and report generation still
   materialize substantial historical data. Follow-up and admin directories still send
   complete datasets. Implement database pagination/projections and aggregate queries,
   with measured query plans and indexes, before declaring large-dataset capacity.
2. **Follow-up/report screens still use Angular and JSON for interactions.** Their initial
   duplicate fetch is removed, but complete Thymeleaf conversion remains. Convert one
   complete workflow at a time: customer directory GET filters/page, order details GET,
   checkpoint POST + redirect, then report GET filters. Retire matching legacy JSON routes
   after checking external consumers. No REST fallback should be introduced.
3. **Checklist GET currently creates/synchronizes checks.** Move this to order ingestion
   and explicit preset reconciliation, add a uniqueness constraint for order/preset, and
   test concurrent reconciliation before making the GET genuinely read-only. Simply
   removing the writes today would hide missing milestones in existing orders.
4. **Fine-grained permissions need a complete endpoint audit.** Several legacy endpoints
   grant any administrator write access or check database permissions within the method.
   Centralize the permission policy, define how session authorities refresh after role
   changes, and test denied actions for each permission. The route filter is a baseline.
5. **Production provisioning remains separate.** Existing dependencies need a supported
   version/security review. Default local database credentials and demo seed credentials
   must not be used for deployment. Introduce versioned database migrations; switching
   a configuration key is not a migration strategy. Existing schema is validated at boot.
6. **Entity/DTO and validation debt remains.** Legacy endpoints accept entities, some
   derived presentation fields live on entities, and monetary amounts use floating point.
   Introduce focused input/page DTOs and decimal money storage with reviewed migrations.

## Acceptance checks

- Login and dashboard work without Angular and contain server-rendered data.
- Dashboard makes zero JSON data requests on page load.
- Forms include CSRF; missing-token POST is forbidden.
- Anonymous HTML requests redirect to login; JSON requests return 401.
- Ordinary admin without staff-management permission cannot read staff directory.
- Existing mutations are tested within rollback transactions.
- Empty summary results avoid unnecessary dependent queries; filtered lookups restrict IDs.

No load-test result or capacity guarantee is implied by these checks. Before deploying on
the shared host, measure p95 latency, query counts, pool wait time, CPU and heap at the
expected row counts and concurrency. Tune pool/worker limits within the host-wide budget.

## References

- [Spring Security CSRF and Thymeleaf integration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Thymeleaf template caching](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)

## Verification result for this pass

`mvn test` passed 30 tests, including a test with separate root and MVC application
contexts matching the real servlet startup. JavaScript syntax checks and `git diff --check`
passed. Browser checks covered form login, dashboard feedback dialog, follow-up directory
and satisfaction report. The dashboard loads only its interaction script and navigation
script; neither contains a data request.

The development instance runs at `http://localhost:8081` because another editor process
owns port 8080. It uses `THYMELEAF_CACHE=false` for local template edits; production default
remains cached. No production load benchmark has been performed.
