# Campus Lost & Found Management System

A high-performance, enterprise-grade **Campus Lost & Found Management System** built with **100% pure Java 17** (no external AI services, no Python). Featuring a stateless Spring Boot 3.2 backend with Spring Security 6, JJWT 0.12.5, MySQL 8.0 with HikariCP, and a modern JavaFX 17 desktop application styled after dark glassmorphism dashboard aesthetics.

---

## 🏛️ System Architecture

```text
+---------------------------------------------------------------------------------------+
|                                    JAVA CLIENT                                        |
|   OpenJFX 17 Desktop Client (FXML + CSS + Asynchronous HttpClient + Jackson)          |
|                                                                                       |
|   [ Login View ]       [ Modern Dark Dashboard ]        [ Camera Capture & Upload ]  |
|   - Gradient Canvas    - 3 Metric Gradient Cards        - Viewfinder HUD Snapshot     |
|   - Centered Avatar    - Real-Time Activity Table       - Image Thumbnail Preview     |
|   - Quick Demo Fill    - Location Density Progress      - Multipart Form-Data Builder |
+-------------------------------------------+-------------------------------------------+
                                            | REST API (JSON / Multipart HTTP)
                                            v
+---------------------------------------------------------------------------------------+
|                                  SPRING BOOT BACKEND                                  |
|                                                                                       |
|   [ Security Layer ]                                                                  |
|   - Spring Security 6 (Stateless SessionCreationPolicy.STATELESS)                     |
|   - JJWT 0.12.5 Parser & Base64-256 HMAC-SHA Key Verification                         |
|   - Role-Based Access Control (@EnableMethodSecurity: STUDENT, STAFF, ADMIN)          |
|                                                                                       |
|   [ Business Engines ]                                                                |
|   - StateTransitionService: Strict Enum Lifecycle Validation (HTTP 409 on violation)  |
|   - MatchingService: Deterministic 5-Factor Weighted Jaccard Matching (Score >= 55)   |
|   - ClaimService: Human Verification Desk, Proof Documents, Handover Audit Trail      |
|   - RetentionScheduler: Daily Cron (0 0 2 * * *) auto-expiring uncollected inventory  |
|                                                                                       |
|   [ Data Access Layer ]                                                               |
|   - Spring Data JPA + Hibernate 6                                                     |
|   - @Version Optimistic Locking on Item records                                       |
|   - Dedicated Blob Separation: ItemImage & ClaimProof (Shared PK @MapsId)             |
+-------------------------------------------+-------------------------------------------+
                                            | JDBC / HikariCP Pool
                                            v
+---------------------------------------------------------------------------------------+
|                                    MYSQL DATABASE                                     |
|   Schema: campus_lostfound (MySQL 8.0+)                                               |
|   Tables: users, items, item_images, claims, claim_proofs, matches, notifications...  |
+---------------------------------------------------------------------------------------+
```

---

## 🛠️ Strict Tech Stack

- **Backend:**
  - Java 17 (OpenJDK / Temurin)
  - Spring Boot 3.2.0 (`jakarta.*` namespace)
  - Spring Security 6 + JJWT 0.12.5 (JJwt 0.12 API using `.getPayload()`)
  - Spring Data JPA + Hibernate 6 (`@Version` optimistic locking, `@Transactional`)
  - Lombok (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
  - HikariCP Connection Pool
  - MySQL 8.0+ (`LONGBLOB` for images and proofs, zero blobs in list DTOs)
  - H2 Database (In-memory MySQL compatibility mode for unit and integration tests)
- **Frontend:**
  - JavaFX 17 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`, `javafx-swing`)
  - `javafx-maven-plugin:0.0.8`
  - Java 11+ `java.net.http.HttpClient` with custom `MultipartBodyPublisher`
  - Jackson Databind + `jackson-datatype-jsr310` (Java 8 date/time serialization)
  - Custom camera snapshot utility (`CameraCaptureDialog`) for item photos
- **Root Multi-Module Build:**
  - Parent `pom.xml` aggregating `backend` and `client` modules.

---

## 🚀 Setup & Run Instructions (Windows)

### Prerequisites
- **JDK 17** installed (e.g. `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`)
- **Maven 3.8+** installed
- **MySQL 8.0+** running locally on port 3306 (or Docker)

### Option A: Running Database with Docker
If MySQL is not installed locally, start MySQL 8.0 via Docker Compose:
```bash
docker-compose up -d
```

### Option B: Local MySQL Configuration
The application connects by default to:
- **URL:** `jdbc:mysql://localhost:3306/campus_lostfound?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- **Username:** `root`
- **Password:** `Akash10071845` (configurable via `DB_PASSWORD` environment variable)

---

### Option C: One-Click Quick Launch (Windows)
Double-click `run.bat` in the project root:
- Starts the Spring Boot backend server on **Port 8088** in a dedicated window
- Waits 6 seconds for Tomcat to initialize
- Launches the JavaFX desktop client GUI automatically

Alternatively, run individually:
- **Backend:** Double-click `run-backend.bat`
- **Frontend:** Double-click `run-client.bat`

---

### Running from Command Line / PowerShell

#### 1. Compile and Test Both Modules
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean test
```

#### 2. Start the Backend Server (Port 8088)
Open a terminal window and run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
cd backend
mvn spring-boot:run
```
*The server starts on `http://localhost:8088` (custom port to avoid collisions with other local services) and automatically seeds default users and sample data on its first run.*

#### 3. Start the JavaFX Desktop Client
Open a second terminal window and run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
cd client
mvn javafx:run
```

#### 4. Run Automated Smoke Test
To verify the entire end-to-end lifecycle (Registration, Login, Report Lost, Log Found, Matching Engine, Claim Verification, Notifications):
```powershell
powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1
```

---

### Running in IntelliJ IDEA / VS Code

1. **Open Project:** Open `C:\Users\Akash P\Downloads\Lost-and-Found-Portal` as a Maven project.
2. **Project SDK:** Ensure Project SDK is set to **Java 17**.
3. **Backend Run Configuration:**
   - Main class: `com.campus.lostfound.BackendApplication`
   - Working directory: `C:\Users\Akash P\Downloads\Lost-and-Found-Portal\backend`
4. **Client Run Configuration:**
   - Main class: `com.campus.lostfound.client.MainApp`
   - Working directory: `C:\Users\Akash P\Downloads\Lost-and-Found-Portal\client`
   - VM options (if required by IDE modularity):
     `--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing`

---

## 🔑 Seeded Demo Accounts

On startup, the system automatically seeds default accounts with pre-hashed BCrypt credentials:

| Role | Email | Roll Number | Password | Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@campus.edu` | — | `Admin@123` | System stats, user management, item merging, manual matching, audit log |
| **STAFF** | `staff@campus.edu` | — | `Staff@123` | Log found items, warehouse inventory, claim review desk, item disposal |
| **STUDENT** | `student@campus.edu` | `21CS001` | `Student@123` | Report lost items, view smart matches, submit ownership claims |

*Note: The login view includes one-click demo login buttons to instantly auto-fill credentials for any role.*

---

## 📋 Complete REST API Reference

All protected endpoints require `Authorization: Bearer <JWT_TOKEN>`.

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register new student (`STUDENT` role only). Rejects duplicates with 409. |
| `POST` | `/api/auth/login` | Public | Authenticate via email or roll number + password. Returns JWT token. |
| `GET` | `/api/auth/me` | Authenticated | Get current authenticated user profile. |

### Items (`/api/items`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/items` | Authenticated | Create lost (Student) or found (Staff/Admin) item. Supports multipart JSON + photo. |
| `GET` | `/api/items/search` | Authenticated | Search items with JPA Specifications (role-based visibility applied). |
| `GET` | `/api/items/mine` | Student | List all lost reports filed by the caller. |
| `GET` | `/api/items/inventory` | Staff, Admin | Paginated warehouse inventory of held found items. |
| `GET` | `/api/items/{id}` | Authenticated | View item details (student receives `ItemPublicDto`; staff receives `ItemStaffDto`). |
| `GET` | `/api/items/{id}/photo` | Authenticated | Stream photo bytes with correct `Content-Type` (JPEG/PNG). |
| `PUT` | `/api/items/{id}` | Owner, Admin | Edit lost report details while in `OPEN` or `MATCHED` status. |
| `PATCH/POST` | `/api/items/{id}/cancel` | Owner, Admin | Cancel a lost item report. |
| `POST` | `/api/items/{id}/dispose` | Staff, Admin | Mark expired found item as `DONATED` or `DISPOSED`. |

### Claims (`/api/claims`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/claims` | Student | Submit ownership claim (supports optional proof document). 3-rejection limit enforced. |
| `GET` | `/api/claims/mine` | Student | List all claims submitted by the caller. |
| `GET` | `/api/claims` | Staff, Admin | View and filter all claims across campus. |
| `POST` | `/api/claims/{id}/review` | Staff, Admin | Review claim (`APPROVE` or `REJECT` with mandatory rejection note). |
| `GET` | `/api/claims/{id}/proof` | Staff, Admin | Stream claimant's uploaded proof document (JPEG, PNG, or PDF). |

### Smart Matches (`/api/matches`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/matches` | Authenticated | List suggested matches for student's items (or all matches for staff/admin). |
| `PATCH/POST` | `/api/matches/{id}/dismiss` | Owner, Staff | Dismiss a match. Reverts item from `MATCHED` to `OPEN` if no matches remain. |

### Notifications (`/api/notifications`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/notifications` | Authenticated | Paginated in-app notifications for caller. |
| `GET` | `/api/notifications/unread-count` | Authenticated | Get real-time unread notification count. |
| `PATCH/POST` | `/api/notifications/{id}/read` | Authenticated | Mark notification as read. |
| `PATCH/POST` | `/api/notifications/read-all` | Authenticated | Mark all caller's notifications as read. |

### Administration (`/api/admin`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/stats` | Admin | Comprehensive system recovery statistics, location density, and return rate. |
| `GET` | `/api/admin/users` | Admin | List all registered users. |
| `POST` | `/api/admin/users` | Admin | Create privileged `STAFF` or `ADMIN` accounts. |
| `PATCH/POST` | `/api/admin/users/{id}/active` | Admin | Deactivate/reactivate user account (self-deactivation forbidden). |
| `PATCH/POST` | `/api/admin/users/{id}/role` | Admin | Change user role (self-demotion forbidden). |
| `POST` | `/api/admin/matches` | Admin | Create a forced manual match between lost and found items. |
| `POST` | `/api/admin/items/merge` | Admin | Merge duplicate item records and migrate existing matches. |
| `GET` | `/api/admin/audit` | Admin | Immutable paginated audit log of system operations. |

---

## 🔄 State Machine & Business Integrity

### LOST Item Lifecycle
```text
           [ Student Reports ]
                   |
                   v
               +-------+
               | OPEN  | <----+
               +-------+      | (All matches
                |     \       |  dismissed)
 (Match Score   |      \      |
    >= 55)      v       \     |
            +---------+  \    |
            | MATCHED | --+---+
            +---------+   |
                |         | (Owner cancels)
(Claim Approved/|         v
Owner Recovered)|     +-----------+
                +---> | CANCELLED |
                |     +-----------+
                v
            +--------+
            | CLOSED |
            +--------+
```

### FOUND Item Lifecycle
```text
            [ Staff Logs Found ]
                     |
                     v
                 +--------+
                 | LISTED | <-----+
                 +--------+       | (Claim
                  /   |   \       |  Rejected)
                 /    |    +------+
  (Claim Submit)/     | (90-Day Retention Expired)
               v      v
      +---------------+  +---------+
      | CLAIM_PENDING |  | EXPIRED |
      +---------------+  +---------+
              |             /     \
(Claim Review |  (Staff    /       \ (Staff
   Approved)  |   Donates)/         \ Disposes)
              v          v           v
         +----------+ +---------+ +----------+
         | RETURNED | | DONATED | | DISPOSED |
         +----------+ +---------+ +----------+
```

### Enforced Business Invariants
1. **Illegal State Transitions:** Calling an invalid transition throws `IllegalStateTransitionException` resulting in HTTP 409 Conflict.
2. **Optimistic Locking:** The `items` table employs `@Version Long version` to detect and reject concurrent modifications.
3. **Claim Limits:** A student is strictly capped at **3 rejected claims** per found item to eliminate brute-force guessing of secret details.
4. **Data Isolation:** `ItemPublicDto` completely strips `hiddenDetail`, `storedAt`, and `ownerRollNumber`. Students can never inspect other students' lost reports.
5. **Separation of Blobs:** Photos and proofs reside in isolated 1-to-1 `@MapsId` tables (`ItemImage`, `ClaimProof`). Querying item or claim lists never transfers byte arrays over the wire.

---

## ⚡ Deterministic 5-Factor Matching Engine

The rule-based matching engine scores pairs on a **0 to 100** scale with deterministic weights:

1. **Category Matching (20 points):** Hard requirement. Items in different categories immediately score `0`.
2. **Temporal Consistency (Hard Rule + 20/10 points):** Found event date cannot precede the lost event date.
   - Gap $\le$ 2 days: `+20 points`
   - Gap $\le$ 7 days: `+10 points`
3. **Location Similarity (30 points):** Case-insensitive match on campus location (e.g., "Library").
4. **Color Matching (10 points):** Case-insensitive comparison of non-blank color attributes.
5. **Tokenized Text Similarity ($\le 20$ points):**
   - Combines title, description, and brand.
   - Cleans punctuation and filters English stop words (`the`, `with`, `near`, `found`, etc.) and words shorter than 3 characters.
   - Calculates **Jaccard Similarity Coefficient**:
     $$J(A, B) = \frac{|A \cap B|}{|A \cup B|}$$
   - Weighted by 20 points: $\text{Score} += \text{round}(J(A, B) \times 20)$.
6. **Threshold & Notifications:** Pairs with score $\ge 55$ automatically transition the lost item to `MATCHED` and dispatch an in-app notification to the owner detailing the top 3 matches.

---

## 💼 Resume & Portfolio Highlights

- **Full-Stack Multi-Module Java Architecture:** Engineered an end-to-end campus lost & found system comprising a Spring Boot 3.2 service and an OpenJFX 17 desktop client communicating via asynchronous `java.net.http.HttpClient` with custom multipart form-data builders.
- **Enterprise Spring Security 6 & JJWT 0.12.5:** Architected a stateless token security pipeline implementing Base64-256 HMAC-SHA key derivation, custom JSON 401/403 entry points, and `@EnableMethodSecurity` role isolation across Student, Staff, and Admin tiers.
- **Deterministic 5-Factor Rule-Based Matching Engine:** Implemented an in-memory heuristic algorithm combining category constraints, date chronology, location scoring, and Jaccard token set similarity to discover lost-and-found matches with zero external AI dependencies.
- **Strict State Machine & Concurrency Control:** Designed an optimistic-locking state machine enforcing item lifecycles with HTTP 409 transition rejection and a 3-rejection claim fraud protection rule.
- **High-Performance JPA Blob Separation:** Architected database schema utilizing `@MapsId` shared primary key mappings for `ItemImage` and `ClaimProof`, eliminating LONGBLOB payload overhead from JPA list queries and pagination.
- **Automated Retention Scheduler & Live Desktop Polling:** Developed a daily Spring `@Scheduled` cron job to automate uncollected property expiration and warning alerts, paired with a JavaFX 30-second background polling timeline for live notification delivery.
