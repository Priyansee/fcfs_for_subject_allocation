package com.uni.subjectallocation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "studentregistrations")
public class StudentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "srgid")
    private Long srgid;

    @Column(name = "srgstdid")
    private Long srgstdid;

    public StudentRegistration() {
    }

    public Long getSrgid() {
        return srgid;
    }

    public void setSrgid(Long srgid) {
        this.srgid = srgid;
    }

    public Long getSrgstdid() {
        return srgstdid;
    }

    public void setSrgstdid(Long srgstdid) {
        this.srgstdid = srgstdid;
    }
}
