# Twekl Management Dashboard — Agent Architecture & Engineering Guide (`AGENT.md`)

> **Note for Future AI Agents:** This document is the single source of truth for the architecture, coding standards, design system, database setup, and user preferences of the **Twekl Management Dashboard**. Follow these principles strictly when continuing development.

---

## 0. STRICT PERMANENT RULE: ZERO EMOJIS (NEVER USE EMOJIS NOW OR IN FUTURE)

> **ABSOLUTE PROHIBITION FOR ALL AGENTS AND DEVELOPERS:**
> Under NO circumstances should any emojis (such as greeting hands, checkmarks, sirens, people, lightning, money bags, stars, phones, charts, party poppers, etc.) ever be used in this codebase.
> - **Zero emojis in HTML templates.**
> - **Zero emojis in CSS stylesheets.**
> - **Zero emojis in JavaScript source code, logs, or UI text.**
> - **Zero emojis in Java code, comments, or log messages.**
> - **Zero emojis in agent chat output or responses.**
> Use clean, lightweight SVG vector icons or crisp typographic badges instead. This rule is permanent and non-negotiable.

---

## 1. Project Overview & Tech Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend** | Pure Spring MVC 6.1.13 + Jakarta EE 10 | **ZERO Spring Boot**. Embedded Apache Tomcat 10.1.30, `AppInitializer.java`, `WebMvcConfig.java`, `SecurityConfig.java`, `JpaConfig.java`, Hibernate 6.5.3, Lombok, Maven WAR/Jar execution |
| **Partition Architecture** | Windows Disk Partition Pattern (`administration`) | Dedicated partition for the administration portal. Backend code is isolated under `com.twekl.dashboard.administration.*`. Web views are isolated under `src/main/webapp/WEB-INF/views/administration/`. Future portals will get their own isolated partition without mixing files |
| **Templating Engine** | Thymeleaf 3.1 + Ultraq Layout Dialect 3.3 | Master layout in `WEB-INF/views/administration/fragments/adminLayout.html`. Feature views decorate layout using `layout:decorate="~{administration/fragments/adminLayout}"` |
| **Frontend** | AngularJS 1.8.2 (Modular Screen Architecture) | Categorized domain folders. Controllers and services combined into **ONE single file** per screen |
| **Naming Convention** | STRICT Single-Dot Filenames | Zero multiple dots (e.g., `admins.js`, `roles.js`, `customers.js`, `timeFilters.js`, `reports.js`, `dashboard.js`, `app.js`, `auth.js`, `common.js`). Forbidden: `*.controller.js`, `*.service.js`, `*.module.js` |
| **Static Script Directory** | Categorized Domain Folders | `src/main/resources/static/js/{core, admin, customer, dashboard, report, timefilter}/` |
| **Database** | MySQL 8.0 (Docker) | Container: `twekl-mysql` (port 3306), db: `twekl_db`, user: `twekl_user`, pass: `twekl_pass_2026` |
| **Authentication** | Stateful Session-Based (`HttpSession`) | `HttpSessionSecurityContextRepository` with `JSESSIONID` cookies; strictly NO JWT / Bearer tokens |
| **Remote Repository** | GitHub | `https://github.com/duduwui/twekle_management_dashboard.git` (`main` branch) |

---

## 2. Workspace & Execution Commands

- **Primary Workspace Directory:** `/home/emz/Desktop/twekl-dashboard`

### Standard Commands:
```bash
# Run unit and integration tests
mvn test

# Compile backend classes and templates
mvn compile

# Launch embedded Tomcat 10.1 server on port 8080
mvn exec:java -Dexec.mainClass="com.twekl.dashboard.TomcatServer"

# Hot-sync templates/static assets during development without server restart
cp -ru src/main/resources/* target/classes/
```

---

## 3. Database Credentials & Access Commands

### Connection Details:
* **Host:** `127.0.0.1` (or `localhost`)
* **Port:** `3306`
* **Database Name:** `twekl_db`
* **Username:** `twekl_user`
* **Password:** `twekl_pass_2026`
* **Root Password:** `root`
* **JDBC URL:** `jdbc:mysql://127.0.0.1:3306/twekl_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8&useUnicode=true`

### Docker Container Commands:
* **Docker Container Name:** `twekl-mysql`
* **Start Container:** `docker start twekl-mysql`
* **Check Status:** `docker ps`
* **Open MySQL Shell:**
  ```bash
  docker exec -it twekl-mysql mysql -utwekl_user -ptwekl_pass_2026 --default-character-set=utf8mb4 twekl_db
  ```
* **One-Liner Query Command:**
  ```bash
  docker exec -i twekl-mysql mysql -utwekl_user -ptwekl_pass_2026 --default-character-set=utf8mb4 twekl_db -e "SELECT * FROM TIME_FILTER_PRESETS;"
  ```

### Default Credentials:
* **Super Admin Username:** `twekl_super_admin`
* **Super Admin Password:** `Super@2026`

---

## 4. Backend Architecture: Portal Partition (`administration`)

Following senior developer guidelines, the codebase implements the **Windows Disk Partition pattern** to cleanly isolate portals. All components for the current management portal live inside the `administration` partition. When future portals (such as a customer, vendor, or driver portal) are introduced, they will reside in their own dedicated root package (`com.twekl.dashboard.<portal>`) and views folder (`src/main/webapp/WEB-INF/views/<portal>/`) without collision.

```text
com.twekl.dashboard/
├── TomcatServer.java                          # Embedded Tomcat bootstrap launcher
└── administration/                            # Current portal partition
    ├── config/                                # Spring MVC, Security, JPA, DataInitializer
    │   ├── AppInitializer.java
    │   ├── DataInitializer.java
    │   ├── JpaConfig.java
    │   ├── RestAccessDeniedHandler.java
    │   ├── RestAuthenticationEntryPoint.java
    │   ├── SecurityConfig.java
    │   ├── UpperCasePhysicalNamingStrategy.java
    │   └── WebMvcConfig.java
    ├── controller/
    │   ├── admin/                             # Admin Staff & Authentication Controllers
    │   │   ├── AdminStaffController.java
    │   │   └── AuthController.java
    │   ├── role/                              # Dedicated Role Domain Controller
    │   │   └── RoleController.java
    │   ├── customer/                          # Dedicated Customer Domain Controller
    │   │   └── CustomerController.java
    │   ├── order/                             # Dedicated Order & Follow-up Domain Controllers
    │   │   ├── OrderController.java
    │   │   └── FollowupController.java
    │   ├── report/                            # Dashboard stats & Report Controllers
    │   │   ├── DashboardStatsController.java
    │   │   └── ReportController.java
    │   ├── timefilter/                        # Timeline & time filter presets Controller
    │   │   └── TimelinePresetController.java
    │   └── web/                               # Thymeleaf view routing (/admin/*, /login)
    │       └── AdminWebController.java
    ├── dto/                                   # Data Transfer Objects organized by domain
    │   ├── admin/                             # CreateAdminRequest, UpdateAdminRequest, LoginRequest, AdminPermissionDto
    │   ├── role/                              # RoleDto
    │   ├── customer/                          # CustomerFollowupSummary
    │   ├── order/                             # FollowupCheckRequest
    │   ├── report/                            # SatisfactionReportDto
    │   └── timefilter/                        # TimeFilterPresetDto
    ├── exception/                             # Global exception handler & custom exceptions
    │   ├── BadRequestException.java
    │   ├── GlobalExceptionHandler.java
    │   └── ResourceNotFoundException.java
    ├── model/                                 # JPA Persistent Entities
    │   ├── admin/                             # Admin, AdminPermission
    │   ├── role/                              # Role (decoupled domain)
    │   ├── customer/                          # Customer (decoupled domain)
    │   ├── order/                             # CustomerOrder, OrderFollowupCheck, CustomerFeedback (decoupled domain)
    │   └── timefilter/                        # TimeFilterPreset
    ├── repository/                            # Spring Data JPA Repositories
    │   ├── admin/                             # AdminRepository
    │   ├── role/                              # RoleRepository
    │   ├── customer/                          # CustomerRepository
    │   ├── order/                             # CustomerOrderRepository, OrderFollowupCheckRepository, CustomerFeedbackRepository
    │   └── timefilter/                        # TimeFilterPresetRepository
    ├── response/                              # Unified ApiResponse<T>, ErrorResponse, AuthResponse
    ├── security/                              # Custom UserDetailsService and security components
    ├── service/                               # Transactional Business Services
    │   ├── admin/                             # AdminService, AuthService
    │   ├── role/                              # RoleService (decoupled domain)
    │   ├── customer/                          # CustomerService
    │   ├── report/                            # ReportService
    │   └── timefilter/                        # TimeFilterPresetService
    └── util/                                  # Shared utilities (DateUtil)
```

### Backend Design Rules:
1. **Clean Controller Naming (Strictly Zero `*ApiController`):** Every controller is named `*Controller.java`. Rest controllers provide data endpoints without using `Api` in class or method names.
2. **Clean Controller URLs (Zero `api` in primary routes):** Primary routes use canonical clean URLs (`/admin/admins`, `/admin/roles`, `/admin/customers`, `/admin/orders`, `/admin/followups`, `/admin/timelines`, `/admin/reports`, `/admin/stats`, `/admin/auth`). Backward compatibility aliases (`/admin/api/**`, `/internal/**`, `/api/**`) are retained for legacy clients.
3. **Decoupled Business Domains:**
   - **Role domain** (`model/role/`, `repository/role/`, `service/role/`, `controller/role/`, `dto/role/`, `views/administration/role/roles.html`, `js/role/roles.js`) is strictly isolated from Admin staff.
   - **Order domain** (`model/order/`, `repository/order/`, `controller/order/`, `dto/order/`, `js/order/orders.js`) is strictly isolated from Customer directory.
4. **Explicit Domain ID Naming (Zero Knots in Developer Mind):**
   - Entities provide explicit ID accessors alongside generic `getId()`: `getOrderId()`, `getCustomerId()`, `getRoleId()`, `getAdminId()`, `getCheckId()`, `getPresetId()`, `getFeedbackId()`.
   - Method signatures and path variables declare explicit domain identifiers (e.g. `@PathVariable("orderId") Long orderId`, `@PathVariable("customerId") Long customerId`, `@PathVariable("roleId") Long roleId`), preventing confusion when passing parameters across domains.
5. **Spring Security Responsibility:** Authentication and authorization decisions belong in `SecurityConfig.java`, not inside REST controller bodies.
6. **Pure Relational Mapping:** Persistent JPA entities represent normalized database tables.
7. **No `@Transient` UI Pollution:** Computed fields are housed in projection DTOs (e.g. `CustomerFollowupSummary.java`).
8. **No Duplicate Columns:** Do not duplicate fields (e.g., use `notes` not `note` + `notes`, use `completed` not `isCompleted` + `completed`).
9. **Lazy Fetching by Default:** Use `fetch = FetchType.LAZY` on collections to prevent N+1 query cascades.

---

## 5. View Hierarchy & Partitioned Views

All Thymeleaf views are isolated within the `administration` portal directory under `src/main/webapp/WEB-INF/views/administration/`:

```text
src/main/webapp/WEB-INF/views/administration/
├── login.html                                 # Standalone login screen
├── dashboard/
│   └── dashboard.html                         # Executive overview dashboard
├── admin/
│   └── admins.html                            # Administrators listing & management modal
├── role/
│   └── roles.html                             # Role and permission templates (isolated domain)
├── customer/
│   └── customers.html                         # Follow-up lifecycle & customer timeline
├── timefilter/
│   └── timeFilters.html                       # Dynamic time filter presets
├── report/
│   └── reports.html                           # Customer satisfaction & milestone reports
└── fragments/
    ├── adminLayout.html                       # Master layout (<html layout:decorate="...">)
    └── admin-dashboard-fragments.html         # Sidebar, header, modals, toasts, scripts
```

### Layout Template Pattern:
Each view decorates `administration/fragments/adminLayout`:
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{administration/fragments/adminLayout}">
<head>
    <title>
        <th:block layout:fragment="title">Page Title | Twekl Administration</th:block>
    </title>
</head>
<body>
    <div layout:fragment="content">
        <!-- Page body -->
    </div>
</body>
</html>
```

---

## 6. Frontend Script & Resource Partitioning

All static assets belonging to the administration portal are wrapped under `src/main/resources/administration/`:

```text
src/main/resources/administration/
├── css/
│   └── style.css                              # Core administration stylesheets
├── images/
│   └── twekl-logo.png                         # Portal branding logo
└── js/
    ├── core/
    │   ├── app.js                             # Angular module config & LayoutController
    │   ├── auth.js                            # AuthService
    │   └── common.js                          # ToastService, I18nService, FilterHelperService
    ├── admin/
    │   └── admins.js                          # AdminService + AdminController
    ├── role/
    │   └── roles.js                           # RoleService + RoleController (isolated domain)
    ├── customer/
    │   └── customers.js                       # CustomerService + FollowupController
    ├── order/
    │   └── orders.js                          # OrderService + FollowupService (isolated domain)
    ├── dashboard/
    │   └── dashboard.js                       # DashboardStatsService + DashboardController
    ├── report/
    │   └── reports.js                         # ReportService + ReportController
    └── timefilter/
        └── timeFilters.js                     # TimeFilterService + TimeFilterController
```

### Frontend Design Rules:
1. **NO Multiple Dots in Filenames:** Filenames must strictly contain only a single extension dot (`.js`).
   - [OK] Correct: `admins.js`, `roles.js`, `customers.js`, `orders.js`, `timeFilters.js`, `reports.js`, `dashboard.js`, `app.js`, `auth.js`, `common.js`
   - [FORBIDDEN]: `admin.controller.js`, `app.module.js`, `auth.service.js`, `layout.controller.js`
2. **Controller + Service in ONE File:** Each feature screen combines its controller and service into **ONE single file**.
3. **Dependency Import Order in `admin-dashboard-fragments.html`:**
```html
<!-- Core Foundation -->
<script src="/js/core/app.js?v=7.0"></script>
<script src="/js/core/common.js?v=7.0"></script>
<script src="/js/core/auth.js?v=7.0"></script>

<!-- Feature Domains -->
<script src="/js/dashboard/dashboard.js?v=7.0"></script>
<script src="/js/admin/admins.js?v=7.0"></script>
<script src="/js/role/roles.js?v=7.0"></script>
<script src="/js/customer/customers.js?v=7.0"></script>
<script src="/js/order/orders.js?v=7.0"></script>
<script src="/js/timefilter/timeFilters.js?v=7.0"></script>
<script src="/js/report/reports.js?v=7.0"></script>
```

---

## 7. URL & Route Mapping

All dashboard routes use clean, canonical URLs under the `administration` portal prefix `/admin`:

### Web View Routes:
- `/login` (Sign In Screen)
- `/admin/dashboard` (Executive Overview Dashboard)
- `/admin/customers` (Follow-up Lifecycle & Customer Timelines)
- `/admin/admins` (Administrators Management)
- `/admin/roles` (Roles & Permissions Management)
- `/admin/timelines` (Dynamic Time Presets)
- `/admin/reports` (Customer Satisfaction & Milestone Reports)

### Pure Thymeleaf SSR Endpoints (Explicit Semantic Actions & PRG Pattern):
- Staff Administration (`/admin/admins`):
  - `POST /admin/admins/create-admin` -> `createAdmin(...)`
  - `POST /admin/admins/{adminId}/toggle-status` -> `toggleAdminStatus(...)`
  - `POST /admin/admins/{adminId}/delete-admin` -> `deleteAdmin(...)`
- Role Templates (`/admin/roles`):
  - `POST /admin/roles/create-role` -> `createRole(...)`
  - `POST /admin/roles/{roleId}/update-role` -> `updateRole(...)`
  - `POST /admin/roles/{roleId}/delete-role` -> `deleteRole(...)`
- Timelines & Presets (`/admin/timelines`):
  - `POST /admin/timelines/create-preset` -> `createTimelinePreset(...)`
  - `POST /admin/timelines/{presetId}/toggle-status` -> `togglePresetStatus(...)`
  - `POST /admin/timelines/{presetId}/delete-preset` -> `deleteTimelinePreset(...)`

---

## 8. Database Naming & SQL Standards: STRICT ALL UPPERCASE (Permanent Standard)

> **MANDATORY RULE FOR ALL FUTURE AI AGENTS & DEVELOPERS:**
> Every SQL/JPQL keyword, database table, column, sequence, and join table MUST STRICTLY BE IN UPPERCASE. Never write or generate lowercase table names, column names, or SQL keywords.

### 1. SQL & JPQL Keywords:
All queries (`@Query`, native SQL, JPQL, HQL) **MUST** have all keywords capitalized:
- [REQUIRED]: `SELECT`, `FROM`, `WHERE`, `JOIN`, `LEFT JOIN FETCH`, `ORDER BY`, `GROUP BY`, `INSERT INTO`, `UPDATE`, `DELETE`, `ASC`, `DESC`, `CASE`, `WHEN`, `THEN`, `ELSE`, `END`, `LOWER`, `UPPER`, `DISTINCT`, `LIMIT`
- [FORBIDDEN]: `select`, `from`, `where`, `join`, `order by`, `delete`

### 2. Database Tables & Columns:
All database identifiers are strictly **UPPERCASE `SNAKE_CASE`**:
- **Tables:** `CUSTOMERS`, `CUSTOMER_ORDERS`, `ADMINS`, `ROLES`, `ADMIN_ROLES`, `CUSTOMER_FEEDBACKS`, `TIME_FILTER_PRESETS`, `ORDER_FOLLOWUP_CHECKS`, `FLYWAY_SCHEMA_HISTORY`
- **Columns:** `ID`, `NAME`, `PHONE_NUMBER`, `CREATED_AT`, `UPDATED_AT`, `TOTAL_SPENT`, `LAST_ORDER_DATE`, `ORDER_NUMBER`, `ITEMS_SUMMARY`, etc.
- **Physical Naming Strategy:** Hibernate uses `com.twekl.dashboard.administration.config.UpperCasePhysicalNamingStrategy` in `JpaConfig.java` to enforce uppercase naming on every entity and attribute.
- **Entity Annotations:** All `@Table(name = "...")`, `@Column(name = "...")`, `@JoinColumn(name = "...")`, and `@JoinTable(name = "...")` annotations **must** be written explicitly in UPPERCASE (e.g. `@Table(name = "CUSTOMERS")`).
- **Linux Case-Sensitivity:** MySQL runs on Linux with `lower_case_table_names=0`. Queries with lowercase table names will fail with `ERROR 1146: Table doesn't exist`. Always use uppercase table names in raw queries.

---

## 9. Modern Java 21 & Spring 6 Code Conventions

1. **Temporal Fields (`Instant`):**
   - Strictly use `java.time.Instant` for all timestamps across entities, DTOs, API envelopes (`ApiResponse<T>`), filters, and helpers.
   - Do **NOT** use `LocalDateTime` or `java.util.Date`.
   - Produces standard ISO-8601 UTC timestamps ending with `Z`.

2. **Dependency Injection:**
   - Use **Constructor Injection** with `final` fields.
   - Do **NOT** use `@Autowired` on fields or single constructors. Modern Spring 6 resolves single constructors automatically.

3. **Data Transfer Objects (DTOs):**
   - Request DTOs and immutable data models use Java 21 `record` with backward-compatible getter bridges where needed.
