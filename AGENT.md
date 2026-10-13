# Twekl engineering guide

This guide reflects the current owner's requirements. Read it before changing code.

## Structure

Use technical layer > portal > feature code. The current portal name is `administration`.

- Java: `com.twekl.dashboard.controller.administration`, `service.administration`,
  `repository.administration`, `model.administration`, `dto.administration`,
  `security.administration`, `config.administration`, `exception.administration`.
- Templates: `src/main/webapp/WEB-INF/views/administration/<feature>`.
- Assets: `src/main/resources/{js,css,images}/administration/<feature>`.

Keep related backend methods in a cohesive domain class. Do not create a class per method,
artificial interfaces with one implementation, or generic abstractions for one call site.
Frontend files have one clear screen or interaction responsibility; fragment names describe
what they render. Use single-dot filenames, e.g. `followupService.js`.

## Rendering and requests

Use Spring MVC `@Controller`, Thymeleaf `th:each`, `th:text`, `th:action`, and ordinary
GET navigation / POST forms for portal workflows. A successful mutation redirects to its
GET page (Post/Redirect/Get). Display validation errors without pretending success.
Use `@RestController` / `@ResponseBody` only for a documented requirement that server
rendering cannot satisfy. Existing legacy JSON routes are migration debt, not a precedent.
Never retry a failed Thymeleaf operation through REST or a different HTTP method.
Do not hydrate a page and then refetch the same data. Do not poll without a requirement.
Never infer successful mutations from a redirect or optimistic DOM changes.

## Java choices

- One constructor and `private final` dependencies: dependencies are explicit and cannot
  be reassigned; Spring resolves the single constructor without `@Autowired`.
- Services own transactions. Use `@Transactional(readOnly = true)` for actual reads and
  normal transactions for writes. Read-only is an optimization hint, not authorization.
- Repositories own queries. Filter, aggregate and paginate in SQL when the data can grow.
  A browser showing ten rows does not make an unbounded `findAll()` query paginated.
- Use `JOIN FETCH` for associations the use case actually reads; do not fetch multiple
  large collections together or combine collection fetch joins with pagination.
- Use `List` for ordered output and `Set` for membership checks. Sort once only when the
  output requires ordering. Use one captured `Instant` for a calculation batch.
- Avoid parallel streams for request processing on a shared host. Do not invent cache
  invalidation rules; measure before introducing application data caches.
- JPA entities represent persistence; use DTOs for new page projections and inputs.
  Do not bind unrestricted entity fields directly from untrusted forms.
- Use `Instant`, explicit domain IDs (`customerId`, `orderId`), uppercase SQL/JPQL keywords
  and uppercase database identifiers. Never put passwords in browser output.
- Spring Security owns authentication, CSRF and route authorization. UI visibility alone
  is not permission enforcement. Mutations must require CSRF with cookie-based sessions.

## Runtime

Plain Spring MVC, Java 21+, embedded Tomcat; no Spring Boot. `application.properties` is
explicitly loaded by `JpaConfig`; adding arbitrary `spring.*` settings has no effect.

`mvn clean test` runs the suite against the configured MySQL database. Controller tests
roll back transactions; never delete test data by broad username/name matching.
`mvn compile exec:java` starts the embedded server (default port 8080).

Deployment settings: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_MAX`, `DB_POOL_MIN`,
`DB_SCHEMA_ACTION`, `THYMELEAF_CACHE`, `PORT`, `HTTP_MAX_THREADS`, `HTTP_MAX_CONNECTIONS`.
Defaults use the existing local development database; deployment must supply its own
credentials and TLS connection settings. Schema validation is the default; schema changes
need reviewed migrations. Demo seeding is opt-in with `-Dspring.profiles.active=demo` and
must never be enabled on a shared production database.

Template caching defaults to on. Set `THYMELEAF_CACHE=false` during template editing.
The embedded connector defaults to 40 workers and the database pool to 5 connections.
These are starting limits, not proof of capacity: benchmark with realistic data and the
other applications sharing the host. An externally managed Tomcat needs its own limits.

## Verification and remaining work

Read `ARCHITECTURE_REVIEW.md` for known limitations. Run relevant tests and validate rendered
pages, request counts and permission failures. Do not claim production readiness from a
successful build. Do not silently remove features to pass tests. No emojis in code or output.
