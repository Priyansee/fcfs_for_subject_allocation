package com.uni.subjectallocation.repository;

import com.uni.subjectallocation.entity.StudentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRegistrationRepository extends JpaRepository<StudentRegistration, Long> {

    StudentRegistration findFirstBySrgstdidOrderBySrgidDesc(Long studentId);
}
