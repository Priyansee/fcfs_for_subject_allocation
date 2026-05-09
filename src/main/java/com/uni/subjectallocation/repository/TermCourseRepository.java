package com.uni.subjectallocation.repository;

import com.uni.subjectallocation.entity.TermCourse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TermCourseRepository extends JpaRepository<TermCourse, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TermCourse tc WHERE tc.tcrid = :tcrid")
    TermCourse findByIdWithLock(@Param("tcrid") Long tcrid);

    List<TermCourse> findByTcridIn(List<Long> tcrIds);

}
