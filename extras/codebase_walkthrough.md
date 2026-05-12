# FCFS Subject Allocation: Codebase Documentation

## 1. Project Architecture Overview
The system is built using a **layered architecture** to ensure separation of concerns and high-concurrency safety.

| Layer | Component | Primary Responsibility |
| :--- | :--- | :--- |
| **Presentation** | Thymeleaf / HTML5 | Captures student input and displays allocation status. |
| **Web** | Spring MVC Controllers | Handles request routing and basic input validation. |
| **Service** | Spring Service (@Service) | **Core Engine:** Implements the 3-Phase FCFS logic. |
| **Data Access** | Spring Data JPA | Manages persistence and handles **Pessimistic Locking**. |
| **Database** | PostgreSQL 16 | The source of truth; serializes requests via row locks. |

---

## 2. File Inventory & Responsibilities

### **A. Domain Entities (`com.uni.subjectallocation.entity`)**
| File | Role | Key Fields / Data |
| :--- | :--- | :--- |
| `Course.java` | Subject definition | `crsid`, `crsname` |
| `TermCourse.java` | Slot Mapping | `tcrid`, `tcrslot`, `tcrcrsid` |
| `TermCourseAvailableFor.java` | **Lock Target** | `tcaid`, `tca_seats`, `tca_booked` |
| `StudentRegistration.java` | Student Master | `srgid`, `srgstdid` |
| `StudentRegistrationCourses.java` | Enrollment Map | `srcid`, `srctcrid`, `srcsrgid` |

### **B. Service & Business Logic (`com.uni.subjectallocation.service`)**
| File | Method | logic |
| :--- | :--- | :--- |
| `AllocationService.java` | `allocateSlot()` | Implements FCFS using sorted pessimistic locking. |

### **C. Controllers & Routing (`com.uni.subjectallocation.controller`)**
| File | Route | Description |
| :--- | :--- | :--- |
| `RegistrationController.java` | `/register` | Captures student ID and slot; triggers allocation. |

---

## 3. The 3-Phase FCFS Algorithm Logic

The `AllocationService` enforces FCFS through three distinct phases within a single `@Transactional` boundary.

### **Phase 0: Wait-Free Validation**
- **Action:** Fetch all `tcaid`s for the requested slot.
- **Action:** Check if the student exists and is not already enrolled.
- **Benefit:** Fails early without consuming database locks.

### **Phase 1: Deterministic Locking**
- **Action:** Sort all target `tcaid`s numerically.
- **Action:** Execute `SELECT ... FOR NO KEY UPDATE` for each ID.
- **Benefit:** Prevents deadlocks (sorted order) and serializes users (FIFO).

### **Phase 2: Atomic Commitment**
- **Action:** Final capacity verification (`seats > booked`).
- **Action:** Increment booked count and save the mapping record.
- **Benefit:** Guaranteed atomicity; if one subject is full, the whole slot allocation is rolled back.

---

## 4. Database Schema Details (`ec2` schema)

| Table Name | Primary Key | Foreign Key | Critical Column |
| :--- | :--- | :--- | :--- |
| `courses` | `crsid` | - | `crsname` |
| `termcourses` | `tcrid` | `tcrcrsid` | `tcrslot` |
| `termcourseavailablefor` | `tcaid` | `tcatcrid` | `tca_booked` |
| `studentregistrations` | `srgid` | - | `srgstdid` |
| `studentregistrationcourses` | `srcid` | `srctcrid` | - |

---

## 5. System Configuration (`application.properties`)

| Property | Value | Rationale |
| :--- | :--- | :--- |
| `hibernate.ddl-auto` | `validate` | Prevents runtime schema corruption. |
| `hikari.maximum-pool-size`| `20` | Optimizes DB connection pressure under load. |
| `hibernate.dialect` | `PostgreSQLDialect` | Enables PostgreSQL-specific locking syntax. |

---

## 6. Execution & Testing
To run the system locally:
1. Ensure PostgreSQL is active.
2. Run `.\mvnw spring-boot:run`.
3. Verify concurrency using `ConcurrencyTest.java` (simulates 1,000 threads).
