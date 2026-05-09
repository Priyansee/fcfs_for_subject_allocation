package com.uni.subjectallocation.service;

import com.uni.subjectallocation.config.AppConstants;
import com.uni.subjectallocation.entity.StudentRegistration;
import com.uni.subjectallocation.entity.StudentRegistrationCourses;
import com.uni.subjectallocation.exception.AlreadyEnrolledException;
import com.uni.subjectallocation.exception.SlotNotFoundException;
import com.uni.subjectallocation.repository.StudentRegistrationCoursesRepository;
import com.uni.subjectallocation.repository.StudentRegistrationRepository;
import com.uni.subjectallocation.repository.TermCourseAvailableForRepository;
import com.uni.subjectallocation.repository.TermCourseRepository;
import com.uni.subjectallocation.repository.CourseRepository;
import com.uni.subjectallocation.entity.Course;
import com.uni.subjectallocation.entity.TermCourse;
import com.uni.subjectallocation.entity.TermCourseAvailableFor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AllocationService {

    private final TermCourseAvailableForRepository termCourseAvailableForRepository;
    private final StudentRegistrationCoursesRepository studentRegistrationCoursesRepository;
    private final StudentRegistrationRepository studentRegistrationRepository;
    private final CourseRepository courseRepository;
    private final TermCourseRepository termCourseRepository;

    public AllocationService(TermCourseAvailableForRepository termCourseAvailableForRepository,
            StudentRegistrationCoursesRepository studentRegistrationCoursesRepository,
            StudentRegistrationRepository studentRegistrationRepository,
            CourseRepository courseRepository,
            TermCourseRepository termCourseRepository) {
        this.termCourseAvailableForRepository = termCourseAvailableForRepository;
        this.studentRegistrationCoursesRepository = studentRegistrationCoursesRepository;
        this.studentRegistrationRepository = studentRegistrationRepository;
        this.courseRepository = courseRepository;
        this.termCourseRepository = termCourseRepository;
    }

    public String getEnrolledSlot(Long studentId) {
        StudentRegistration registration = studentRegistrationRepository.findFirstBySrgstdidOrderBySrgidDesc(studentId);
        if (registration == null) {
            return null;
        }

        List<StudentRegistrationCourses> enrollments = studentRegistrationCoursesRepository.findBySrcsrgid(registration.getSrgid());
        if (enrollments.isEmpty()) {
            return null;
        }

        // Get the first enrollment and look up its slot
        Long tcrid = enrollments.get(0).getSrctcrid();
        TermCourse termCourse = (tcrid != null) ? termCourseRepository.findById(tcrid).orElse(null) : null;
        
        return (termCourse != null) ? String.valueOf(termCourse.getSlot()) : null;
    }

    @Transactional
    public String allocateSlot(Long studentId, String slot) {

        Long slotId = Long.valueOf(slot);
        
        // Fetch all available course offering IDs for this slot
        List<Long> tcaIdsToLock = termCourseAvailableForRepository.findTcaIdsBySlot(slotId);
        if (tcaIdsToLock.isEmpty()) {
            throw new SlotNotFoundException(slot);
        }

        // Fetch student registration
        StudentRegistration registration = studentRegistrationRepository.findFirstBySrgstdidOrderBySrgidDesc(studentId);
        if (registration == null) {
            throw new RuntimeException("Student registration record not found");
        }

        Long srgid = registration.getSrgid();

        // 1 Student gets only 1 slot: check if the student has already registered for any courses
        long existingRegistrations = studentRegistrationCoursesRepository.countBySrcsrgid(srgid);
        if (existingRegistrations > 0) {
            throw new AlreadyEnrolledException();
        }

        // Two-Phase Locking (2PL): Step 1 - sort to prevent deadlock
        Collections.sort(tcaIdsToLock);

        // Growing Phase: Acquire locks & Check Seats
        List<TermCourseAvailableFor> lockedTcafs = new ArrayList<>();
        for (Long tcaid : tcaIdsToLock) {
            TermCourseAvailableFor lockedTca = termCourseAvailableForRepository.findTcafByIdWithLock(tcaid);
            lockedTcafs.add(lockedTca);

            int currentBooked = lockedTca.getTcaBooked() != null ? lockedTca.getTcaBooked() : 0;
            int maxSeats = lockedTca.getTcaSeats() != null ? lockedTca.getTcaSeats() : AppConstants.MAX_SEATS;

            if (currentBooked >= maxSeats) {
                String courseName = "Unknown Course";
                if (lockedTca.getTermCourse() != null) {
                    Long courseId = lockedTca.getTermCourse().getCourseId();
                    if (courseId != null) {
                        Course course = courseRepository.findById(courseId).orElse(null);
                        if (course != null) {
                            courseName = course.getCrsname();
                        }
                    }
                }
                throw new RuntimeException("Slot Full: No seats remaining for subject - " + courseName);
            }
        }

        // Modify Phase & Insert combinations (Atomic / Part of Transaction)
        for (TermCourseAvailableFor lockedTca : lockedTcafs) {
            // "decrease by one in the database" -> available seats decrease by incrementing the booked count
            int currentBooked = lockedTca.getTcaBooked() != null ? lockedTca.getTcaBooked() : 0;
            lockedTca.setTcaBooked(currentBooked + 1);
            termCourseAvailableForRepository.save(lockedTca); // This persists the seat decrease

            StudentRegistrationCourses enrollment = new StudentRegistrationCourses();
            enrollment.setSrcsrgid(srgid);
            enrollment.setSrctcrid(lockedTca.getTermCourse().getTcrid());
            studentRegistrationCoursesRepository.save(enrollment);
        }

        return "Allocation Successful";
    }

}
