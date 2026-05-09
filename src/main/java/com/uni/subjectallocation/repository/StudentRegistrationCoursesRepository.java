package com.uni.subjectallocation.repository;

import com.uni.subjectallocation.entity.StudentRegistrationCourses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRegistrationCoursesRepository extends JpaRepository<StudentRegistrationCourses, Long> {

    @Query("SELECT COUNT(src) FROM StudentRegistrationCourses src WHERE src.srctcrid = :tcrid")
    long countBySrctcrid(@Param("tcrid") Long tcrid);

    @Query("SELECT COUNT(src) FROM StudentRegistrationCourses src WHERE src.srcsrgid = :srgid")
    long countBySrcsrgid(@Param("srgid") Long srgid);

    @Query("SELECT COUNT(src) FROM StudentRegistrationCourses src WHERE src.srcsrgid = :srgid AND src.srctcrid IN :tcrids")
    long countBySrcsrgidAndSrctcridIn(@Param("srgid") Long srgid, @Param("tcrids") List<Long> tcrids);

    List<StudentRegistrationCourses> findBySrcsrgid(Long srcsrgid);

    @org.springframework.transaction.annotation.Transactional
    void deleteBySrcsrgid(Long srcsrgid);
}
