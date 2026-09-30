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

Always maintain parity between these two local local directories:
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
- **Customer Compliments / Notes & Photos:** `/admin/followups/feedback/:id`
- **Admin Management:** `/admin/admins`
- **Admin Actions:** `/admin/admins/create`, `/admin/admins/inspect/:id`, `/admin/admins/delete/:id`
- **Users & Permissions:** `/admin/users`
- **User Actions:** `/admin/users/create`, `/admin/users/inspect/:id`, `/admin/users/update/:id`, `/admin/users/delete/:id`
- **Authentication:** `/login`

> **Backend Mapping:** In `WebViewController.java` and `SecurityConfig.java`, all `/admin/**` and SPA paths forward to `index.html` with HTTP 200 so direct reloads and bookmarks never return 404 or incomplete chunk errors.

---

## 5. UI Architecture & Core Features

### 1. Follow-ups Table & Order Log Integration:
- In the main Follow-ups table, the **"Remaining Follow-ups"** column dynamically displays only the milestone duration (e.g. `24 Hours Ago`, `1 Week Ago`, or `✓ All Done` with no `(ORD-...)` suffix).
- The milestone badge is a clickable interactive button (`.comic-milestone-link-btn`). Clicking it immediately opens that customer's **Orders Log** and **auto-expands that specific pending order checklist**.

### 2. View-Only Order Log vs. Compliments Tree & Cross-Navigation:
- **Section 4.2 (Order Log):** View-only audit mode. Users can toggle status checkboxes (✔ / ✖) and view existing notes / photos.
  - Each milestone card has a direct `+ Note / Photo ➔` button that seamlessly transitions the user to the Compliments & Reviews view, expands that order, and opens the note/photo modal immediately for that specific checkpoint.
  - Order row includes a `Compliments ➔` button for quick switching.
- **Section 4.3 (Compliments / Reviews & Notes):** Exact replica tree of the Order Log. Checkpoints feature a `+ Add / Edit Note & Photo` button to upload notes and proof photos.
  - Order row includes an `Order Log ➔` button to jump back to audit mode.
- **Data Synchronization:** Both Section 4.2 and Section 4.3 read from and write to the same `order_followup_checks` database table. Any status mark, note, or photo edit is automatically synchronized in real time between both views.

### 3. Unified Users & Permissions Tab:
- Users and Role configuration are unified under **Users & Permissions** (`/admin/users`).
- **Top Segmented Switcher:** `[ Create & Configure User ]` and `[ All Users Directory ]` (no distracting emoji clutter).
- **User Profile Creation:**
  - Multilingual user names: **English**, **Arabic (`dir="rtl"`)**, and **Kurdish (`dir="rtl"`)**.
  - Password (phone number and status inputs removed from create form to streamline user onboarding).
  - **3-Module Matrix:** Software, Sales, and Product Management with individual Create, Read, Update, Delete switches and preset buttons (`Full CRUD`, `Read Only`, `Revoke`).
  - Module cards feature clear, high-contrast dark text and tags (`#000000` / `#0F172A`) for maximum readability.
- **All Users Directory:** Clean DataTable with Date Range filters, dynamic Time Filter presets modal, search, multilingual name badges, and action modals (`Permissions Matrix`, `Edit`, `Delete`).

### 4. Table Formatting & Eye-Comfort Design:
- Column widths and table padding (`10px 13px`) fit 100% of standard desktop screens without horizontal side-scrolling.
- Removed harsh red/green thick inset borders and tinted backgrounds from table rows for a calm, comfortable visual experience.
- Phone numbers, counts, dates, prices, and status badges remain strictly on a single line (`white-space: nowrap !important;`).
- Use classes: `.nowrap`, `.col-phone`, `.col-num`, `.col-date`, `.col-action`.

### 5. Comic / Neobrutalism Design Rules:
- **Borders:** Crisp black borders (`2px solid #000000` or `var(--border-black)`).
- **Shadows:** Hard offset shadows (`box-shadow: var(--shadow-comic-md)`).
- **Palette:** Twekl Teal (`#35B89F` / `#229E86`), Dark Slate (`#0F172A`), Pale Canvas (`#FDFCF7`), and Red Accent (`#EF4444`).
- **Modals:** Centered card with clean header, structured body, and separate footer buttons.

### 6. Dynamic Time Filter Presets:
- Administrators can configure reusable time filter presets (e.g., *Last 24 Hours*, *Last 7 Days*, *Last 30 Days*, *Last 6 Months*).
- Presets are selected through a clean 3-column modal popup (`Time Filters`) with straightforward checkboxes and a confirm button.

### 7. Thymeleaf Template Integrity:
- Never include duplicate HTML attributes (e.g. duplicate `class="..."` or `style="..."` on the same HTML tag) in `index.html` to avoid `ERR_INCOMPLETE_CHUNKED_ENCODING`.

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
