# Twekl Management Dashboard — Agent Architecture & Engineering Guide (`AGENT.md`)

> **Note for Future AI Agents:** This document is the single source of truth for the architecture, coding standards, design system, database setup, and user preferences of the **Twekl Management Dashboard**. Follow these principles strictly when continuing development.

---

## 1. Project Overview & Tech Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend** | Spring Boot 3.3.4 (Java 21) | Spring Data JPA, Spring Security 6, Thymeleaf, Lombok, Maven |
| **Frontend** | AngularJS 1.8.2 + Modern JS | Single Page App (SPA) architecture within Thymeleaf |
| **Styling** | Comic / Neobrutalism CSS | Custom CSS (`style.css`), unified Twekl Teal theme, bold borders, retro comic buttons |
| **Database** | MySQL 8.0 (Docker) | Container: `twekl-mysql` (port 3306), db: `twekl_db`, user: `twekl` |
| **Routing** | HTML5 History API | Standard clean URLs without `#` hash (e.g. `/admin/followups`, `/admin/users`) |
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

# 4. Fast-forward / sync Desktop repo
cd /home/emz/Desktop/twekl-dashboard && git pull --rebase origin main
```

---

## 3. Database & Server Management

### MySQL in Docker:
The database runs as a Docker container named `twekl-mysql`.
- **Start Database:** `docker start twekl-mysql`
- **Check Status:** `docker ps`
- **Database Connection:** `jdbc:mysql://localhost:3306/twekl_db` (User: `twekl`, Pass: `twekl_pass`)

### Running the Application:
- **Run Backend:** `mvn spring-boot:run` (runs on `http://localhost:8080`)
- **Default Super Admin:** Username: `twekl_super_admin`, Password: `Super@2026`

---

## 4. URL & Routing Standards (Crucial Rule: No `#` Hash)

All dashboard pages **must** use clean, standard URLs via the HTML5 History API (`window.history.pushState` / `popstate`). Never revert to AngularJS hash `#` routing.

### Route Mapping:
- **Main Follow-ups Table:** `/admin/followups`
- **Customer Orders & Product Log:** `/admin/followups/orders/:id`
- **Customer Notes & Photos:** `/admin/followups/feedback/:id`
- **Admin Management:** `/admin/admins`
- **Admin Actions:** `/admin/admins/create`, `/admin/admins/inspect/:id`, `/admin/admins/delete/:id`
- **Users & Permissions:** `/admin/users`
- **User Actions:** `/admin/users/create`, `/admin/users/inspect/:id`, `/admin/users/update/:id`, `/admin/users/delete/:id`
- **Role Templates:** `/admin/role-templates`
- **Authentication:** `/login`

> **Backend Mapping:** In [`WebViewController.java`](file:///home/emz/.gemini/antigravity/scratch/twekl-dashboard/src/main/java/com/twekl/dashboard/controller/WebViewController.java) and [`SecurityConfig.java`](file:///home/emz/.gemini/antigravity/scratch/twekl-dashboard/src/main/java/com/twekl/dashboard/config/SecurityConfig.java), all `/admin/**` and SPA paths forward to `index.html` with HTTP 200 so direct reloads and bookmarks never return 404.

---

## 5. Coding Conventions & Style Rules

### 1. Naming & Labels:
- Always use **"Follow-ups"** (never "Customer Follow-up" or "Followup").
- Keep terminology consistent across tables, badges, headers, and navigation.

### 2. Table Column Wrapping (Single-Line Rule):
- Phone numbers, numeric badge counts, dates, prices, and status pills across **all** tables must **always remain on a single line** (`white-space: nowrap !important;`).
- Use classes: `.nowrap`, `.col-phone`, `.col-num`, `.col-date`, `.col-action`.

### 3. Comic / Neobrutalism UI Rules:
- **Borders:** Crisp black borders (`2px solid #000000` or `var(--border-black)`).
- **Shadows:** Hard offset shadows (`box-shadow: var(--shadow-comic-md)`).
- **Palette:** Twekl Teal (`#35B89F` / `#229E86`), Dark Slate (`#0F172A`), Pale Canvas (`#FDFCF7`), and Red Accent (`#EF4444`).
- **Modals:** Centered card with clean header, structured body, and separate footer buttons.

### 4. Milestone Progression Logic:
- In the main Follow-ups table, the **"Remaining Order Follow-ups"** column dynamically displays the earliest pending milestone and order number (e.g. `24 Hours Ago (ORD-805)`).
- As checkpoints are checked off in the purchase history tree, the table advances sequentially to the next pending milestone.
- When all milestones across all orders are done, the badge displays **`✓ All Done`** (with no remaining count numbers).

### 5. Notes & Image Uploading:
- Customer notes support attached photos (proof / WhatsApp screenshots).
- Images are compressed in the browser via HTML5 Canvas before sending as Base64 strings to `LONGTEXT` columns.
- Clickable thumbnails open a high-res lightbox preview.
- Do not add star ratings or category tags to notes; keep them simple (Customer profile header, order association, note text, and photo).

### 6. Dynamic Time Filter Presets:
- Administrators can configure reusable time filter presets (e.g., *Last 24 Hours*, *Last 7 Days*, *Last 30 Days*, *Last 6 Months*).
- Presets are selected through a clean 3-column modal popup (`Time Filters`) with straightforward checkboxes and a confirm button.

---

## 6. Project Directory Layout

```text
twekl-dashboard/
├── pom.xml                                    # Maven Build Configuration
├── AGENT.md                                   # Architecture & Agent Rules (This File)
├── src/main/java/com/twekl/dashboard/
│   ├── config/
│   │   ├── SecurityConfig.java                # Spring Security rules & SPA route permissions
│   │   ├── DataInitializer.java               # Database seeding & LONGTEXT migrations
│   │   └── RestAuthenticationEntryPoint.java  # 401/403 handlers
│   ├── controller/
│   │   ├── WebViewController.java             # SPA route controller (forwards to index.html)
│   │   ├── AuthApiController.java             # Login/Logout & session endpoints
│   │   ├── AdminApiController.java            # Administrator CRUD
│   │   ├── UserApiController.java             # Users & 3-Module permission CRUD
│   │   ├── RoleApiController.java             # Role template CRUD
│   │   ├── CustomerApiController.java         # Customers, Orders, Feedback & Follow-ups
│   │   └── TimeFilterPresetApiController.java # Dynamic time filter presets
│   ├── model/                                 # JPA Entities
│   │   ├── AdminUser.java
│   │   ├── User.java
│   │   ├── RoleTemplate.java
│   │   ├── Customer.java
│   │   ├── CustomerOrder.java
│   │   ├── OrderFollowupCheck.java
│   │   ├── CustomerFeedback.java
│   │   └── TimeFilterPreset.java
│   ├── repository/                            # Spring Data JPA Repositories
│   └── service/                               # Business Logic & Totals Syncing
└── src/main/resources/
    ├── application.properties                 # Server port (8080) & MySQL config
    ├── templates/
    │   └── index.html                         # SPA Main HTML & Modals
    └── static/
        ├── css/style.css                      # Comic / Neobrutalism Design System
        ├── js/app.js                          # AngularJS 1.8.2 Controller & API Logic
        └── images/                            # Twekl brand logo & assets
```

---

## 7. Working With the User (Best Practices)

- **Be Direct & Proactive:** Execute commands, compile with Maven, and restart servers autonomously.
- **Maintain Code Integrity:** Do not remove existing docstrings, features, or routes unless explicitly asked.
- **Keep Both Workspaces Synced:** Always push clean commits to `origin/main` and ensure `/home/emz/Desktop/twekl-dashboard` is mirrored.
