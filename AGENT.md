# Twekl Management Dashboard — Agent Architecture & Engineering Guide (`AGENT.md`)

> **Note for Future AI Agents:** This document is the single source of truth for the architecture, coding standards, design system, database setup, and user preferences of the **Twekl Management Dashboard**. Follow these principles strictly when continuing development.

---

## 1. Project Overview & Tech Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend** | Pure Spring MVC 6.1.13 + Jakarta EE 10 | **ZERO Spring Boot**. Embedded Apache Tomcat 10.1.30, `AppInitializer.java`, `WebMvcConfig.java`, `SecurityConfig.java`, `JpaConfig.java`, Hibernate 6.5.3, Lombok, Maven WAR/Jar execution |
| **Frontend** | AngularJS 1.8.2 (Feature-Cohesive Architecture) | Single Page Application (SPA) architecture within Thymeleaf. Sub-views partitioned by action drill-down (`ng-include`) |
| **Services** | Unified Feature Services (`static/app/services/`) | Single-responsibility, cohesive domain services communicating via `/internal/*` and fallback `/api/*` |
| **Styling** | Comic / Neobrutalism CSS | Custom CSS (`style.css`), unified Twekl Teal theme, crisp black borders (`2px solid #000`), hard offset shadows |
| **Database** | MySQL 8.0 (Docker) | Container: `twekl-mysql` (port 3306), db: `twekl_db`, user: `twekl_user`, pass: `twekl_pass_2026` |
| **Authentication** | Session-Based (Stateful) | `HttpSession` with `JSESSIONID` cookies; strictly NO JWT / Bearer tokens |
| **Routing** | HTML5 History API | Clean URLs without `#` hash (e.g. `/admin/followups`, `/admin/users`, `/admin/admins`, `/admin/reports`) |
| **Git Remote** | GitHub Repository | `https://github.com/duduwui/twekle_management_dashboard.git` (`main` branch) |

---

## 2. Workspace & Git Workflow

The canonical project workspace is:
* **Primary Workspace Directory:** `/home/emz/Desktop/twekl-dashboard`

### Standard Build & Run Commands:
```bash
# Compile and verify backend
mvn clean compile

# Run the Tomcat server on port 8080
mvn exec:java

# Hot-sync templates/static assets during development without restart
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
  docker exec -i twekl-mysql mysql -utwekl_user -ptwekl_pass_2026 --default-character-set=utf8mb4 twekl_db -e "SELECT * FROM time_filter_presets;"
  ```

### Default Super Admin Credentials:
* **Username:** `twekl_super_admin`
* **Password:** `Super@2026`

---

## 4. Frontend Architecture: Feature-Cohesive, Action-Drilldown Pattern

To prevent AngularJS scope shadowing, event cascade overhead, and script-tag bloat:
1. **Views (Action-Level Drill-Down):** Sub-views are partitioned by action (`admin-list.html`, `admin-create.html`, `admin-inspect.html`, `admin-delete.html`) loaded dynamically via `ng-include`.
2. **Feature Coordinator Controllers:** One authoritative controller per feature domain (`admin.controller.js`, `user.controller.js`, `followup.controller.js`, `time-filter.controller.js`, `report.controller.js`).
3. **Global Layout Coordinator:** `layout.controller.js` manages root modal overlays (`modals.html`), sidebar accordions, current route synchronization, and global presets.
4. **Unified Feature Services:** Single-responsibility domain services (`admin.service.js`, `user.service.js`, `customer.service.js`, `followup.service.js`, `time-filter.service.js`, `report.service.js`).

```text
src/main/resources/static/app/
├── app.module.js                              # Module definition & 401/403 interceptors
├── app.controller.js                          # Root coordinator
├── services/                                  # Domain API services
│   ├── auth.service.js
│   ├── admin.service.js
│   ├── user.service.js
│   ├── role.service.js
│   ├── customer.service.js
│   ├── followup.service.js
│   ├── time-filter.service.js
│   ├── report.service.js
│   └── common/                                # Shared utilities
│       ├── toast.service.js
│       ├── filter-helper.service.js
│       └── i18n.service.js
└── features/                                  # Domain features with drill-down views
    ├── admin/
    │   ├── controllers/admin.controller.js    # Feature state & action coordinator
    │   └── views/                             # Sub-views (list, create, inspect, delete)
    ├── users/
    │   ├── controllers/user.controller.js
    │   └── views/                             # Sub-views (list, create, update, delete, inspect)
    ├── followups/
    │   ├── controllers/followup.controller.js
    │   └── views/                             # Sub-views (list, orders, feedback)
    ├── time-filters/
    │   ├── controllers/time-filter.controller.js
    │   └── views/                             # Presets management view
    ├── reports/
    │   ├── controllers/report.controller.js
    │   └── views/                             # Sub-views (milestones, satisfaction, products, directory)
    └── layout/
        ├── controllers/layout.controller.js   # Shell, route syncing, modals & presets coordinator
        └── views/                             # Modals partial, header, sidebar
```

---

## 5. URL & Routing Standards (Crucial Rule: No `#` Hash)

All dashboard pages **must** use clean, standard URLs via the HTML5 History API (`window.history.pushState` / `popstate`). Never revert to AngularJS hash `#` routing.

### Route Mapping:
- **Main Follow-ups Table:** `/admin/followups`
- **Customer Orders & Product Log:** `/admin/followups/orders/:id`
- **Customer Compliments / Notes & Photos:** `/admin/followups/feedback/:id`
- **Admin Management:** `/admin/admins`
- **Admin Actions:** `/admin/admins/create`, `/admin/admins/inspect/:id`, `/admin/admins/delete/:id`
- **Users & Permissions:** `/admin/users`
- **User Actions:** `/admin/users/create`, `/admin/users/inspect/:id`, `/admin/users/update/:id`, `/admin/users/delete/:id`
- **Time Filters:** `/admin/time-filters`
- **Reports:** `/admin/reports`, `/admin/reports/milestones`, `/admin/reports/satisfaction`, `/admin/reports/products`, `/admin/reports/customers`
- **Authentication:** `/login`

---

## 6. Comic / Neobrutalism Design Rules

- **Borders:** Crisp black borders (`2px solid #000000` or `var(--border-black)`).
- **Shadows:** Hard offset shadows (`box-shadow: var(--shadow-comic-md)`).
- **Palette:** Twekl Teal (`#35B89F` / `#229E86`), Dark Slate (`#0F172A`), Pale Canvas (`#FDFCF7`), and Red Accent (`#EF4444`).
- **Tables:** Single-line data cells (`white-space: nowrap !important;`), calm background colors, zero numeric badge noise in sidebar.
- **Modals:** Must remain at the `<body>` root level ([`modals.html`](file:///home/emz/Desktop/twekl-dashboard/src/main/resources/static/app/features/layout/views/modals.html)) with `position: fixed` to ensure full-viewport backdrop dimming (0 to 100vw, 0 to 100vh). Never nest modal overlays inside `.comic-card` containers with CSS transforms.
