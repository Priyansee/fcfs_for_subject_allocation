# FCFS Subject Allocation System — Complete Codebase Documentation

**Project:** Implementation of FCFS Algorithm for Subject Allocation  
**Author:** Priyansee Mukeshkumar Soni (202511011)  
**Supervisor:** Prof. P M Jat  
**Stack:** Java 21 · Spring Boot 3.x · PostgreSQL 16 · Hibernate 6 · Thymeleaf

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Architecture Overview](#2-architecture-overview)
3. [Entities — Data Models](#3-entities--data-models)
4. [Repositories — Data Access Layer](#4-repositories--data-access-layer)
5. [Services — Business Logic](#5-services--business-logic)
6. [Controllers — Web & API Layer](#6-controllers--web--api-layer)
7. [DTOs — Data Transfer Objects](#7-dtos--data-transfer-objects)
8. [Exceptions — Error Handling](#8-exceptions--error-handling)
9. [Configuration](#9-configuration)
10. [Frontend — Thymeleaf Templates](#10-frontend--thymeleaf-templates)
11. [The 3-Phase FCFS Algorithm](#11-the-3-phase-fcfs-algorithm)
12. [Database Schema](#12-database-schema)
13. [Testing Infrastructure](#13-testing-infrastructure)
14. [How to Run](#14-how-to-run)

---

## 1. Project Structure

```
subjectallocation/
├── pom.xml                                      # Maven build config
├── mvnw / mvnw.cmd                              # Maven wrapper scripts
├── src/
│   ├── main/
│   │   ├── java/com/uni/subjectallocation/
│   │   │   ├── SubjectallocationApplication.java   # Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   └── AppConstants.java               # Global constants
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java             # Root redirect
│   │   │   │   ├── RegistrationController.java     # Thymeleaf UI controller
│   │   │   │   └── SlotController.java             # REST API controller
│   │   │   ├── dto/
│   │   │   │   ├── CourseInfo.java                  # Course display DTO
│   │   │   │   ├── SlotDTO.java                    # Slot grouping DTO
│   │   │   │   └── request/
│   │   │   │       └── SlotAllocationRequest.java  # API request body
│   │   │   ├── entity/
│   │   │   │   ├── Course.java                     # Base course entity
│   │   │   │   ├── TermCourse.java                 # Slot-to-course mapping
│   │   │   │   ├── TermCourseAvailableFor.java     # Seat tracker (LOCK TARGET)
│   │   │   │   ├── StudentRegistration.java        # Student-term record
│   │   │   │   └── StudentRegistrationCourses.java # Enrollment record
│   │   │   ├── exception/
│   │   │   │   ├── AlreadyEnrolledException.java   # Duplicate registration
│   │   │   │   ├── SlotFullException.java          # No seats remaining
│   │   │   │   └── SlotNotFoundException.java      # Invalid slot ID
│   │   │   ├── repository/
│   │   │   │   ├── CourseRepository.java            # Course data access
│   │   │   │   ├── TermCourseRepository.java        # Term-course data access
│   │   │   │   ├── TermCourseAvailableForRepository.java  # LOCKING QUERIES
│   │   │   │   ├── StudentRegistrationRepository.java     # Student lookup
│   │   │   │   └── StudentRegistrationCoursesRepository.java # Enrollment queries
│   │   │   └── service/
│   │   │       ├── AllocationService.java          # CORE: 3-Phase FCFS engine
│   │   │       └── SlotService.java                # Slot listing for UI
│   │   └── resources/
│   │       ├── application.properties              # DB, JPA, Hikari config
│   │       └── templates/
│   │           ├── slot-selection.html              # Slot selection page
│   │           └── result.html                     # Allocation result page
│   └── test/
│       └── java/com/uni/subjectallocation/
│           ├── ConcurrencyTest.java                # 1,000-thread stress test
│           ├── FunctionalTest.java                 # Edge-case validations
│           └── SubjectallocationApplicationTests.java # Boot context test
└── extras/                                         # Documentation & presentations
```

---

## 2. Architecture Overview

The system uses a standard **5-layer architecture** where each layer has a single responsibility.

| # | Layer | Package / File | Responsibility |
| :---: | :--- | :--- | :--- |
| 1 | **Presentation** | `templates/*.html` | Renders the UI using Thymeleaf |
| 2 | **Web** | `controller/` | HTTP routing, request parsing, redirects |
| 3 | **Service** | `service/` | **Core FCFS algorithm**, transaction management |
| 4 | **Data Access** | `repository/` | JPA queries, **pessimistic lock acquisition** |
| 5 | **Database** | PostgreSQL `ec2` schema | Row-level locks, FIFO thread queuing |

**Request Flow:**
```
Browser → HomeController (redirect) → RegistrationController → AllocationService → Repository → PostgreSQL
```

---

## 3. Entities — Data Models

All entities map to tables in the `ec2` PostgreSQL schema.

| Entity | Table | Purpose | Key Fields |
| :--- | :--- | :--- | :--- |
| `Course` | `ec2.courses` | Base course definitions | `crsid` (PK), `crsname`, `crscode` |
| `TermCourse` | `ec2.termcourses` | Links courses to slots and terms | `tcrid` (PK), `slot`, `termid`, `courseId` (FK) |
| `TermCourseAvailableFor` | `ec2.termcourseavailablefor` | **Tracks seat availability** — this is the row we lock | `tcaid` (PK), `tca_seats`, `tca_booked`, `tcatcrid` (FK) |
| `StudentRegistration` | `ec2.studentregistrations` | Student's master record for the term | `srgid` (PK), `srgstdid` |
| `StudentRegistrationCourses` | `ec2.studentregistrationcourses` | Final enrollment mapping | `srcid` (PK), `srcsrgid` (FK), `srctcrid` (FK) |

---

## 4. Repositories — Data Access Layer

| Repository | Key Method | SQL Generated | Purpose |
| :--- | :--- | :--- | :--- |
| `TermCourseAvailableForRepository` | `findTcaIdsBySlot(slot)` | `SELECT tcaid ... WHERE slot = ?` | Fetches IDs for Phase 0 (discovery) |
| | `findTcafByIdWithLock(tcaid)` | `SELECT ... FOR NO KEY UPDATE` | **Acquires pessimistic row lock** for Phase 1 |
| | `findDistinctSlots()` | `SELECT DISTINCT slot ...` | Populates the slot-selection UI |
| `StudentRegistrationRepository` | `findFirstBySrgstdidOrderBySrgidDesc(id)` | Standard JPA | Finds the student's latest registration |
| `StudentRegistrationCoursesRepository` | `countBySrcsrgid(srgid)` | `SELECT COUNT(*) ...` | Checks if student is already enrolled |
| `CourseRepository` | `findById(id)` | Standard JPA | Looks up course name for error messages |
| `TermCourseRepository` | `findAllById(ids)` | Standard JPA | Batch-fetches courses for UI display |

**Critical Code — The Locking Query:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT tcaf FROM TermCourseAvailableFor tcaf WHERE tcaf.tcaid = :tcaid")
TermCourseAvailableFor findTcafByIdWithLock(@Param("tcaid") Long tcaid);
```
This annotation tells Hibernate to append `FOR NO KEY UPDATE` to the SQL, causing PostgreSQL to **block** any other transaction trying to read this same row until the current transaction commits.

---

## 5. Services — Business Logic

| Service | Method | Description |
| :--- | :--- | :--- |
| `AllocationService` | `allocateSlot(studentId, slot)` | **The core FCFS engine.** Executes the 3-phase algorithm inside a `@Transactional` boundary. |
| | `getEnrolledSlot(studentId)` | Checks if a student already has an allocation and returns their slot number. |
| `SlotService` | `getAvailableCoursesBySlot()` | Read-only query that builds a `Map<String, List<CourseInfo>>` for the UI, grouping courses by slot. |

---

## 6. Controllers — Web & API Layer

| Controller | Type | Route | Method | Action |
| :--- | :--- | :--- | :--- | :--- |
| `HomeController` | `@Controller` | `GET /` | `index()` | Redirects to `/registration/slots` |
| `RegistrationController` | `@Controller` | `GET /registration/slots` | `showSlotSelection()` | Renders the slot-selection Thymeleaf page |
| | | `POST /registration/allocate` | `allocateSlot()` | Calls `AllocationService`, redirects to result page |
| | | `GET /registration/result` | `showResult()` | Renders the success/error result page |
| `SlotController` | `@RestController` | `GET /slots` | `getSlots()` | Returns all slots as JSON (REST API) |
| | | `POST /slots/allocate` | `allocateSlot()` | Accepts JSON body + `X-Student-Id` header (REST API) |

> The system exposes **two interfaces**: a Thymeleaf UI (`RegistrationController`) and a REST API (`SlotController`).

---

## 7. DTOs — Data Transfer Objects

| DTO | Fields | Usage |
| :--- | :--- | :--- |
| `CourseInfo` | `tcrid`, `courseName`, `courseCode` | Carries course display data to the frontend |
| `SlotDTO` | `slot`, `List<CourseInfo>` | Groups courses by slot (used in earlier iterations) |
| `SlotAllocationRequest` | `slot` | Request body for the REST API `POST /slots/allocate` |

---

## 8. Exceptions — Error Handling

| Exception | Thrown When | HTTP Status (REST) |
| :--- | :--- | :--- |
| `AlreadyEnrolledException` | Student already has courses registered | `409 Conflict` |
| `SlotFullException` | All seats in the slot are booked | `409 Conflict` |
| `SlotNotFoundException` | The requested slot ID doesn't exist | `404 Not Found` |

---

## 9. Configuration

### `application.properties` — Full Breakdown

| Property | Value | Why It Matters |
| :--- | :--- | :--- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/localBTP` | Points to the local PostgreSQL database |
| `spring.datasource.username` | `postgres` | Database login credentials |
| `spring.jpa.hibernate.ddl-auto` | `validate` | **Safety:** Refuses to start if schema doesn't match entities |
| `spring.jpa.properties.hibernate.default_schema` | `ec2` | All queries target the `ec2` schema |
| `spring.jpa.show-sql` | `true` | Logs every SQL query (useful for debugging locks) |
| `spring.jpa.open-in-view` | `false` | Prevents lazy-loading issues outside transactions |
| `spring.datasource.hikari.maximum-pool-size` | `20` | Limits concurrent DB connections to prevent overload |
| `server.port` | `8080` | Application runs on `http://localhost:8080` |

### `AppConstants.java`

| Constant | Value | Purpose |
| :--- | :--- | :--- |
| `MAX_SEATS` | `60` | Default fallback seat limit if `tca_seats` is null |

---

## 10. Frontend — Thymeleaf Templates

| Template | URL | Description |
| :--- | :--- | :--- |
| `slot-selection.html` | `/registration/slots` | Displays all available slots with their courses. Each slot has a "Register" button that submits a POST request. |
| `result.html` | `/registration/result` | Shows the outcome — either "Allocation Successful" (green) or an error message (red). |

---

## 11. The 3-Phase FCFS Algorithm

This is the heart of the project, implemented in `AllocationService.allocateSlot()`.

### Phase 0 — Wait-Free Discovery
| Step | Code | Purpose |
| :--- | :--- | :--- |
| Fetch target IDs | `findTcaIdsBySlot(slotId)` | Identifies which rows to lock |
| Find student record | `findFirstBySrgstdidOrderBySrgidDesc(studentId)` | Validates student existence |
| Duplicate check | `countBySrcsrgid(srgid) > 0` | Prevents double registration |

```java
List<Long> tcaIdsToLock = termCourseAvailableForRepository.findTcaIdsBySlot(slotId);
long existingRegistrations = studentRegistrationCoursesRepository.countBySrcsrgid(srgid);
if (existingRegistrations > 0) {
    throw new AlreadyEnrolledException();
}
```

### Phase 1 — Deterministic Locking (Growing Phase)
| Step | Code | Purpose |
| :--- | :--- | :--- |
| Sort IDs | `Collections.sort(tcaIdsToLock)` | **Prevents deadlocks** by enforcing global lock order |
| Acquire row locks | `findTcafByIdWithLock(tcaid)` | Serializes concurrent requests into a FIFO queue |
| Capacity check | `currentBooked >= maxSeats` | Rejects if full (while holding lock) |

```java
Collections.sort(tcaIdsToLock);

for (Long tcaid : tcaIdsToLock) {
    TermCourseAvailableFor lockedTca = termCourseAvailableForRepository.findTcafByIdWithLock(tcaid);
    if (currentBooked >= maxSeats) {
        throw new RuntimeException("Slot Full: No seats remaining for subject - " + courseName);
    }
}
```

### Phase 2 — Atomic Commitment (Shrinking Phase)
| Step | Code | Purpose |
| :--- | :--- | :--- |
| Increment booked | `lockedTca.setTcaBooked(currentBooked + 1)` | Reserves the seat |
| Save entity | `termCourseAvailableForRepository.save(lockedTca)` | Persists to DB |
| Create enrollment | `studentRegistrationCoursesRepository.save(enrollment)` | Links student to course |

```java
for (TermCourseAvailableFor lockedTca : lockedTcafs) {
    lockedTca.setTcaBooked(currentBooked + 1);
    termCourseAvailableForRepository.save(lockedTca);

    StudentRegistrationCourses enrollment = new StudentRegistrationCourses();
    enrollment.setSrcsrgid(srgid);
    enrollment.setSrctcrid(lockedTca.getTermCourse().getTcrid());
    studentRegistrationCoursesRepository.save(enrollment);
}
// @Transactional commits here → all locks released
```

---

## 12. Database Schema

All tables reside in the `ec2` PostgreSQL schema.

| Table | Primary Key | Foreign Keys | Critical Columns |
| :--- | :--- | :--- | :--- |
| `courses` | `crsid` | — | `crsname`, `crscode` |
| `termcourses` | `tcrid` | `courseId → courses.crsid` | `slot`, `termid` |
| `termcourseavailablefor` | `tcaid` | `tcatcrid → termcourses.tcrid` | `tca_seats`, `tca_booked` |
| `studentregistrations` | `srgid` | — | `srgstdid` |
| `studentregistrationcourses` | `srcid` | `srcsrgid → studentregistrations.srgid`, `srctcrid → termcourses.tcrid` | — |

> **Note:** The `termcourseavailablefor` table is the **contention point**. Every concurrent allocation request must lock rows in this table.

---

## 13. Testing Infrastructure

### `ConcurrencyTest.java` — Stress Test
| Parameter | Value |
| :--- | :--- |
| Threads | 1,000 |
| Mechanism | `ExecutorService` + `CountDownLatch` |
| Seats Available | 20 |
| Expected Successes | 20 |
| Expected Rejections | 980 |
| Expected Overbookings | 0 |

### `FunctionalTest.java` — Edge Cases
| Test Case | Validates |
| :--- | :--- |
| `testValidAllocation()` | A valid student can register successfully |
| `testSlotNotFound()` | Invalid slot ID throws `SlotNotFoundException` |
| `testAlreadyEnrolled()` | Duplicate registration throws `AlreadyEnrolledException` |
| `testStudentNotFound()` | Missing student throws `RuntimeException` |

---

## 14. How to Run

### Prerequisites
- Java 21 (JDK)
- PostgreSQL 16 (running on `localhost:5432`)
- Maven 3.9+

### Start the Application
```powershell
.\mvnw spring-boot:run
```
Then open: **http://localhost:8080**

### Run Tests
```powershell
.\mvnw test
```

---

*Document generated for MTech Minor Project Defense — DAU, 2025.*
