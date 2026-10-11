<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=9,10,16,22,25&height=220&section=header&text=Lost+%26+Found+Portal&fontSize=44&fontColor=fff&animation=twinkling&fontAlignY=35&desc=Futuristic+Dark+Glassmorphism+Campus+Management+System&descAlignY=65&descSize=16"/>

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Theme-Futuristic%20Dark%20Glass-6366f1?style=for-the-badge&logo=css3&logoColor=white"/>
  <img src="https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/Tests-18%2F18%20Passing-10b981?style=for-the-badge&logo=junit5&logoColor=white"/>
</div>

---

## 📖 Overview

The **Lost & Found Portal** is an enterprise-grade, full-stack campus management web application built with **Java Spring Boot**, **MySQL**, and an ultra-modern **Futuristic Dark Glassmorphism Dashboard UI** (inspired by modern fintech & SaaS control panels).

It replaces outdated physical bulletin boards with an automated digital ecosystem featuring:
- **Glowing Gradient Stat Cards & Neon Category Analytics** on a deep obsidian canvas (`#090a10`).
- **Rule-Based Automated Item Matching Engine** that scores similarity across 5 dimensions.
- **Dual-Proof Claim Verification Workflow** (requiring both Government ID and Product Proof).
- **Atomic Return Confirmation Workflow** (`@Transactional`) updating 4 database entities simultaneously.
- **8-Feature Admin Control Center** for campus authorities.
- **Persistent In-App Notification Stream** alerting finders and claimants in real time.

---

## 🌌 Modern Visual Experience

The application is styled with a sleek obsidian glass aesthetic:
- **Left Dark Sidebar**: Deep dark backdrop (`#0e101a`) with glowing violet logo badge, illuminated active tab indicators, and developer credit badge.
- **Top Row KPI Cards**:
  - 🔵 **Total Lost Items**: Radiant Royal Blue Gradient (`#3b82f6` $\rightarrow$ `#1d4ed8`) with upward-trend badge.
  - 🟢 **Found Items**: Radiant Emerald Green Gradient (`#10b981` $\rightarrow$ `#059669`) with hand-holding badge.
  - 🔴 **Returned Items**: Radiant Coral/Crimson Gradient (`#f43f5e` $\rightarrow$ `#be123c`) with checkmark badge.
  - 🟡 **Pending Claims**: Radiant Amber/Orange Gradient (`#f59e0b` $\rightarrow$ `#d97706`) with hourglass badge.
- **Recent Reports Stream**: Dark surface card with circular item icons, category tags, and glowing status capsules (`Active`, `Returned`, `Completed`, `Pending`).
- **Category Overview**: Futuristic neon progress bars with glowing drop shadows:
  - 🟠 **Electronics**: Glowing Amber Bar
  - 🟢 **Bags & Wallets**: Glowing Cyan-Green Bar
  - 🔴 **Documents & Cards**: Glowing Coral Bar
  - 🔵 **Keys & Accessories**: Glowing Electric Blue Bar

---

## ⚙️ Core Architecture & Workflows

### 1. 🤖 Intelligent Auto-Matching Engine
Whenever a student reports a lost or found item, Spring Boot's `MatchingService` scans existing opposite reports in the database:
- **Category Match:** $+30$ points
- **Title / Keyword Similarity (3+ letter words):** $+25$ points
- **Colour Match:** $+15$ points
- **Location Similarity:** $+20$ points
- **Date Proximity (within 30 days):** $+10$ points

When the cumulative score is **$\ge 50$**, the engine records an `ItemMatch` and dispatches **in-app notifications** to both the finder and the loser.

### 2. 📑 3-Stage Claim & Evidence Verification
Students claiming a found item must submit:
1. Claimant Details (Full Name, Age, Mobile Number).
2. Identification Details (Specific identifying private marks or serial numbers).
3. **Dual Evidence Upload:**
   - Government or College ID Proof Image.
   - Product Ownership Proof Image (bill, box, warranty, or past photo).

**Status Progression:**  
`PENDING_VERIFICATION` $\longrightarrow$ `APPROVED_AWAITING_HANDOVER` $\longrightarrow$ `COMPLETED` *(or `REJECTED` with review notes)*.

### 3. 🤝 Atomic Physical Return Handover
When the student visits the desk to collect their item:
1. Administrator inspects the physical ID against uploaded proof.
2. Administrator clicks **Confirm Return** in the Return Management portal.
3. In a single atomic `@Transactional` boundary:
   - Claim status $\rightarrow$ `COMPLETED`
   - Found Item status $\rightarrow$ `RESOLVED`
   - Linked Lost Item status $\rightarrow$ `RETURNED`
   - ItemMatch status $\rightarrow$ `RESOLVED`
   - Handover timestamp and confirming Administrator ID are recorded.
4. Automated notifications are sent to **both students** confirming safe handover.

### 4. 🛡️ 8-Feature Admin Control Center (`/admin-claims.html`)
- **Dashboard Overview:** KPI stat cards, urgent review alerts, and handover counter.
- **Pending Verification:** Filtered evidence review stream with side-by-side document modal.
- **Return Management:** Handover queue for items approved and waiting for desk collection.
- **All Claims:** Searchable master claims database.
- **All Items:** Manage all lost and found reports (Active, Returned, Resolved, Removed, or Delete).
- **Users Management:** Student roster with instant role toggle (Promote to Admin / Demote to User).
- **Reports & History:** Category distribution analytics and immutable Completed Return Log.
- **System Notifications:** Global system-wide activity and audit log.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend Framework** | Java Spring Boot 3.2.0 | Java 17, Spring Web, Spring Data JPA |
| **Security** | Spring Security 6 + JJWT | Stateless JWT token auth, BCrypt password hashing |
| **Database** | MySQL 8.0 | Hibernate ORM, `ddl-auto=update` |
| **Frontend** | HTML5, CSS3, Vanilla JS | Bootstrap 5.3, Font Awesome 6.5, Plus Jakarta Sans |
| **Testing** | JUnit 5 + Mockito 5 | Mockito Subclass MockMaker, H2 test database |
| **Build Tool** | Apache Maven | Packaging and dependency resolution |

---

## 🗄️ Database Architecture

The system utilizes 5 relational tables:

```
users (id, username, email, password, full_name, role, created_at)
  │
  ├──< item_posts (id, title, category, colour, description, location, date_event, lost, photo, status, reported_by_id)
  │        │
  │        ├──< item_matches (id, lost_item_id, found_item_id, score, status, created_at)
  │        │
  │        └──< claims (id, item_id, user_id, full_name, age, mobile, proof_type, reason,
  │                     gov_proof_image, product_proof_image, status, review_notes,
  │                     resolved_by, handover_confirmed_by, handover_confirmed_at)
  │
  └──< notifications (id, recipient_id, message, type, related_id, is_read, created_at)
```

---

## 🚀 Quick Setup & Installation

### 1. Prerequisites
- **JDK 17** (Temurin / Adoptium or Oracle JDK)
- **MySQL Server 8.x**
- **Maven** or **IntelliJ IDEA**

### 2. Prepare MySQL Database
```sql
CREATE DATABASE IF NOT EXISTS lostfound
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Application Properties
Configuration is located in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/lostfound?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
server.port=8081
```

### 4. Build and Run
From the terminal:
```powershell
# Run Unit Tests
mvn test

# Start Application Server
mvn spring-boot:run
```

Or open the folder in **IntelliJ IDEA** and execute `LostFoundApplication.java`.

Access the application in your browser:  
👉 **`http://localhost:8081`**

---

## 🔑 Default Accounts

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Full access to Admin Dashboard (`/admin-claims.html`) |
| **Student** | Any registered user | As registered | Standard student features: report, claim, and search |

---

## 🧪 Unit Testing Suite

The application includes an automated unit test suite covering critical business logic:

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.example.lostfound.service.ClaimServiceTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
Running com.example.lostfound.service.MatchingServiceTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

Results:
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- **ClaimServiceTest (9 Tests):** Claim submission validation, own-item protection, status state machine, double-return prevention, and transactional resolution.
- **MatchingServiceTest (9 Tests):** Multi-factor scoring weights, location similarity, date tolerance, text comparison, and notification trigger thresholds.

---

## 📡 REST API Reference

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/auth/register` | `POST` | Public | Register new student account |
| `/api/auth/login` | `POST` | Public | Sign in and obtain JWT token |
| `/api/items` | `GET` | Public | Search items with query, category, location, and type filters |
| `/api/items` | `POST` | Student | Submit lost or found report (Multipart with image) |
| `/api/items/{id}` | `GET` | Public | Retrieve single item details |
| `/api/items/my-reports` | `GET` | Student | Retrieve user's submitted reports |
| `/api/dashboard/summary` | `GET` | Public | Total lost, found, returned, and pending counts |
| `/api/dashboard/recent` | `GET` | Public | 10 most recent reports |
| `/api/claims/{itemId}` | `POST` | Student | Submit claim with dual evidence upload |
| `/api/claims/my` | `GET` | Student | Retrieve user's submitted claims |
| `/api/claims/all` | `GET` | Admin | Retrieve all claims across all statuses |
| `/api/claims/{id}/approve` | `PATCH` | Admin | Approve claim for desk handover |
| `/api/claims/{id}/reject` | `PATCH` | Admin | Reject claim with mandatory review notes |
| `/api/claims/{id}/confirm-return` | `POST` | Admin | Confirm physical return (Atomic Handover) |
| `/api/admin/stats` | `GET` | Admin | Summary metrics for admin dashboard |
| `/api/admin/items` | `GET` | Admin | Master items list for admin moderation |
| `/api/admin/users` | `GET` | Admin | List all student and administrator accounts |
| `/api/admin/users/{id}/role` | `PUT` | Admin | Promote to Admin or Demote to User |
| `/api/admin/reports` | `GET` | Admin | Category breakdown and completed return log |
| `/api/admin/notifications` | `GET` | Admin | System-wide audit log of notifications |
| `/api/notifications` | `GET` | Student | Retrieve current user's notification stream |
| `/api/notifications/unread-count`| `GET` | Student | Count of unread notifications |
| `/api/notifications/{id}/read` | `PATCH` | Student | Mark specific notification as read |
| `/api/notifications/read-all` | `PATCH` | Student | Mark all notifications as read |

---

## 👨‍💻 Developer & College Credits

<div align="center">
  <h3>Project Developed by</h3>
  <h2>AKASH P</h2>
  <p><b>Department of Computer Science and Engineering (III CSE A)</b></p>
  <p><b>PSG Institute of Technology and Applied Research (PSG iTech)</b></p>
  <p>Coimbatore, Tamil Nadu, India</p>
  <br/>
  <img src="https://img.shields.io/badge/Student%20ID-24Z109%20%2F%20III%20CSE%20A-6366f1?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Institution-PSG%20iTech-0284c7?style=for-the-badge"/>
</div>

---

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=9,10,16,22,25&height=120&section=footer&animation=twinkling"/>
