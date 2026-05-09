// package com.uni.subjectallocation.service;

// import com.uni.subjectallocation.dto.CourseInfo;
// import com.uni.subjectallocation.dto.SlotDTO;
// import com.uni.subjectallocation.entity.TermCourse;
// import com.uni.subjectallocation.repository.TermCourseAvailableForRepository;
// import com.uni.subjectallocation.repository.TermCourseRepository;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.ArrayList;
// import java.util.List;

// @Service
// public class SlotService {

//     private final TermCourseAvailableForRepository termCourseAvailableForRepository;
//     private final TermCourseRepository termCourseRepository;

//     public SlotService(TermCourseAvailableForRepository termCourseAvailableForRepository,
//             TermCourseRepository termCourseRepository) {
//         this.termCourseAvailableForRepository = termCourseAvailableForRepository;
//         this.termCourseRepository = termCourseRepository;
//     }

//     @Transactional(readOnly = true)
//     public List<SlotDTO> getAvailableSlots() {

//         List<String> distinctSlots = termCourseAvailableForRepository.findDistinctSlots();
//         List<SlotDTO> slotDTOs = new ArrayList<>();

//         for (String slot : distinctSlots) {

//             List<Integer> courseIds = termCourseAvailableForRepository.findCourseIdsBySlot(slot);

//             // ✅ Optimized fetch
//             List<TermCourse> courses = termCourseRepository.findByTcridIn(courseIds);

//             List<CourseInfo> courseInfos = new ArrayList<>();

//             for (TermCourse course : courses) {
//                 courseInfos.add(new CourseInfo(
//                         course.getTcrid(),
//                         course.getCourseName(),
//                         course.getCourseCode()));
//             }

//             slotDTOs.add(new SlotDTO(slot, courseInfos));
//         }

//         return slotDTOs;
//     }
// }

package com.uni.subjectallocation.service;

import com.uni.subjectallocation.dto.CourseInfo;
import com.uni.subjectallocation.entity.TermCourse;
import com.uni.subjectallocation.repository.TermCourseAvailableForRepository;
import com.uni.subjectallocation.repository.TermCourseRepository;
import com.uni.subjectallocation.repository.CourseRepository;
import com.uni.subjectallocation.entity.Course;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SlotService {

    private final TermCourseAvailableForRepository termCourseAvailableForRepository;
    private final TermCourseRepository termCourseRepository;
    private final CourseRepository courseRepository;

    public SlotService(TermCourseAvailableForRepository termCourseAvailableForRepository,
            TermCourseRepository termCourseRepository,
            CourseRepository courseRepository) {
        this.termCourseAvailableForRepository = termCourseAvailableForRepository;
        this.termCourseRepository = termCourseRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    // Returns the exact Map you requested for Thymeleaf!
    public Map<String, List<CourseInfo>> getAvailableCoursesBySlot() {

        List<Long> distinctSlots = termCourseAvailableForRepository.findDistinctSlots();
        Map<String, List<CourseInfo>> slotMap = new LinkedHashMap<>(); // Maintains slot order

        for (Long slotLong : distinctSlots) {
            String slotStr = String.valueOf(slotLong);
            List<Long> courseIds = termCourseAvailableForRepository.findCourseIdsBySlot(slotLong);
            
            if (courseIds == null || courseIds.isEmpty()) {
                slotMap.put(slotStr, new ArrayList<>());
                continue;
            }

            List<TermCourse> courses = termCourseRepository.findAllById(courseIds);

            List<CourseInfo> courseInfos = new ArrayList<>();
            for (TermCourse course : courses) {
                Long courseId = course.getCourseId();
                Course courseDetails = (courseId != null) ? courseRepository.findById(courseId).orElse(null) : null;
                
                courseInfos.add(new CourseInfo(
                        course.getTcrid(),
                        courseDetails != null ? courseDetails.getCrsname() : "N/A",
                        courseDetails != null ? courseDetails.getCrscode() : "N/A"));
            }
            slotMap.put(slotStr, courseInfos);
        }
        return slotMap;
    }
}