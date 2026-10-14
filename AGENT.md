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

### 3. Action Deduplication & Functional Runners
- Any controller handling form submissions or transactional actions with error feedback (flash attributes) MUST use a `@FunctionalInterface` action runner (e.g. `AdminAction`, `RoleAction`, `TimeFilterAction`) and a shared `executeAction(...)` helper.
- Never write repetitive `try { service.call(); flash.addFlashAttribute(...); } catch (Exception ex) { ... }` blocks across individual controller methods. Keep action methods to clean 1-line execution calls.

### 4. Canonical SSR Routing
- Pure Thymeleaf SSR: Exactly one canonical route per view and per action.
- Strictly eliminate fallback route arrays (e.g. `{"/", "/admin"}` or `{"/create", "/create_preset"}`), wildcard matchers (`/admin/followups/**`), and redundant redirect fallbacks.
- Single clean path per endpoint:
  - `/` -> Root view (Administration Dashboard)
  - `/admin/dashboard` -> Dashboard view
  - `/admin/admins` -> Staff Admins view
  - `/admin/followups` -> Followups view
  - `/admin/reports` -> Reports view
  - `/admin/roles` -> Roles view
  - `/admin/timelines` -> Timelines view

### 5. Frontend & Template Architecture
- **Single Layout Fragment File**: The `fragments/` directory contains solely `adminLayout.html`. All layout sub-fragments (`head`, `sidebar`, `header`, `sharedModals`, `scripts`, `pager`) are consolidated directly inside `adminLayout.html`.
- **Single Self-Contained Template per Module**: Every administrative domain module has exactly ONE canonical, self-contained view template. No intermediate empty shell files or fragmented includes:
  - `dashboard/` -> `dashboard.html` (KPIs, progress bars, attention table, and all dialog modals)
  - `followup/` -> `followups.html` (customer directory, purchase order history, and feedback ledger)
  - `report/` -> `reports.html` (milestones, satisfaction, products, and followup summaries)
  - `role/` -> `roles.html` (staff member directory and CRUD permission matrix)
  - `timefilter/` -> `timeFilters.html` (create preset form and configured rule table)
  - `admin/` -> `admins.html` (administrator directory, creation form, and local filters)
- **M-V-E (Module-View-Events) Client-Side Standard**: Feature scripts are strictly partitioned into a standardized 3-layer architecture:
  1. `[feature]-module.js`: Pure data calculations, validation rules, formatting, and API fetch calls. Zero DOM queries or style modifications allowed.
  2. `[feature]-view.js`: DOM queries (`getElementById`, `querySelectorAll`), UI manipulation, styling/classes, and modal opening/closing. Delegates calculations to Module.
  3. `[feature]-events.js`: Event wireframe binding (`addEventListener`), page lifecycle initialization (`DOMContentLoaded`), and interval timers.
  - Loaded in strict canonical order in `adminLayout.html`: `*-module.js` -> `*-view.js` -> `*-events.js`.
- **Core JS Consolidation**: Core layout coordination (`LayoutController`), toast notifications (`ToastService`), and sidebar navigation (`toggleCategoryNav`) are consolidated into a single `/js/core/layout.js` file.

### 6. Security & Authentication Isolation
- All authentication endpoints (`/login`, `/admin/auth/login`, `/admin/auth/logout`, `/admin/auth/me`), security configuration (`SecurityConfig`), user details services, entry points, and view advice reside strictly in `com.twekl.dashboard.security.administration`.
- Password encryption (BCrypt) and role/permission checks (`isSuperAdmin`, `hasRole`, `hasPermission`, `canManageUsers`, etc.) are centralized and transparently accessible via `PortalAccess`.
- Business controllers must not contain manual `isAuthenticated()` checks.
- `SecurityConfig.java` centrally secures `/admin/**` with `.hasAnyRole("SUPER_ADMIN", "ADMIN")` and `/admin/admins` with `.hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")`.

### 7. Plural Model Naming
- Collections and model attributes must use pure plural naming without the `List` suffix:
  - Use `admins` instead of `adminList`.
  - Use `roles` instead of `roleList`.
  - Use `presets` instead of `presetList`.

### 8. Clean Code & Dead Code Elimination
- Dead code such as unused query repositories (e.g. `PortalQueryRepository`) must be deleted.
- k6 stress test reports and test endpoints are completely removed and must not be re-added.
- Unit and integration tests in `src/test/java` must pass with zero failures (`mvn test`).
- Database requires local Docker MySQL (`twekl-mysql`) container on port 3306.

---

## Change History & Memory Log

### 2026-10-14: Domain Controller Segregation & Constructor Sanitization
- **Domain Controller Responsibility**: Eliminated the 6-dependency "God constructor" in `AdminController`. Dispatched SSR views and route management directly to their respective domain controllers:
  - `DashboardController`: Handles `/` redirect, `/admin/dashboard` SSR view, and `/admin/stats/summary` REST API. Injects `DashboardService`, `CustomerService`, `FollowupService`, `ReportService`.
  - `FollowupController`: Handles `/admin/followups` SSR view and all milestone check operations. Injects `FollowupService`, `AdminService`, `TimeFilterService`.
  - `ReportController`: Handles `/admin/reports` SSR view, analytics generation, satisfaction details, and CSV exports. Injects `ReportService`, `AdminService`, `TimeFilterService`.
  - `AdminController`: Stripped down strictly to administrator staff management (`/admin/admins`, creation, status toggle, deletion). Injects only `AdminService` and `RoleService` (2 dependencies).
- **Autowired Constructor Integrity**: Maintained explicit `@Autowired` constructor injection with `final` parameters across all refactored controllers.
- **Test Suite Expansion**: Added test assertions in `DashboardRenderingTest` ensuring `/`, `/admin/followups`, and `/admin/reports` render cleanly. Total tests expanded to 34, all passing with zero failures.

### 2026-10-14: Sidebar Profile CSS Fix & Legacy Dead Code Cleanup
- **Sidebar Profile CSS Fix**: Fixed unstyled layout at the bottom of the sidebar. Corrected HTML markup to `.sidebar-footer` and `.sidebar-user-card`, styled `.sidebar-avatar` as a circular initial badge, wrapped username and role in a flex container with text truncation, and styled the logout button with a clean hover effect.
- **Dead File Cleanup**:
  - Deleted `dashboard.css` (inlined dialog styles directly into `style.css`).
  - Deleted `portal.css` (legacy prototype styling).
  - Deleted `CustomerForm.java` (unused legacy DTO).
  - Removed obsolete `pager` fragment in `adminLayout.html`.
  - Re-styled `error.html` with the comic design system.
- **Compiler Fix**: Added missing `Autowired` import to `DataInitializer.java`.
- **Test Verification**: All 31 tests passed cleanly (`mvn test`).

### 2026-10-14: Complete Elimination of Orchestrator Shells Across All Modules
- **Universal Single-File Architecture**: Consolidated every module into a single self-contained template, completely eliminating all intermediate `th:replace` shells and fragment sub-files:
  - `dashboard/`: Inlined `dashboardKpis`, `priorityTable`, `dashboardModals`, `dashboardDetailsModal`, `dashboardQuickActions` into `dashboard.html`. Deleted the 5 fragment files.
  - `followup/`: Inlined `followupDirectory`, `orderDirectory`, `feedbackDirectory` into `followups.html`. Deleted the 3 fragments and removed dead code (`customerForm.html`, `checklist.html`).
  - `role/`: Inlined `roleStaffDirectory` and `rolePermissions` into `roles.html`. Deleted the 2 fragment files.
  - `timefilter/`: Inlined `timeFilterForm` and `timeFilterDirectory` into `timeFilters.html`. Deleted the 2 fragment files.
  - `admin/`: Inlined `adminList` and `adminForm` into `admins.html`. Deleted the 2 fragment files.
  - `report/`: Consolidated 4 sub-tabs directly into `reports.html`. Deleted the 4 fragment files.
- **Verification**: All 31 tests passed cleanly (`mvn test`). All endpoints returning HTTP 200.

### 2026-10-14: Reports Consolidation into Single Self-Contained Template
- **Orchestrator Elimination**: Consolidated all 4 report tab fragments (`reportMilestones.html`, `reportSatisfaction.html`, `reportProducts.html`, `reportFollowups.html`) directly into `reports.html`.
- **Eliminated File Bloat**: Deleted the 4 redundant sub-fragment files so `views/administration/report/` contains only `reports.html`.
- **Test Verification**: All 31 tests passed cleanly.

### 2026-10-14: Frontend Consolidation (Layout, Fragments & Admin Views)
- **Core JS Merge**: Merged `toast.js`, `layoutController.js`, and `navigation.js` into a single `/js/core/layout.js` file. Deleted the separate files.
- **Unified Fragments**: Consolidated all 7 files in `fragments/` into single `adminLayout.html` (inlined head, sidebar, header, shared modals, scripts, and pager). Deleted the 6 redundant fragment files.
- **Admin Views Simplification**: Enforced exactly 3 files in `admin/` (`admins.html`, `adminList.html`, `adminForm.html`). Renamed `adminDirectory.html` to `adminList.html`. Deleted unused `adminModals.html` and deleted `admin.js` (scripts inlined into `adminList.html`).
- **Test Verification**: Verified all 31 tests pass cleanly.

### 2026-10-14: Action Deduplication via Functional Runners Across All Controllers
- **Functional Runners**: Introduced `@FunctionalInterface` action runners (`AdminAction`, `RoleAction`, `TimeFilterAction`) with shared `executeAction(...)` engines in `AdminController`, `RoleController`, and `TimeFilterController`.
- **Boilerplate Elimination**: Replaced repetitive try-catch-flash blocks with clean 1-line execution calls.
- **Route Normalization**: Removed fallback route arrays in `RoleController` (`/create`, `/{roleId}/update`, `/assign_user_role`, `/{roleId}/delete`) and `TimeFilterController` (`/create`, `/{presetId}/toggle`, `/{presetId}/delete`).
- **Template Alignment**: Updated `timeFilterForm.html` and `timeFilterDirectory.html` action URLs to match canonical routes.
- **Verification**: Verified 100% test pass rate (31/31 tests).

### 2026-10-14: Service Consolidation, Final Constructor Parameters, Single SSR Routes, Security Centralization
- **Service Consolidation**: Removed `service/administration/impl/` completely. Consolidated all 8 services into single concrete `@Service` classes directly in `com.twekl.dashboard.service.administration`.
- **Constructor Final Parameters**: Added `final` to all constructor parameters in `@Autowired` constructors across controllers, services, and configurations.
- **Canonical SSR Routing**: Removed fallback route arrays (e.g. `{"/", "/admin", "/admin/"}`), wildcards (`{"/admin/followups", "/admin/followups/**"}`), and redundant redirects. Enforced single canonical route per view.
- **Security Centralization**: Moved `SecurityConfig` to `com.twekl.dashboard.security.administration.SecurityConfig`. Centralized BCrypt password encryption and explicit role/permission checks in `PortalAccess`.
- **Dead Code Cleanup**: Deleted `PortalQueryRepository.java`.
- **Test Verification**: All 31 tests passed with zero failures.
