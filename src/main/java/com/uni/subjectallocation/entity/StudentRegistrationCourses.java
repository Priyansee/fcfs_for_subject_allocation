package com.uni.subjectallocation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "studentregistrationcourses", schema = "ec2")
public class StudentRegistrationCourses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "srcid")
    private Long srcid;

    // The current semester registration ID (std.id + str.id)
    @Column(name = "srcsrgid")
    private Long srcsrgid;

    // The Course ID they selected
    @Column(name = "srctcrid")
    private Long srctcrid;

    // You can add other columns here like srcstatus, srccreatedat, etc.

    public StudentRegistrationCourses() {
    }

    public Long getSrcid() {
        return srcid;
    }

    public void setSrcid(Long srcid) {
        this.srcid = srcid;
    }

    public Long getSrcsrgid() {
        return srcsrgid;
    }

    public void setSrcsrgid(Long srcsrgid) {
        this.srcsrgid = srcsrgid;
    }

    public Long getSrctcrid() {
        return srctcrid;
    }

    public void setSrctcrid(Long srctcrid) {
        this.srctcrid = srctcrid;
    }

    @Column(name = "srctype")
    private String srctype = "M";

    @Column(name = "srcstatus")
    private String srcstatus = "ACTIVE";

    @Column(name = "srccreatedat")
    private java.time.LocalDateTime srccreatedat = java.time.LocalDateTime.now();

    @Column(name = "srcrowstate")
    private Long srcrowstate = 1L;

    public String getSrctype() {
        return srctype;
    }

    public void setSrctype(String srctype) {
        this.srctype = srctype;
    }

    public String getSrcstatus() {
        return srcstatus;
    }

    public void setSrcstatus(String srcstatus) {
        this.srcstatus = srcstatus;
    }

    public java.time.LocalDateTime getSrccreatedat() {
        return srccreatedat;
    }

    public void setSrccreatedat(java.time.LocalDateTime srccreatedat) {
        this.srccreatedat = srccreatedat;
    }

    public Long getSrcrowstate() {
        return srcrowstate;
    }

    public void setSrcrowstate(Long srcrowstate) {
        this.srcrowstate = srcrowstate;
    }
}