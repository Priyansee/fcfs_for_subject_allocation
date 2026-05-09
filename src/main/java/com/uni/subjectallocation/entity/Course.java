package com.uni.subjectallocation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses", schema = "ec2")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crsid")
    private Long crsid;

    @Column(name = "crsname")
    private String crsname;

    @Column(name = "crscode")
    private String crscode;

    public Course() {
    }

    public Long getCrsid() {
        return crsid;
    }

    public void setCrsid(Long crsid) {
        this.crsid = crsid;
    }

    public String getCrsname() {
        return crsname;
    }

    public void setCrsname(String crsname) {
        this.crsname = crsname;
    }

    public String getCrscode() {
        return crscode;
    }

    public void setCrscode(String crscode) {
        this.crscode = crscode;

    }
}
