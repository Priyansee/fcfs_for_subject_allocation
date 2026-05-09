package com.uni.subjectallocation.dto;

public class CourseInfo {

    private Long tcrid;
    private String courseName;
    private String courseCode;

    public CourseInfo() {
    }

    public CourseInfo(Long tcrid, String courseName, String courseCode) {
        this.tcrid = tcrid;
        this.courseName = courseName;
        this.courseCode = courseCode;
    }

    public Long getTcrid() {
        return tcrid;
    }

    public void setTcrid(Long tcrid) {
        this.tcrid = tcrid;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
}
