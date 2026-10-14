# Twekl Engineering Guide & Architecture Memory

This guide records project conventions, architectural decisions, and update history. Check this file before modifying code to prevent conflicting or reverting decisions.

## Architectural Conventions

### 1. Dependency Injection & Autowiring
- Inject **interfaces**, not concrete implementation classes (e.g. `AdminService`, `RoleService`, `CustomerService`).
- All injected fields must be declared `private final`.
- Every constructor performing dependency injection must be explicitly annotated with `@Autowired`.
- Concrete implementations reside under `service.administration.impl.*`.

### 2. Controller Operations & Interfaces
- Controllers implement a domain operations contract (e.g. `AdminController implements AdminOperations`, `RoleController implements RoleOperations`).
- To prevent unnecessary file bloat, declare the interface at the top of the controller `.java` file (package-private).
- Methods implementing the interface must have `@Override`.

### 3. Security & Authentication Isolation
- All authentication endpoints (`/login`, `/admin/auth/login`, `/admin/auth/logout`, `/admin/auth/me`), user details services, entry points, and view advice reside strictly in `com.twekl.dashboard.security.administration`.
- Business controllers (`AdminController`, `RoleController`, `CustomerController`, etc.) must not contain manual `isAuthenticated()` checks or redundant method-level `@PreAuthorize` annotations.
- `SecurityConfig.java` centrally secures `/admin/**` with `.hasAnyRole("SUPER_ADMIN", "ADMIN")` and `/admin/admins` with `.hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")`.

### 4. Plural Model Naming
- Collections and model attributes must use pure plural naming without the `List` suffix:
  - Use `admins` instead of `adminList`.
  - Use `roles` instead of `roleList`.

### 5. Testing & Tooling
- k6 stress test reports and test endpoints are completely removed and must not be re-added.
- Unit and integration tests in `src/test/java` must pass with zero failures (`mvn test`).
- Database requires local Docker MySQL (`twekl-mysql`) container on port 3306.

---

## Change History & Memory Log

### 2026-10-14: Autowired Finalization, k6 Cleanup, and Interface Visibility
- **k6 Removal**: Deleted `src/main/webapp/k6-report.html`, `k6_summary.json`, `showK6Report()` endpoint, and security permitAll rule.
- **Autowired & Final**: Added `@Autowired` to constructors across all controllers, service implementations, and security components. Verified 100% of injected dependencies are `private final` interfaces.
- **Interface Placement**: Positioned `AdminOperations` and `RoleOperations` interfaces at the top of their respective controller files for immediate visibility.
- **Database Availability**: Started local `twekl-mysql` Docker container after server reboot; verified all 31 tests pass cleanly.

### 2026-10-13: Security Relocation and Plural Naming
- **Security Consolidation**: Moved `AuthController` and `PortalModelAdvice` (renamed to `SecurityModelAdvice`) into `com.twekl.dashboard.security.administration`.
- **Plural Attributes**: Replaced `adminList` and `roleList` with `admins` and `roles` across controllers, tests, and Thymeleaf views (`adminDirectory.html`, `roleStaffDirectory.html`, etc.).
- **Interface Architecture**: Extracted 8 service interfaces (`AdminService`, `RoleService`, `CustomerService`, `FollowupService`, `ReportService`, `TimeFilterService`, `DashboardService`, `AuthService`).
