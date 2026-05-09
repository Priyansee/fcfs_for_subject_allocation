// package com.uni.subjectallocation.repository;

// import com.uni.subjectallocation.entity.TermCourseAvailableFor;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository;

// import java.util.List;

// @Repository
// public interface TermCourseAvailableForRepository extends JpaRepository<TermCourseAvailableFor, Integer> {

//     @Query("SELECT tcaf.tcrid FROM TermCourseAvailableFor tcaf WHERE tcaf.slot = :slot ORDER BY tcaf.tcrid")
//     List<Integer> findCourseIdsBySlot(@Param("slot") String slot);

//     @Query("SELECT DISTINCT tcaf.slot FROM TermCourseAvailableFor tcaf ORDER BY tcaf.slot")
//     List<String> findDistinctSlots();

// }

package com.uni.subjectallocation.repository;

import com.uni.subjectallocation.entity.TermCourseAvailableFor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TermCourseAvailableForRepository extends JpaRepository<TermCourseAvailableFor, Long> { // Changed
                                                                                                        // Integer to
                                                                                                        // Long

    // 1. Join with TermCourse to get the distinct slots
    @Query("SELECT DISTINCT t.slot FROM TermCourseAvailableFor tcaf JOIN tcaf.termCourse t WHERE t.slot IS NOT NULL AND t.termid = 78 ORDER BY t.slot")
    List<Long> findDistinctSlots();

    // 2. Join with TermCourse to filter by slot
    @Query("SELECT t.tcrid FROM TermCourseAvailableFor tcaf JOIN tcaf.termCourse t WHERE t.slot = :slot AND t.termid = 78 ORDER BY t.tcrid")
    List<Long> findCourseIdsBySlot(@Param("slot") Long slot);

    @Query("SELECT tcaf FROM TermCourseAvailableFor tcaf JOIN tcaf.termCourse t WHERE t.slot = :slot AND t.termid = 78")
    List<TermCourseAvailableFor> findTcafBySlot(@Param("slot") Long slot);

    @Query("SELECT tcaf.tcaid FROM TermCourseAvailableFor tcaf JOIN tcaf.termCourse t WHERE t.slot = :slot AND t.termid = 78")
    List<Long> findTcaIdsBySlot(@Param("slot") Long slot);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tcaf FROM TermCourseAvailableFor tcaf WHERE tcaf.tcaid = :tcaid")
    TermCourseAvailableFor findTcafByIdWithLock(@Param("tcaid") Long tcaid);

    // 3. The Concurrency Lock (from our previous step)
    @Modifying
    @Transactional
    @Query(value = "UPDATE ec2.termcourseavailablefor SET tca_booked = tca_booked + 1 WHERE tcaid = :tcaid AND tca_booked < tca_seats", nativeQuery = true)
    int attemptToLockSeat(@Param("tcaid") Long tcaid);
}