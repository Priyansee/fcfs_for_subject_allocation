# Concurrency-Safe FCFS Slot Allocation — Implementation Plan

## Overview

Design a First Come First Serve (FCFS) slot-based course allocation system using Spring Boot and PostgreSQL.
Students pick **one slot** → all courses in that slot are atomically allocated or none are.

---

## 1. Configuration

### `application.properties`
- Set datasource URL, username, password for PostgreSQL
- Set `spring.jpa.properties.hibernate.dialect` → `PostgreSQLDialect`
- Disable `spring.jpa.open-in-view` (set to `false`) — prevents lazy loading outside transactions
- Set `spring.datasource.hikari.maximum-pool-size` to handle concurrent requests

### `MAX_SEATS` Constant
- Define as a static final integer in a dedicated `AppConstants` class (e.g., `MAX_SEATS = 60`)
- Used in the service layer for seat comparison — not stored in DB

---

## 2. Repository Layer

### `TermCourseRepository` (extends `JpaRepository`)
- **`findById(tcrid)`** — standard lookup
- **`findByIdWithLock(tcrid)`** — custom query:
  ```
  SELECT * FROM termcourses WHERE tcrid = :tcrid FOR UPDATE
  ```
  Annotated with `@Lock(LockModeType.PESSIMISTIC_WRITE)` or via `@Query` with native SQL
  > **This is where row-level locking occurs**

### `TermCourseAvailableForRepository`
- **`findCourseIdsBySlot(slot)`** — query:
  ```
  SELECT tcrid FROM termcourseavailablefor WHERE slot = :slot
  ```
  Returns a list of `tcrid` values for the selected slot

### `StudentRegistrationCoursesRepository`
- **`countBySrctcrid(tcrid)`** — query:
  ```
  SELECT COUNT(*) FROM studentregistrationcourses WHERE srctcrid = :tcrid
  ```
  Returns current enrollment count for a course
- **`save(entity)`** — inserts one enrollment row per course

### `StudentRegistrationRepository`
- **`findBySrgstdid(studentId)`** — fetch the student's registration record (`srgid`)
- Used to populate `srcsrgid` when inserting into `studentregistrationcourses`

---

## 3. Service Layer

### `SlotService`

#### Method: `getAvailableSlots()`
- Query all distinct slots from `termcourseavailablefor`
- For each slot, fetch associated `tcrid` list and join with `termcourses` for course metadata
- Return a list of `SlotDTO` objects (slot → list of course info)
- **No locking here** — read-only, display only

---

### `AllocationService`

#### Method: `allocateSlot(studentId, slot)` — **Core Transactional Method**

> **Transaction boundary starts here** (`@Transactional`)

**Step 1 — Fetch course IDs for slot**
- Call `findCourseIdsBySlot(slot)` → get list of `tcrid` values
- If empty → throw `SlotNotFoundException`

**Step 2 — Sort course IDs (Deadlock Prevention)**
- Sort `tcrid` list in **ascending order** before locking
- Ensures all concurrent transactions acquire locks in the same order → eliminates circular wait

**Step 3 — Lock each course row (2PL Phase 1: Growing Phase)**
- For each sorted `tcrid`:
  - Call `findByIdWithLock(tcrid)` → issues `SELECT ... FOR UPDATE`
  - PostgreSQL holds the row lock until the transaction commits or rolls back
- **All locks acquired before any seat check**

**Step 4 — Check seat availability**
- For each locked `tcrid`:
  - Call `countBySrctcrid(tcrid)` → get current enrollment
  - If `count >= MAX_SEATS` → **immediately rollback** and return `"Slot Full"`
- Atomic check: all courses must pass; one failure aborts everything

**Step 5 — Fetch student registration record**
- Call `findBySrgstdid(studentId)` → get `srgid`
- Required to build `studentregistrationcourses` rows

**Step 6 — Insert enrollments**
- For each `tcrid` in the slot:
  - Create a `StudentRegistrationCourses` entity:
    - `srcsrgid` = student's `srgid`
    - `srctcrid` = `tcrid`
  - Call `save(entity)`
- All inserts happen within the **same open transaction**

**Step 7 — Commit**
- Transaction commits → all locks released (2PL Phase 2: Shrinking Phase)
- All inserts become visible atomically

> **Transaction boundary ends here**

#### Rollback Behavior
- Any exception (checked or unchecked) triggers `@Transactional` rollback
- Explicit rollback via `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` for business rule failures (slot full)
- No partial state is ever committed

---

## 4. Controller Layer

### `SlotController`

| Endpoint | Method | Description |
|---|---|---|
| `GET /slots` | `getSlots()` | Returns all available slots with their courses |
| `POST /slots/allocate` | `allocateSlot(@RequestBody SlotAllocationRequest)` | Triggers slot allocation for logged-in student |

#### `GET /slots`
- Calls `SlotService.getAvailableSlots()`
- Returns `List<SlotDTO>` as JSON
- No authentication enforcement here (assume handled by security filter)

#### `POST /slots/allocate`
- Accepts `{ "slot": "SLOT_1" }` in request body + student ID from session/JWT
- Calls `AllocationService.allocateSlot(studentId, slot)`
- Returns:
  - `200 OK` with `"Allocation Successful"` on success
  - `409 Conflict` with `"Slot Full"` if any course is at capacity
  - `404 Not Found` if slot doesn't exist
  - `500 Internal Server Error` for unexpected failures

---

## 5. Concurrency Summary

| Concern | Strategy |
|---|---|
| **Atomicity** | Single `@Transactional` method — all inserts or none |
| **Row-level locking** | `SELECT ... FOR UPDATE` on each `termcourses` row |
| **Deadlock prevention** | Sort `tcrid` list ascending before acquiring locks |
| **Two-Phase Locking (2PL)** | Growing phase: all locks acquired → Shrinking phase: all released at commit |
| **Seat count accuracy** | Count queried **after** lock acquired, within same transaction |
| **Partial allocation** | Impossible — all inserts in one transaction, rollback on any failure |
| **Isolation level** | PostgreSQL default `READ COMMITTED`; row locks provide stronger guarantee for locked rows |
| **Concurrent same slot** | Second transaction blocks on `FOR UPDATE` until first commits; then re-counts, may see `Slot Full` |

---

## Verification Plan

### Unit Tests
- `AllocationService` with mocked repositories: test full path, slot-full rollback, missing slot
- Assert no inserts happen when seat check fails

### Integration Tests
- Use `@SpringBootTest` + embedded or real PostgreSQL
- Simulate 2 concurrent threads both trying to allocate the same full slot
- Assert exactly `MAX_SEATS` rows in `studentregistrationcourses`; one thread gets `"Slot Full"`

### Manual / API Tests
- Use Postman or curl: `POST /slots/allocate` with concurrent requests via Collection Runner
- Verify no over-enrollment in DB after load test
