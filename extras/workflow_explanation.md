# 🎓 Subject Allocation System — End-to-End Workflow & File Guide

> **Tech Stack:** Spring Boot 3.5 · Spring Data JPA · PostgreSQL · Thymeleaf · Java 21 · Maven

---

## 🗺️ Architecture Overview

```
Browser (Student)
      │
      ▼
┌─────────────────────────────────────┐
│         Controllers (HTTP Layer)     │
│   RegistrationController (MVC)       │
│   SlotController       (REST API)    │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│          Service Layer               │
│   AllocationService  (write logic)   │
│   SlotService        (read logic)    │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│        Repository Layer (JPA)        │
│   TermCourseAvailableForRepository   │
│   StudentRegistrationCoursesRepo     │
│   StudentRegistrationRepository      │
│   TermCourseRepository               │
│   CourseRepository                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│     PostgreSQL Database (ec2 schema) │
│   courses · termcourses              │
│   termcourseavailablefor             │
│   studentregistrations               │
│   studentregistrationcourses         │
└─────────────────────────────────────┘
```

---

## 📂 Complete File-by-File Breakdown

### ⚙️ Configuration & Bootstrap

| File | Role |
|------|------|
| `SubjectallocationApplication.java` | Spring Boot entry point — runs `SpringApplication.run()` to boot the entire app |
| `application.properties` | Configures DB connection (PostgreSQL on port 5432, DB: `localBTP`, schema: `ec2`), JPA settings (`ddl-auto=validate` — never modifies existing tables), Thymeleaf settings, HikariCP pool (max 20 connections), server on port 8080 |
| `AppConstants.java` | Holds the constant `MAX_SEATS = 60` — the default fallback if a slot doesn't have an explicit seat count in the DB |
| `pom.xml` | Maven build config — declares dependencies: `spring-boot-starter-data-jpa`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-web`, `postgresql` driver |

---

### 🗄️ Entity Layer (DB Table Mappings)

These classes are direct Java mirrors of your PostgreSQL tables.

#### `Course.java` → table `ec2.courses`
- Maps columns: `crsid` (PK), `crsname` (subject name), `crscode` (subject code e.g. CS501)
- **Purpose:** Master catalog of all university subjects

#### `TermCourse.java` → table `ec2.termcourses`
- Maps columns: `tcrid` (PK), `tcrcrsid` (FK → courses.crsid), `tcrslot` (slot number as `Long`), `tcrtrmid` (term ID)
- **Purpose:** A specific offering of a course in a particular term and slot (e.g., "CS501 in Term 78, Slot 2")
- **Key design:** `termid = 78` is hardcoded in queries to filter for the current active term

#### `TermCourseAvailableFor.java` → table `ec2.termcourseavailablefor`
- Maps columns: `tcaid` (PK), `tcatcrid` (FK → termcourses via `@ManyToOne`), `tcabchid` (batch ID), `tca_seats` (total seats), `tca_booked` (seats already booked)
- **Purpose:** Tracks how many seats are available for each term course offering per batch. This is the **seat inventory table** — the most critical table for concurrency.

#### `StudentRegistration.java` → table `studentregistrations`
- Maps columns: `srgid` (PK), `srgstdid` (FK → student master)
- **Purpose:** Represents a student's enrollment record for a particular term. Links a student (by ID) to the registration system.

#### `StudentRegistrationCourses.java` → table `ec2.studentregistrationcourses`
- Maps columns: `srcid` (PK), `srcsrgid` (FK → studentregistrations.srgid), `srctcrid` (FK → termcourses.tcrid), `srctype` (default `"M"`), `srcstatus` (default `"ACTIVE"`), `srccreatedat` (auto timestamp), `srcrowstate` (default `1`)
- **Purpose:** The **allocation record** — one row per course a student is registered in. When a slot is allocated, multiple rows are inserted here (one per course in that slot).

---

### 📦 DTO Layer (Data Transfer Objects)

#### `CourseInfo.java`
- A lightweight read-only object carrying: `tcrid`, `courseName`, `courseCode`
- **Purpose:** Transfers course data from the service layer to the Thymeleaf templates without exposing raw JPA entities. Used in `Map<String, List<CourseInfo>>` to group courses by slot.

#### `SlotDTO.java`
- Wraps a slot string + a list of `CourseInfo` objects
- Currently used only in the commented-out old version of `SlotService`; the active code uses `Map<String, List<CourseInfo>>` directly.

#### `dto/request/SlotAllocationRequest.java`
- JSON request body DTO for the REST API endpoint (`SlotController`)
- Carries the `slot` string sent by a REST client

---

### 🗃️ Repository Layer (Database Queries)

#### `TermCourseAvailableForRepository.java`
The most important repository — contains custom JPQL + native SQL queries:

| Method | What it does |
|--------|--------------|
| `findDistinctSlots()` | Returns all unique slot numbers for term 78 (JPQL join with TermCourse) |
| `findCourseIdsBySlot(slotId)` | Returns `tcrid` list for all courses in a given slot for term 78 |
| `findTcafBySlot(slotId)` | Returns full `TermCourseAvailableFor` objects for a slot (used before locking) |
| `findTcafByIdWithLock(tcaid)` | **Acquires a pessimistic write lock** (`SELECT FOR UPDATE`) on a specific row — the core of concurrency safety |
| `attemptToLockSeat(tcaid)` | Native SQL atomic update (`tca_booked + 1 WHERE tca_booked < tca_seats`) — alternative atomic approach |

#### `StudentRegistrationCoursesRepository.java`
| Method | What it does |
|--------|--------------|
| `countBySrcsrgid(srgid)` | Checks if student already has any allocations (prevents double-enrollment) |
| `countBySrctcrid(tcrid)` | Counts registrations for a specific course |
| `countBySrcsrgidAndSrctcridIn(...)` | Checks overlap between student's registrations and a set of courses |
| `findBySrcsrgid(srgid)` | Retrieves all course enrollments for a student's registration |

#### `StudentRegistrationRepository.java`
- `findFirstBySrgstdidOrderBySrgidDesc(studentId)` — Finds the most recent registration record for a student

#### `TermCourseRepository.java`
- Standard `JpaRepository<TermCourse, Long>` — uses `findAllById()` to fetch course records by ID list

#### `CourseRepository.java`
- Standard `JpaRepository<Course, Long>` — used to look up course name/code by `crsid`

---

### 🧠 Service Layer (Business Logic)

#### `SlotService.java` — Read-Only Logic
**Single method:** `getAvailableCoursesBySlot()`

**Step-by-step:**
1. Calls `findDistinctSlots()` → gets list of slot numbers (e.g., `[1, 2, 3]`) for term 78
2. For each slot, calls `findCourseIdsBySlot(slot)` → gets the `tcrid` list
3. Calls `termCourseRepository.findAllById(courseIds)` → fetches `TermCourse` records
4. For each `TermCourse`, looks up `Course` by `courseId` → gets name and code
5. Builds `CourseInfo` objects and groups them: `Map<"1", [CS501, CS502]>, <"2", [CS601]>, ...`
6. Returns the map to the controller for Thymeleaf rendering

**Annotation:** `@Transactional(readOnly = true)` — no DB writes, read-optimized

---

#### `AllocationService.java` — Write Logic (Concurrency-Safe)

**Method 1: `getEnrolledSlot(studentId)`**
- Looks up `StudentRegistration` for the student
- Finds their `StudentRegistrationCourses` entries
- Traces back to `TermCourse` to get the `slot` number
- Returns the slot string (e.g., `"2"`) or `null` if not yet enrolled

**Method 2: `allocateSlot(studentId, slot)` — The Core Algorithm**

This is a **Two-Phase Locking (2PL) + FCFS** allocation:

```
Phase 0: Validation
  ├── Convert slot string → Long slotId
  ├── findTcafBySlot(slotId) → get all TCAF rows for this slot
  ├── If empty → throw SlotNotFoundException
  ├── findFirstBySrgstdidOrderBySrgidDesc(studentId) → get registration
  └── countBySrcsrgid(srgid) > 0 → throw AlreadyEnrolledException (1 slot per student)

Phase 1: Growing Phase (Acquire Locks)
  ├── Sort TCAF IDs in ascending order (prevents deadlocks between concurrent transactions)
  ├── For each TCAF ID (sorted):
  │     ├── findTcafByIdWithLock(tcaid) → SELECT FOR UPDATE (PostgreSQL row lock)
  │     ├── Check: tca_booked >= tca_seats?
  │     └── If full → throw RuntimeException("Slot Full: No seats for <course name>")

Phase 2: Modify Phase (Atomic Writes — inside @Transactional)
  └── For each locked TCAF:
        ├── tca_booked += 1 → save() (seat decremented)
        └── INSERT StudentRegistrationCourses (srgid, tcrid) → save()

Return: "Allocation Successful"
```

**Why it's safe:** PostgreSQL's `SELECT FOR UPDATE` ensures no two transactions can modify the same seat row simultaneously. If one transaction holds the lock, all others block until it commits or rolls back.

---

### 🌐 Controller Layer (HTTP Handling)

#### `RegistrationController.java` — MVC (Browser/Thymeleaf)
Base path: `/registration`

| Endpoint | Method | What happens |
|----------|--------|--------------|
| `GET /registration/slots?studentId=11926` | GET | Calls `getEnrolledSlot()` + `getAvailableCoursesBySlot()`, puts results in Model, renders `slot-selection.html` |
| `POST /registration/allocate` | POST | Reads `slot` + `studentId` from form, calls `allocateSlot()`, redirects to result page with flash message |
| `GET /registration/result` | GET | Renders `result.html` (reads flash attributes set by redirect) |

#### `SlotController.java` — REST API (JSON)
Base path: `/slots`

| Endpoint | Method | What happens |
|----------|--------|--------------|
| `GET /slots` | GET | Returns `Map<String, List<CourseInfo>>` as JSON |
| `POST /slots/allocate` | POST | Reads `SlotAllocationRequest` from JSON body + `X-Student-Id` header, calls `allocateSlot()`, returns `200 OK` or error status codes (`404`, `409`, `500`) |

---

### 🖼️ Thymeleaf Templates (UI)

#### `slot-selection.html`
- Shows a **green banner** if student is already enrolled (with their slot number)
- Shows a **yellow warning** if not yet enrolled
- Renders each slot as a **radio button** with a list of courses beneath it
- If already enrolled: radio buttons are **disabled** (cannot re-select)
- Submit button only visible if not yet enrolled
- Form POSTs to `/registration/allocate`

#### `result.html`
- Shows allocation result message in **green box** (success) or **red box** (error)
- Has a "Back to Selection" link → `/registration/slots`

---

### 🛡️ Exception Classes

| Class | HTTP Status (REST) | Meaning |
|-------|--------------------|---------|
| `AlreadyEnrolledException` | 409 Conflict | Student already has a slot allocation |
| `SlotNotFoundException` | 404 Not Found | No courses found for the requested slot |
| `SlotFullException` | 409 Conflict | All seats in the slot are taken |

---

### 🔧 Utility / Debug Files

| File | Purpose |
|------|---------|
| `CheckData.java` → `CheckData7.java` | Standalone JDBC scripts used during development to directly query the DB and verify data. **Not part of the Spring Boot app.** |
| `FixDB.java` | Standalone JDBC script to fix DB schema issues during development. **Not part of the Spring Boot app.** |
| `GetSchema.class` | Compiled class to inspect DB schema |
| `subject_allocation_workflow.html` | A previously generated HTML workflow diagram |

---

## 🔄 Complete End-to-End Flow: Student Registers for a Slot

```
1. Student opens browser → GET /registration/slots?studentId=11926
   │
   ├─ RegistrationController.showSlotSelection()
   │     ├─ AllocationService.getEnrolledSlot(11926)
   │     │     ├─ Find StudentRegistration by studentId
   │     │     ├─ Find StudentRegistrationCourses by srgid
   │     │     └─ Look up TermCourse.slot → return "2" or null
   │     │
   │     └─ SlotService.getAvailableCoursesBySlot()
   │           ├─ findDistinctSlots() → [1, 2, 3]
   │           ├─ For each slot → findCourseIdsBySlot() → [tcrid1, tcrid2]
   │           ├─ findAllById([tcrid1, tcrid2]) → TermCourse list
   │           └─ Look up Course names → build Map<slot, [CourseInfo]>
   │
   └─ Renders slot-selection.html with slots + enrolledSlot

2. Student selects Slot 2 → POST /registration/allocate (slot=2, studentId=11926)
   │
   └─ RegistrationController.allocateSlot()
         └─ AllocationService.allocateSlot(11926, "2")  [@Transactional]
               ├─ findTcafBySlot(2) → [TCAF#5, TCAF#8]
               ├─ findFirstByStudentId → srgid = 303
               ├─ countBySrcsrgid(303) == 0 ✓ (not yet enrolled)
               ├─ Sort IDs: [5, 8]
               │
               ├─ LOCK TCAF#5 (SELECT FOR UPDATE)
               │     tcaBooked=10 < tcaSeats=60 ✓
               ├─ LOCK TCAF#8 (SELECT FOR UPDATE)
               │     tcaBooked=25 < tcaSeats=60 ✓
               │
               ├─ UPDATE TCAF#5: tca_booked = 11 → save()
               ├─ INSERT StudentRegistrationCourses(srgid=303, tcrid=X) → save()
               ├─ UPDATE TCAF#8: tca_booked = 26 → save()
               └─ INSERT StudentRegistrationCourses(srgid=303, tcrid=Y) → save()
               
               → DB COMMIT (all or nothing)

3. Redirect → GET /registration/result
   └─ Shows "Allocation Successful" in green box
```

---

## 🗃️ Database Schema Summary

```
ec2.courses          ec2.termcourses              ec2.termcourseavailablefor
──────────────       ─────────────────────────    ────────────────────────────────
crsid (PK)     ←──  tcrcrsid (FK)                tcaid (PK)
crsname              tcrid (PK)                   tcatcrid (FK) ──→ termcourses.tcrid
crscode              tcrslot (slot number)        tcabchid (batch)
                     tcrtrmid (term=78)           tca_seats  (total seats)
                                                  tca_booked (seats taken) ← LOCK TARGET

studentregistrations          ec2.studentregistrationcourses
────────────────────          ──────────────────────────────
srgid (PK)                    srcid (PK)
srgstdid (FK→students)        srcsrgid (FK→studentregistrations.srgid)
                               srctcrid (FK→termcourses.tcrid)
                               srctype, srcstatus, srccreatedat, srcrowstate
```

---

## 🔐 Key Design Decisions

| Decision | Why |
|----------|-----|
| **One slot per student** | `countBySrcsrgid()` check before any write ensures a student can't grab two slots |
| **Sort lock IDs before acquiring** | Prevents deadlocks when two transactions try to lock the same rows in different orders |
| **`SELECT FOR UPDATE` (Pessimistic Lock)** | Ensures seat count is accurate under concurrent load — no phantom reads |
| **`@Transactional` on `allocateSlot()`** | All DB writes (seat decrement + enrollment inserts) are atomic — either all succeed or all roll back |
| **`ddl-auto=validate`** | Spring Boot only validates schema at startup, never alters or drops production tables |
| **`termid = 78` hardcoded in queries** | Scopes all queries to the current active academic term |
