# Twekl Engineering Guide & Architecture Memory

This guide records project conventions, architectural decisions, and update history. Check this file before modifying code to prevent conflicting or reverting decisions.

## Architectural Conventions

### 1. Dependency Injection & Autowiring
- Inject concrete services directly (`AdminService`, `RoleService`, `CustomerService`, etc.). Do not create separate `impl/` subfolders or duplicate interface/implementation pairs for services.
- All injected fields must be declared `private final`.
- Every constructor performing dependency injection must be explicitly annotated with `@Autowired` and all parameters must be marked `final` (e.g. `@Autowired public AdminController(final AdminService adminService, ...)`).
- Services reside directly under `service.administration.*` without an `impl` package.

### 2. Controller Operations & Interfaces
- Controllers implement a domain operations contract (e.g. `AdminController implements AdminOperations`, `RoleController implements RoleOperations`).
- Declare the interface contract at the top of the controller `.java` file (package-private) to avoid extra file bloat.
- Methods implementing the interface must have `@Override`.

### 3. Canonical SSR Routing
- Pure Thymeleaf SSR: Exactly one canonical route per view.
- Strictly eliminate fallback route arrays (e.g. `{"/", "/admin"}`), wildcard matchers (`/admin/followups/**`), and redundant redirect fallbacks.
- Single clean path per endpoint:
  - `/` -> Root view (Administration Dashboard)
  - `/admin/dashboard` -> Dashboard view
  - `/admin/admins` -> Staff Admins view
  - `/admin/followups` -> Followups view
  - `/admin/reports` -> Reports view
  - `/admin/roles` -> Roles view

### 4. Security & Authentication Isolation
- All authentication endpoints (`/login`, `/admin/auth/login`, `/admin/auth/logout`, `/admin/auth/me`), security configuration (`SecurityConfig`), user details services, entry points, and view advice reside strictly in `com.twekl.dashboard.security.administration`.
- Password encryption (BCrypt) and role/permission checks (`isSuperAdmin`, `hasRole`, `hasPermission`, `canManageUsers`, etc.) are centralized and transparently accessible via `PortalAccess`.
- Business controllers must not contain manual `isAuthenticated()` checks.
- `SecurityConfig.java` centrally secures `/admin/**` with `.hasAnyRole("SUPER_ADMIN", "ADMIN")` and `/admin/admins` with `.hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")`.

### 5. Plural Model Naming
- Collections and model attributes must use pure plural naming without the `List` suffix:
  - Use `admins` instead of `adminList`.
  - Use `roles` instead of `roleList`.

### 6. Clean Code & Dead Code Elimination
- Dead code such as unused query repositories (e.g. `PortalQueryRepository`) must be deleted.
- k6 stress test reports and test endpoints are completely removed and must not be re-added.
- Unit and integration tests in `src/test/java` must pass with zero failures (`mvn test`).
- Database requires local Docker MySQL (`twekl-mysql`) container on port 3306.

---

## Change History & Memory Log

### 2026-10-14: Service Consolidation, Final Constructor Parameters, Single SSR Routes, Security Centralization
- **Service Consolidation**: Removed `service/administration/impl/` completely. Consolidated all 8 services into single concrete `@Service` classes directly in `com.twekl.dashboard.service.administration`.
- **Constructor Final Parameters**: Added `final` to all constructor parameters in `@Autowired` constructors across controllers, services, and configurations.
- **Canonical SSR Routing**: Removed fallback route arrays (e.g. `{"/", "/admin", "/admin/"}`), wildcards (`{"/admin/followups", "/admin/followups/**"}`), and redundant redirects. Enforced single canonical route per view.
- **Security Centralization**: Moved `SecurityConfig` to `com.twekl.dashboard.security.administration.SecurityConfig`. Centralized BCrypt password encryption and explicit role/permission checks in `PortalAccess`.
- **Dead Code Cleanup**: Deleted `PortalQueryRepository.java`.
- **Test Verification**: All 31 tests passed with zero failures.

### 2026-10-14: Autowired Finalization, k6 Cleanup, and Interface Visibility
- **k6 Removal**: Deleted `src/main/webapp/k6-report.html`, `k6_summary.json`, `showK6Report()` endpoint, and security permitAll rule.
- **Autowired & Final**: Added `@Autowired` to constructors across all controllers, service implementations, and security components. Verified 100% of injected dependencies are `private final`.
- **Interface Placement**: Positioned `AdminOperations` and `RoleOperations` interfaces at the top of their respective controller files for immediate visibility.
- **Database Availability**: Started local `twekl-mysql` Docker container after server reboot; verified all 31 tests pass cleanly.

### 2026-10-13: Security Relocation and Plural Naming
- **Security Consolidation**: Moved `AuthController` and `PortalModelAdvice` (renamed to `SecurityModelAdvice`) into `com.twekl.dashboard.security.administration`.
- **Plural Attributes**: Replaced `adminList` and `roleList` with `admins` and `roles` across controllers, tests, and Thymeleaf views (`adminDirectory.html`, `roleStaffDirectory.html`, etc.).
- **Interface Architecture**: Extracted 8 service interfaces (`AdminService`, `RoleService`, `CustomerService`, `FollowupService`, `ReportService`, `TimeFilterService`, `DashboardService`, `AuthService`).
