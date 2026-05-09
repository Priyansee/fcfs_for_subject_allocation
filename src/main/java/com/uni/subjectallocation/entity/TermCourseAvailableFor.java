// package com.uni.subjectallocation.entity;

// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;

// @Entity
// @Table(name = "termcourseavailablefor")
// public class TermCourseAvailableFor {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "tcaid")
//     private Integer tcaid;

//     @Column(name = "tcrid")
//     private Integer tcrid;

//     @Column(name = "slot")
//     private String slot;

//     public TermCourseAvailableFor() {
//     }

//     public Integer getTcaid() {
//         return tcaid;
//     }

//     public void setTcaid(Integer tcaid) {
//         this.tcaid = tcaid;
//     }

//     public Integer getTcrid() {
//         return tcrid;
//     }

//     public void setTcrid(Integer tcrid) {
//         this.tcrid = tcrid;
//     }

//     public String getSlot() {
//         return slot;
//     }

//     public void setSlot(String slot) {
//         this.slot = slot;
//     }
// }
package com.uni.subjectallocation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "termcourseavailablefor", schema = "ec2")
public class TermCourseAvailableFor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tcaid")
    private Long tcaid;

    // Here is your Foreign Key connection!
    @ManyToOne
    @JoinColumn(name = "tcatcrid", referencedColumnName = "tcrid")
    private TermCourse termCourse;

    @Column(name = "tcabchid")
    private Long batchId;

    @Column(name = "tca_seats")
    private Integer tcaSeats;

    @Column(name = "tca_booked")
    private Integer tcaBooked;

    public TermCourseAvailableFor() {
    }

    // --- Getters and Setters ---
    public Long getTcaid() {
        return tcaid;
    }

    public void setTcaid(Long tcaid) {
        this.tcaid = tcaid;
    }

    public TermCourse getTermCourse() {
        return termCourse;
    }

    public void setTermCourse(TermCourse termCourse) {
        this.termCourse = termCourse;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getTcaSeats() {
        return tcaSeats;
    }

    public void setTcaSeats(Integer tcaSeats) {
        this.tcaSeats = tcaSeats;
    }

    public Integer getTcaBooked() {
        return tcaBooked;
    }

    public void setTcaBooked(Integer tcaBooked) {
        this.tcaBooked = tcaBooked;
    }
}