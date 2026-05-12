# Codebase Walkthrough: FCFS Subject Allocation System

This document provides a detailed breakdown of the project's structure, the purpose of each file, and the core logic that ensures high-concurrency safety.

---

## 1. Project Overview & Tech Stack
This is a **Spring Boot** application designed to handle high-concurrency subject allocation.
- **Backend:** Java 21, Spring Boot 3.x
- **Database:** PostgreSQL 16
- **ORM:** Hibernate 6 (JPA)
- **Frontend:** Thymeleaf (HTML5)
- **Concurrency Strategy:** **Pessimistic Row-Level Locking** (`SELECT FOR UPDATE`) with **Global Lock Ordering** (to prevent deadlocks).

---

## 2. Directory Structure & Key Files

### **A. Core Domain Logic (Entities)**
*Location: `src/main/java/com/uni/subjectallocation/entity/`*

These files define the database schema and object relationships.

1.  **`Course.java`**: Represents a base academic course (e.g., "Advanced Java").
2.  **`TermCourse.java`**: Links a `Course` to a specific **Slot** (e.g., Slot 1) and a **Term**.
3.  **`TermCourseAvailableFor.java`**: The **Contention Point**. It stores:
    - `tca_seats`: Maximum capacity.
    - `tca_booked`: Current enrollment count.
    - *Logic:* This is the row we lock during registration.
4.  **`StudentRegistration.java`**: Links a student to a specific term (the "Master" record for a student's semester).
5.  **`StudentRegistrationCourses.java`**: The final "Transaction" record. If a student is allocated a course, a row is inserted here.

---

### **B. Data Access Layer (Repositories)**
*Location: `src/main/java/com/uni/subjectallocation/repository/`*

These interfaces handle all database communication.

1.  **`TermCourseAvailableForRepository.java`**: 
    - `findTcafByIdWithLock(Long tcaid)`: Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
    - *Why?* This forces PostgreSQL to queue every other transaction trying to access the same row until the current one commits.
2.  **`StudentRegistrationCoursesRepository.java`**: Used to check if a student is already registered (Phase 0 check).

---

### **C. The Engine (Service Layer)**
*Location: `src/main/java/com/uni/subjectallocation/service/`*

1.  **`AllocationService.java`**: The "Heart" of the project. It implements the **3-Phase FCFS Algorithm**.

#### **The 3-Phase Logic in `allocateSlot()`:**
| Phase | Action | Purpose |
| :--- | :--- | :--- |
| **Phase 0: Discovery** | Fetch IDs and Validate student eligibility. | Quick, wait-free check. If student is already enrolled, we fail early without locking anything. |
| **Phase 1: Locking** | `Collections.sort(tcaIds)` and then `findTcafByIdWithLock()`. | **Sort IDs** to prevent AB-BA deadlocks. Then, acquire locks to serialize concurrent users. |
| **Phase 2: Commitment** | Check `seats > booked`, increment `booked`, and insert registration record. | Atomic final check and update. If something fails, the whole transaction rolls back. |

---

### **D. Web Layer (Controllers)**
*Location: `src/main/java/com/uni/subjectallocation/controller/`*

1.  **`RegistrationController.java`**: 
    - Handles GET requests to show the slot selection page.
    - Handles POST requests to `/register`. It captures the `studentId` and `slot` and passes them to the `AllocationService`.

---

## 3. High-Concurrency Testing
*Location: `src/test/java/com/uni/subjectallocation/`*

1.  **`ConcurrencyTest.java`**: 
    - Uses `ExecutorService` to spawn **1,000 parallel threads**.
    - Uses `CountDownLatch` to make them all hit the database at the exact same moment.
    - *Goal:* Verify that if 20 seats are available, exactly 20 students succeed and 980 fail (No Overbooking).
2.  **`FunctionalTest.java`**:
    - Verifies business rules (e.g., "Can a student register twice?", "What if the slot ID is wrong?").

---

## 4. Key Configurations
*Location: `src/main/resources/`*

1.  **`application.properties`**:
    - `spring.datasource.*`: Database connection details.
    - `spring.jpa.hibernate.ddl-auto=validate`: Ensures the app only runs if the DB schema matches the code perfectly.
    - `hikari.maximum-pool-size=20`: Limits the DB connection pool to prevent overloading PostgreSQL.

---

## 5. Sequence of a Single Registration
1. **User** clicks "Register" in `slot-selection.html`.
2. **Controller** receives the POST request.
3. **Service** starts a `@Transactional` session.
4. **Service** finds which courses are in that slot.
5. **Service** sorts those course IDs and locks the rows in the DB.
6. **Service** checks seat availability while holding the locks.
7. **Service** updates the seat count and saves the student mapping.
8. **Transaction Commits**: Locks are released, and the user sees "Success".
