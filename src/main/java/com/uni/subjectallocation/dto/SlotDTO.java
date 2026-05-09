package com.uni.subjectallocation.dto;

import java.util.List;

public class SlotDTO {

    private String slot;
    private List<CourseInfo> courses;

    public SlotDTO() {
    }

    public SlotDTO(String slot, List<CourseInfo> courses) {
        this.slot = slot;
        this.courses = courses;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public List<CourseInfo> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseInfo> courses) {
        this.courses = courses;
    }
}
