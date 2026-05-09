// package com.uni.subjectallocation.entity;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;

// @Entity
// @Table(name = "termcourses")
// public class TermCourse {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "tcrid")
//     private Integer tcrid;

//     @Column(name = "course_name")
//     private String courseName;

//     @Column(name = "course_code")
//     private String courseCode;

//     public TermCourse() {
//     }

//     public Integer getTcrid() {
//         return tcrid;
//     }

//     public void setTcrid(Integer tcrid) {
//         this.tcrid = tcrid;
//     }

//     public String getCourseName() {
//         return courseName;
//     }

//     public void setCourseName(String courseName) {
//         this.courseName = courseName;
//     }

//     public String getCourseCode() {
//         return courseCode;
//     }

//     public void setCourseCode(String courseCode) {
//         this.courseCode = courseCode;
//     }
// }
package com.uni.subjectallocation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "termcourses", schema = "ec2")
public class TermCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tcrid")
    private Long tcrid;

    // This is the FK to your Courses table for the subject details
    @Column(name = "tcrcrsid")
    private Long courseId;

    @Column(name = "tcrslot")
    private Long slot;

    @Column(name = "tcrtrmid")
    private Long termid;

    public TermCourse() {
    }

    // --- Getters and Setters ---
    public Long getTcrid() {
        return tcrid;
    }

    public void setTcrid(Long tcrid) {
        this.tcrid = tcrid;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getSlot() {
        return slot;
    }

    public void setSlot(Long slot) {
        this.slot = slot;
    }

    public Long getTermid() {
        return termid;
    }

    public void setTermid(Long termid) {
        this.termid = termid;
    }
}