# Twekl Management Dashboard — Agent Architecture & Engineering Guide (`AGENT.md`)

> **Note for Future AI Agents:** This document is the single source of truth for the architecture, coding standards, design system, database setup, and user preferences of the **Twekl Management Dashboard**. Follow these principles strictly when continuing development.

---

## 1. Project Overview & Tech Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend** | Pure Spring MVC 6.1.13 + Jakarta EE 10 | **ZERO Spring Boot**. Embedded Apache Tomcat 10.1.30, `AppInitializer.java`, `WebMvcConfig.java`, `SecurityConfig.java`, Hibernate 6.5.3, Lombok, Maven WAR packaging |
| **Frontend** | AngularJS 1.8.2 (Action-Level Micro-Nested) | Single Page Application (SPA) architecture within Thymeleaf. Every action/sub-feature is in its own isolated micro-file |
| **Services** | Super-Micro Angular Services (`static/app/services/`) | Single-responsibility, 15–30 line services communicating strictly via `/internal/*` |
| **Styling** | Comic / Neobrutalism CSS | Custom CSS (`style.css`), unified Twekl Teal theme, bold black borders, hard offset shadows |
| **Database** | MySQL 8.0 (Docker) | Container: `twekl-mysql` (port 3306), db: `twekl_db`, user: `twekl_user`, pass: `twekl_pass_2026` |
| **Authentication** | Session-Based (Stateful) | `HttpSession` with `JSESSIONID` cookies; strictly NO JWT / Bearer tokens |
| **Routing** | HTML5 History API | Clean URLs without `#` hash (e.g. `/admin/followups`, `/admin/users`, `/admin/admins`) |
| **Git Remote** | GitHub Repository | `https://github.com/duduwui/twekle_management_dashboard.git` (`main` branch) |

---

## 2. Workspaces & Git Syncing Workflow

Always maintain parity between these two local directories:
1. **Active Primary Workspace:** `/home/emz/.gemini/antigravity/scratch/twekl-dashboard`
2. **Desktop Mirror & Export:** `/home/emz/Desktop/twekl-dashboard`

### Standard Sync & Deployment Routine:
Whenever you make changes, compile, test, rsync to Desktop, and commit:
```bash
# 1. Compile and verify backend
mvn clean compile

# 2. Sync changes to Desktop
rsync -av --delete --exclude='.git' --exclude='target' /home/emz/.gemini/antigravity/scratch/twekl-dashboard/ /home/emz/Desktop/twekl-dashboard/

# 3. Commit and push from scratch
git add -A
git commit -m "feat: <description>"
git push origin main
```

---

## 3. Database & Server Management

### MySQL in Docker:
The database runs as a Docker container named `twekl-mysql`.
- **Start Database:** `docker start twekl-mysql`
- **Check Status:** `docker ps`
- **Database Connection:** `jdbc:mysql://localhost:3306/twekl_db` (User: `twekl_user`, Pass: `twekl_pass_2026`)

### Running the Application (Pure Spring MVC on Tomcat 10):
- **Run Backend:** `mvn compile exec:java` (launches `com.twekl.dashboard.TomcatServer` on `http://localhost:8080`)
- **Default Super Admin:** Username: `twekl_super_admin`, Password: `Super@2026`

---

## 4. Frontend Architecture: Action-Level Micro-Nesting

To guarantee extreme maintainability, clean debugging, and zero cognitive load:
1. **Zero Monoliths:** No multi-thousand-line or 100+ line catch-all controllers.
2. **Single Responsibility Files:** Each micro-controller handles exactly **one** single action or view (e.g. Create Admin, Delete Admin, List Admins, Inspect Admin).
3. **Super-Micro Services:** Services are thin API callers (15-30 lines) returning `$http` promises for `/internal/*` endpoints.

```text
src/main/resources/static/app/
├── app.module.js                              # Module definition & 401/403 interceptors
├── app.controller.js                          # Root coordinator (< 10 lines)
├── services/                                  # Super-micro API service callers (~20 lines each)
│   ├── auth.service.js
│   ├── admin.service.js
│   ├── user.service.js
│   ├── customer.service.js
│   ├── followup.service.js
│   ├── time-filter.service.js
│   ├── report.service.js
│   ├── role.service.js
│   ├── toast.service.js
│   ├── filter-helper.service.js
│   └── i18n.service.js
└── features/                                  # Domain features with micro-actions & views
    ├── admin/
    │   ├── admin.controller.js                # Micro-controller (or action-specific controllers)
    │   └── views/                             # Sub-views (list, create, inspect, delete)
    ├── users/
    │   ├── user.controller.js
    │   └── views/
    ├── followups/
    │   ├── followup.controller.js
    │   └── views/
    ├── time-filters/
    │   ├── time-filter.controller.js
    │   └── views/
    ├── reports/
    │   ├── report.controller.js
    │   └── views/
    └── layout/
        ├── layout.controller.js
        └── views/
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
- **Authentication:** `/login`

---

## 6. Comic / Neobrutalism Design Rules

- **Borders:** Crisp black borders (`2px solid #000000` or `var(--border-black)`).
- **Shadows:** Hard offset shadows (`box-shadow: var(--shadow-comic-md)`).
- **Palette:** Twekl Teal (`#35B89F` / `#229E86`), Dark Slate (`#0F172A`), Pale Canvas (`#FDFCF7`), and Red Accent (`#EF4444`).
- **Tables:** Single-line data cells (`white-space: nowrap !important;`), calm background colors, zero numeric badge noise in sidebar.

---

## 7. Working With the User (Best Practices)

- **Be Direct, Honest & Precise:** Always verify the codebase state directly before answering.
- **Micro-Nesting First:** Break down any complex logic or controllers into micro-action units.
- **Synchronize Workspaces:** Keep `/home/emz/Desktop/twekl-dashboard` and scratch workspace in 100% sync.
